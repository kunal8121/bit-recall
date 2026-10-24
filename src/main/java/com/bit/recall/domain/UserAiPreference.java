package com.bit.recall.domain;

import com.bit.recall.ai.AIProvider;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@Entity
@Table(name = "user_ai_preference")
@AllArgsConstructor
@NoArgsConstructor
public class UserAiPreference {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AIProvider preferredProvider;

    @Column(nullable = false)
    private String encryptedApiKey;

    @DateCreated
    private Instant createdAt;

    @DateUpdated
    private Instant updatedAt;

}
