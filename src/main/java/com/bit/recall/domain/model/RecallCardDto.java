package com.bit.recall.domain.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;

@Serdeable
@Builder
public record RecallCardDto(String title, String body, String question, String answer) {
}
