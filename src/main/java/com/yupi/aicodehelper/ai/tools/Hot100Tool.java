package com.yupi.aicodehelper.ai.tools;

import com.yupi.aicodehelper.hot100.Hot100ProblemDetailView;
import com.yupi.aicodehelper.hot100.Hot100ProblemSummaryView;
import com.yupi.aicodehelper.hot100.Hot100Service;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Hot100Tool {

    private final Hot100Service hot100Service;

    public Hot100Tool(Hot100Service hot100Service) {
        this.hot100Service = hot100Service;
    }

    @Tool(name = "findProblemByTitle", value = "Find a Hot100 problem by title keyword and return basic metadata.")
    public String findProblemByTitle(@P("problem title keyword") String titleKeyword) {
        List<Hot100ProblemSummaryView> list = hot100Service.listProblems(titleKeyword, null, null);
        if (list.isEmpty()) {
            return "No Hot100 problem found by title keyword: " + titleKeyword;
        }
        Hot100ProblemSummaryView first = list.get(0);
        return formatSummary(first);
    }

    @Tool(name = "listProblemsByTag", value = "List Hot100 problems by tag, optionally filter by difficulty.")
    public String listProblemsByTag(@P("tag name") String tag,
                                    @P("difficulty, optional: easy/medium/hard") String difficulty) {
        String normalizedDifficulty = (difficulty == null || difficulty.isBlank()) ? null : difficulty;
        List<Hot100ProblemSummaryView> list = hot100Service.listProblems(null, tag, normalizedDifficulty);
        if (list.isEmpty()) {
            return "No Hot100 problems found under tag=" + tag + ", difficulty=" + normalizedDifficulty;
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(10, list.size());
        for (int i = 0; i < limit; i++) {
            sb.append(i + 1).append(". ").append(formatSummary(list.get(i))).append("\n");
        }
        return sb.toString().trim();
    }

    @Tool(name = "getProblemSummary", value = "Get structured summary for a Hot100 problem by slug.")
    public String getProblemSummary(@P("problem slug, e.g. two-sum") String slug) {
        Hot100ProblemDetailView problem = hot100Service.getProblem(slug);
        return """
                title: %s
                slug: %s
                difficulty: %s
                tags: %s
                pattern: %s
                summary: %s
                coreIdea: %s
                complexity: %s
                """.formatted(
                problem.title(),
                problem.slug(),
                problem.difficulty(),
                problem.tags(),
                problem.pattern(),
                problem.summary(),
                problem.coreIdea(),
                problem.complexity()
        ).trim();
    }

    @Tool(name = "getProblemPitfalls", value = "Get common pitfalls and interview follow-up hints for a Hot100 problem.")
    public String getProblemPitfalls(@P("problem slug, e.g. two-sum") String slug) {
        Hot100ProblemDetailView problem = hot100Service.getProblem(slug);
        return """
                title: %s
                pitfalls: %s
                markdownNotes: %s
                """.formatted(problem.title(), problem.pitfalls(), problem.markdownContent()).trim();
    }

    @Tool(name = "recommendProblemsByDifficulty", value = "Recommend Hot100 problems by difficulty, optionally with tag.")
    public String recommendProblemsByDifficulty(@P("difficulty: easy/medium/hard") String difficulty,
                                                @P("optional tag filter") String tag) {
        List<Hot100ProblemSummaryView> list = hot100Service.listProblems(null, tag, difficulty);
        if (list.isEmpty()) {
            return "No recommendation found for difficulty=" + difficulty + ", tag=" + tag;
        }
        StringBuilder sb = new StringBuilder("Recommended problems:\n");
        int limit = Math.min(5, list.size());
        for (int i = 0; i < limit; i++) {
            sb.append("- ").append(formatSummary(list.get(i))).append("\n");
        }
        return sb.toString().trim();
    }

    private String formatSummary(Hot100ProblemSummaryView p) {
        return "%s (%s) slug=%s tags=%s pattern=%s".formatted(
                p.title(),
                p.difficulty(),
                p.slug(),
                p.tags(),
                p.pattern()
        );
    }
}
