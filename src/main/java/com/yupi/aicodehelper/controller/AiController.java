package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.ai.AiCodeHelperService;
import com.yupi.aicodehelper.ai.RoleCardService;
import com.yupi.aicodehelper.ai.mcp.BigModelCapabilityService;
import com.yupi.aicodehelper.auth.CurrentUserService;
import com.yupi.aicodehelper.chat.ChatPersistenceService;
import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.hot100.Hot100ChatContextService;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @Resource
    private RoleCardService roleCardService;

    @Resource
    private ChatPersistenceService chatPersistenceService;

    @Resource
    private Hot100ChatContextService hot100ChatContextService;

    @Resource
    private BigModelCapabilityService bigModelCapabilityService;

    @Resource
    private CurrentUserService currentUserService;

    @GetMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(@RequestParam int memoryId,
                                              @RequestParam String message,
                                              @RequestParam(defaultValue = "assistant") String roleId,
                                              @RequestParam(required = false, defaultValue = "guided") String solvingMode,
                                              @RequestParam(required = false) String currentProblemSlug) {
        if (memoryId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "memoryId 必须为正整数");
        }
        if (message == null || message.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "message 不能为空");
        }
        Long userId = currentUserService.getCurrentUserIdOrNull();
        chatPersistenceService.recordUserMessage(memoryId, roleId, message, userId);
        String roleCard = roleCardService.getRoleCard(roleId);
        String coachingStrategy = hot100ChatContextService.buildCoachingStrategy(roleId);
        String solvingModeStrategy = hot100ChatContextService.buildSolvingModeStrategy(solvingMode);
        String problemContext = hot100ChatContextService.buildProblemContext(currentProblemSlug);
        String userFocusPrefix = hot100ChatContextService.buildUserFocusPrefix(currentProblemSlug);
        String effectiveMessage = userFocusPrefix.isBlank() ? message : userFocusPrefix + "\n\nUser question: " + message;
        String bigModelCapabilityNotice = bigModelCapabilityService.buildCapabilityNotice();
        StringBuffer aiAnswerBuffer = new StringBuffer();
        return aiCodeHelperService.chatStream(
                        memoryId,
                        effectiveMessage,
                        roleCard,
                        coachingStrategy,
                        solvingModeStrategy,
                        problemContext,
                        bigModelCapabilityNotice
                )
                .doOnNext(aiAnswerBuffer::append)
                .doOnComplete(() -> chatPersistenceService.recordAiMessage(memoryId, roleId, aiAnswerBuffer.toString(), userId))
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }
}
