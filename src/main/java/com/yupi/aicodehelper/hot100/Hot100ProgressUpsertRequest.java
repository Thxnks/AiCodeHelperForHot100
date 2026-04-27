package com.yupi.aicodehelper.hot100;

public record Hot100ProgressUpsertRequest(
        String problemSlug,
        String status,
        String notes
) {
}
