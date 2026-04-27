package com.yupi.aicodehelper.chat;

import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.entity.ChatMessage;
import com.yupi.aicodehelper.entity.ConversationSession;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.repository.ChatMessageRepository;
import com.yupi.aicodehelper.repository.ConversationSessionRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatPersistenceService {

    @Resource
    private ConversationSessionRepository conversationSessionRepository;

    @Resource
    private ChatMessageRepository chatMessageRepository;

    @Transactional
    public void recordUserMessage(Integer memoryId, String roleId, String message) {
        upsertSession(memoryId, roleId, message);
        saveMessage(memoryId, "user", roleId, message);
    }

    @Transactional
    public void recordAiMessage(Integer memoryId, String roleId, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        upsertSession(memoryId, roleId, message);
        saveMessage(memoryId, "assistant", roleId, message);
    }

    public List<ConversationSessionView> listSessions() {
        return conversationSessionRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(ConversationSessionView::from)
                .toList();
    }

    public List<ChatMessageView> listMessages(Integer memoryId) {
        if (memoryId == null || memoryId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "memoryId 必须为正整数");
        }
        return chatMessageRepository.findByMemoryIdOrderByCreatedAtAsc(memoryId).stream()
                .map(ChatMessageView::from)
                .toList();
    }

    private void upsertSession(Integer memoryId, String roleId, String message) {
        ConversationSession session = conversationSessionRepository.findByMemoryId(memoryId)
                .orElseGet(() -> {
                    ConversationSession newSession = new ConversationSession();
                    newSession.setMemoryId(memoryId);
                    newSession.setTitle(buildTitle(message));
                    return newSession;
                });
        session.setRoleId(normalizeRoleId(roleId));
        if (session.getTitle() == null || session.getTitle().isBlank()) {
            session.setTitle(buildTitle(message));
        }
        conversationSessionRepository.save(session);
    }

    private void saveMessage(Integer memoryId, String sender, String roleId, String content) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMemoryId(memoryId);
        chatMessage.setSender(sender);
        chatMessage.setRoleId(normalizeRoleId(roleId));
        chatMessage.setContent(content);
        chatMessageRepository.save(chatMessage);
    }

    private String buildTitle(String text) {
        String cleaned = (text == null ? "" : text.trim()).replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            return "新会话";
        }
        return cleaned.length() > 40 ? cleaned.substring(0, 40) : cleaned;
    }

    private String normalizeRoleId(String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return "assistant";
        }
        return roleId;
    }
}
