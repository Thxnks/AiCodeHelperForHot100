package com.yupi.aicodehelper.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @Schema(description = "Refresh token", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "refreshToken cannot be blank")
        String refreshToken
) {
}
