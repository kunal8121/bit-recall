package com.bit.recall.ai;

import dev.langchain4j.model.chat.ChatModel;

public interface AiModel {
    AIProvider getProvider();

    ChatModel getChatModel(String apiKey);
}
