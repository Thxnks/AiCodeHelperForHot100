package com.yupi.aicodehelper.agent;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Hot100AgentRunRequest(
        @NotBlank(message = "goal cannot be blank")
        @Size(max = 2000, message = "goal is too long")
        String goal,
        @Size(max = 120, message = "problemSlug is too long")
        String problemSlug,
        @Size(max = 24, message = "status is too long")
        String status,
        @Min(value = 1, message = "limit must be >= 1")
        @Max(value = 20, message = "limit must be <= 20")
        Integer limit,
        @Min(value = 7, message = "days must be 7 or 14")
        @Max(value = 14, message = "days must be 7 or 14")
        Integer days,
        @Size(max = 12000, message = "userCode is too long")
        String userCode,
        @Size(max = 4000, message = "errorDescription is too long")
        String errorDescription,
        @Size(max = 4000, message = "notes is too long")
        String notes
) {
}
