package com.bit.recall.exception;

import lombok.Builder;

@Builder
public record ErrorResponse(String message, String status) {
}
