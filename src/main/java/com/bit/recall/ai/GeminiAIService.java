package com.bit.recall.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.client.BlockingHttpClient;
import jakarta.inject.Singleton;

import java.net.http.HttpClient;

@Singleton
public class GeminiAIService implements AIService{

    @Override
    public String chat(String prompt) {
        return "";
    }

    @Override
    public <T> T generateStructuredOutput(String apiKey, String systemPrompt, String userPrompt, Class<T> responseType) {

        return null;
    }

    @Override
    public AIProvider getProvider() {
        return null;
    }
}
