package com.yupi.aicodehelper.hot100;

import com.yupi.aicodehelper.entity.Hot100ProblemProgress;

import java.time.LocalDateTime;

public record Hot100ProgressView(
        Long id,
        String problemSlug,
        String status,
        String notes,
        LocalDateTime lastReviewedAt,
        LocalDateTime updatedAt
) {
    public static Hot100ProgressView from(Hot100ProblemProgress progress) {
        return new Hot100ProgressView(
                progress.getId(),
                progress.getProblemSlug(),
                progress.getStatus(),
                progress.getNotes(),
                progress.getLastReviewedAt(),
                progress.getUpdatedAt()
        );
    }
}
