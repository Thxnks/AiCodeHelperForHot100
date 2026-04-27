package com.yupi.aicodehelper.hot100;

import java.time.LocalDateTime;

public record Hot100AsyncTaskSubmitView(
        String taskId,
        Hot100AsyncTaskType taskType,
        Hot100AsyncTaskStatus status,
        LocalDateTime createdAt
) {
}
