package com.bit.recall.domain.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;

import java.time.Instant;

@Serdeable
@Builder
public record ContentResponse(
        String id,
        String text,
        Instant createdAt) {}
