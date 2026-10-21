package com.bit.recall.ai;

import com.bit.recall.domain.model.RecallCardDto;
import com.bit.recall.domain.model.RecallCardDtoWrapper;
import com.bit.recall.exception.BitRecallErrorCode;
import com.bit.recall.exception.BitRecallException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.service.AiServices;

import java.io.IOException;
import java.util.List;

/** Shared LangChain4j implementation of the project's AIService contract. */
public abstract class LangChain4jAIService implements AIService {

    private final ObjectMapper objectMapper;

    protected LangChain4jAIService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected abstract ChatModel createChatModel(String apiKey);

    @Override
    public String chat(String prompt) {
        throw new UnsupportedOperationException("Use generateStructuredOutput with a user API key");
    }

    @Override
    public <T> T generateStructuredOutput(String apiKey, String systemPrompt, String userPrompt, Class<T> responseType) {
        try {
            ChatModel chatModel = createChatModel(apiKey);
            if (responseType == RecallCardDtoWrapper.class) {
                List<RecallCardDto> recallItems = AiServices
                        .create(RecallItemExtractor.class, chatModel)
                        .extractRecallItems(systemPrompt, userPrompt);
                if (recallItems == null) {
                    throw new BitRecallException(BitRecallErrorCode.STRUCTURED_OUTPUT_ERROR,
                            "LangChain4j returned null recall items");
                }
                return responseType.cast(new RecallCardDtoWrapper(recallItems));
            }

            var request = ChatRequest.builder()
                    .messages(SystemMessage.from(systemPrompt), UserMessage.from(userPrompt))
                    .responseFormat(ResponseFormat.builder().type(ResponseFormatType.JSON).build())
                    .build();
            var response = chatModel.chat(request);
            if (response == null || response.aiMessage() == null || response.aiMessage().text() == null
                    || response.aiMessage().text().isBlank()) {
                throw new BitRecallException(BitRecallErrorCode.STRUCTURED_OUTPUT_ERROR,
                        "LangChain4j returned an empty response");
            }

            JsonNode output = objectMapper.readTree(response.aiMessage().text());
            return objectMapper.treeToValue(output, responseType);

        } catch (BitRecallException e) {
            throw e;
        } catch (IOException e) {
            throw new BitRecallException(BitRecallErrorCode.STRUCTURED_OUTPUT_ERROR,
                    "LangChain4j returned malformed structured output", e);
        } catch (RuntimeException e) {
            throw new BitRecallException(BitRecallErrorCode.AI_SERVICE_ERROR,
                    "LangChain4j request failed for provider " + getProvider(), e);
        }
    }
}
