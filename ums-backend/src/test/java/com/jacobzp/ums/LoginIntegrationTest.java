package com.jacobzp.ums;

import static org.junit.jupiter.api.Assertions.*;

import com.jacobzp.ums.security.SessionAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:logintest;MODE=MySQL;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password="
})
@Import(LoginIntegrationTest.SessionTestConfiguration.class)
@Sql(value = "/user-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class LoginIntegrationTest {
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    private final HttpClient client = HttpClient.newHttpClient();

    @TestConfiguration(proxyBeanMethods = false)
    static class SessionTestConfiguration {
        @Bean SessionProbe sessionProbe() { return new SessionProbe(); }
    }

    @RestController
    static class SessionProbe {
        @GetMapping("/test/session/create")
        String create(HttpServletRequest request) {
            request.getSession(true).setAttribute("marker", "original");
            return "created";
        }

        @GetMapping("/test/session")
        Map<String, String> inspect(HttpServletRequest request) {
            var session = request.getSession(false);
            if (session == null) return Map.of("exists", "false");
            return Map.of("exists", "true", "userId", String.valueOf(session.getAttribute(SessionAttributes.USER_ID)),
                    "marker", String.valueOf(session.getAttribute("marker")));
        }
    }

    @BeforeEach
    void clearUsers() { jdbc.update("DELETE FROM `user`"); }

    private HttpResponse<String> send(String method, String path, String body, String cookie) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (cookie != null) builder.header("Cookie", cookie);
        if ("POST".equals(method)) builder.header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        else builder.GET();
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> login(String account, String password, String cookie) throws Exception {
        return send("POST", "/api/user/login", "{\"account\":\"" + account + "\",\"password\":\"" + password + "\"}", cookie);
    }

    private String cookie(HttpResponse<String> response) {
        return response.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
    }

    private void register() throws Exception {
        assertEquals(201, send("POST", "/api/user/register", "{\"account\":\"Alice123\",\"password\":\"password123\"}", null).statusCode());
    }

    @Test
    void loginReturnsSafeUserAndReplacesOldSession() throws Exception {
        register();
        long id = jdbc.queryForObject("SELECT id FROM `user` WHERE account = ?", Long.class, "Alice123");
        String oldCookie = cookie(send("GET", "/test/session/create", null, null));
        var response = login("alice123", "password123", oldCookie);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"id\":" + id));
        assertTrue(response.body().contains("\"account\":\"Alice123\""));
        assertTrue(response.body().contains("\"nickname\":\"Alice123\""));
        assertTrue(response.body().contains("\"avatarUrl\":null"));
        assertTrue(response.body().contains("\"role\":0"));
        assertFalse(response.body().contains("password"));
        String newCookie = cookie(response);
        assertNotEquals(oldCookie, newCookie);
        String newSession = send("GET", "/test/session", null, newCookie).body();
        assertTrue(newSession.contains("\"userId\":\"" + id + "\""));
        assertTrue(newSession.contains("\"marker\":\"null\""));
        assertTrue(send("GET", "/test/session", null, oldCookie).body().contains("\"exists\":\"false\""));
    }

    @Test
    void rejectsCredentialsWithoutChangingSession() throws Exception {
        register();
        String oldCookie = cookie(send("GET", "/test/session/create", null, null));
        for (String account : List.of("Alice123", "Missing")) {
            var response = login(account, "wrongpass", oldCookie);
            assertEquals(401, response.statusCode());
            assertTrue(response.headers().allValues("set-cookie").isEmpty());
        }
        assertTrue(send("GET", "/test/session", null, oldCookie).body().contains("\"marker\":\"original\""));
        var withoutSession = login("Missing", "wrongpass", null);
        assertEquals(401, withoutSession.statusCode());
        assertTrue(withoutSession.headers().allValues("set-cookie").isEmpty());
        jdbc.update("UPDATE `user` SET status = 1 WHERE account = ?", "Alice123");
        assertEquals(401, login("Alice123", "password123", null).statusCode());
        jdbc.update("UPDATE `user` SET status = 0, is_deleted = 1 WHERE account = ?", "Alice123");
        assertEquals(401, login("Alice123", "password123", null).statusCode());
    }

    @Test
    void rejectsInvalidInputWithoutCreatingSession() throws Exception {
        for (String body : List.of("{}", "{\"account\":null,\"password\":\"12345678\"}",
                "{\"account\":\"bad-name\",\"password\":\"12345678\"}",
                "{\"account\":\"" + "a".repeat(65) + "\",\"password\":\"12345678\"}",
                "{\"account\":\"Alice\",\"password\":null}", "{\"account\":\"Alice\",\"password\":\"1234567\"}")) {
            var response = send("POST", "/api/user/login", body, null);
            assertEquals(400, response.statusCode(), body);
            assertTrue(response.headers().allValues("set-cookie").isEmpty());
        }
    }
}
