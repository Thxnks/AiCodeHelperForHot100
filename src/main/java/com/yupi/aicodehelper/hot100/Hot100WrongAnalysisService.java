package com.yupi.aicodehelper.hot100;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.aicodehelper.ai.AiCodeHelperService;
import com.yupi.aicodehelper.agent.AgentMemoryService;
import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.entity.AiCallLog;
import com.yupi.aicodehelper.entity.Hot100ProblemProgress;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.repository.AiCallLogRepository;
import com.yupi.aicodehelper.repository.Hot100ProblemProgressRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class Hot100WrongAnalysisService {

    private static final String BUSINESS_TYPE = "HOT100_WRONG_ANALYSIS";

    private final Hot100Service hot100Service;

    private final Hot100ProblemProgressRepository progressRepository;

    private final AiCodeHelperService aiCodeHelperService;

    private final ObjectMapper objectMapper;

    private final AiCallLogRepository aiCallLogRepository;
    private final AgentMemoryService agentMemoryService;

    public Hot100WrongAnalysisService(Hot100Service hot100Service,
                                      Hot100ProblemProgressRepository progressRepository,
                                      AiCodeHelperService aiCodeHelperService,
                                      ObjectMapper objectMapper,
                                      AiCallLogRepository aiCallLogRepository,
                                      AgentMemoryService agentMemoryService) {
        this.hot100Service = hot100Service;
        this.progressRepository = progressRepository;
        this.aiCodeHelperService = aiCodeHelperService;
        this.objectMapper = objectMapper;
        this.aiCallLogRepository = aiCallLogRepository;
        this.agentMemoryService = agentMemoryService;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = Hot100CacheNames.RECOMMENDATION, allEntries = true),
            @CacheEvict(cacheNames = Hot100CacheNames.STUDY_PLAN, allEntries = true),
            @CacheEvict(cacheNames = Hot100CacheNames.TAG_MASTERY, key = "#userId")
    })
    public Hot100WrongAnswerAnalysisView analyzeWrongAnswer(Hot100WrongAnswerAnalyzeRequest request, Long userId) {
        validateRequest(request);
        Hot100ProblemDetailView problem = hot100Service.getProblem(request.problemSlug());
        String prompt = buildWrongAnswerAnalysisPrompt(problem, request);
        String requestHash = sha256(problem.slug() + "\n" + prompt);

        long startedAt = System.currentTimeMillis();
        boolean repaired = false;
        boolean fallbackUsed = false;
        boolean success = false;
        String errorMessage = null;
        Hot100WrongAnswerAnalysisView analysis;

        try {
            String aiResponse = aiCodeHelperService.analyzeHot100WrongAnswer(prompt);
            try {
                analysis = parseWrongAnswerAnalysis(problem.slug(), aiResponse);
                success = true;
            } catch (BusinessException parseError) {
                repaired = true;
                String repairedResponse = aiCodeHelperService.repairHot100WrongAnalysisJson(buildRepairPrompt(aiResponse, parseError.getMessage()));
                analysis = parseWrongAnswerAnalysis(problem.slug(), repairedResponse);
                success = true;
            }
        } catch (Exception e) {
            fallbackUsed = true;
            errorMessage = e.getMessage();
            analysis = fallbackAnalysis(problem, request);
        }

        saveAnalysis(userId, problem.slug(), request.notes(), analysis);
        recordAiCall(userId, problem.slug(), requestHash, System.currentTimeMillis() - startedAt,
                success, repaired, fallbackUsed, errorMessage);
        return analysis;
    }

    private void validateRequest(Hot100WrongAnswerAnalyzeRequest request) {
        if (request == null || request.problemSlug() == null || request.problemSlug().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "problemSlug cannot be blank");
        }
        if (isBlank(request.userCode()) && isBlank(request.errorDescription()) && isBlank(request.notes())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "userCode, errorDescription or notes is required");
        }
    }

    private void saveAnalysis(Long userId, String problemSlug, String notes, Hot100WrongAnswerAnalysisView analysis) {
        Hot100ProblemProgress entity = progressRepository.findByUserIdAndProblemSlug(userId, problemSlug)
                .orElseGet(Hot100ProblemProgress::new);
        entity.setUserId(userId);
        entity.setProblemSlug(problemSlug);
        entity.setStatus(Hot100ProgressStatus.WRONG.name());
        entity.setNotes(notes);
        entity.setWrongReason(analysis.wrongReason());
        entity.setKnowledgePoint(analysis.knowledgePoint());
        entity.setAiFeedback(analysis.aiFeedback());
        entity.setNextAction(analysis.nextAction());
        entity.setLastReviewedAt(LocalDateTime.now());
        Hot100ProblemProgress saved = progressRepository.save(entity);
        agentMemoryService.rememberProgress(
                userId,
                saved.getProblemSlug(),
                saved.getStatus(),
                saved.getNotes(),
                saved.getKnowledgePoint(),
                saved.getWrongReason(),
                saved.getNextAction()
        );
    }

    private void recordAiCall(Long userId,
                              String businessKey,
                              String requestHash,
                              long latencyMs,
                              boolean success,
                              boolean repaired,
                              boolean fallbackUsed,
                              String errorMessage) {
        AiCallLog log = new AiCallLog();
        log.setUserId(userId);
        log.setBusinessType(BUSINESS_TYPE);
        log.setBusinessKey(businessKey);
        log.setRequestHash(requestHash);
        log.setLatencyMs(latencyMs);
        log.setSuccess(success);
        log.setRepaired(repaired);
        log.setFallbackUsed(fallbackUsed);
        log.setErrorMessage(truncate(errorMessage, 1000));
        aiCallLogRepository.save(log);
    }

    private String buildWrongAnswerAnalysisPrompt(Hot100ProblemDetailView problem,
                                                  Hot100WrongAnswerAnalyzeRequest request) {
        return """
                Problem context:
                - title: %s
                - slug: %s
                - difficulty: %s
                - tags: %s
                - pattern: %s
                - summary: %s
                - core idea: %s
                - common pitfalls: %s
                - complexity: %s

                User wrong-answer evidence:
                - user code:
                %s

                - error description:
                %s

                - user notes:
                %s

                Analyze the most likely wrong reason and return JSON only.
                """.formatted(
                problem.title(),
                problem.slug(),
                problem.difficulty(),
                problem.tags(),
                problem.pattern(),
                problem.summary(),
                problem.coreIdea(),
                problem.pitfalls(),
                problem.complexity(),
                blankToPlaceholder(request.userCode()),
                blankToPlaceholder(request.errorDescription()),
                blankToPlaceholder(request.notes())
        ).trim();
    }

    private String buildRepairPrompt(String rawResponse, String parseError) {
        return """
                Parse error:
                %s

                Original model output:
                %s

                Repair it into the required JSON schema.
                """.formatted(blankToPlaceholder(parseError), blankToPlaceholder(rawResponse));
    }

    private Hot100WrongAnswerAnalysisView parseWrongAnswerAnalysis(String problemSlug, String aiResponse) {
        String json = extractJsonObject(aiResponse);
        try {
            Hot100WrongAnswerAnalysisPayload payload = objectMapper.readValue(json, Hot100WrongAnswerAnalysisPayload.class);
            return new Hot100WrongAnswerAnalysisView(
                    problemSlug,
                    requireAnalysisField(payload.wrongReason(), "wrongReason"),
                    requireAnalysisField(payload.knowledgePoint(), "knowledgePoint"),
                    requireAnalysisField(payload.aiFeedback(), "aiFeedback"),
                    requireAnalysisField(payload.nextAction(), "nextAction")
            );
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI wrong-answer analysis returned invalid JSON");
        }
    }

    private Hot100WrongAnswerAnalysisView fallbackAnalysis(Hot100ProblemDetailView problem,
                                                           Hot100WrongAnswerAnalyzeRequest request) {
        String evidence = firstNonBlank(request.errorDescription(), request.notes(), "用户提交了错误代码但模型分析暂不可用");
        return new Hot100WrongAnswerAnalysisView(
                problem.slug(),
                "AI 分析暂不可用，已基于题目模式和用户描述生成保守错因：" + truncate(evidence, 160),
                blankToPlaceholder(problem.pattern()),
                "建议先对照题目的核心模式和常见错误检查边界条件、状态更新顺序和重复元素处理。",
                "重新阅读题目核心思路，补充 2-3 个边界测试用例后再提交代码。"
        );
    }

    private String extractJsonObject(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI wrong-answer analysis returned empty result");
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI wrong-answer analysis returned no JSON object");
        }
        return value.substring(start, end + 1);
    }

    private String requireAnalysisField(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI wrong-answer analysis missed field: " + fieldName);
        }
        return value.trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String blankToPlaceholder(String value) {
        return isBlank(value) ? "(not provided)" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Hot100WrongAnswerAnalysisPayload(
            String wrongReason,
            String knowledgePoint,
            String aiFeedback,
            String nextAction
    ) {
    }
}
