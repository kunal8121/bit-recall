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
    @Column(name = "contentId")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topicId")
    private Topic topic;

    private String text;

    @DateCreated
    private Instant createdAt;

    @Builder.Default
    private Status status = Status.UNPROCESSED;

    @Builder.Default
    private RevisionDepth revisionDepth = RevisionDepth.COMPREHENSIVE;

    @Builder.Default
    private Integer processingAttemptCount = 0;

    private UUID lastProcessingAttemptId;

    private Instant lastProcessingAttemptAt;

    private Instant nextAttemptAt;

}
