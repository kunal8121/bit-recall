package com.bit.recall.service.impl;

import com.bit.recall.ai.AIProvider;
import com.bit.recall.domain.UserAiPreference;
import com.bit.recall.domain.model.ConfigureAiRequest;
import com.bit.recall.repo.AiConfigureRepository;
import com.bit.recall.service.AiConfigureService;
import com.bit.recall.service.EncryptionService;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor
public class AiConfigureServiceImpl implements AiConfigureService {

    private final AiConfigureRepository preferenceRepository;
    private final EncryptionService encryptionService; // AES-256 Utility

    @Transactional
    @Override
    public void configureAiProvider(String userId, ConfigureAiRequest request) {
        String encryptedKey = encryptionService.encrypt(request.apiKey());

        UserAiPreference preference = preferenceRepository.findByUserId(UUID.fromString(userId))
                .map(existingPreference -> {
                    existingPreference.setPreferredProvider(AIProvider.valueOf(request.provider()));
                    existingPreference.setEncryptedApiKey(encryptedKey);
                    return existingPreference;
                })
                .orElseGet(() -> UserAiPreference.builder()
                        .id(UUID.randomUUID())
                        .userId(UUID.fromString(userId))
                        .preferredProvider(AIProvider.valueOf(request.provider()))
                        .encryptedApiKey(encryptedKey)
                        .build());

        preferenceRepository.save(preference);
    }

    public UserAiPreference getPreferenceForUser(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("AI Provider API Key not configured. Please configure in settings."));
    }
}
