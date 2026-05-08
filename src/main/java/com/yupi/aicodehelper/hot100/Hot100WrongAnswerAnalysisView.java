package com.yupi.aicodehelper.hot100;

public record Hot100WrongAnswerAnalysisView(
        String problemSlug,
        String wrongReason,
        String knowledgePoint,
        String aiFeedback,
        String nextAction
) {
}
