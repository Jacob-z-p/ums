package com.jacobzp.ums;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiIntegrationTest {
    @LocalServerPort private int port;
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void servesKnife4jUiAndDocumentedEndpoints() throws Exception {
        var page = get("/doc.html");
        assertEquals(200, page.statusCode());
        var asset = Pattern.compile("src=[\"']?([^\"' >]+\\.js)").matcher(page.body());
        assertTrue(asset.find(), "Knife4j page should reference its JavaScript");
        String assetPath = asset.group(1);
        assertEquals(200, get(assetPath.startsWith("/") ? assetPath : "/" + assetPath).statusCode());
        assertEquals(200, get("/v3/api-docs/swagger-config").statusCode());
        var docs = get("/v3/api-docs");
        assertEquals(200, docs.statusCode());
        for (String expected : new String[] {"/api/user/register", "/api/user/login", "RegistrationRequest",
                "RegistrationResponse", "LoginRequest", "LoginResponse", "ErrorResponse", "\"401\"", "\"409\""}) {
            assertTrue(docs.body().contains(expected), expected);
        }
        assertFalse(docs.body().contains("passwordHash"));
    }
}
