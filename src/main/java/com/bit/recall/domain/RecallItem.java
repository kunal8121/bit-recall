package com.bit.recall.domain;

import io.micronaut.data.annotation.DateCreated;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "recall_items",
        indexes = {
                @Index(name = "idx_recall_items_created_at", columnList = "id, created_at, topic_id")
        }
)
@Builder(toBuilder = true)
public class RecallItem {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(length = 512)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String summaryBody;

    @Column(columnDefinition = "TEXT")
    private String metaData;

    @DateCreated
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}