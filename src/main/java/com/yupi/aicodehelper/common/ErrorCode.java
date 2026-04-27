package com.yupi.aicodehelper.common;

public enum ErrorCode {

    SUCCESS(0, 200, "COMMON", "ok"),
    BAD_REQUEST(40000, 400, "COMMON", "Bad request"),
    VALIDATION_ERROR(40001, 400, "COMMON", "Validation failed"),
    UNAUTHORIZED(40100, 401, "AUTH", "Unauthorized"),
    AUTH_TOKEN_INVALID(40101, 401, "AUTH", "Invalid token"),
    AUTH_TOKEN_EXPIRED(40102, 401, "AUTH", "Token expired"),
    AUTH_REFRESH_TOKEN_REVOKED(40103, 401, "AUTH", "Refresh token revoked"),
    FORBIDDEN(40300, 403, "AUTH", "Forbidden"),
    NOT_FOUND(40400, 404, "COMMON", "Resource not found"),
    RESOURCE_CONFLICT(40900, 409, "COMMON", "Resource conflict"),
    SYSTEM_ERROR(50000, 500, "SYSTEM", "System error");

    private final int code;
    private final int httpStatus;
    private final String category;

    private final String message;

    ErrorCode(int code, int httpStatus, String category, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.category = category;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getCategory() {
        return category;
    }
}
