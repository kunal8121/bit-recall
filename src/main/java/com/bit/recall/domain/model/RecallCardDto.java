package com.bit.recall.domain.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;

@Serdeable
@Builder
public record RecallCardDto(
        String title,
        String type,
        String explanation,
        String example,
        String code,
        String pattern,
        String recall) {
}
