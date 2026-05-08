package com.yupi.aicodehelper.hot100;

import java.util.List;

public record Hot100RecommendedProblemView(
        Integer problemId,
        String title,
        String slug,
        String leetCodeUrl,
        String difficulty,
        List<String> tags,
        String pattern,
        String summary,
        String complexity,
        String reason,
        String trainingFocus
) {
    public static Hot100RecommendedProblemView from(Hot100ProblemSummaryView problem,
                                                    String reason,
                                                    String trainingFocus) {
        return new Hot100RecommendedProblemView(
                problem.problemId(),
                problem.title(),
                problem.slug(),
                problem.leetCodeUrl(),
                problem.difficulty(),
                problem.tags(),
                problem.pattern(),
                problem.summary(),
                problem.complexity(),
                reason,
                trainingFocus
        );
    }
}
