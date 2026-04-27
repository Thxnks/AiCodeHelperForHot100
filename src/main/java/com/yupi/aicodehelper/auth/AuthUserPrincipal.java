package com.yupi.aicodehelper.auth;

public record AuthUserPrincipal(
        Long userId,
        String username,
        String role
) {
}
