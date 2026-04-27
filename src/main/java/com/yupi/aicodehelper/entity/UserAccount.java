package com.yupi.aicodehelper.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_account")
@Data
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 128)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "role", nullable = false, length = 32)
    private String role;

    @Column(name = "token_version", nullable = false)
    private Long tokenVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null || this.status.isBlank()) {
            this.status = "ACTIVE";
        }
        if (this.role == null || this.role.isBlank()) {
            this.role = "USER";
        }
        if (this.tokenVersion == null) {
            this.tokenVersion = 0L;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
