package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.chat.ChatMessageView;
import com.yupi.aicodehelper.chat.ChatPersistenceService;
import com.yupi.aicodehelper.chat.ConversationSessionView;
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

    @GetMapping
    public BaseResponse<List<ConversationSessionView>> listSessions() {
        return BaseResponse.success(chatPersistenceService.listSessions());
    }

    @GetMapping("/{memoryId}/messages")
    public BaseResponse<List<ChatMessageView>> listMessages(@PathVariable Integer memoryId) {
        return BaseResponse.success(chatPersistenceService.listMessages(memoryId));
    }
}
