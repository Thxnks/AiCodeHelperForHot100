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
@Table(name = "hot100_problem_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hot100_user_problem", columnNames = {"user_id", "problem_slug"})
        })
@Data
public class Hot100ProblemProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "problem_slug", nullable = false, length = 120)
    private String problemSlug;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.lastReviewedAt == null) {
            this.lastReviewedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
