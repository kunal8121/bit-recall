package com.bit.recall.ai;

import com.bit.recall.ai.client.OpenApiClient;
import com.bit.recall.ai.client.OpenApiRequest;
import com.bit.recall.ai.client.OpenApiResponse;
import com.bit.recall.domain.model.RecallCardDto;
import com.bit.recall.domain.model.RecallCardDtoWrapper;
import com.bit.recall.exception.BitRecallErrorCode;
import com.bit.recall.exception.BitRecallException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
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
                    .responseFormat(OpenApiRequest.ResponseFormat.builder().type("JSON").build())
                    .build();
            var openApiResponse = openApiClient.generateCompletion(BEARER_PREFIX + apiKey, openApiRequest);
            var jsonContent = extractJsonContent(openApiResponse);
            if (responseType == RecallCardDtoWrapper.class) {
                List<RecallCardDto> recallCards = objectMapper.readValue(jsonContent, new TypeReference<List<RecallCardDto>>() {});
                if (recallCards == null) {
                    throw new BitRecallException(BitRecallErrorCode.STRUCTURED_OUTPUT_ERROR, "OpenAI returned null recall cards");
                }
                return responseType.cast(new RecallCardDtoWrapper(recallCards));
            }
            T output = objectMapper.readValue(jsonContent, responseType);
            if (output == null) {
                throw new BitRecallException(BitRecallErrorCode.STRUCTURED_OUTPUT_ERROR,
                        "OpenAI returned JSON null instead of " + responseType.getSimpleName());
            }
            return output;
        } catch (BitRecallException e) {
            log.error("Invalid OpenAI response for class {}: {}", responseType.getSimpleName(), e.getMessage(), e);
            throw e;
        } catch (JsonProcessingException e) {
            log.error("OpenAI returned malformed or truncated JSON for class {}", responseType.getSimpleName(), e);
            throw new BitRecallException(BitRecallErrorCode.STRUCTURED_OUTPUT_ERROR,
                    "OpenAI returned malformed or truncated JSON", e);
        } catch (HttpClientResponseException e) {
            log.error("OpenAI provider returned HTTP {} for response class {}", e.getStatus().getCode(),
                    responseType.getSimpleName(), e);
            throw new BitRecallException(BitRecallErrorCode.AI_SERVICE_ERROR,
                    "OpenAI provider returned HTTP " + e.getStatus().getCode(), e);
        } catch (Exception e) {
            log.error("OpenAI request failed for response class {}", responseType.getSimpleName(), e);
            throw new BitRecallException(BitRecallErrorCode.AI_SERVICE_ERROR, "OpenAI request failed", e);
        }
    }

    private String extractJsonContent(OpenApiResponse response) {
        if (response == null) {
            throw new BitRecallException(BitRecallErrorCode.STRUCTURED_OUTPUT_ERROR, "OpenAI returned an empty response");
        }
        if (response.error() != null) {
            throw new BitRecallException(BitRecallErrorCode.AI_SERVICE_ERROR,
                    "OpenAI returned an error: " + response.error().message());
        }
        if (response.choices() == null || response.choices().isEmpty()) {
            throw new BitRecallException(BitRecallErrorCode.STRUCTURED_OUTPUT_ERROR, "OpenAI response contained no choices");
        }
        var choice = response.choices().get(0);
        if (choice == null || choice.message() == null) {
            throw new BitRecallException(BitRecallErrorCode.STRUCTURED_OUTPUT_ERROR, "OpenAI response contained no message");
        }
        if ("length".equalsIgnoreCase(choice.finishReason())) {
            throw new BitRecallException(BitRecallErrorCode.STRUCTURED_OUTPUT_ERROR,
                    "OpenAI response was truncated because it reached the output token limit");
        }
        if (choice.message().content() == null || choice.message().content().isBlank()) {
            throw new BitRecallException(BitRecallErrorCode.STRUCTURED_OUTPUT_ERROR, "OpenAI response contained empty content");
        }
        return choice.message().content();
    }

    @Override
    public AIProvider getProvider() {
        return AIProvider.OPENAI; // Assuming OpenAI corresponds to OPENAI
    }
}
