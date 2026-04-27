package com.yupi.aicodehelper.hot100;

import org.springframework.stereotype.Service;

@Service
public class Hot100ChatContextService {

    private final Hot100Service hot100Service;

    public Hot100ChatContextService(Hot100Service hot100Service) {
        this.hot100Service = hot100Service;
    }

    public String buildProblemContext(String currentProblemSlug) {
        if (currentProblemSlug == null || currentProblemSlug.isBlank()) {
            return """
                    No current Hot100 problem is selected.
                    Answer based on user message and role only.
                    """.trim();
        }
        Hot100ProblemDetailView p = hot100Service.getProblem(currentProblemSlug);
        return """
                Current Hot100 problem context:
                - title: %s
                - slug: %s
                - difficulty: %s
                - tags: %s
                - pattern: %s
                - summary: %s
                - coreIdea: %s
                - pitfalls: %s
                - complexity: %s
                Focus rule:
                - Treat ambiguous user questions as referring to this current problem.
                - Do not switch topic unless user explicitly asks to change problem/topic.
                """.formatted(
                p.title(),
                p.slug(),
                p.difficulty(),
                p.tags(),
                p.pattern(),
                p.summary(),
                p.coreIdea(),
                p.pitfalls(),
                p.complexity()
        ).trim();
    }

    public String buildCoachingStrategy(String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return defaultStrategy();
        }
        return switch (roleId) {
            case "assistant" -> """
                    Role strategy (programming learning assistant):
                    - Prioritize hints and step-by-step reasoning first.
                    - In guided mode, do not output full final code unless user asks explicitly.
                    - Keep explanation tied to the current problem when it exists.
                    """.trim();
            case "interviewer" -> """
                    Role strategy (interviewer mode):
                    - Ask follow-up questions on complexity, edge cases, and tradeoffs.
                    - Prefer probing and evaluating before giving full answers.
                    """.trim();
            case "resume-coach" -> """
                    Role strategy (resume coach):
                    - After problem explanation, provide one resume-ready bullet if relevant.
                    - Emphasize measurable outcomes and engineering clarity.
                    """.trim();
            case "maid" -> """
                    Role strategy (execution-oriented):
                    - Give concise and practical next steps.
                    - Highlight risky or error-prone points clearly.
                    """.trim();
            case "tsundere" -> """
                    Role strategy (beginner-friendly):
                    - Explain terms in simple language.
                    - Move from small hints to deeper explanation progressively.
                    """.trim();
            default -> defaultStrategy();
        };
    }

    public String buildSolvingModeStrategy(String solvingMode) {
        if (solvingMode == null || solvingMode.isBlank()) {
            return guidedModeStrategy();
        }
        return switch (solvingMode.trim().toLowerCase()) {
            case "direct" -> """
                    Solving mode: direct
                    - Provide complete approach (and code if needed).
                    - Explain complexity and edge cases.
                    """.trim();
            case "code_review" -> """
                    Solving mode: code review
                    - Ask user code first if not provided.
                    - Review by correctness, complexity, readability, robustness, and testability.
                    - Output format: issues -> fix suggestions -> improved reference.
                    """.trim();
            default -> guidedModeStrategy();
        };
    }

    public String buildUserFocusPrefix(String currentProblemSlug) {
        if (currentProblemSlug == null || currentProblemSlug.isBlank()) {
            return "";
        }
        Hot100ProblemDetailView p = hot100Service.getProblem(currentProblemSlug);
        return """
                [Focus Constraint]
                Current problem is "%s" (slug: %s).
                Interpret short/ambiguous questions as this problem by default.
                """.formatted(p.title(), p.slug()).trim();
    }

    private String guidedModeStrategy() {
        return """
                Solving mode: guided (default)
                - Give hints first, not full code.
                - Guide in steps: understand -> pattern -> approach -> complexity.
                - Only provide full implementation when user explicitly requests it.
                """.trim();
    }

    private String defaultStrategy() {
        return """
                Default role strategy:
                - Give conclusion first, then steps and reasons.
                - Keep answer executable and practical.
                """.trim();
    }
}

