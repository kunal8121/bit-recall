package com.bit.recall.repo;

import com.bit.recall.domain.RecallItem;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import jakarta.inject.Singleton;

import java.util.UUID;

@Repository
public interface RecallItemRepository extends JpaRepository<RecallItem, UUID> {
}
