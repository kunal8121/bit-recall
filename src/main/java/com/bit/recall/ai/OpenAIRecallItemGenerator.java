package com.bit.recall.ai;

import com.bit.recall.domain.model.RecallCardDto;
import com.bit.recall.exception.BitRecallErrorCode;
import com.bit.recall.exception.BitRecallException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;

/**
 * Alternative OpenAI implementation using LangChain4j's typed AI Service API.
 * The API key is supplied per call so user credentials are not stored in a shared model bean.
 */
@Singleton
@Slf4j
@RequiredArgsConstructor
public class OpenAIRecallItemGenerator {


    private static final String MODEL_NAME = "gpt-4o-mini";

    public List<RecallCardDto> generateRecallItems(String apiKey, String systemPrompt, String userPrompt) {
        try {
            ChatModel chatModel = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(MODEL_NAME)
                    .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
                    .strictJsonSchema(true)
                    .build();

            RecallItemExtractor extractor = AiServices.create(RecallItemExtractor.class, chatModel);
            List<RecallCardDto> recallItems = extractor.extractRecallItems(systemPrompt, userPrompt);
            if (recallItems == null) {
                throw new BitRecallException(BitRecallErrorCode.STRUCTURED_OUTPUT_ERROR,
                        "OpenAI returned null recall items");
            }
            return recallItems;
        } catch (BitRecallException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("LangChain4j OpenAI recall item generation failed", e);
            throw new BitRecallException(BitRecallErrorCode.AI_SERVICE_ERROR,
                    "LangChain4j OpenAI recall item generation failed", e);
        }
    }
}
