package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.agent.AgentStepView;
import com.yupi.aicodehelper.agent.AgentTaskView;
import com.yupi.aicodehelper.agent.Hot100AgentRunRequest;
import com.yupi.aicodehelper.agent.Hot100AgentService;
import com.yupi.aicodehelper.auth.CurrentUserService;
import com.yupi.aicodehelper.common.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/agent/hot100")
public class Hot100AgentController {

    private final Hot100AgentService hot100AgentService;
    private final CurrentUserService currentUserService;

    public Hot100AgentController(Hot100AgentService hot100AgentService,
                                 CurrentUserService currentUserService) {
        this.hot100AgentService = hot100AgentService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/run")
    public BaseResponse<AgentTaskView> run(@Valid @RequestBody Hot100AgentRunRequest request) {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(hot100AgentService.run(request, userId));
    }

    @PostMapping("/tasks")
    public BaseResponse<AgentTaskView> submit(@Valid @RequestBody Hot100AgentRunRequest request) {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(hot100AgentService.submit(request, userId));
    }

    @GetMapping("/tasks/{taskId}")
    public BaseResponse<AgentTaskView> getTask(@PathVariable String taskId) {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(hot100AgentService.getTask(taskId, userId));
    }

    @GetMapping("/tasks/{taskId}/steps")
    public BaseResponse<List<AgentStepView>> listSteps(@PathVariable String taskId) {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(hot100AgentService.listSteps(taskId, userId));
    }
}
