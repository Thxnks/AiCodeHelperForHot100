package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.agent.AgentMemoryProfileView;
import com.yupi.aicodehelper.agent.AgentMemorySaveRequest;
import com.yupi.aicodehelper.agent.AgentMemoryService;
import com.yupi.aicodehelper.agent.AgentMemoryView;
import com.yupi.aicodehelper.auth.CurrentUserService;
import com.yupi.aicodehelper.common.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/agent/memory")
public class AgentMemoryController {

    private final AgentMemoryService agentMemoryService;
    private final CurrentUserService currentUserService;

    public AgentMemoryController(AgentMemoryService agentMemoryService,
                                 CurrentUserService currentUserService) {
        this.agentMemoryService = agentMemoryService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public BaseResponse<List<AgentMemoryView>> recall(@RequestParam(required = false) String query,
                                                      @RequestParam(defaultValue = "10") int limit) {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(agentMemoryService.recall(userId, query, limit));
    }

    @GetMapping("/profile")
    public BaseResponse<AgentMemoryProfileView> profile() {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(agentMemoryService.profile(userId));
    }

    @PostMapping
    public BaseResponse<AgentMemoryView> remember(@Valid @RequestBody AgentMemorySaveRequest request) {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(agentMemoryService.remember(
                userId,
                request.type(),
                request.scope(),
                request.subject(),
                request.content(),
                request.importance() == null ? 5 : request.importance(),
                request.source()
        ));
    }
}
