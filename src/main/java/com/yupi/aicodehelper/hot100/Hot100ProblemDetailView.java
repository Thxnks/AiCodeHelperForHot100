package com.yupi.aicodehelper.hot100;

import java.util.List;

public record Hot100ProblemDetailView(
        Integer problemId,
        String title,
        String slug,
        String difficulty,
        List<String> tags,
        String pattern,
        String summary,
        String coreIdea,
        String pitfalls,
        String complexity,
        String markdownContent
) {
    public static Hot100ProblemDetailView from(Hot100Problem problem) {
        return new Hot100ProblemDetailView(
                problem.getProblemId(),
                problem.getTitle(),
                problem.getSlug(),
                problem.getDifficulty(),
                problem.getTags(),
                problem.getPattern(),
                problem.getSummary(),
                problem.getCoreIdea(),
                problem.getPitfalls(),
                problem.getComplexity(),
                problem.getMarkdownContent()
        );
    }
}
