package com.yupi.aicodehelper.hot100;

import java.time.LocalDateTime;

public record Hot100AsyncTaskDetailView(
        String taskId,
        Hot100AsyncTaskType taskType,
        Hot100AsyncTaskStatus status,
        Object result,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
