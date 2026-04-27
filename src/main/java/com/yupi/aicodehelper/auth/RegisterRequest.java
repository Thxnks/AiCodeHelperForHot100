package com.yupi.aicodehelper.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(description = "Username, unique", example = "alice")
        @NotBlank(message = "username cannot be blank")
        @Size(min = 3, max = 64, message = "username length must be between 3 and 64")
        String username,
        @Schema(description = "Email, unique", example = "alice@example.com")
        @NotBlank(message = "email cannot be blank")
        @Email(message = "email format is invalid")
        String email,
        @Schema(description = "Plain password", example = "Passw0rd!")
        @NotBlank(message = "password cannot be blank")
        @Size(min = 6, max = 128, message = "password length must be between 6 and 128")
        String password
) {
}
