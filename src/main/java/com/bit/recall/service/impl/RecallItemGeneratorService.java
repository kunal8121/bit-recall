package com.bit.recall.service.impl;

import com.bit.recall.ai.RecallItemAiService;
import com.bit.recall.domain.model.RecallCardDto;
import com.bit.recall.exception.BitRecallErrorCode;
import com.bit.recall.exception.BitRecallException;
import com.bit.recall.service.RecallItemGenerator;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.inject.Singleton;

import java.util.List;

/** Shared LangChain4j implementation of the typed recall extraction contract. */
@Singleton
public class RecallItemGeneratorService implements RecallItemGenerator {

    @Override
    public List<RecallCardDto> generateRecallItems(ChatModel chatModel, String systemPrompt, String userPrompt) {
        try {
            RecallItemAiService extractor = AiServices.create(RecallItemAiService.class, chatModel);
            List<RecallCardDto> items = extractor.extractRecallItems(systemPrompt, userPrompt);
            if (items == null) {
                throw new BitRecallException(BitRecallErrorCode.STRUCTURED_OUTPUT_ERROR,
                        "LangChain4j returned no recall items");
            }
            return items;
        } catch (BitRecallException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BitRecallException(BitRecallErrorCode.AI_SERVICE_ERROR,
                    "LangChain4j recall generation request failed", e);
        }
    }
}
