package com.bit.recall.ai;

import com.bit.recall.ai.client.OpenApiClient;
import com.bit.recall.ai.client.OpenApiRequest;
import com.bit.recall.domain.model.RecallCardDto;
import com.bit.recall.domain.model.RecallCardDtoWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
            if (responseType == RecallCardDtoWrapper.class) {
                var recallCards = objectMapper.readValue(jsonContent, new TypeReference<List<RecallCardDto>>() {});
                return responseType.cast(new RecallCardDtoWrapper(recallCards));
            }
            return objectMapper.readValue(jsonContent, responseType);
        } catch (Exception e) {
            log.error("Error executing OpenAI request", e);
            throw new RuntimeException("OpenAI call failed", e);
        }
    }

    @Override
    public AIProvider getProvider() {
        return AIProvider.OPENAI; // Assuming OpenAI corresponds to OPENAI
    }
}
