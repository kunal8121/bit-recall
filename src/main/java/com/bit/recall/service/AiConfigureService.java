package com.bit.recall.service;


import com.bit.recall.domain.model.ConfigureAiRequest;
import com.bit.recall.domain.UserAiPreference;

import java.util.UUID;

public interface AiConfigureService {

    void configureAiProvider(String userId, ConfigureAiRequest request);

    UserAiPreference getPreferenceForUser(UUID userId);
}
