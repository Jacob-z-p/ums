package com.jacobzp.ums;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:registration;MODE=MySQL;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password="
})
@Sql(value = "/user-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class RegistrationIntegrationTest {
    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void clearUsers() {
        jdbc.update("DELETE FROM `user`");
    }

    private HttpResponse<String> register(String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/user/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void registersWithHashedPasswordAndDefaults() throws Exception {
        var response = register("{\"account\":\"Alice123\",\"password\":\"password123\"}");
        assertEquals(201, response.statusCode());
        var user = jdbc.queryForMap("SELECT * FROM `user` WHERE account = ?", "Alice123");
        assertEquals("{\"id\":" + user.get("ID") + "}", response.body());
        assertEquals("Alice123", user.get("NICKNAME"));
        assertEquals(0, ((Number) user.get("ROLE")).intValue());
        assertEquals(0, ((Number) user.get("STATUS")).intValue());
        assertEquals(0, ((Number) user.get("IS_DELETED")).intValue());
        String hash = (String) user.get("PASSWORD_HASH");
        assertNotEquals("password123", hash);
        assertTrue(Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8().matches("password123", hash));
        assertFalse(response.body().contains("password"));
    }

    @Test
    void rejectsInvalidInputsWithoutInserting() throws Exception {
        for (String body : List.of("{}", "{\"account\":null,\"password\":\"12345678\"}",
                "{\"account\":\"\",\"password\":\"12345678\"}",
                "{\"account\":\"bad-name\",\"password\":\"12345678\"}",
                "{\"account\":\"" + "a".repeat(65) + "\",\"password\":\"12345678\"}",
                "{\"account\":\"alice\",\"password\":null}",
                "{\"account\":\"alice\",\"password\":\"1234567\"}")) {
            assertEquals(400, register(body).statusCode(), body);
        }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class));
    }

    @Test
    void duplicateAndDeletedAccountsRemainReservedIgnoringCase() throws Exception {
        assertEquals(201, register("{\"account\":\"Alice\",\"password\":\"12345678\"}").statusCode());
        assertEquals(409, register("{\"account\":\"alice\",\"password\":\"12345678\"}").statusCode());
        jdbc.update("UPDATE `user` SET is_deleted = 1 WHERE account = ?", "Alice");
        assertEquals(409, register("{\"account\":\"Alice\",\"password\":\"12345678\"}").statusCode());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class));
    }

    @Test
    void concurrentRegistrationsHaveOneWinner() throws Exception {
        String body = "{\"account\":\"racer\",\"password\":\"12345678\"}";
        var tasks = List.of(CompletableFuture.supplyAsync(() -> sendUnchecked(body)),
                CompletableFuture.supplyAsync(() -> sendUnchecked(body)));
        List<Integer> statuses = tasks.stream().map(CompletableFuture::join).sorted().toList();
        assertEquals(List.of(201, 409), statuses);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class));
    }

    private int sendUnchecked(String body) {
        try {
            return register(body).statusCode();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
