package com.bit.recall.repo;

import com.bit.recall.domain.RecallItem;
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
            SELECT * FROM recall_items ri
            WHERE ri.topicId = :topicId
            AND ri.topicId IN
            (SELECT t.topicId FROM topics t WHERE t.userId = :userId)
            AND (:lastEvaluatedCursor IS NULL
                 OR ri.createdAt < :lastEvaluatedCursor
                 OR (ri.createdAt = :lastEvaluatedCursor AND ri.id < :lastEvaluatedId))
            ORDER BY ri.createdAt DESC, ri.id DESC
            LIMIT :pageSize
            """)
    List<RecallItem> findByTopicId(UUID topicId,
                                   UUID userId,
                                   int pageSize,
                                   Instant lastEvaluatedCursor,
                                   UUID lastEvaluatedId);
}
