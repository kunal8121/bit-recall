package com.bit.recall.exception;

import io.micronaut.core.annotation.Order;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.NotFoundException;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@Produces
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler implements ExceptionHandler<Throwable, HttpResponse<ErrorResponse>> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, Throwable exception) {
        BitRecallException applicationException = normalize(exception);
        if (applicationException.getErrorCode().getHttpStatus().getCode() >= 500) {
            log.error("Request {} {} failed with {}", request.getMethod(), request.getPath(),
                    applicationException.getErrorCode(), exception);
        } else {
            log.warn("Request {} {} rejected with {}: {}", request.getMethod(), request.getPath(),
                    applicationException.getErrorCode(), applicationException.getMessage());
        }

        ErrorResponse body = ErrorResponse.builder()
                .message(applicationException.getMessage())
                .status(applicationException.getErrorCode().name())
                .build();
        return HttpResponse.status(applicationException.getErrorCode().getHttpStatus()).body(body);
    }

    private BitRecallException normalize(Throwable exception) {
        if (exception instanceof BitRecallException bitRecallException) {
            return bitRecallException;
        }
        if (exception instanceof jakarta.validation.ConstraintViolationException) {
            return new BitRecallException(BitRecallErrorCode.BAD_REQUEST, "Request validation failed", exception);
        }
        if (exception instanceof HttpStatusException statusException) {
            BitRecallErrorCode code = switch (statusException.getStatus()) {
                case BAD_REQUEST -> BitRecallErrorCode.BAD_REQUEST;
                case UNAUTHORIZED -> BitRecallErrorCode.UNAUTHORIZED;
                case FORBIDDEN -> BitRecallErrorCode.FORBIDDEN;
                case NOT_FOUND -> BitRecallErrorCode.NOT_FOUND;
                case CONFLICT -> BitRecallErrorCode.CONFLICT;
                default -> BitRecallErrorCode.SERVER_ERROR;
            };
            return new BitRecallException(code, statusException.getMessage(), exception);
        }
        if (exception instanceof NotFoundException) {
            return new BitRecallException(BitRecallErrorCode.NOT_FOUND, exception.getMessage(), exception);
        }
        if (exception instanceof java.util.NoSuchElementException) {
            return new BitRecallException(BitRecallErrorCode.NOT_FOUND, exception.getMessage(), exception);
        }
        if (exception instanceof IllegalArgumentException) {
            return new BitRecallException(BitRecallErrorCode.BAD_REQUEST, exception.getMessage(), exception);
        }
        if (exception instanceof IllegalStateException) {
            return new BitRecallException(BitRecallErrorCode.CONFLICT, exception.getMessage(), exception);
        }
        return new BitRecallException(BitRecallErrorCode.SERVER_ERROR,
                BitRecallErrorCode.SERVER_ERROR.getDefaultMessage(), exception);
    }
}
