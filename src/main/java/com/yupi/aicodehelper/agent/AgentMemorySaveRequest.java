package com.yupi.aicodehelper.agent;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AgentMemorySaveRequest(
        String type,
        String scope,
        @NotBlank(message = "subject cannot be blank")
        String subject,
        @NotBlank(message = "content cannot be blank")
        String content,
        @Min(value = 1, message = "importance must be at least 1")
        @Max(value = 10, message = "importance must be at most 10")
        Integer importance,
        String source
) {
}
