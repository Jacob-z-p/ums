package com.jacobzp.ums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:apiresult;MODE=MySQL;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password="
})
@Sql("/user-test-schema.sql")
class ApiResultIntegrationTest {
    private static final String REGISTER = "/api/user/register";
    private static final String LOGIN = "/api/user/login";
    private static final String CURRENT = "/api/user/current";
    private static final String LOGOUT = "/api/user/logout";
    private static final String USERS = "/api/admin/users";

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = new JsonMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void registrationAndAuthenticationUseResultAndKeepSessionBehavior() throws Exception {
        var credentials = credentials("Alice123", "password123");
        var registered = assertResult(post(REGISTER, credentials, null), 201, 0);
        long id = registered.at("/data/id").asLong();
        assertTrue(id > 0);
        assertResult(post(REGISTER, credentials, null), 409, 2002);
        assertResult(post(REGISTER, "{malformed", null), 400, 1001);
        assertResult(post(LOGIN, null, null), 400, 1001);
        assertResult(post(LOGIN, credentials("Alice123", "short"), null), 400, 1001);

        var wrongPassword = assertResult(post(LOGIN, credentials("Alice123", "wrongpass"), null), 401, 2001);
        var missingAccount = assertResult(post(LOGIN, credentials("Unknown1", "wrongpass"), null), 401, 2001);
        assertEquals(wrongPassword, missingAccount);
        assertResult(send("GET", CURRENT, null, null), 401, 2003);

        var login = post(LOGIN, credentials, null);
        var loginResult = assertResult(login, 200, 0);
        assertEquals(id, loginResult.at("/data/id").asLong());
        assertEquals("Alice123", loginResult.at("/data/account").asString());
        assertFalse(login.body().contains("password"));
        assertEquals("no-store", login.headers().firstValue("Cache-Control").orElseThrow());
        String cookie = cookie(login);
        var current = send("GET", CURRENT, null, cookie);
        assertEquals(loginResult.path("data"), assertResult(current, 200, 0).path("data"));
        assertEquals("no-store", current.headers().firstValue("Cache-Control").orElseThrow());

        assertTrue(assertResult(post(LOGOUT, null, cookie), 200, 0).path("data").isNull());
        assertResult(send("GET", CURRENT, null, cookie), 401, 2003);
        assertTrue(assertResult(post(LOGOUT, null, cookie), 200, 0).path("data").isNull());
    }

    @Test
    void adminResponsesDistinguishAuthenticationPermissionAndDeletion() throws Exception {
        long adminId = assertResult(post(REGISTER, credentials("Admin1", "password123"), null), 201, 0)
                .at("/data/id").asLong();
        long userId = assertResult(post(REGISTER, credentials("User1", "password123"), null), 201, 0)
                .at("/data/id").asLong();
        jdbc.update("UPDATE `user` SET role = 1 WHERE id = ?", adminId);
        String adminCookie = login("Admin1");
        String userCookie = login("User1");

        assertResult(send("GET", USERS, null, null), 401, 2003);
        assertResult(send("GET", USERS, null, userCookie), 403, 3001);
        assertResult(send("GET", USERS + "?page=word", null, adminCookie), 400, 1001);
        var page = assertResult(send("GET", USERS + "?account=user1&size=1", null, adminCookie), 200, 0)
                .path("data");
        assertEquals(1, page.path("total").asInt());
        assertEquals(1, page.path("page").asInt());
        assertEquals(1, page.path("size").asInt());
        assertEquals(userId, page.at("/items/0/id").asLong());
        assertFalse(page.at("/items/0").has("passwordHash"));

        assertResult(send("DELETE", USERS + "/invalid", null, adminCookie), 400, 1001);
        assertResult(send("DELETE", USERS + "/" + adminId, null, adminCookie), 403, 3002);
        var deleted = assertResult(send("DELETE", USERS + "/" + userId, null, adminCookie), 200, 0);
        assertTrue(deleted.path("data").isNull());
        assertResult(send("DELETE", USERS + "/" + userId, null, adminCookie), 404, 3003);
        assertResult(send("GET", CURRENT, null, userCookie), 401, 2003);
    }

    @Test
    void unsupportedMethodReturnsResultAndPreservesAllowHeader() throws Exception {
        var response = send("GET", REGISTER, null, null);
        assertResult(response, 405, 1001);
        assertEquals("POST", response.headers().firstValue("Allow").orElseThrow());
    }

    @Test
    void openApiDescribesTheResponseEnvelopeAndItsData() throws Exception {
        var response = send("GET", "/v3/api-docs", null, null);
        assertEquals(200, response.statusCode());
        var spec = json.readTree(response.body());
        var paths = spec.path("paths");
        assertResponseSchema(spec, paths.path(REGISTER).path("post"), "201", "RegistrationVO");
        assertResponseSchema(spec, paths.path(LOGIN).path("post"), "200", "LoginVO");
        assertResponseSchema(spec, paths.path(CURRENT).path("get"), "200", "LoginVO");
        assertResponseSchema(spec, paths.path(USERS).path("get"), "200", "AdminUserPageVO");
        assertResponseSchema(spec, paths.path(LOGOUT).path("post"), "200", null);
        assertResponseSchema(spec, paths.path(USERS + "/{id}").path("delete"), "200", null);
        assertResponseSchema(spec, paths.path(REGISTER).path("post"), "409", null);
    }

    private void assertResponseSchema(JsonNode spec, JsonNode operation, String status, String dataSchema) {
        var content = operation.path("responses").path(status).path("content");
        assertFalse(content.isEmpty(), "Missing OpenAPI response content for HTTP " + status);
        var schema = content.iterator().next().path("schema");
        if (schema.has("$ref")) {
            schema = spec.at(schema.path("$ref").asString().substring(1));
        }
        var properties = schema.path("properties");
        for (String name : List.of("code", "data", "message", "description")) {
            assertTrue(properties.has(name), name + " missing from " + schema);
        }
        if (dataSchema != null) {
            assertTrue(properties.path("data").toString().contains("#/components/schemas/" + dataSchema),
                    properties.path("data").toString());
        }
    }

    private JsonNode assertResult(HttpResponse<String> response, int status, int code) {
        assertEquals(status, response.statusCode(), response.body());
        var body = json.readTree(response.body());
        assertEquals(4, body.size(), response.body());
        assertEquals(code, body.path("code").asInt(-1), response.body());
        assertTrue(body.has("data"), response.body());
        assertFalse(body.path("message").asString().isBlank());
        assertTrue(body.path("description").isString());
        if (code == 0) {
            assertEquals("", body.path("description").asString());
        } else {
            assertTrue(body.path("data").isNull());
            assertFalse(body.path("description").asString().isBlank());
        }
        return body;
    }

    private String login(String account) throws Exception {
        var response = post(LOGIN, credentials(account, "password123"), null);
        assertResult(response, 200, 0);
        return cookie(response);
    }

    private String cookie(HttpResponse<String> response) {
        return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
    }

    private String credentials(String account, String password) {
        return json.createObjectNode().put("account", account).put("password", password).toString();
    }

    private HttpResponse<String> post(String path, String body, String cookie) throws Exception {
        return send("POST", path, body, cookie);
    }

    private HttpResponse<String> send(String method, String path, String body, String cookie) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return client.send(request.method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
