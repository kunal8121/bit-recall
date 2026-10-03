package com.bit.recall.repo;

import com.bit.recall.domain.UserAiPreference;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import jakarta.inject.Singleton;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiConfigureRepository extends JpaRepository<UserAiPreference, UUID> {

    Optional<UserAiPreference> findByUserId(UUID userId);
}
