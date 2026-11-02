package com.bit.recall.service.impl;

import com.bit.recall.ai.AIProvider;
import com.bit.recall.domain.UserAiPreference;
import com.bit.recall.domain.model.ConfigureAiRequest;
import com.bit.recall.repo.AiConfigureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiConfigureServiceImplTest {
    @Mock AiConfigureRepository preferenceRepository;
    @Mock EncryptionService encryptionService;
    @InjectMocks AiConfigureServiceImpl service;

    @Test
    void createsPreferenceWithEncryptedApiKey() {
        UUID userId = UUID.randomUUID();
        when(encryptionService.encrypt("api-key")).thenReturn("cipher");

        service.configureAiProvider(userId.toString(), new ConfigureAiRequest("api-key", "OPENAI"));

        ArgumentCaptor<UserAiPreference> savedPreference = ArgumentCaptor.forClass(UserAiPreference.class);
        verify(preferenceRepository).save(savedPreference.capture());
        assertEquals(userId, savedPreference.getValue().getUserId());
        assertEquals(AIProvider.OPENAI, savedPreference.getValue().getPreferredProvider());
        assertEquals("cipher", savedPreference.getValue().getEncryptedApiKey());
    }

    @Test
    void updatesExistingPreference() {
        UUID userId = UUID.randomUUID();
        UserAiPreference existing = UserAiPreference.builder().id(UUID.randomUUID()).userId(userId).build();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(encryptionService.encrypt("new-key")).thenReturn("encrypted");

        service.configureAiProvider(userId.toString(), new ConfigureAiRequest("new-key", "OPENAI"));

        verify(preferenceRepository).save(existing);
        assertEquals("encrypted", existing.getEncryptedApiKey());
    }

    @Test
    void throwsWhenPreferenceIsMissing() {
        UUID userId = UUID.randomUUID();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.getPreferenceForUser(userId));
    }
}
