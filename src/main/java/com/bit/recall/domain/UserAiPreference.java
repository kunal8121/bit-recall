package com.bit.recall.domain;

import com.bit.recall.ai.AIProvider;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(
        name = "user_ai_preference",
        indexes = {
                @Index(name = "idx_user_ai_preference_user_id", columnList = "user_id", unique = true) // 1 user = 1 preference usually
        }
)
public class UserAiPreference {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_provider", nullable = false)
    private AIProvider preferredProvider;

    @Column(name = "encrypted_api_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedApiKey;

    @DateCreated
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @DateUpdated
    @Column(name = "updated_at")
    private Instant updatedAt;
}