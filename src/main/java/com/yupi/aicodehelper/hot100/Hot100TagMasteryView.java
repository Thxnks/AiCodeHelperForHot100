package com.yupi.aicodehelper.hot100;

public record Hot100TagMasteryView(
        String tag,
        int totalProblems,
        int practicedCount,
        int masteredCount,
        int wrongCount,
        double masteryRate
) {
}
