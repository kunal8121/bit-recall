package com.bit.recall.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;

/** OpenAI ChatModel adapter for the shared LangChain4j AIService implementation. */
@Singleton
public class OpenAIService implements AiModel {

    private static final String MODEL_NAME = "gpt-4o-mini";

    private final String baseUrl;

    public OpenAIService(@Value("${bit-recall.ai.openai.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public ChatModel getChatModel(String apiKey) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(MODEL_NAME)
                .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
                .strictJsonSchema(true)
                .build();
    }

    @Override
    public AIProvider getProvider() {
        return AIProvider.OPENAI;
    }
}
