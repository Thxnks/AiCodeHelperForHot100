package com.yupi.aicodehelper.repository;

import com.yupi.aicodehelper.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByMemoryIdOrderByCreatedAtAsc(Integer memoryId);
}
