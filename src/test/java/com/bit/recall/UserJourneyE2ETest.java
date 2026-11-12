package com.bit.recall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.testcontainers.containers.PostgreSQLContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserJourneyE2ETest implements TestPropertyProvider {

    private static final WireMockServer OPENAI = new WireMockServer(options().dynamicPort());
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Override
    public Map<String, String> getProperties() {
        if (!OPENAI.isRunning()) {
            OPENAI.start();
        }
        if (!POSTGRES.isRunning()) {
            try {
                POSTGRES.start();
            } catch (RuntimeException startupFailure) {
                throw new IllegalStateException(
                        "Could not start the PostgreSQL Testcontainer. Verify that the CI Docker daemon is available and can pull postgres:16-alpine.",
                        startupFailure);
            }
        }
        return Map.of(
                "bit-recall.ai.openai.base-url", OPENAI.baseUrl() + "/v1",
                "micronaut.security.token.jwt.secret", "user-journey-test-jwt-secret-which-is-long-enough-123456",
                "datasources.default.url", POSTGRES.getJdbcUrl(),
                "datasources.default.username", POSTGRES.getUsername(),
                "datasources.default.password", POSTGRES.getPassword(),
                "datasources.default.driver-class-name", "org.postgresql.Driver",
                "datasources.default.dialect", "POSTGRES",
                "jpa.default.properties.hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect",
                "jpa.default.properties.hibernate.hbm2ddl.auto", "create-drop"
        );
    }

    @AfterAll
    static void stopOpenAiMock() {
        if (OPENAI.isRunning()) {
            OPENAI.stop();
        }
        // Keep PostgreSQL alive until the Micronaut context closes: Hibernate's
        // create-drop schema cleanup runs while that context is being disposed.
        // Testcontainers' resource reaper removes the container when the test JVM exits.
    }

    @BeforeEach
    void stubOpenAiCompletion() {
        OPENAI.resetAll();
        OPENAI.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .willReturn(okJson("""
                        {
                          "id": "chatcmpl-wiremock",
                          "object": "chat.completion",
                          "created": 1730000000,
                          "model": "gpt-4o-mini",
                          "choices": [{
                            "index": 0,
                            "message": {
                              "role": "assistant",
                              "content": "{\\\"values\\\":[{\\\"title\\\":\\\"Photosynthesis\\\",\\\"type\\\":\\\"concept\\\",\\\"explanation\\\":\\\"Plants convert light into chemical energy.\\\",\\\"example\\\":\\\"A leaf uses sunlight.\\\",\\\"code\\\":null,\\\"pattern\\\":null,\\\"recall\\\":\\\"What does photosynthesis convert?\\\"}]}"
                            },
                            "finish_reason": "stop"
                          }],
                          "usage": {"prompt_tokens": 20, "completion_tokens": 30, "total_tokens": 50}
                        }
                        """)));
    }

    @Test
    void userCanSignUpLoginCreateTopicAndContentAndRetrieveGeneratedRecallCard() throws Exception {
        String username = "journey-" + UUID.randomUUID();
        String password = "Secure-test-password-42";
        String email = username + "@example.com";

        JsonNode signup = postJson("/users/signup", """
                {"username":"%s","password":"%s","email":"%s"}
                """.formatted(username, password, email), null);
        assertEquals(username, signup.path("username").asText());
        assertEquals(email, signup.path("email").asText());

        JsonNode login = postJson("/login", """
                {"username":"%s","password":"%s"}
                """.formatted(username, password), null);
        String accessToken = login.path("access_token").asText();
        assertFalse(accessToken.isBlank(), "login should return a JWT access token");

        HttpResponse<String> aiConfigResponse = httpClient.toBlocking().exchange(
                authenticatedPost("/api/1/ai/configure", """
                        {"apiKey":"wiremock-test-key","provider":"OPENAI"}
                        """, accessToken), String.class);
        assertEquals(200, aiConfigResponse.code());

        JsonNode topic = postJson("/api/1/topics", """
                {"title":"Photosynthesis","description":"Plant biology notes"}
                """, accessToken);
        String topicId = topic.path("topicId_id").asText();
        assertFalse(topicId.isBlank(), "topic creation should return its id");

        JsonNode content = postJson("/api/1/topics/%s/contents".formatted(topicId), """
                {"content_text":"Photosynthesis converts light energy into chemical energy in plants.","revision_depth":"BALANCED"}
                """, accessToken);
        String contentId = content.path("id").asText();
        assertFalse(contentId.isBlank(), "content creation should return its id");

        JsonNode cards = awaitRecallCard(topicId, accessToken);
        assertEquals(1, cards.path("data").size());
        JsonNode card = cards.path("data").get(0);
        assertEquals(contentId, card.path("contentId").asText());
        assertEquals("Photosynthesis", card.path("title").asText());
        assertEquals("What does photosynthesis convert?", card.path("question").asText());
        assertEquals("Plants convert light into chemical energy.", card.path("summaryBody").asText());

        OPENAI.verify(1, postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer wiremock-test-key")));
    }

    private JsonNode postJson(String path, String body, String token) throws Exception {
        HttpResponse<String> response = httpClient.toBlocking().exchange(
                authenticatedPost(path, body, token), String.class);
        assertTrue(response.code() >= 200 && response.code() < 300,
                () -> "Expected successful response for " + path + ", got " + response.code() + ": " + response.body());
        return JSON.readTree(response.body());
    }

    private MutableHttpRequest<String> authenticatedPost(String path, String body, String token) {
        MutableHttpRequest<String> request = HttpRequest.POST(path, body)
                .contentType("application/json")
                .accept("application/json");
        return token == null ? request : request.header("Authorization", "Bearer " + token);
    }

    private JsonNode awaitRecallCard(String topicId, String token) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        String path = "/api/1/recall-cards/" + topicId;
        JsonNode latest = null;
        while (Instant.now().isBefore(deadline)) {
            HttpResponse<String> response = httpClient.toBlocking().exchange(
                    HttpRequest.GET(path).header("Authorization", "Bearer " + token)
                            .accept("application/json"), String.class);
            assertEquals(200, response.code(), "recall cards endpoint should return success");
            latest = JSON.readTree(response.body());
            if (latest.path("data").size() > 0) {
                return latest;
            }
            Thread.sleep(100);
        }
        fail("Timed out waiting for async recall-card generation; last response: " + latest);
        return latest;
    }
}
