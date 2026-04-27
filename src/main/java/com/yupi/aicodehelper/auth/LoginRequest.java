package com.yupi.aicodehelper.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Username", example = "alice")
        @NotBlank(message = "username cannot be blank")
        String username,
        @Schema(description = "Plain password", example = "Passw0rd!")
        @NotBlank(message = "password cannot be blank")
        String password
) {
}
