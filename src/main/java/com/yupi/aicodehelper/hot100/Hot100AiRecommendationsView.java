package com.yupi.aicodehelper.hot100;

import java.util.List;

public record Hot100AiRecommendationsView(
        List<String> weakTags,
        List<String> masteredTags,
        List<String> recentWrongProblems,
        String coachSummary,
        List<Hot100RecommendedProblemView> recommendedProblems,
        List<Hot100StudyPlanItemView> trainingPlan
) {
}
