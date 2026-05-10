package com.yupi.aicodehelper.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.aicodehelper.ai.AiCodeHelperService;
import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.entity.AgentStep;
import com.yupi.aicodehelper.entity.AgentTask;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.hot100.Hot100AiRecommendationsView;
import com.yupi.aicodehelper.hot100.Hot100ProblemDetailView;
import com.yupi.aicodehelper.hot100.Hot100ProblemSummaryView;
import com.yupi.aicodehelper.hot100.Hot100ProgressService;
import com.yupi.aicodehelper.hot100.Hot100ProgressUpsertRequest;
import com.yupi.aicodehelper.hot100.Hot100ProgressView;
import com.yupi.aicodehelper.hot100.Hot100Service;
import com.yupi.aicodehelper.hot100.Hot100StudyPlanItemView;
import com.yupi.aicodehelper.hot100.Hot100TagMasteryView;
import com.yupi.aicodehelper.hot100.Hot100WeakTagView;
import com.yupi.aicodehelper.hot100.Hot100WrongAnalysisService;
import com.yupi.aicodehelper.hot100.Hot100WrongAnswerAnalyzeRequest;
import com.yupi.aicodehelper.hot100.Hot100WrongBookItemView;
import com.yupi.aicodehelper.repository.AgentStepRepository;
import com.yupi.aicodehelper.repository.AgentTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class Hot100AgentService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int DEFAULT_DAYS = 7;

    private final Hot100Service hot100Service;
    private final Hot100ProgressService hot100ProgressService;
    private final Hot100WrongAnalysisService hot100WrongAnalysisService;
    private final AiCodeHelperService aiCodeHelperService;
    private final AgentKnowledgeService agentKnowledgeService;
    private final AgentTaskRepository agentTaskRepository;
    private final AgentStepRepository agentStepRepository;
    private final ObjectMapper objectMapper;

    public Hot100AgentService(Hot100Service hot100Service,
                              Hot100ProgressService hot100ProgressService,
                              Hot100WrongAnalysisService hot100WrongAnalysisService,
                              AiCodeHelperService aiCodeHelperService,
                              AgentKnowledgeService agentKnowledgeService,
                              AgentTaskRepository agentTaskRepository,
                              AgentStepRepository agentStepRepository,
                              ObjectMapper objectMapper) {
        this.hot100Service = hot100Service;
        this.hot100ProgressService = hot100ProgressService;
        this.hot100WrongAnalysisService = hot100WrongAnalysisService;
        this.aiCodeHelperService = aiCodeHelperService;
        this.agentKnowledgeService = agentKnowledgeService;
        this.agentTaskRepository = agentTaskRepository;
        this.agentStepRepository = agentStepRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AgentTaskView run(Hot100AgentRunRequest request, Long userId) {
        AgentTask task = createTask(request, userId);
        AgentRunContext context = new AgentRunContext(task.getTaskId(), request, userId);
        try {
            executePlan(context);
            task.setStatus(AgentTaskStatus.SUCCESS.name());
            task.setFinalAnswer(buildFinalAnswer(context));
            task.setErrorMessage(null);
        } catch (Exception e) {
            task.setStatus(AgentTaskStatus.FAILED.name());
            task.setErrorMessage(truncate(e.getMessage(), 1000));
            task.setFinalAnswer("Agent task failed before completing the Hot100 workflow.");
        }
        AgentTask saved = agentTaskRepository.save(task);
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public AgentTaskView getTask(String taskId, Long userId) {
        AgentTask task = agentTaskRepository.findByUserIdAndTaskId(userId, taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Agent task not found"));
        return toView(task);
    }

    @Transactional(readOnly = true)
    public List<AgentStepView> listSteps(String taskId, Long userId) {
        agentTaskRepository.findByUserIdAndTaskId(userId, taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Agent task not found"));
        return agentStepRepository.findByTaskIdOrderByStepOrderAsc(taskId).stream()
                .map(AgentStepView::from)
                .toList();
    }

    private AgentTask createTask(Hot100AgentRunRequest request, Long userId) {
        AgentTask task = new AgentTask();
        task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        task.setUserId(userId);
        task.setGoal(request.goal().trim());
        task.setStatus(AgentTaskStatus.RUNNING.name());
        return agentTaskRepository.save(task);
    }

    private void executePlan(AgentRunContext context) {
        List<PlannedToolCall> modelPlan = planWithModel(context);
        if (!modelPlan.isEmpty()) {
            executePlannedToolCalls(context, modelPlan);
        } else {
            executeFallbackPlan(context);
        }

        if (context.outputs().isEmpty()) {
            int limit = limit(context.request());
            executeTool(context, "aiRecommendations", Map.of("limit", limit));
        }
    }

    private List<PlannedToolCall> planWithModel(AgentRunContext context) {
        try {
            String response = aiCodeHelperService.planHot100AgentTask(buildPlannerInput(context.request()));
            List<PlannedToolCall> plan = parsePlannerResponse(response);
            if (plan.isEmpty()) {
                return List.of();
            }
            invokeTool(context, "planHot100AgentTask", Map.of("goal", context.request().goal()), () -> plan);
            return plan;
        } catch (Exception e) {
            invokeTool(context, "planHot100AgentTask", Map.of("goal", context.request().goal()), () -> Map.of(
                    "fallback", true,
                    "reason", blankToPlaceholder(truncate(e.getMessage(), 500))
            ));
            return List.of();
        }
    }

    private String buildPlannerInput(Hot100AgentRunRequest request) {
        return """
                User goal:
                %s

                Optional runtime fields:
                - problemSlug: %s
                - status: %s
                - limit: %s
                - days: %s
                - hasUserCode: %s
                - hasErrorDescription: %s
                - notes: %s
                """.formatted(
                request.goal(),
                blankToNullText(request.problemSlug()),
                blankToNullText(request.status()),
                request.limit(),
                request.days(),
                !isBlank(request.userCode()),
                !isBlank(request.errorDescription()),
                blankToNullText(request.notes())
        ).trim();
    }

    private List<PlannedToolCall> parsePlannerResponse(String response) throws JsonProcessingException {
        if (isBlank(response)) {
            return List.of();
        }
        String json = extractJsonObject(response);
        JsonNode root = objectMapper.readTree(json);
        JsonNode toolCalls = root.get("toolCalls");
        if (toolCalls == null || !toolCalls.isArray()) {
            return List.of();
        }
        List<PlannedToolCall> plan = new ArrayList<>();
        for (JsonNode item : toolCalls) {
            String toolName = item.path("toolName").asText("");
            if (!isAllowedTool(toolName)) {
                continue;
            }
            JsonNode arguments = item.path("arguments");
            Map<String, Object> args = arguments.isObject()
                    ? objectMapper.convertValue(arguments, Map.class)
                    : Map.of();
            plan.add(new PlannedToolCall(toolName, args));
        }
        return plan;
    }

    private void executePlannedToolCalls(AgentRunContext context, List<PlannedToolCall> plan) {
        for (PlannedToolCall toolCall : plan) {
            executeTool(context, toolCall.toolName(), toolCall.arguments());
        }
    }

    private void executeFallbackPlan(AgentRunContext context) {
        Hot100AgentRunRequest request = context.request();
        String normalizedGoal = normalize(request.goal());

        executeTool(context, "getUserProgress", Map.of());
        executeTool(context, "getWeakTags", Map.of());
        executeTool(context, "getTagMastery", Map.of());

        if (!isBlank(request.problemSlug())) {
            executeTool(context, "getProblemDetail", Map.of("problemSlug", request.problemSlug()));
        }

        if (isProgressUpdateIntent(request)) {
            executeTool(context, "updateProgress", Map.of("problemSlug", request.problemSlug(), "status", request.status()));
        }

        if (isWrongAnalysisIntent(normalizedGoal, request)) {
            requireProblemSlug(request);
            executeTool(context, "analyzeWrongAnswer", Map.of("problemSlug", request.problemSlug()));
        }

        if (containsAny(normalizedGoal, "错题", "wrong", "薄弱", "weak")) {
            executeTool(context, "getWrongBook", Map.of());
        }

        if (containsAny(normalizedGoal, "解释", "题解", "知识", "面试", "复习", "explain", "solution", "knowledge", "review")) {
            executeTool(context, "retrieveKnowledge", Map.of("query", request.goal(), "limit", 5));
        }

        if (containsAny(normalizedGoal, "推荐", "下一题", "next", "recommend")) {
            int limit = limit(request);
            executeTool(context, "recommendNext", Map.of("limit", limit));
            executeTool(context, "aiRecommendations", Map.of("limit", limit));
        }

        if (containsAny(normalizedGoal, "计划", "plan", "7天", "14天", "study")) {
            executeTool(context, "generateStudyPlan", Map.of("days", days(request)));
        }
    }

    private Object executeTool(AgentRunContext context, String toolName, Map<String, Object> args) {
        Hot100AgentRunRequest request = context.request();
        return switch (toolName) {
            case "getUserProgress" ->
                    invokeTool(context, toolName, args, () -> hot100ProgressService.listProgress(context.userId()));
            case "getWeakTags" ->
                    invokeTool(context, toolName, args, () -> hot100ProgressService.weakTags(context.userId()));
            case "getTagMastery" ->
                    invokeTool(context, toolName, args, () -> hot100ProgressService.tagMastery(context.userId()));
            case "getProblemDetail" -> {
                String slug = stringArg(args, "problemSlug", request.problemSlug());
                if (isBlank(slug)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "problemSlug is required for getProblemDetail");
                }
                yield invokeTool(context, toolName, args, () -> hot100Service.getProblem(slug));
            }
            case "updateProgress" -> {
                String slug = stringArg(args, "problemSlug", request.problemSlug());
                String status = stringArg(args, "status", request.status());
                if (isBlank(slug) || isBlank(status)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "problemSlug and status are required for updateProgress");
                }
                yield invokeTool(context, toolName, Map.of("problemSlug", slug, "status", status),
                        () -> hot100ProgressService.upsert(new Hot100ProgressUpsertRequest(
                                slug,
                                status,
                                request.notes(),
                                null,
                                null,
                                null,
                                null
                        ), context.userId()));
            }
            case "analyzeWrongAnswer" -> {
                String slug = stringArg(args, "problemSlug", request.problemSlug());
                if (isBlank(slug)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "problemSlug is required for analyzeWrongAnswer");
                }
                yield invokeTool(context, toolName, Map.of("problemSlug", slug),
                        () -> hot100WrongAnalysisService.analyzeWrongAnswer(new Hot100WrongAnswerAnalyzeRequest(
                                slug,
                                request.userCode(),
                                request.errorDescription(),
                                request.notes()
                        ), context.userId()));
            }
            case "getWrongBook" ->
                    invokeTool(context, toolName, args, () -> hot100ProgressService.wrongBookAnalysis(context.userId()));
            case "recommendNext" -> {
                int limit = intArg(args, "limit", limit(request));
                yield invokeTool(context, toolName, Map.of("limit", limit),
                        () -> hot100ProgressService.recommendNext(limit, context.userId()));
            }
            case "aiRecommendations" -> {
                int limit = intArg(args, "limit", limit(request));
                yield invokeTool(context, toolName, Map.of("limit", limit),
                        () -> hot100ProgressService.aiRecommendations(limit, context.userId()));
            }
            case "generateStudyPlan" -> {
                int days = normalizeDays(intArg(args, "days", days(request)));
                yield invokeTool(context, toolName, Map.of("days", days),
                        () -> hot100ProgressService.buildStudyPlan(days, context.userId()));
            }
            case "searchProblems" -> {
                String keyword = stringArg(args, "keyword", null);
                String tag = stringArg(args, "tag", null);
                String difficulty = stringArg(args, "difficulty", null);
                yield invokeTool(context, toolName, args,
                        () -> hot100Service.listProblems(keyword, tag, difficulty));
            }
            case "retrieveKnowledge" -> {
                String query = stringArg(args, "query", request.goal());
                int limit = intArg(args, "limit", 5);
                yield invokeTool(context, toolName, Map.of("query", query, "limit", limit),
                        () -> agentKnowledgeService.retrieve(query, limit));
            }
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported agent tool: " + toolName);
        };
    }

    private Object invokeTool(AgentRunContext context,
                              String toolName,
                              Map<String, Object> input,
                              Supplier<Object> supplier) {
        int stepOrder = context.nextStepOrder();
        long startedAt = System.currentTimeMillis();
        AgentStep step = new AgentStep();
        step.setTaskId(context.taskId());
        step.setStepOrder(stepOrder);
        step.setToolName(toolName);
        step.setToolInput(toJson(input));
        try {
            Object output = supplier.get();
            step.setStatus(AgentStepStatus.SUCCESS.name());
            step.setToolOutput(truncate(toJson(output), 12000));
            step.setLatencyMs(System.currentTimeMillis() - startedAt);
            agentStepRepository.save(step);
            context.outputs().put(toolName, output);
            return output;
        } catch (Exception e) {
            step.setStatus(AgentStepStatus.FAILED.name());
            step.setErrorMessage(truncate(e.getMessage(), 1000));
            step.setLatencyMs(System.currentTimeMillis() - startedAt);
            agentStepRepository.save(step);
            throw e;
        }
    }

    private String buildFinalAnswer(AgentRunContext context) {
        Map<String, Object> outputs = context.outputs();
        StringBuilder answer = new StringBuilder();
        answer.append("Agent completed the Hot100 workflow for goal: ")
                .append(context.request().goal().trim())
                .append("\n\n");

        List<Hot100WeakTagView> weakTags = castList(outputs.get("getWeakTags"));
        if (weakTags != null && !weakTags.isEmpty()) {
            answer.append("Weak tags: ");
            answer.append(weakTags.stream()
                    .limit(5)
                    .map(item -> item.tag() + "(" + item.wrongCount() + ")")
                    .toList());
            answer.append("\n");
        }

        List<Hot100TagMasteryView> mastery = castList(outputs.get("getTagMastery"));
        if (mastery != null && !mastery.isEmpty()) {
            answer.append("Lowest mastery tags: ");
            answer.append(mastery.stream()
                    .limit(5)
                    .map(item -> item.tag() + ":" + Math.round(item.masteryRate() * 100) + "%")
                    .toList());
            answer.append("\n");
        }

        Hot100ProblemDetailView problem = cast(outputs.get("getProblemDetail"));
        if (problem != null) {
            answer.append("Focused problem: ")
                    .append(problem.title())
                    .append(" / ")
                    .append(problem.difficulty())
                    .append(" / ")
                    .append(problem.pattern())
                    .append("\n");
        }

        List<Hot100ProblemSummaryView> recommendations = castList(outputs.get("recommendNext"));
        if (recommendations != null && !recommendations.isEmpty()) {
            answer.append("Recommended next problems: ");
            answer.append(recommendations.stream()
                    .limit(5)
                    .map(Hot100ProblemSummaryView::title)
                    .toList());
            answer.append("\n");
        }

        Hot100AiRecommendationsView aiRecommendations = cast(outputs.get("aiRecommendations"));
        if (aiRecommendations != null && aiRecommendations.coachSummary() != null) {
            answer.append("Coach summary: ").append(aiRecommendations.coachSummary()).append("\n");
        }

        List<Hot100StudyPlanItemView> plan = castList(outputs.get("generateStudyPlan"));
        if (plan != null && !plan.isEmpty()) {
            answer.append("Study plan preview: ");
            answer.append(plan.stream()
                    .limit(5)
                    .map(item -> "Day " + item.day() + " " + item.title())
                    .toList());
            answer.append("\n");
        }

        List<KnowledgeSnippetView> snippets = castList(outputs.get("retrieveKnowledge"));
        if (snippets != null && !snippets.isEmpty()) {
            answer.append("Retrieved knowledge: ");
            answer.append(snippets.stream()
                    .limit(3)
                    .map(KnowledgeSnippetView::source)
                    .toList());
            answer.append("\n");
        }

        Object wrongAnalysis = outputs.get("analyzeWrongAnswer");
        if (wrongAnalysis != null) {
            answer.append("Wrong-answer analysis has been generated and saved into the wrong-book record.\n");
        }

        Object progress = outputs.get("updateProgress");
        if (progress instanceof Hot100ProgressView progressView) {
            answer.append("Progress updated: ")
                    .append(progressView.problemSlug())
                    .append(" -> ")
                    .append(progressView.status())
                    .append("\n");
        }

        List<Hot100WrongBookItemView> wrongBook = castList(outputs.get("getWrongBook"));
        if (wrongBook != null) {
            answer.append("Wrong-book size: ").append(wrongBook.size()).append("\n");
        }
        return answer.toString().trim();
    }

    private AgentTaskView toView(AgentTask task) {
        List<AgentStepView> steps = agentStepRepository.findByTaskIdOrderByStepOrderAsc(task.getTaskId()).stream()
                .map(AgentStepView::from)
                .toList();
        return AgentTaskView.from(task, steps);
    }

    private boolean isWrongAnalysisIntent(String normalizedGoal, Hot100AgentRunRequest request) {
        return containsAny(normalizedGoal, "错因", "错题分析", "为什么错", "wrong answer", "analyze")
                || !isBlank(request.userCode())
                || !isBlank(request.errorDescription());
    }

    private boolean isProgressUpdateIntent(Hot100AgentRunRequest request) {
        return !isBlank(request.problemSlug()) && !isBlank(request.status());
    }

    private void requireProblemSlug(Hot100AgentRunRequest request) {
        if (isBlank(request.problemSlug())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "problemSlug is required for wrong-answer analysis");
        }
    }

    private int limit(Hot100AgentRunRequest request) {
        return request.limit() == null ? DEFAULT_LIMIT : Math.max(1, Math.min(request.limit(), 20));
    }

    private int days(Hot100AgentRunRequest request) {
        if (request.days() == null) {
            return DEFAULT_DAYS;
        }
        return normalizeDays(request.days());
    }

    private int normalizeDays(int days) {
        return days <= 7 ? 7 : 14;
    }

    private boolean isAllowedTool(String toolName) {
        return switch (toolName) {
            case "getUserProgress",
                 "getWeakTags",
                 "getTagMastery",
                 "getProblemDetail",
                 "updateProgress",
                 "analyzeWrongAnswer",
                 "getWrongBook",
                 "recommendNext",
                 "aiRecommendations",
                 "generateStudyPlan",
                 "searchProblems",
                 "retrieveKnowledge" -> true;
            default -> false;
        };
    }

    private String stringArg(Map<String, Object> args, String key, String fallback) {
        Object value = args == null ? null : args.get(key);
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private int intArg(Map<String, Object> args, String key, int fallback) {
        Object value = args == null ? null : args.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String extractJsonObject(String value) {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Agent planner returned no JSON object");
        }
        return value.substring(start, end + 1);
    }

    private String blankToNullText(String value) {
        return isBlank(value) ? "null" : value.trim();
    }

    private String blankToPlaceholder(String value) {
        return isBlank(value) ? "(not provided)" : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... keywords) {
        if (value == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object value) {
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> castList(Object value) {
        if (value instanceof List<?> list) {
            return (List<T>) list;
        }
        return null;
    }

    private record PlannedToolCall(String toolName, Map<String, Object> arguments) {
    }

    private static class AgentRunContext {
        private final String taskId;
        private final Hot100AgentRunRequest request;
        private final Long userId;
        private final Map<String, Object> outputs = new LinkedHashMap<>();
        private int stepOrder = 0;

        private AgentRunContext(String taskId, Hot100AgentRunRequest request, Long userId) {
            this.taskId = taskId;
            this.request = request;
            this.userId = userId;
        }

        private String taskId() {
            return taskId;
        }

        private Hot100AgentRunRequest request() {
            return request;
        }

        private Long userId() {
            return userId;
        }

        private Map<String, Object> outputs() {
            return outputs;
        }

        private int nextStepOrder() {
            return ++stepOrder;
        }
    }
}
