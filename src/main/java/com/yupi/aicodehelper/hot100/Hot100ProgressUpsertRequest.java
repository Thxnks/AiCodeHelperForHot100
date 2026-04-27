package com.yupi.aicodehelper.hot100;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Hot100ProgressUpsertRequest(
        @NotBlank(message = "problemSlug cannot be blank")
        String problemSlug,
        @NotBlank(message = "status cannot be blank")
        String status,
        @Size(max = 4000, message = "notes is too long")
        String notes
) {
}
