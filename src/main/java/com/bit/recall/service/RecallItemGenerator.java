package com.bit.recall.service;

import com.bit.recall.domain.model.RecallCardDto;
import dev.langchain4j.model.chat.ChatModel;

import java.util.List;

public interface RecallItemGenerator {
    List<RecallCardDto> generateRecallItems(ChatModel chatModel, String systemPrompt, String userPrompt);
}
