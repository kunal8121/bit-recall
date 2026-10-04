package com.bit.recall.repo;

import com.bit.recall.domain.RecallItem;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecallItemRepository extends JpaRepository<RecallItem, UUID> {

    List<RecallItem> findByContentId(UUID contentId);
}
