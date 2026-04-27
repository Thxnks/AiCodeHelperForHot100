package com.yupi.aicodehelper.hot100;

import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Service
public class Hot100AsyncTaskService {

    private static final int MAX_TASKS = 500;

    private static final int CLEANUP_KEEP = 300;

    private final Hot100ProgressService hot100ProgressService;

    private final Executor hot100TaskExecutor;

    private final Map<String, TaskState> taskStore = new ConcurrentHashMap<>();

    public Hot100AsyncTaskService(Hot100ProgressService hot100ProgressService,
                                  @Qualifier("hot100TaskExecutor") Executor hot100TaskExecutor) {
        this.hot100ProgressService = hot100ProgressService;
        this.hot100TaskExecutor = hot100TaskExecutor;
    }

    public Hot100AsyncTaskSubmitView submitRecommendationTask(int limit, Long userId) {
        return submitTask(
                Hot100AsyncTaskType.RECOMMENDATION,
                () -> hot100ProgressService.recommendNext(limit, userId)
        );
    }

    public Hot100AsyncTaskSubmitView submitStudyPlanTask(int days, Long userId) {
        return submitTask(
                Hot100AsyncTaskType.STUDY_PLAN,
                () -> hot100ProgressService.buildStudyPlan(days, userId)
        );
    }

    public Hot100AsyncTaskDetailView getTaskDetail(String taskId) {
        TaskState state = taskStore.get(taskId);
        if (state == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Task not found");
        }
        return state.toView();
    }

    private Hot100AsyncTaskSubmitView submitTask(Hot100AsyncTaskType taskType, Supplier<Object> supplier) {
        pruneFinishedTasksIfNeeded();
        String taskId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        TaskState state = new TaskState(taskId, taskType, Hot100AsyncTaskStatus.PENDING, null, null, now, now);
        taskStore.put(taskId, state);

        CompletableFuture.runAsync(() -> {
            state.markRunning();
            try {
                Object result = supplier.get();
                state.markSuccess(result);
            } catch (Exception e) {
                state.markFailed(e.getMessage());
            }
        }, hot100TaskExecutor);

        return new Hot100AsyncTaskSubmitView(taskId, taskType, Hot100AsyncTaskStatus.PENDING, now);
    }

    private void pruneFinishedTasksIfNeeded() {
        if (taskStore.size() < MAX_TASKS) {
            return;
        }
        List<TaskState> candidates = new ArrayList<>(taskStore.values());
        candidates.sort(Comparator.comparing(TaskState::updatedAt));
        int removeCount = Math.max(0, candidates.size() - CLEANUP_KEEP);
        for (int i = 0; i < removeCount; i++) {
            TaskState state = candidates.get(i);
            if (state.status() == Hot100AsyncTaskStatus.RUNNING || state.status() == Hot100AsyncTaskStatus.PENDING) {
                continue;
            }
            taskStore.remove(state.taskId());
        }
    }

    private static class TaskState {
        private final String taskId;
        private final Hot100AsyncTaskType taskType;
        private volatile Hot100AsyncTaskStatus status;
        private volatile Object result;
        private volatile String errorMessage;
        private final LocalDateTime createdAt;
        private volatile LocalDateTime updatedAt;

        private TaskState(String taskId,
                          Hot100AsyncTaskType taskType,
                          Hot100AsyncTaskStatus status,
                          Object result,
                          String errorMessage,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
            this.taskId = taskId;
            this.taskType = taskType;
            this.status = status;
            this.result = result;
            this.errorMessage = errorMessage;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        private synchronized void markRunning() {
            this.status = Hot100AsyncTaskStatus.RUNNING;
            this.updatedAt = LocalDateTime.now();
        }

        private synchronized void markSuccess(Object result) {
            this.status = Hot100AsyncTaskStatus.SUCCESS;
            this.result = result;
            this.errorMessage = null;
            this.updatedAt = LocalDateTime.now();
        }

        private synchronized void markFailed(String errorMessage) {
            this.status = Hot100AsyncTaskStatus.FAILED;
            this.errorMessage = errorMessage;
            this.updatedAt = LocalDateTime.now();
        }

        private Hot100AsyncTaskDetailView toView() {
            return new Hot100AsyncTaskDetailView(
                    taskId,
                    taskType,
                    status,
                    result,
                    errorMessage,
                    createdAt,
                    updatedAt
            );
        }

        private String taskId() {
            return taskId;
        }

        private Hot100AsyncTaskStatus status() {
            return status;
        }

        private LocalDateTime updatedAt() {
            return updatedAt;
        }
    }
}
