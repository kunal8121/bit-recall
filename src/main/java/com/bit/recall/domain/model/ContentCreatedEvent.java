package com.bit.recall.domain.model;

import com.bit.recall.ai.AIProvider;
import io.micronaut.core.annotation.Nullable;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ContentCreatedEvent(String id,
                                  String topicId,
                                  String contentId,
                                  String text,
                                  Instant createdAt,
                                  @Nullable AIProvider aiProvider,
                                  @Nullable String apiKey) {
}
