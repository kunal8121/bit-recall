package com.bit.recall.service;


import com.bit.recall.domain.model.ConfigureAiRequest;

public interface AiConfigureService {

    void configureAiProvider(String userId, ConfigureAiRequest request);
}
