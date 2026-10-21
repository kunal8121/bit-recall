package com.bit.recall.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.inject.Singleton;

import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;

/** OpenAI ChatModel adapter for the shared LangChain4j AIService implementation. */
@Singleton
public class OpenAIService extends LangChain4jAIService {

    private static final String MODEL_NAME = "gpt-4o-mini";

    public OpenAIService(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    protected ChatModel createChatModel(String apiKey) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
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
