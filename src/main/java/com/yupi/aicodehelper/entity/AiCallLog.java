package com.yupi.aicodehelper.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_call_log")
@Data
public class AiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "business_type", nullable = false, length = 64)
    private String businessType;

    @Column(name = "business_key", length = 160)
    private String businessKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "repaired", nullable = false)
    private Boolean repaired;

    @Column(name = "fallback_used", nullable = false)
    private Boolean fallbackUsed;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.success == null) {
            this.success = false;
        }
        if (this.repaired == null) {
            this.repaired = false;
        }
        if (this.fallbackUsed == null) {
            this.fallbackUsed = false;
        }
    }
}
