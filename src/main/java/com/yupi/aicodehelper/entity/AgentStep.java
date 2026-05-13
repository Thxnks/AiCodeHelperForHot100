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
@Table(name = "agent_step")
@Data
public class AgentStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;

    @Column(name = "runtime_id", length = 80)
    private String runtimeId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "tool_name", nullable = false, length = 120)
    private String toolName;

    @Column(name = "tool_input", columnDefinition = "TEXT")
    private String toolInput;

    @Column(name = "tool_output", columnDefinition = "TEXT")
    private String toolOutput;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
