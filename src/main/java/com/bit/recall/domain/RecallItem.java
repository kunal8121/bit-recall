package com.bit.recall.domain;

import io.micronaut.data.annotation.DateCreated;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "recall_items")
@Table(
        name = "recall_items",
        indexes = {
                @Index(name = "idx_recall_items_crated_at", columnList = "id, created_at, topic_id")
        }
)
@Builder
public class RecallItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topicId")
    private Topic topic;

    private String title;

    private String question;

    @Lob
    private String summaryBody;

    @Lob
    private String metaData;

    @DateCreated
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
