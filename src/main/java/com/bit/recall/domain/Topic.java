package com.bit.recall.domain;

import io.micronaut.data.annotation.DateCreated;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "topics")
public class Topic {

    @Id
    @Column(name = "topic_id") // Fixed: Standard snake_case
    private UUID id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Fixed: Standard snake_case
    private User user;

    @Column(columnDefinition = "TEXT") // Description lamba ho sakta hai isliye TEXT best hai
    private String description;

    @Builder.Default
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Content> contents = new ArrayList<>();

    @DateCreated
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}