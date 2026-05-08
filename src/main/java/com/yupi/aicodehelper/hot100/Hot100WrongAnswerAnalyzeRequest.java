package com.yupi.aicodehelper.hot100;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Hot100WrongAnswerAnalyzeRequest(
        @NotBlank(message = "problemSlug cannot be blank")
        String problemSlug,
        @Size(max = 12000, message = "userCode is too long")
        String userCode,
        @Size(max = 4000, message = "errorDescription is too long")
        String errorDescription,
        @Size(max = 4000, message = "notes is too long")
        String notes
) {
}
