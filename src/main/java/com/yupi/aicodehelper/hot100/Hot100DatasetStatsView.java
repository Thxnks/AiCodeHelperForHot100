package com.yupi.aicodehelper.hot100;

public record Hot100DatasetStatsView(
        int loadedCount,
        int targetCount,
        boolean completed
) {
}
