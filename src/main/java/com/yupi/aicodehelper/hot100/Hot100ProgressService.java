package com.yupi.aicodehelper.hot100;

import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.entity.Hot100ProblemProgress;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.repository.Hot100ProblemProgressRepository;
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

@Service
public class Hot100ProgressService {

    private final Hot100ProblemProgressRepository progressRepository;

    private final Hot100Service hot100Service;

    private final Hot100ProblemLoader hot100ProblemLoader;

    public Hot100ProgressService(Hot100ProblemProgressRepository progressRepository,
                                 Hot100Service hot100Service,
                                 Hot100ProblemLoader hot100ProblemLoader) {
        this.progressRepository = progressRepository;
        this.hot100Service = hot100Service;
        this.hot100ProblemLoader = hot100ProblemLoader;
    }

    @Transactional
    public Hot100ProgressView upsert(Hot100ProgressUpsertRequest request) {
        if (request == null || request.problemSlug() == null || request.problemSlug().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "problemSlug 不能为空");
        }
        // Validate slug exists
        hot100Service.getProblem(request.problemSlug());
        Hot100ProgressStatus status;
        try {
            status = Hot100ProgressStatus.from(request.status());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status 非法，支持：NOT_STARTED/COMPLETED/WRONG/MASTERED");
        }

        Hot100ProblemProgress entity = progressRepository.findByProblemSlug(request.problemSlug())
                .orElseGet(Hot100ProblemProgress::new);
        entity.setProblemSlug(request.problemSlug());
        entity.setStatus(status.name());
        entity.setNotes(request.notes());
        entity.setLastReviewedAt(LocalDateTime.now());
        Hot100ProblemProgress saved = progressRepository.save(entity);
        return Hot100ProgressView.from(saved);
    }

    public List<Hot100ProgressView> listProgress() {
        return progressRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(Hot100ProgressView::from)
                .toList();
    }

    public List<Hot100ProblemSummaryView> wrongBook() {
        List<Hot100ProblemProgress> wrong = progressRepository.findByStatusOrderByUpdatedAtDesc(Hot100ProgressStatus.WRONG.name());
        List<Hot100ProblemSummaryView> result = new ArrayList<>();
        for (Hot100ProblemProgress p : wrong) {
            Hot100ProblemSummaryView summary = findSummaryBySlug(p.getProblemSlug());
            if (summary != null) {
                result.add(summary);
            }
        }
        return result;
    }

    public List<Hot100WeakTagView> weakTags() {
        List<Hot100ProblemProgress> wrong = progressRepository.findByStatusOrderByUpdatedAtDesc(Hot100ProgressStatus.WRONG.name());
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

    public List<Hot100ProblemSummaryView> recommendNext(int limit) {
        int realLimit = Math.max(1, Math.min(limit, 20));
        Set<String> finished = collectFinishedSlugs();
        List<Hot100WeakTagView> weakTags = weakTags();

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

        // fallback: fill with not-started problems
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

    public List<Hot100StudyPlanItemView> buildStudyPlan(int days) {
        int realDays = days <= 7 ? 7 : 14;
        List<Hot100ProblemSummaryView> candidates = recommendNext(realDays);
        List<Hot100StudyPlanItemView> plan = new ArrayList<>();
        for (int i = 0; i < realDays; i++) {
            Hot100ProblemSummaryView problem = i < candidates.size() ? candidates.get(i) : null;
            if (problem == null) {
                plan.add(new Hot100StudyPlanItemView(i + 1, "-", "复盘错题与总结", "-", "复盘"));
                continue;
            }
            String focus = switch (problem.difficulty().toLowerCase()) {
                case "easy" -> "巩固基础与编码速度";
                case "hard" -> "训练复杂度分析与边界处理";
                default -> "强化套路迁移与面试表达";
            };
            plan.add(new Hot100StudyPlanItemView(i + 1, problem.slug(), problem.title(), problem.difficulty(), focus));
        }
        return plan;
    }

    private Set<String> collectFinishedSlugs() {
        Set<String> finished = new HashSet<>();
        for (Hot100ProblemProgress p : progressRepository.findAll()) {
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
}
