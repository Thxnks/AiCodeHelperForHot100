package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.common.BaseResponse;
import com.yupi.aicodehelper.auth.CurrentUserService;
import com.yupi.aicodehelper.hot100.Hot100ProblemDetailView;
import com.yupi.aicodehelper.hot100.Hot100ProgressService;
import com.yupi.aicodehelper.hot100.Hot100ProgressUpsertRequest;
import com.yupi.aicodehelper.hot100.Hot100ProgressView;
import com.yupi.aicodehelper.hot100.Hot100ProblemSummaryView;
import com.yupi.aicodehelper.hot100.Hot100Service;
import com.yupi.aicodehelper.hot100.Hot100TagMasteryView;
import com.yupi.aicodehelper.hot100.Hot100DatasetStatsView;
import com.yupi.aicodehelper.hot100.Hot100AsyncTaskDetailView;
import com.yupi.aicodehelper.hot100.Hot100AsyncTaskService;
import com.yupi.aicodehelper.hot100.Hot100AsyncTaskSubmitView;
import com.yupi.aicodehelper.hot100.Hot100StudyPlanItemView;
import com.yupi.aicodehelper.hot100.Hot100WeakTagView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping("/hot100")
@Validated
public class Hot100Controller {

    private final Hot100Service hot100Service;
    private final Hot100ProgressService hot100ProgressService;
    private final Hot100AsyncTaskService hot100AsyncTaskService;
    private final CurrentUserService currentUserService;

    public Hot100Controller(Hot100Service hot100Service,
                            Hot100ProgressService hot100ProgressService,
                            Hot100AsyncTaskService hot100AsyncTaskService,
                            CurrentUserService currentUserService) {
        this.hot100Service = hot100Service;
        this.hot100ProgressService = hot100ProgressService;
        this.hot100AsyncTaskService = hot100AsyncTaskService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/problems")
    public BaseResponse<List<Hot100ProblemSummaryView>> listProblems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String difficulty) {
        return BaseResponse.success(hot100Service.listProblems(keyword, tag, difficulty));
    }

    @GetMapping("/problems/{slug}")
    public BaseResponse<Hot100ProblemDetailView> getProblem(@PathVariable String slug) {
        return BaseResponse.success(hot100Service.getProblem(slug));
    }

    @GetMapping("/tags")
    public BaseResponse<List<String>> listTags() {
        return BaseResponse.success(hot100Service.listTags());
    }

    @PostMapping("/progress")
    public BaseResponse<Hot100ProgressView> upsertProgress(@Valid @RequestBody Hot100ProgressUpsertRequest request) {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(hot100ProgressService.upsert(request, userId));
    }

    @GetMapping("/progress")
    public BaseResponse<List<Hot100ProgressView>> listProgress() {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(hot100ProgressService.listProgress(userId));
    }

    @GetMapping("/wrong-book")
    public BaseResponse<List<Hot100ProblemSummaryView>> wrongBook() {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(hot100ProgressService.wrongBook(userId));
    }

    @GetMapping("/weak-tags")
    public BaseResponse<List<Hot100WeakTagView>> weakTags() {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(hot100ProgressService.weakTags(userId));
    }

    @GetMapping("/recommendations")
    public BaseResponse<List<Hot100ProblemSummaryView>> recommendNext(
            @RequestParam(defaultValue = "5") @Min(value = 1, message = "limit must be >= 1")
            @Max(value = 20, message = "limit must be <= 20") int limit) {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(hot100ProgressService.recommendNext(limit, userId));
    }

    @PostMapping("/tasks/recommendations")
    public BaseResponse<Hot100AsyncTaskSubmitView> recommendNextAsync(
            @RequestParam(defaultValue = "5") @Min(value = 1, message = "limit must be >= 1")
            @Max(value = 20, message = "limit must be <= 20") int limit) {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(hot100AsyncTaskService.submitRecommendationTask(limit, userId));
    }

    @GetMapping("/study-plan")
    public BaseResponse<List<Hot100StudyPlanItemView>> studyPlan(
            @RequestParam(defaultValue = "7") @Min(value = 7, message = "days must be 7 or 14")
            @Max(value = 14, message = "days must be 7 or 14") int days) {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(hot100ProgressService.buildStudyPlan(days, userId));
    }

    @PostMapping("/tasks/study-plan")
    public BaseResponse<Hot100AsyncTaskSubmitView> studyPlanAsync(
            @RequestParam(defaultValue = "7") @Min(value = 7, message = "days must be 7 or 14")
            @Max(value = 14, message = "days must be 7 or 14") int days) {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(hot100AsyncTaskService.submitStudyPlanTask(days, userId));
    }

    @GetMapping("/tasks/{taskId}")
    public BaseResponse<Hot100AsyncTaskDetailView> taskDetail(@PathVariable String taskId) {
        return BaseResponse.success(hot100AsyncTaskService.getTaskDetail(taskId));
    }

    @GetMapping("/tag-mastery")
    public BaseResponse<List<Hot100TagMasteryView>> tagMastery() {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(hot100ProgressService.tagMastery(userId));
    }

    @GetMapping("/dataset-stats")
    public BaseResponse<Hot100DatasetStatsView> datasetStats() {
        return BaseResponse.success(hot100ProgressService.datasetStats());
    }
}
