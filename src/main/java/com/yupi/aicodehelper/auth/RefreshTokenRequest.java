package com.yupi.aicodehelper.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken cannot be blank")
        String refreshToken
) {
}
