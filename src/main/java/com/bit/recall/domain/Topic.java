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
@Builder
@Entity
@Table(name = "topics")
public class Topic {
    @Id
    @Column(name = "topicId")
    private UUID id;

    @NotBlank
    private String title;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    private String description;

    @Builder.Default
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Content> contents = new ArrayList<>();

    @DateCreated
    private Instant createdAt;
}
