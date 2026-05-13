package com.yupi.aicodehelper.agent;

import java.util.List;

public record AgentMemoryProfileView(
        List<String> weaknesses,
        List<String> wrongAnswerPatterns,
        List<String> nextActions,
        List<String> preferences,
        List<AgentMemoryView> recentImportantMemories
) {
}
