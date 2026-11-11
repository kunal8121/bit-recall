package com.bit.recall.exception;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;

@Builder
@Serdeable
public record ErrorResponse(String message, String status) {
}
