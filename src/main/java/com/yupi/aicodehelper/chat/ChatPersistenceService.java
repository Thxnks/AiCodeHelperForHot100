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
    public void recordUserMessage(Integer memoryId, String roleId, String message, Long userId) {
        upsertSession(memoryId, roleId, message, userId);
        saveMessage(memoryId, "user", roleId, message, userId);
    }

    @Transactional
    public void recordAiMessage(Integer memoryId, String roleId, String message, Long userId) {
        if (message == null || message.isBlank()) {
            return;
        }
        upsertSession(memoryId, roleId, message, userId);
        saveMessage(memoryId, "assistant", roleId, message, userId);
    }

    public List<ConversationSessionView> listSessions(Long userId) {
        return conversationSessionRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ConversationSessionView::from)
                .toList();
    }

    public List<ChatMessageView> listMessages(Integer memoryId, Long userId) {
        if (memoryId == null || memoryId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "memoryId must be positive");
        }
        return chatMessageRepository.findByMemoryIdAndUserIdOrderByCreatedAtAsc(memoryId, userId).stream()
                .map(ChatMessageView::from)
                .toList();
    }

    private void upsertSession(Integer memoryId, String roleId, String message, Long userId) {
        ConversationSession session = conversationSessionRepository.findByMemoryIdAndUserId(memoryId, userId)
                .orElseGet(() -> {
                    ConversationSession newSession = new ConversationSession();
                    newSession.setMemoryId(memoryId);
                    newSession.setUserId(userId);
                    newSession.setTitle(buildTitle(message));
                    return newSession;
                });
        session.setUserId(userId);
        session.setRoleId(normalizeRoleId(roleId));
        if (session.getTitle() == null || session.getTitle().isBlank()) {
            session.setTitle(buildTitle(message));
        }
        conversationSessionRepository.save(session);
    }

    private void saveMessage(Integer memoryId, String sender, String roleId, String content, Long userId) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMemoryId(memoryId);
        chatMessage.setUserId(userId);
        chatMessage.setSender(sender);
        chatMessage.setRoleId(normalizeRoleId(roleId));
        chatMessage.setContent(content);
        chatMessageRepository.save(chatMessage);
    }

    private String buildTitle(String text) {
        String cleaned = (text == null ? "" : text.trim()).replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            return "New Session";
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

