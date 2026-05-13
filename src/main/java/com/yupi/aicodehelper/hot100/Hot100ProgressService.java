package com.yupi.aicodehelper.hot100;

import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.agent.AgentMemoryService;
import com.yupi.aicodehelper.entity.Hot100ProblemProgress;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.repository.Hot100ProblemProgressRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class Hot100ProgressService {

    private final Hot100ProblemProgressRepository progressRepository;

    private final Hot100Service hot100Service;

    private final Hot100ProblemLoader hot100ProblemLoader;
    private final AgentMemoryService agentMemoryService;

    public Hot100ProgressService(Hot100ProblemProgressRepository progressRepository,
                                 Hot100Service hot100Service,
                                 Hot100ProblemLoader hot100ProblemLoader,
                                 AgentMemoryService agentMemoryService) {
        this.progressRepository = progressRepository;
        this.hot100Service = hot100Service;
        this.hot100ProblemLoader = hot100ProblemLoader;
        this.agentMemoryService = agentMemoryService;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = Hot100CacheNames.RECOMMENDATION, allEntries = true),
            @CacheEvict(cacheNames = Hot100CacheNames.STUDY_PLAN, allEntries = true),
            @CacheEvict(cacheNames = Hot100CacheNames.TAG_MASTERY, key = "#userId")
    })
    public Hot100ProgressView upsert(Hot100ProgressUpsertRequest request, Long userId) {
        if (request == null || request.problemSlug() == null || request.problemSlug().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "problemSlug cannot be blank");
        }
        hot100Service.getProblem(request.problemSlug());
        Hot100ProgressStatus status;
        try {
            status = Hot100ProgressStatus.from(request.status());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status must be NOT_STARTED/COMPLETED/WRONG/MASTERED");
        }

        Hot100ProblemProgress entity = progressRepository.findByUserIdAndProblemSlug(userId, request.problemSlug())
                .orElseGet(Hot100ProblemProgress::new);
        entity.setUserId(userId);
        entity.setProblemSlug(request.problemSlug());
        entity.setStatus(status.name());
        entity.setNotes(request.notes());
        entity.setWrongReason(request.wrongReason());
        entity.setKnowledgePoint(request.knowledgePoint());
        entity.setAiFeedback(request.aiFeedback());
        entity.setNextAction(request.nextAction());
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
        return Hot100ProgressView.from(saved);
    }

    public List<Hot100ProgressView> listProgress(Long userId) {
        return progressRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(Hot100ProgressView::from)
                .toList();
    }

    public List<Hot100ProblemSummaryView> wrongBook(Long userId) {
        List<Hot100ProblemProgress> wrong = progressRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(
                userId, Hot100ProgressStatus.WRONG.name());
        List<Hot100ProblemSummaryView> result = new ArrayList<>();
        for (Hot100ProblemProgress p : wrong) {
            Hot100ProblemSummaryView summary = findSummaryBySlug(p.getProblemSlug());
            if (summary != null) {
                result.add(summary);
            }
        }
        return result;
    }

    public List<Hot100WrongBookItemView> wrongBookAnalysis(Long userId) {
        List<Hot100ProblemProgress> wrong = progressRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(
                userId, Hot100ProgressStatus.WRONG.name());
        List<Hot100WrongBookItemView> result = new ArrayList<>();
        for (Hot100ProblemProgress p : wrong) {
            Hot100ProblemSummaryView summary = findSummaryBySlug(p.getProblemSlug());
            if (summary != null) {
                result.add(new Hot100WrongBookItemView(
                        summary,
                        p.getWrongReason(),
                        p.getKnowledgePoint(),
                        p.getAiFeedback(),
                        p.getNextAction(),
                        p.getNotes(),
                        p.getUpdatedAt()
                ));
            }
        }
        return result;
    }

    public List<Hot100WeakTagView> weakTags(Long userId) {
        List<Hot100ProblemProgress> wrong = progressRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(
                userId, Hot100ProgressStatus.WRONG.name());
        Map<String, Long> counter = new HashMap<>();
        for (Hot100ProblemProgress p : wrong) {
            Hot100ProblemSummaryView summary = findSummaryBySlug(p.getProblemSlug());
            if (summary == null || summary.tags() == null) {
                continue;
            }
            for (String tag : summary.tags()) {
                counter.put(tag, counter.getOrDefault(tag, 0L) + 1);
            }
        }
        return counter.entrySet().stream()
                .map(entry -> new Hot100WeakTagView(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(Hot100WeakTagView::wrongCount).reversed())
                .toList();
    }

    @Cacheable(cacheNames = Hot100CacheNames.RECOMMENDATION, key = "#userId + '|' + #limit")
    public List<Hot100ProblemSummaryView> recommendNext(int limit, Long userId) {
        int realLimit = Math.max(1, Math.min(limit, 20));
        Set<String> finished = collectFinishedSlugs(userId);
        List<Hot100WeakTagView> weakTags = weakTags(userId);

        List<Hot100ProblemSummaryView> recommendations = new ArrayList<>();
        Set<String> selected = new HashSet<>();

        for (Hot100WeakTagView weakTag : weakTags) {
            List<Hot100ProblemSummaryView> byTag = hot100Service.listProblems(null, weakTag.tag(), null);
            for (Hot100ProblemSummaryView summary : byTag) {
                if (finished.contains(summary.slug()) || selected.contains(summary.slug())) {
                    continue;
                }
                recommendations.add(summary);
                selected.add(summary.slug());
                if (recommendations.size() >= realLimit) {
                    return recommendations;
                }
            }
        }

        for (Hot100Problem problem : hot100ProblemLoader.listAll()) {
            if (finished.contains(problem.getSlug()) || selected.contains(problem.getSlug())) {
                continue;
            }
            recommendations.add(Hot100ProblemSummaryView.from(problem));
            selected.add(problem.getSlug());
            if (recommendations.size() >= realLimit) {
                break;
            }
        }
        return recommendations;
    }

    public Hot100AiRecommendationsView aiRecommendations(int limit, Long userId) {
        int realLimit = Math.max(1, Math.min(limit, 20));
        List<Hot100TagMasteryView> mastery = tagMastery(userId);
        List<String> weakTagNames = pickWeakTags(mastery);
        List<String> masteredTagNames = mastery.stream()
                .filter(tag -> tag.practicedCount() > 0)
                .filter(tag -> tag.masteryRate() >= 0.8D && tag.wrongCount() == 0)
                .sorted(Comparator.comparingDouble(Hot100TagMasteryView::masteryRate).reversed())
                .limit(5)
                .map(Hot100TagMasteryView::tag)
                .toList();

        List<Hot100WrongBookItemView> wrongItems = wrongBookAnalysis(userId);
        List<String> recentWrongProblems = wrongItems.stream()
                .limit(5)
                .map(item -> item.problem().slug())
                .toList();

        List<Hot100ProblemSummaryView> candidates = recommendNext(realLimit, userId);
        List<Hot100RecommendedProblemView> recommendedProblems = candidates.stream()
                .map(problem -> Hot100RecommendedProblemView.from(
                        problem,
                        buildRecommendationReason(problem, weakTagNames, wrongItems),
                        buildTrainingFocus(problem, weakTagNames)
                ))
                .toList();

        String coachSummary = buildCoachSummary(weakTagNames, masteredTagNames, recentWrongProblems);
        List<Hot100StudyPlanItemView> trainingPlan = buildStudyPlan(Math.min(Math.max(realLimit, 7), 14), userId);
        return new Hot100AiRecommendationsView(
                weakTagNames,
                masteredTagNames,
                recentWrongProblems,
                coachSummary,
                recommendedProblems,
                trainingPlan
        );
    }

    @Cacheable(cacheNames = Hot100CacheNames.STUDY_PLAN, key = "#userId + '|' + #days")
    public List<Hot100StudyPlanItemView> buildStudyPlan(int days, Long userId) {
        int realDays = days <= 7 ? 7 : 14;
        List<Hot100ProblemSummaryView> candidates = recommendNext(realDays, userId);
        List<Hot100StudyPlanItemView> plan = new ArrayList<>();
        for (int i = 0; i < realDays; i++) {
            Hot100ProblemSummaryView problem = i < candidates.size() ? candidates.get(i) : null;
            if (problem == null) {
                plan.add(new Hot100StudyPlanItemView(i + 1, "-", "Review wrong answers", "-", "Review"));
                continue;
            }
            String focus = switch (problem.difficulty().toLowerCase()) {
                case "easy" -> "Consolidate basics and coding speed";
                case "hard" -> "Train complexity analysis and edge handling";
                default -> "Strengthen pattern transfer and interview expression";
            };
            plan.add(new Hot100StudyPlanItemView(i + 1, problem.slug(), problem.title(), problem.difficulty(), focus));
        }
        return plan;
    }

    @Cacheable(cacheNames = Hot100CacheNames.TAG_MASTERY, key = "#userId")
    public List<Hot100TagMasteryView> tagMastery(Long userId) {
        List<Hot100Problem> allProblems = hot100ProblemLoader.listAll();
        Map<String, Hot100ProblemProgress> progressMap = progressRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Hot100ProblemProgress::getProblemSlug, p -> p, (a, b) -> a));

        Map<String, Integer> totalByTag = new HashMap<>();
        Map<String, Integer> practicedByTag = new HashMap<>();
        Map<String, Integer> masteredByTag = new HashMap<>();
        Map<String, Integer> completedByTag = new HashMap<>();
        Map<String, Integer> wrongByTag = new HashMap<>();

        for (Hot100Problem problem : allProblems) {
            List<String> tags = problem.getTags();
            if (tags == null || tags.isEmpty()) {
                continue;
            }
            Hot100ProblemProgress progress = progressMap.get(problem.getSlug());
            for (String tag : tags) {
                if (tag == null || tag.isBlank()) {
                    continue;
                }
                totalByTag.put(tag, totalByTag.getOrDefault(tag, 0) + 1);
                if (progress != null) {
                    practicedByTag.put(tag, practicedByTag.getOrDefault(tag, 0) + 1);
                    if (Hot100ProgressStatus.MASTERED.name().equalsIgnoreCase(progress.getStatus())) {
                        masteredByTag.put(tag, masteredByTag.getOrDefault(tag, 0) + 1);
                    }
                    if (Hot100ProgressStatus.COMPLETED.name().equalsIgnoreCase(progress.getStatus())) {
                        completedByTag.put(tag, completedByTag.getOrDefault(tag, 0) + 1);
                    }
                    if (Hot100ProgressStatus.WRONG.name().equalsIgnoreCase(progress.getStatus())) {
                        wrongByTag.put(tag, wrongByTag.getOrDefault(tag, 0) + 1);
                    }
                }
            }
        }

        return totalByTag.entrySet().stream()
                .map(entry -> {
                    String tag = entry.getKey();
                    int total = entry.getValue();
                    int practiced = practicedByTag.getOrDefault(tag, 0);
                    int mastered = masteredByTag.getOrDefault(tag, 0);
                    int completed = completedByTag.getOrDefault(tag, 0);
                    int wrong = wrongByTag.getOrDefault(tag, 0);
                    int masteryCount = mastered + completed;
                    double masteryRate = total == 0 ? 0D : (double) masteryCount / total;
                    return new Hot100TagMasteryView(tag, total, practiced, mastered, wrong, masteryRate);
                })
                .sorted(Comparator.comparingDouble(Hot100TagMasteryView::masteryRate).thenComparing(Hot100TagMasteryView::tag))
                .toList();
    }

    public Hot100DatasetStatsView datasetStats() {
        int loadedCount = hot100ProblemLoader.listAll().size();
        int targetCount = 100;
        return new Hot100DatasetStatsView(loadedCount, targetCount, loadedCount >= targetCount);
    }

    private Set<String> collectFinishedSlugs(Long userId) {
        Set<String> finished = new HashSet<>();
        for (Hot100ProblemProgress p : progressRepository.findByUserId(userId)) {
            if (Hot100ProgressStatus.MASTERED.name().equalsIgnoreCase(p.getStatus())) {
                finished.add(p.getProblemSlug());
            }
        }
        return finished;
    }

    private Hot100ProblemSummaryView findSummaryBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        Hot100Problem problem = hot100ProblemLoader.getBySlug(slug);
        return problem == null ? null : Hot100ProblemSummaryView.from(problem);
    }

    private List<String> pickWeakTags(List<Hot100TagMasteryView> mastery) {
        List<String> weak = mastery.stream()
                .filter(tag -> tag.practicedCount() > 0 || tag.wrongCount() > 0)
                .filter(tag -> tag.wrongCount() > 0 || tag.masteryRate() < 0.6D)
                .sorted(Comparator
                        .comparingInt(Hot100TagMasteryView::wrongCount).reversed()
                        .thenComparingDouble(Hot100TagMasteryView::masteryRate)
                        .thenComparing(Hot100TagMasteryView::tag))
                .limit(5)
                .map(Hot100TagMasteryView::tag)
                .toList();
        if (!weak.isEmpty()) {
            return weak;
        }
        return mastery.stream()
                .filter(tag -> tag.practicedCount() > 0)
                .sorted(Comparator.comparingDouble(Hot100TagMasteryView::masteryRate)
                        .thenComparing(Hot100TagMasteryView::tag))
                .limit(3)
                .map(Hot100TagMasteryView::tag)
                .toList();
    }

    private String buildRecommendationReason(Hot100ProblemSummaryView problem,
                                             List<String> weakTags,
                                             List<Hot100WrongBookItemView> wrongItems) {
        String matchedWeakTag = firstMatchedTag(problem.tags(), weakTags);
        if (matchedWeakTag != null) {
            return "This problem targets your current weak tag: " + matchedWeakTag
                    + ". It is suitable for reinforcing " + readablePattern(problem) + ".";
        }
        String relatedWrongPoint = wrongItems.stream()
                .map(Hot100WrongBookItemView::knowledgePoint)
                .filter(point -> point != null && !point.isBlank())
                .findFirst()
                .orElse(null);
        if (relatedWrongPoint != null) {
            return "This problem is recommended after your recent wrong answers. It can help revisit: "
                    + relatedWrongPoint + ".";
        }
        return "This problem fills the next unmastered slot in the Hot100 path and keeps your practice balanced.";
    }

    private String buildTrainingFocus(Hot100ProblemSummaryView problem, List<String> weakTags) {
        String matchedWeakTag = firstMatchedTag(problem.tags(), weakTags);
        if (matchedWeakTag != null) {
            return "Focus on the key decisions behind " + matchedWeakTag
                    + ", then summarize the reusable pattern after solving.";
        }
        String difficulty = problem.difficulty() == null ? "" : problem.difficulty().toLowerCase();
        return switch (difficulty) {
            case "easy" -> "Use this as speed training. Keep the implementation clean and test edge cases.";
            case "hard" -> "Break the problem into states, invariants, and edge cases before coding.";
            default -> "Practice pattern transfer and explain the approach in interview language.";
        };
    }

    private String buildCoachSummary(List<String> weakTags,
                                     List<String> masteredTags,
                                     List<String> recentWrongProblems) {
        if (weakTags.isEmpty() && recentWrongProblems.isEmpty()) {
            return "Not enough practice data yet. Start with foundational Hot100 problems and mark progress after each attempt.";
        }
        String weakPart = weakTags.isEmpty()
                ? "no stable weak tag has emerged"
                : "your main weak tags are " + String.join(", ", weakTags);
        String masteredPart = masteredTags.isEmpty()
                ? "no tag is stable enough to mark as mastered yet"
                : "you are relatively stable on " + String.join(", ", masteredTags);
        return "Based on your progress, " + weakPart + "; " + masteredPart
                + ". The next practice should prioritize targeted repair over simply doing problems in order.";
    }

    private String firstMatchedTag(List<String> problemTags, List<String> targetTags) {
        if (problemTags == null || targetTags == null || targetTags.isEmpty()) {
            return null;
        }
        for (String tag : problemTags) {
            if (targetTags.contains(tag)) {
                return tag;
            }
        }
        return null;
    }

    private String readablePattern(Hot100ProblemSummaryView problem) {
        if (problem.pattern() != null && !problem.pattern().isBlank()) {
            return problem.pattern();
        }
        if (problem.tags() != null && !problem.tags().isEmpty()) {
            return String.join(", ", problem.tags());
        }
        return "the underlying algorithm pattern";
    }

}
