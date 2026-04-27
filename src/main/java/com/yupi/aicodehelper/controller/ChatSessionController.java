package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.chat.ChatMessageView;
import com.yupi.aicodehelper.chat.ChatPersistenceService;
import com.yupi.aicodehelper.chat.ConversationSessionView;
import com.yupi.aicodehelper.auth.CurrentUserService;
import com.yupi.aicodehelper.common.BaseResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chat/sessions")
public class ChatSessionController {

    @Resource
    private ChatPersistenceService chatPersistenceService;

    @Resource
    private CurrentUserService currentUserService;

    @GetMapping
    public BaseResponse<List<ConversationSessionView>> listSessions() {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(chatPersistenceService.listSessions(userId));
    }

    @GetMapping("/{memoryId}/messages")
    public BaseResponse<List<ChatMessageView>> listMessages(@PathVariable Integer memoryId) {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(chatPersistenceService.listMessages(memoryId, userId));
    }
}
