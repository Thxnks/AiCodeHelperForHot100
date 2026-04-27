package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.auth.AuthService;
import com.yupi.aicodehelper.auth.AuthTokenResponse;
import com.yupi.aicodehelper.auth.CurrentUserService;
import com.yupi.aicodehelper.auth.LoginRequest;
import com.yupi.aicodehelper.auth.LogoutRequest;
import com.yupi.aicodehelper.auth.RefreshTokenRequest;
import com.yupi.aicodehelper.auth.RegisterRequest;
import com.yupi.aicodehelper.auth.UserView;
import com.yupi.aicodehelper.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authentication and authorization APIs")
public class AuthController {

    private final AuthService authService;

    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register account")
    public BaseResponse<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return BaseResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and issue access/refresh token")
    public BaseResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return BaseResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token with refresh token")
    public BaseResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return BaseResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke all active refresh token for current user")
    public BaseResponse<Boolean> logout(@RequestBody(required = false) LogoutRequest request) {
        Long userId = currentUserService.requireUserId();
        authService.logout(userId, request == null ? null : request.refreshToken());
        return BaseResponse.success(Boolean.TRUE);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public BaseResponse<UserView> me() {
        Long userId = currentUserService.requireUserId();
        return BaseResponse.success(authService.getUser(userId));
    }
}
