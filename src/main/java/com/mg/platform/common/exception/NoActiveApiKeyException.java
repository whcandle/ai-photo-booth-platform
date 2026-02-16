package com.mg.platform.common.exception;

public class NoActiveApiKeyException extends RuntimeException {
    private final String errorCode;

    public NoActiveApiKeyException(String message) {
        super(message);
        this.errorCode = "NO_ACTIVE_API_KEY";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
