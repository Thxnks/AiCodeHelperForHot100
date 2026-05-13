package com.yupi.aicodehelper.agent.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class AgentLoopService {

    private static final int DEFAULT_MAX_TURNS = 8;
    private static final int MAX_MESSAGES_BEFORE_COMPACT = 12;
    private static final int MAX_RENDERED_MESSAGES_CHARS = 16000;

    private final ObjectMapper objectMapper;
    private final SkillCatalogService skillCatalogService;
    private final AgentPermissionGate permissionGate;
    private final AgentHookManager hookManager;
    private final AgentPromptBuilder promptBuilder;
    private final AgentRecoveryPolicy recoveryPolicy;
    private final TaskGraphService taskGraphService;

    public AgentLoopService(ObjectMapper objectMapper) {
        this(objectMapper, SkillCatalogService.of(Map.of()), new AgentPermissionGate(), new AgentHookManager(),
                new AgentPromptBuilder(objectMapper), new AgentRecoveryPolicy(),
                new TaskGraphService(new InMemoryTaskBoard()));
    }

    public AgentLoopService(ObjectMapper objectMapper, SkillCatalogService skillCatalogService) {
        this(objectMapper, skillCatalogService, new AgentPermissionGate(), new AgentHookManager(),
                new AgentPromptBuilder(objectMapper), new AgentRecoveryPolicy(),
                new TaskGraphService(new InMemoryTaskBoard()));
    }

    public AgentLoopService(ObjectMapper objectMapper,
                            SkillCatalogService skillCatalogService,
                            AgentPermissionGate permissionGate) {
        this(objectMapper, skillCatalogService, permissionGate, new AgentHookManager(),
                new AgentPromptBuilder(objectMapper), new AgentRecoveryPolicy(),
                new TaskGraphService(new InMemoryTaskBoard()));
    }

    public AgentLoopService(ObjectMapper objectMapper,
                            SkillCatalogService skillCatalogService,
                            AgentPermissionGate permissionGate,
                            AgentHookManager hookManager) {
        this(objectMapper, skillCatalogService, permissionGate, hookManager, new AgentPromptBuilder(objectMapper),
                new AgentRecoveryPolicy(), new TaskGraphService(new InMemoryTaskBoard()));
    }

    @Autowired
    public AgentLoopService(ObjectMapper objectMapper,
                            SkillCatalogService skillCatalogService,
                            AgentPermissionGate permissionGate,
                            AgentHookManager hookManager,
                            AgentPromptBuilder promptBuilder,
                            AgentRecoveryPolicy recoveryPolicy,
                            TaskBoard taskBoard) {
        this(objectMapper, skillCatalogService, permissionGate, hookManager, promptBuilder, recoveryPolicy,
                new TaskGraphService(taskBoard));
    }

    public AgentLoopService(ObjectMapper objectMapper,
                            SkillCatalogService skillCatalogService,
                            AgentPermissionGate permissionGate,
                            AgentHookManager hookManager,
                            AgentPromptBuilder promptBuilder,
                            AgentRecoveryPolicy recoveryPolicy,
                            TaskGraphService taskGraphService) {
        this.objectMapper = objectMapper;
        this.skillCatalogService = skillCatalogService;
        this.permissionGate = permissionGate;
        this.hookManager = hookManager;
        this.promptBuilder = promptBuilder;
        this.recoveryPolicy = recoveryPolicy;
        this.taskGraphService = taskGraphService;
    }

    public AgentLoopState run(String userGoal,
                              AgentToolRegistry toolRegistry,
                              AgentTurnClient turnClient,
                              AgentLoopObserver observer) {
        return run(userGoal, toolRegistry, turnClient, observer, DEFAULT_MAX_TURNS);
    }

    public AgentLoopState run(String userGoal,
                              AgentToolRegistry toolRegistry,
                              AgentTurnClient turnClient,
                              AgentLoopObserver observer,
                              AgentPermissionContext permissionContext) {
        return run(userGoal, toolRegistry, turnClient, observer, permissionContext, DEFAULT_MAX_TURNS);
    }

    public AgentLoopState run(String userGoal,
                              AgentToolRegistry toolRegistry,
                              AgentTurnClient turnClient,
                              AgentLoopObserver observer,
                              int maxTurns) {
        return run(userGoal, toolRegistry, turnClient, observer, AgentPermissionContext.readOnly(), maxTurns);
    }

    public AgentLoopState run(String userGoal,
                              AgentToolRegistry toolRegistry,
                              AgentTurnClient turnClient,
                              AgentLoopObserver observer,
                              AgentPermissionContext permissionContext,
                              int maxTurns) {
        AgentLoopState state = new AgentLoopState(userGoal);
        registerTodoTools(toolRegistry, state);
        registerSkillTools(toolRegistry);
        registerTaskTools(toolRegistry);
        AgentLoopObserver safeObserver = observer == null ? AgentLoopObserver.NOOP : observer;

        while (!state.finished() && state.turnCount() < maxTurns) {
            state.incrementTurnCount();
            compactIfNeeded(state);
            String prompt = promptBuilder.build(new AgentPromptContext(state, toolRegistry));
            publishHook(AgentHookEventType.BEFORE_MODEL_TURN, state.turnCount(), null, Map.of(
                    "promptLength", prompt.length()
            ));
            long modelStartedAt = System.currentTimeMillis();
            String modelOutput = turnClient.nextTurn(prompt);
            long modelLatencyMs = System.currentTimeMillis() - modelStartedAt;
            safeObserver.onModelTurn(state.turnCount(), prompt, modelOutput, modelLatencyMs);
            publishHook(AgentHookEventType.AFTER_MODEL_TURN, state.turnCount(), null, Map.of(
                    "promptLength", prompt.length(),
                    "outputLength", modelOutput == null ? 0 : modelOutput.length(),
                    "latencyMs", modelLatencyMs
            ));
            state.messages().add(new AgentMessage("assistant", modelOutput));

            AgentTurnDecision decision = parseDecision(modelOutput);
            if (decision.recoveryDecision() != null) {
                applyRecovery(state, decision.recoveryDecision());
                continue;
            }
            if (decision.finalAnswer() != null) {
                state.finish(decision.finalAnswer());
                break;
            }

            ToolUseBlock toolUse = decision.toolUse();
            if (toolUse == null) {
                state.finish(modelOutput);
                break;
            }

            RegisteredAgentTool tool = toolRegistry.find(toolUse.name()).orElse(null);
            if (tool == null) {
                applyToolRecovery(state, toolUse, recoveryPolicy.unknownTool(toolUse));
                continue;
            }

            long toolStartedAt = System.currentTimeMillis();
            publishHook(AgentHookEventType.BEFORE_TOOL_CALL, state.turnCount(), toolUse.name(), Map.of(
                    "input", toolUse.input(),
                    "permissionLevel", tool.spec().permissionLevel()
            ));
            AgentPermissionDecision permissionDecision = permissionGate.check(tool, permissionContext);
            if (!permissionDecision.allowed()) {
                AgentPermissionDeniedException error = new AgentPermissionDeniedException(
                        "Permission denied for tool " + toolUse.name() + ": " + permissionDecision.reason()
                );
                safeObserver.onToolError(state.turnCount(), toolUse.name(), toolUse.input(), error,
                        System.currentTimeMillis() - toolStartedAt);
                publishHook(AgentHookEventType.ON_PERMISSION_DENIED, state.turnCount(), toolUse.name(), Map.of(
                        "input", toolUse.input(),
                        "permissionLevel", tool.spec().permissionLevel(),
                        "reason", permissionDecision.reason()
                ));
                appendToolResult(state, toolUse, Map.of(
                        "error", "permission_denied",
                        "toolName", toolUse.name(),
                        "permissionLevel", tool.spec().permissionLevel(),
                        "reason", permissionDecision.reason()
                ));
                state.transitionReason("tool_result");
                continue;
            }
            try {
                Object output = tool.handler().handle(toolUse.input());
                long toolLatencyMs = System.currentTimeMillis() - toolStartedAt;
                safeObserver.onToolResult(state.turnCount(), toolUse.name(), toolUse.input(), output,
                        toolLatencyMs);
                publishHook(AgentHookEventType.AFTER_TOOL_CALL, state.turnCount(), toolUse.name(), Map.of(
                        "input", toolUse.input(),
                        "output", output,
                        "latencyMs", toolLatencyMs
                ));
                appendToolResult(state, toolUse, output);
            } catch (Exception e) {
                long toolLatencyMs = System.currentTimeMillis() - toolStartedAt;
                safeObserver.onToolError(state.turnCount(), toolUse.name(), toolUse.input(), e,
                        toolLatencyMs);
                publishHook(AgentHookEventType.ON_TOOL_ERROR, state.turnCount(), toolUse.name(), Map.of(
                        "input", toolUse.input(),
                        "error", blankToPlaceholder(e.getMessage()),
                        "latencyMs", toolLatencyMs
                ));
                applyToolRecovery(state, toolUse, recoveryPolicy.toolError(toolUse, e));
            }
            state.transitionReason("tool_result");
        }

        if (!state.finished()) {
            AgentRecoveryDecision recoveryDecision = recoveryPolicy.maxTurns(maxTurns);
            publishRecoveryHook(state.turnCount(), null, recoveryDecision);
            state.finish(String.valueOf(recoveryDecision.content()));
        }
        return state;
    }

    private void registerTodoTools(AgentToolRegistry toolRegistry, AgentLoopState state) {
        toolRegistry
                .register("todo_read", "Read the current in-session todo list. Input: {}.", input ->
                        new TodoWriteResult(List.copyOf(state.todos())))
                .register("todo_write", "Replace the in-session todo list. Input: {\"todos\":[{\"content\":\"...\",\"status\":\"PENDING|IN_PROGRESS|COMPLETED\"}]}.", input -> {
                    List<TodoItem> todos = parseTodos(input);
                    state.replaceTodos(todos);
                    return new TodoWriteResult(List.copyOf(state.todos()));
                });
    }

    private void registerSkillTools(AgentToolRegistry toolRegistry) {
        toolRegistry
                .register("list_skills", "List locally available skills. Input: {}.", input ->
                        skillCatalogService.listSkills())
                .register("load_skill", "Load one local skill by name. Input: {\"name\":\"skill-name\"}.", input ->
                        skillCatalogService.loadSkill(stringArg(input, "name")));
    }

    private void registerTaskTools(AgentToolRegistry toolRegistry) {
        toolRegistry
                .register("task_create", "Create one persistent task. Input: subject, optional description/owner/blockedBy[].", input -> {
                    String subject = stringArg(input, "subject");
                    if (subject.isBlank()) {
                        throw new IllegalArgumentException("subject is required for task_create");
                    }
                    TaskRecord task = taskGraphService.create(
                            subject,
                            stringArg(input, "description"),
                            stringArg(input, "owner"),
                            longListArg(input, "blockedBy")
                    );
                    return taskToMap(task);
                })
                .register("task_update", "Update a persistent task. Input: id plus optional status/subject/description/owner/addBlocks/removeBlocks/blockedBy.", input -> {
                    long id = longArg(input, "id");
                    TaskRecord task = taskGraphService.update(
                            id,
                            nullableStringArg(input, "subject"),
                            nullableStringArg(input, "description"),
                            nullableStringArg(input, "owner"),
                            parseTaskStatus(nullableStringArg(input, "status")),
                            nullableLongListArg(input, "addBlocks"),
                            nullableLongListArg(input, "removeBlocks"),
                            nullableLongListArg(input, "blockedBy")
                    );
                    return taskToMap(task);
                })
                .register("task_get", "Get one persistent task by id. Input: id.", input -> {
                    long id = longArg(input, "id");
                    return taskToMap(taskGraphService.get(id));
                })
                .register("task_list", "List persistent tasks. Input: optional includeDeleted.", input ->
                        taskGraphService.list(booleanArg(input, "includeDeleted", false)));
    }

    private void compactIfNeeded(AgentLoopState state) {
        String renderedMessages = promptBuilder.renderMessages(state);
        if (state.messages().size() <= MAX_MESSAGES_BEFORE_COMPACT
                && renderedMessages.length() <= MAX_RENDERED_MESSAGES_CHARS) {
            return;
        }
        state.compact(buildCompactSummary(state));
        publishHook(AgentHookEventType.ON_COMPACT, state.turnCount(), null, Map.of(
                "messageCount", state.messages().size(),
                "summaryLength", state.compactSummary() == null ? 0 : state.compactSummary().content().length()
        ));
    }

    private String buildCompactSummary(AgentLoopState state) {
        StringBuilder summary = new StringBuilder();
        summary.append("Turns completed: ").append(state.turnCount()).append("\n");
        summary.append("Todos: ").append(promptBuilder.renderTodos(state)).append("\n");
        summary.append("Recent observations:\n");
        state.messages().stream()
                .skip(Math.max(0, state.messages().size() - 8))
                .forEach(message -> summary.append("- ")
                        .append(message.role())
                        .append(": ")
                        .append(truncate(message.content(), 500))
                        .append("\n"));
        return summary.toString().trim();
    }

    private AgentTurnDecision parseDecision(String modelOutput) {
        try {
            String json = extractJsonObject(modelOutput);
            JsonNode root = objectMapper.readTree(json);
            String type = root.path("type").asText("");
            if ("final_answer".equals(type)) {
                return AgentTurnDecision.finalAnswer(root.path("content").asText(""));
            }
            if ("tool_use".equals(type)) {
                String id = root.path("id").asText("");
                String name = root.path("name").asText("");
                Map<String, Object> input = root.path("input").isObject()
                        ? objectMapper.convertValue(root.path("input"), Map.class)
                        : new LinkedHashMap<>();
                if (id.isBlank()) {
                    id = "toolu_" + UUID.randomUUID().toString().replace("-", "");
                }
                return AgentTurnDecision.toolUse(new ToolUseBlock(id, name, input));
            }
            return AgentTurnDecision.recovery(recoveryPolicy.invalidModelOutput(modelOutput,
                    "Unknown response type: " + type));
        } catch (Exception e) {
            return AgentTurnDecision.recovery(recoveryPolicy.invalidModelOutput(modelOutput,
                    blankToPlaceholder(e.getMessage())));
        }
    }

    private List<TodoItem> parseTodos(Map<String, Object> input) {
        Object rawTodos = input == null ? null : input.get("todos");
        if (!(rawTodos instanceof List<?> rawList)) {
            return List.of();
        }
        List<TodoItem> todos = new ArrayList<>();
        for (Object rawItem : rawList) {
            if (!(rawItem instanceof Map<?, ?> rawMap)) {
                continue;
            }
            Object rawContent = rawMap.get("content");
            if (rawContent == null || String.valueOf(rawContent).isBlank()) {
                continue;
            }
            TodoStatus status = parseTodoStatus(rawMap.get("status"));
            todos.add(new TodoItem(String.valueOf(rawContent).trim(), status));
        }
        return todos;
    }

    private TodoStatus parseTodoStatus(Object value) {
        if (value == null) {
            return TodoStatus.PENDING;
        }
        try {
            return TodoStatus.valueOf(String.valueOf(value).trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return TodoStatus.PENDING;
        }
    }

    private String stringArg(Map<String, Object> input, String key) {
        Object value = input == null ? null : input.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String nullableStringArg(Map<String, Object> input, String key) {
        Object value = input == null ? null : input.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private long longArg(Map<String, Object> input, String key) {
        String value = stringArg(input, key);
        if (value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a number");
        }
    }

    private List<Long> longListArg(Map<String, Object> input, String key) {
        List<Long> value = nullableLongListArg(input, key);
        return value == null ? List.of() : value;
    }

    private List<Long> nullableLongListArg(Map<String, Object> input, String key) {
        Object raw = input == null ? null : input.get(key);
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof List<?> rawList)) {
            throw new IllegalArgumentException(key + " must be an array");
        }
        List<Long> result = new ArrayList<>();
        for (Object item : rawList) {
            if (item == null) {
                continue;
            }
            try {
                result.add(Long.parseLong(String.valueOf(item)));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(key + " contains non-number item");
            }
        }
        return result;
    }

    private boolean booleanArg(Map<String, Object> input, String key, boolean defaultValue) {
        Object raw = input == null ? null : input.get(key);
        if (raw == null) {
            return defaultValue;
        }
        if (raw instanceof Boolean value) {
            return value;
        }
        return Boolean.parseBoolean(String.valueOf(raw));
    }

    private TaskStatus parseTaskStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return TaskStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("status must be one of PENDING|IN_PROGRESS|COMPLETED|DELETED");
        }
    }

    private Map<String, Object> taskToMap(TaskRecord task) {
        return Map.of(
                "id", task.getId(),
                "subject", Objects.toString(task.getSubject(), ""),
                "description", Objects.toString(task.getDescription(), ""),
                "status", task.getStatus(),
                "blockedBy", List.copyOf(task.getBlockedBy()),
                "blocks", List.copyOf(task.getBlocks()),
                "owner", Objects.toString(task.getOwner(), ""),
                "ready", task.isReady()
        );
    }

    private void appendToolResult(AgentLoopState state, ToolUseBlock toolUse, Object output) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("type", "tool_result");
        block.put("tool_use_id", toolUse.id());
        block.put("name", toolUse.name());
        block.put("content", output);
        state.messages().add(new AgentMessage("user", toJson(block)));
    }

    private void applyRecovery(AgentLoopState state, AgentRecoveryDecision recoveryDecision) {
        publishRecoveryHook(state.turnCount(), null, recoveryDecision);
        state.messages().add(new AgentMessage("user", toJson(recoveryDecision.content())));
        state.transitionReason("recovery");
    }

    private void applyToolRecovery(AgentLoopState state,
                                   ToolUseBlock toolUse,
                                   AgentRecoveryDecision recoveryDecision) {
        publishRecoveryHook(state.turnCount(), toolUse.name(), recoveryDecision);
        appendToolResult(state, toolUse, recoveryDecision.content());
        state.transitionReason("tool_result");
    }

    private String extractJsonObject(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Model output is blank");
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Model output has no JSON object");
        }
        return value.substring(start, end + 1);
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

    private String blankToPlaceholder(String value) {
        return value == null || value.isBlank() ? "(no error message)" : value;
    }

    private void publishHook(AgentHookEventType type, int turn, String toolName, Map<String, Object> payload) {
        hookManager.publish(AgentHookEvent.of(type, turn, toolName, payload));
    }

    private void publishRecoveryHook(int turn, String toolName, AgentRecoveryDecision recoveryDecision) {
        publishHook(AgentHookEventType.ON_RECOVERY, turn, toolName, Map.of(
                "type", recoveryDecision.type(),
                "retry", recoveryDecision.retry(),
                "reason", recoveryDecision.reason()
        ));
    }

    private record AgentTurnDecision(ToolUseBlock toolUse,
                                     String finalAnswer,
                                     AgentRecoveryDecision recoveryDecision) {

        static AgentTurnDecision toolUse(ToolUseBlock toolUse) {
            return new AgentTurnDecision(toolUse, null, null);
        }

        static AgentTurnDecision finalAnswer(String finalAnswer) {
            return new AgentTurnDecision(null, finalAnswer, null);
        }

        static AgentTurnDecision recovery(AgentRecoveryDecision recoveryDecision) {
            return new AgentTurnDecision(null, null, recoveryDecision);
        }
    }
}
