package com.community.reporting;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:context_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "app.jwt.secret=test-only-signing-key-for-isolated-tests-123456789"
})
class InfrastructureReportingApplicationTests {
    @Autowired
    Environment environment;

    private HttpResponse<String> request(String method, String path, String json) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + environment.getProperty("local.server.port") + path))
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(json))
            .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void registrationAndLoginBothReturnTokensWithoutAuthentication() throws Exception {
        var registration = request("POST", "/api/auth/register",
            "{\"name\":\"Login Test\",\"email\":\"login-test@example.com\",\"password\":\"test-password-123\"}");
        assertEquals(201, registration.statusCode(), registration.body());
        assertTrue(registration.body().contains("\"token\""));
        var login = request("POST", "/api/auth/login",
            "{\"email\":\"login-test@example.com\",\"password\":\"test-password-123\"}");
        assertEquals(200, login.statusCode(), login.body());
        assertTrue(login.body().contains("\"token\""));
        var invalid = request("POST", "/api/auth/login",
            "{\"email\":\"login-test@example.com\",\"password\":\"wrong-password\"}");
        assertEquals(401, invalid.statusCode(), invalid.body());
    }

    @Test
    void wrongLoginMethodReturnsMethodNotAllowed() throws Exception {
        var response = request("PUT", "/api/auth/login", "{}");
        assertEquals(405, response.statusCode(), response.body());
    }

    @Test
    void protectedRequestsStillRequireAuthentication() throws Exception {
        assertEquals(403, request("POST", "/api/incidents", "{}").statusCode());
        assertEquals(403, request("GET", "/error", "").statusCode());
    }

    @Test
    void malformedLoginJsonReturnsBadRequest() throws Exception {
        var response = request("POST", "/api/auth/login", "{invalid-json");
        assertEquals(400, response.statusCode(), response.body());
    }
}

