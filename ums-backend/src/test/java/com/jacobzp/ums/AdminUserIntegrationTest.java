package com.jacobzp.ums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:adminusers;MODE=MySQL;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password="
})
@Sql(value = "/user-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class AdminUserIntegrationTest {
    private static final String USERS = "/api/admin/users";
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = new JsonMapper();

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clearUsers() {
        jdbc.update("DELETE FROM `user`");
    }

    @Test
    void requiresAnActiveAdministratorOnEveryRequest() throws Exception {
        long admin = register("Admin1");
        long ordinary = register("Alice123");
        jdbc.update("UPDATE `user` SET role = 1 WHERE id = ?", admin);
        String ordinaryCookie = login("Alice123");
        String adminCookie = login("Admin1");

        assertEquals(401, getUsers("", null).statusCode());
        assertEquals(401, deleteUser(ordinary, null).statusCode());
        assertEquals(403, getUsers("", ordinaryCookie).statusCode());
        assertEquals(403, deleteUser(ordinary, ordinaryCookie).statusCode());
        assertEquals(200, getUsers("", adminCookie).statusCode());

        jdbc.update("UPDATE `user` SET role = 0 WHERE id = ?", admin);
        assertEquals(403, getUsers("", adminCookie).statusCode());
        jdbc.update("UPDATE `user` SET role = 1, status = 1 WHERE id = ?", admin);
        assertEquals(401, deleteUser(ordinary, adminCookie).statusCode());
        jdbc.update("UPDATE `user` SET status = 0, is_deleted = 1 WHERE id = ?", admin);
        assertEquals(401, getUsers("", adminCookie).statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT is_deleted FROM `user` WHERE id = ?", Integer.class, ordinary));
    }

    @Test
    void listsOnlyVisibleUsersWithStablePaginationAndExactAccountMatching() throws Exception {
        long admin = register("Admin1");
        long ordinary = register("Alice123");
        long disabled = register("Alice1234");
        long deleted = register("Deleted1");
        jdbc.update("UPDATE `user` SET role = 1 WHERE id = ?", admin);
        jdbc.update("UPDATE `user` SET status = 1 WHERE id = ?", disabled);
        jdbc.update("UPDATE `user` SET is_deleted = 1 WHERE id = ?", deleted);
        String cookie = login("Admin1");

        var all = getUsers("", cookie);
        assertEquals(200, all.statusCode());
        var allBody = json.readTree(all.body());
        assertEquals(1, allBody.path("page").asInt());
        assertEquals(20, allBody.path("size").asInt());
        assertEquals(3, allBody.path("total").asLong());
        assertEquals(3, allBody.path("items").size());

        var firstPage = json.readTree(getUsers("?size=1", cookie).body());
        assertEquals(1, firstPage.path("items").size());
        assertEquals(disabled, firstPage.at("/items/0/id").asLong());
        assertEquals(1, firstPage.at("/items/0/status").asInt());
        assertEquals(3, firstPage.path("total").asLong());
        var secondPage = json.readTree(getUsers("?page=2&size=1", cookie).body());
        assertEquals(ordinary, secondPage.at("/items/0/id").asLong());
        assertEquals(2, secondPage.path("page").asInt());
        var beyondLastPage = json.readTree(getUsers("?page=4&size=1", cookie).body());
        assertEquals(0, beyondLastPage.path("items").size());
        assertEquals(3, beyondLastPage.path("total").asLong());

        var exact = json.readTree(getUsers("?account=alice123", cookie).body());
        assertEquals(1, exact.path("total").asLong());
        var user = exact.at("/items/0");
        assertEquals(ordinary, user.path("id").asLong());
        assertEquals("Alice123", user.path("account").asText());
        for (String field : List.of("id", "account", "nickname", "avatarUrl", "role", "status", "createdAt", "updatedAt")) {
            assertTrue(user.has(field), field);
        }
        assertEquals(8, user.size());
        assertFalse(all.body().contains("password"));
        for (String missing : List.of("Alice", "Deleted1", "Missing")) {
            var result = json.readTree(getUsers("?account=" + missing, cookie).body());
            assertEquals(0, result.path("items").size(), missing);
            assertEquals(0, result.path("total").asLong(), missing);
        }
    }

    @Test
    void rejectsInvalidParameters() throws Exception {
        long admin = register("Admin1");
        jdbc.update("UPDATE `user` SET role = 1 WHERE id = ?", admin);
        String cookie = login("Admin1");

        for (String query : List.of("?page=0", "?page=-1", "?page=word", "?page=", "?size=0", "?size=101",
                "?size=999999999999", "?account=", "?account=invalid-user", "?account=" + "a".repeat(65))) {
            assertEquals(400, getUsers(query, cookie).statusCode(), query);
        }
        for (String id : List.of("0", "-1", "word", "9223372036854775808")) {
            assertEquals(400, send("DELETE", USERS + "/" + id, null, cookie).statusCode(), id);
        }
    }

    @Test
    void logicallyDeletesOrdinaryUsersAndProtectsAdministrators() throws Exception {
        long admin = register("Admin1");
        long otherAdmin = register("Admin2");
        long ordinary = register("Alice123");
        jdbc.update("UPDATE `user` SET role = 1 WHERE id IN (?, ?)", admin, otherAdmin);
        String cookie = login("Admin1");

        assertEquals(403, deleteUser(admin, cookie).statusCode());
        assertEquals(403, deleteUser(otherAdmin, cookie).statusCode());
        var deleted = deleteUser(ordinary, cookie);
        assertEquals(204, deleted.statusCode());
        assertTrue(deleted.body().isEmpty());
        assertEquals(1, jdbc.queryForObject("SELECT is_deleted FROM `user` WHERE id = ?", Integer.class, ordinary));
        assertEquals(404, deleteUser(ordinary, cookie).statusCode());
        assertEquals(404, deleteUser(Long.MAX_VALUE, cookie).statusCode());
        assertEquals(0, json.readTree(getUsers("?account=Alice123", cookie).body()).path("items").size());
        assertEquals(401, credentials("login", "Alice123").statusCode());
        assertEquals(409, credentials("register", "alice123").statusCode());
    }

    private long register(String account) throws Exception {
        var response = credentials("register", account);
        assertEquals(201, response.statusCode(), response.body());
        return json.readTree(response.body()).path("id").asLong();
    }

    private String login(String account) throws Exception {
        var response = credentials("login", account);
        assertEquals(200, response.statusCode(), response.body());
        return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
    }

    private HttpResponse<String> credentials(String action, String account) throws Exception {
        return send("POST", "/api/user/" + action, "{\"account\":\"" + account + "\",\"password\":\"password123\"}", null);
    }

    private HttpResponse<String> getUsers(String query, String cookie) throws Exception {
        return send("GET", USERS + query, null, cookie);
    }

    private HttpResponse<String> deleteUser(long id, String cookie) throws Exception {
        return send("DELETE", USERS + "/" + id, null, cookie);
    }

    private HttpResponse<String> send(String method, String path, String body, String cookie) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        return client.send(request.method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
