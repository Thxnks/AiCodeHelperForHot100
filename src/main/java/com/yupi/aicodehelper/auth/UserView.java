package com.yupi.aicodehelper.auth;

import com.yupi.aicodehelper.entity.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record UserView(
        @Schema(description = "User id", example = "1")
        Long id,
        @Schema(description = "Username", example = "alice")
        String username,
        @Schema(description = "Email", example = "alice@example.com")
        String email,
        @Schema(description = "User status", example = "ACTIVE")
        String status,
        @Schema(description = "Role code", example = "USER")
        String role,
        @Schema(description = "Created time")
        LocalDateTime createdAt
) {
    public static UserView from(UserAccount user) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
