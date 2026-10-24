package com.bit.recall.ai;

import com.bit.recall.exception.BitRecallErrorCode;
import com.bit.recall.exception.BitRecallException;

import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class AiModelFactory {

    private final Map<AIProvider, AiModel> aiModelProvider;

    // Micronaut injects all provider adapters implementing AiModel.
    public AiModelFactory(List<AiModel> aiModels) {
        this.aiModelProvider = aiModels.stream()
                .collect(Collectors.toMap(AiModel::getProvider, Function.identity()));
    }

    public AiModel getModel(AIProvider provider) {
        AiModel service = aiModelProvider.get(provider);
        if (service == null) {
            throw new BitRecallException(BitRecallErrorCode.UNSUPPORTED_PROVIDER, "Unsupported AI provider: " + provider);
        }
        return service;
    }
}
