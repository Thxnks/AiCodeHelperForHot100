package com.yupi.aicodehelper.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_session",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_conversation_user_memory", columnNames = {"user_id", "memory_id"})
        })
@Data
public class ConversationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "memory_id", nullable = false)
    private Integer memoryId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "role_id", nullable = false, length = 64)
    private String roleId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
