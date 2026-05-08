package com.yupi.aicodehelper.hot100;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Hot100ProgressUpsertRequest(
        @NotBlank(message = "problemSlug cannot be blank")
        String problemSlug,
        @NotBlank(message = "status cannot be blank")
        String status,
        @Size(max = 4000, message = "notes is too long")
        String notes,
        @Size(max = 4000, message = "wrongReason is too long")
        String wrongReason,
        @Size(max = 255, message = "knowledgePoint is too long")
        String knowledgePoint,
        @Size(max = 4000, message = "aiFeedback is too long")
        String aiFeedback,
        @Size(max = 4000, message = "nextAction is too long")
        String nextAction
) {
}
