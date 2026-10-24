package com.bit.recall.exception;

import lombok.Getter;

@Getter
public class BitRecallException extends RuntimeException {
    private final BitRecallErrorCode errorCode;

    public BitRecallException(BitRecallErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null);
    }

    public BitRecallException(BitRecallErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BitRecallException(BitRecallErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
