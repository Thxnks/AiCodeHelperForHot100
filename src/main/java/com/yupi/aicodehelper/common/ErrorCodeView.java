package com.yupi.aicodehelper.common;

public record ErrorCodeView(
        int code,
        int httpStatus,
        String category,
        String message
) {

    public static ErrorCodeView from(ErrorCode errorCode) {
        return new ErrorCodeView(
                errorCode.getCode(),
                errorCode.getHttpStatus(),
                errorCode.getCategory(),
                errorCode.getMessage()
        );
    }
}
