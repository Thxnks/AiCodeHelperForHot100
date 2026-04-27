package com.yupi.aicodehelper.hot100;

import java.util.List;

public record Hot100ProblemSummaryView(
        Integer problemId,
        String title,
        String slug,
        String difficulty,
        List<String> tags,
        String pattern,
        String summary,
        String complexity
) {
    public static Hot100ProblemSummaryView from(Hot100Problem problem) {
        return new Hot100ProblemSummaryView(
                problem.getProblemId(),
                problem.getTitle(),
                problem.getSlug(),
                problem.getDifficulty(),
                problem.getTags(),
                problem.getPattern(),
                problem.getSummary(),
                problem.getComplexity()
        );
    }
}
