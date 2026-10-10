package com.bit.recall.ai;

import com.bit.recall.exception.BitRecallErrorCode;
import com.bit.recall.exception.BitRecallException;

import io.micronaut.http.client.BlockingHttpClient;
import jakarta.inject.Singleton;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class AIServiceFactory {

    private final Map<AIProvider, AIService> serviceMap;

    // Micronaut automatically injects all beans implementing AIService interface
    public AIServiceFactory(List<AIService> aiServices) {
        this.serviceMap = aiServices.stream()
                .collect(Collectors.toMap(AIService::getProvider, Function.identity()));
    }

    public AIService getService(AIProvider provider) {
        AIService service = serviceMap.get(provider);
        if (service == null) {
            throw new BitRecallException(BitRecallErrorCode.UNSUPPORTED_PROVIDER, "Unsupported AI provider: " + provider);
        }
        return service;
    }
}
