package com.bit.recall.domain;

import io.micronaut.data.annotation.DateCreated;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "contents")
@Builder(toBuilder = true)
public class Content {

    @Id
    @Column(name = "content_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(columnDefinition = "TEXT")
    private String text;

    @DateCreated
    @Column(name = "created_at")
    private Instant createdAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.UNPROCESSED;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "revision_depth", nullable = false)
    private RevisionDepth revisionDepth = RevisionDepth.COMPREHENSIVE;

    @Builder.Default
    @Column(name = "processing_attempt_count")
    private Integer processingAttemptCount = 0;

    @Column(name = "last_processing_attempt_id")
    private UUID lastProcessingAttemptId;

    @Column(name = "last_processing_attempt_at")
    private Instant lastProcessingAttemptAt;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "process_requested_at")
    private Instant processRequestedAt;
}