package com.jacobzp.ums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

@EnabledIfEnvironmentVariable(named = "UMS_TEST_MYSQL", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RegistrationMysqlIntegrationTest {
    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void mysqlReservesCaseInsensitiveAndDeletedAccount() throws Exception {
        String account = "test" + UUID.randomUUID().toString().replace("-", "");
        boolean created = false;
        try {
            assertEquals(201, register(account));
            created = true;
            assertEquals(409, register(account.toUpperCase()));
            assertEquals(200, login(account.toUpperCase()));
            jdbc.update("UPDATE `user` SET is_deleted = 1 WHERE account = ?", account);
            assertEquals(409, register(account));
            assertEquals(401, login(account));
        } finally {
            if (created) {
                jdbc.update("DELETE FROM `user` WHERE account = ?", account);
            }
        }
    }

    private int register(String account) throws Exception {
        return post("/api/user/register", account);
    }

    private int login(String account) throws Exception {
        return post("/api/user/login", account);
    }

    private int post(String path, String account) throws Exception {
        String body = "{\"account\":\"" + account + "\",\"password\":\"12345678\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
    }
}
