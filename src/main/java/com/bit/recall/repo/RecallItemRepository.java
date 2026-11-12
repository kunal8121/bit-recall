package com.bit.recall.repo;

import com.bit.recall.domain.RecallItem;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecallItemRepository extends JpaRepository<RecallItem, UUID> {

    List<RecallItem> findByContentId(UUID contentId);

    @Query(nativeQuery = true, value = """
            SELECT ri.*
            FROM recall_items ri
            JOIN topics t ON t.topic_id = ri.topic_id
            WHERE ri.topic_id = :topicId
              AND t.user_id = :userId
              AND (
                  CAST(:lastCreatedAt AS TIMESTAMP) IS NULL
                  OR (ri.created_at, ri.id) < (CAST(:lastCreatedAt AS TIMESTAMP), CAST(:lastId AS UUID))
              )
            ORDER BY ri.created_at DESC, ri.id DESC
            LIMIT :pageSize;
            """)
    List<RecallItem> findByTopicId(UUID topicId,
                                   UUID userId,
                                   int pageSize,
                                   @Nullable Instant lastCreatedAt,
                                   @Nullable UUID lastId);

    void deleteByContentId(UUID contentId);

    void deleteByTopicId(UUID topicId);
}
