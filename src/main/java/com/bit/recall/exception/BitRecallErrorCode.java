package com.bit.recall.exception;

import io.micronaut.http.HttpStatus;

public enum BitRecallErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "The request is invalid."),
    INVALID_API_KEY(HttpStatus.BAD_GATEWAY, "The configured AI API key was rejected."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "The specified AI provider is not supported."),
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "An error occurred while communicating with the AI service."),
    INVALID_PROMPT(HttpStatus.BAD_REQUEST, "The provided prompt is invalid or empty."),
    STRUCTURED_OUTPUT_ERROR(HttpStatus.BAD_GATEWAY, "Failed to generate valid structured output from the AI service."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to access this resource."),
    CONFLICT(HttpStatus.CONFLICT, "The requested operation conflicts with the resource state."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    BitRecallErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
