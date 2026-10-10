package com.bit.recall.ai;

public interface AIService {
    String chat(String prompt);

    <T> T generateStructuredOutput(String apiKey,
                                   String systemPrompt,
                                   String userPrompt,
                                   Class<T> responseType);

    AIProvider getProvider();
}
