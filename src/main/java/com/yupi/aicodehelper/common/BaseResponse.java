package com.yupi.aicodehelper.common;

import lombok.Data;

@Data
public class BaseResponse<T> {

    private int code;

    private String message;

    private T data;

    private long timestamp;

    public BaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static BaseResponse<Void> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static BaseResponse<Void> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), message, null);
    }
}
