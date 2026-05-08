package com.yupi.aicodehelper.hot100;

import java.time.LocalDateTime;

public record Hot100WrongBookItemView(
        Hot100ProblemSummaryView problem,
        String wrongReason,
        String knowledgePoint,
        String aiFeedback,
        String nextAction,
        String notes,
        LocalDateTime updatedAt
) {
}
