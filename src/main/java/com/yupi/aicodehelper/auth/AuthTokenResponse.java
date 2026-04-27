package com.yupi.aicodehelper.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthTokenResponse(
        @Schema(description = "Short-lived access token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "Long-lived refresh token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,
        @Schema(description = "Token type", example = "Bearer")
        String tokenType,
        UserView user
) {
}
