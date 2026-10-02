package com.bit.recall.ai;

import com.bit.recall.ai.client.OpenApiClient;
import com.bit.recall.ai.client.OpenApiRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.BlockingHttpClient;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import static io.micronaut.runtime.Micronaut.build;

@Singleton
@Slf4j
@RequiredArgsConstructor
public class OpenAIService implements AIService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String OPENAPI_MODEL = "gpt-4o-mini";
    private static final String SYSTEM = "system";
    private static final String USER = "user";

    private final OpenApiClient openApiClient;
    private final ObjectMapper objectMapper;

    @Override
    public String chat(String prompt) {
        return null;
    }

    @Override
    public <T> T generateStructuredOutput(String apiKey, String systemPrompt, String userPrompt, Class<T> responseType) {
        log.info("Executing OpenAI Structured Output call... for class: {}", responseType.getSimpleName());
        try {
            var openApiRequest = OpenApiRequest.builder()
                    .model(OPENAPI_MODEL)
                    .messages(List.of(
                            OpenApiRequest.Message.builder().role(SYSTEM).content(systemPrompt).build(),
                            OpenApiRequest.Message.builder().role(USER).content(userPrompt).build()
                    ))
                    .build();
            var openApiResponse = openApiClient.generateCompletion(BEARER_PREFIX + apiKey, openApiRequest);
            var jsonContent = openApiResponse.choices().get(0).message().content();
            return objectMapper.readValue(jsonContent, responseType);
        } catch (Exception e) {
            log.error("Error executing OpenAI request", e);
            throw new RuntimeException("OpenAI call failed", e);
        }
    }

    @Override
    public AIProvider getProvider() {
        return AIProvider.OPENAPI; // Assuming OpenAI corresponds to OPENAPI
    }
}
