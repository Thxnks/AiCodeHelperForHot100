package com.yupi.aicodehelper.repository;

import com.yupi.aicodehelper.entity.ConversationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {

    Optional<ConversationSession> findByMemoryId(Integer memoryId);

    List<ConversationSession> findAllByOrderByUpdatedAtDesc();
}
