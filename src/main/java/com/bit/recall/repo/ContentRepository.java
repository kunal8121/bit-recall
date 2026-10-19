package com.bit.recall.repo;

import com.bit.recall.domain.Content;
import com.bit.recall.domain.Status;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {

    @Query(nativeQuery = true, value = "UPDATE contents " +
            "SET status = 'PROCESSING', " +
            "processing_attempt_count = COALESCE(processing_attempt_count, 0) + 1, " +
            "last_processing_attempt_id = :attemptId, " +
            "last_processing_attempt_at = CURRENT_TIMESTAMP, " +
            "next_attempt_at = NULL " +
            "WHERE content_id = :contentId " +
            "AND status <> 'PROCESSED' " +
            "AND COALESCE(processing_attempt_count, 0) < :maxRetries " +
            "AND (next_attempt_at IS NULL OR next_attempt_at <= CURRENT_TIMESTAMP) " +
            "AND (status <> 'PROCESSING' OR last_processing_attempt_at IS NULL " +
            "OR last_processing_attempt_at < :staleBefore)")
    int claimForProcessing(UUID contentId, UUID attemptId, int maxRetries, Instant staleBefore);

    @Query(nativeQuery = true, value = "UPDATE contents SET status = 'PROCESSING_FAILED', " +
            "next_attempt_at = :nextAttemptAt " +
            "WHERE content_id = :contentId AND last_processing_attempt_id = :attemptId")
    int markAttemptFailed(UUID contentId, UUID attemptId, Instant nextAttemptAt);

    @Query(nativeQuery = true, value = "UPDATE contents SET status = 'PROCESSED' " +
            "WHERE content_id = :contentId " +
            "AND last_processing_attempt_id = :attemptId AND status = 'PROCESSING'")
    int markAttemptProcessed(UUID contentId, UUID attemptId);

    List<Content> findByTopicId(UUID topicId);

    @Query(nativeQuery = true,
            value = "SELECT * FROM contents c "
                    + "WHERE c.status = :status "
                    + "AND (c.processing_attempt_count < :maxRetries OR :maxRetries IS NULL) "
                    + "AND ("
                    + "   :staleAfter IS NULL "
                    + "   OR c.last_processing_attempt_at IS NULL "
                    + "   OR (NOW() - c.last_processing_attempt_at) > (:staleAfter * INTERVAL '1 second')"
                    + ") "
                    + "ORDER BY c.created_at ASC")
    List<Content> findByStatus(
            Status status,
            @Nullable Integer maxRetries,
            @Nullable Integer staleAfter);
}
