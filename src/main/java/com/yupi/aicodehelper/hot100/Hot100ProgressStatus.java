package com.yupi.aicodehelper.hot100;

public enum Hot100ProgressStatus {
    NOT_STARTED,
    COMPLETED,
    WRONG,
    MASTERED;

    public static Hot100ProgressStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("status 不能为空");
        }
        return Hot100ProgressStatus.valueOf(value.trim().toUpperCase());
    }
}
