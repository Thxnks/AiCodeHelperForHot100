package com.yupi.aicodehelper.auth;

import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.entity.UserAccount;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.repository.UserAccountRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenService jwtTokenService;

    private final RefreshTokenStoreService refreshTokenStoreService;

    public AuthService(UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       RefreshTokenStoreService refreshTokenStoreService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenStoreService = refreshTokenStoreService;
    }

    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        validateRegisterRequest(request);
        if (userAccountRepository.findByUsername(request.username()).isPresent()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Username already exists");
        }
        if (userAccountRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Email already exists");
        }
        UserAccount user = new UserAccount();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus("ACTIVE");
        user.setRole("USER");
        user.setTokenVersion(0L);
        UserAccount saved = userAccountRepository.save(user);
        return issueTokens(saved);
    }

    public AuthTokenResponse login(LoginRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Username and password are required");
        }
        UserAccount user = userAccountRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid username or password"));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "User is disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
        }
        if (user.getTokenVersion() == null) {
            user.setTokenVersion(0L);
            user = userAccountRepository.save(user);
        }
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
            user = userAccountRepository.save(user);
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        Claims claims;
        try {
            claims = jwtTokenService.parseClaims(request.refreshToken());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Invalid refresh token");
        }
        if (!jwtTokenService.isRefreshToken(claims)) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Invalid refresh token type");
        }
        Long userId = parseUserId(claims);
        String tokenId = claims.getId();
        Long tokenVersion = claims.get(JwtTokenService.CLAIM_TOKEN_VERSION, Long.class);
        if (tokenId == null || tokenId.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Invalid refresh token");
        }
        if (!refreshTokenStoreService.isValidActiveToken(tokenId, request.refreshToken())) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED, "Refresh token has expired or revoked");
        }
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "User not found"));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "User is disabled");
        }
        Long currentTokenVersion = normalizeTokenVersion(user);
        if (!currentTokenVersion.equals(tokenVersion)) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Refresh token is invalid");
        }
        refreshTokenStoreService.revokeToken(tokenId);
        return issueTokens(user);
    }

    @Transactional
    public void logout(Long userId, String refreshToken) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "User not found"));
        Long tokenVersion = normalizeTokenVersion(user);
        user.setTokenVersion(tokenVersion + 1);
        userAccountRepository.save(user);
        refreshTokenStoreService.revokeAllByUserId(userId);

        if (!isBlank(refreshToken)) {
            try {
                Claims claims = jwtTokenService.parseClaims(refreshToken);
                if (jwtTokenService.isRefreshToken(claims) && userId.equals(parseUserId(claims))) {
                    String tokenId = claims.getId();
                    if (!isBlank(tokenId)) {
                        refreshTokenStoreService.revokeToken(tokenId);
                    }
                }
            } catch (Exception ignored) {
                // Ignore invalid refresh token on logout to keep logout idempotent.
            }
        }
    }

    public UserView getUser(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
        return UserView.from(user);
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.email()) || isBlank(request.password())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Username, email and password are required");
        }
        if (request.username().trim().length() < 3) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Username must be at least 3 chars");
        }
        if (request.password().length() < 6) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Password must be at least 6 chars");
        }
        if (!request.email().contains("@")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Email format is invalid");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Long parseUserId(Claims claims) {
        try {
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid token subject");
        }
    }

    private Long normalizeTokenVersion(UserAccount user) {
        if (user.getTokenVersion() == null) {
            user.setTokenVersion(0L);
            userAccountRepository.save(user);
            return 0L;
        }
        return user.getTokenVersion();
    }

    private AuthTokenResponse issueTokens(UserAccount user) {
        Long tokenVersion = normalizeTokenVersion(user);
        String role = isBlank(user.getRole()) ? "USER" : user.getRole().trim().toUpperCase();
        if (!role.equals(user.getRole())) {
            user.setRole(role);
            userAccountRepository.save(user);
        }
        String accessToken = jwtTokenService.createAccessToken(
                user.getId(),
                user.getUsername(),
                role,
                tokenVersion
        );
        String refreshTokenId = jwtTokenService.generateTokenId();
        String refreshToken = jwtTokenService.createRefreshToken(user.getId(), tokenVersion, refreshTokenId);
        LocalDateTime refreshExpireTime = refreshTokenStoreService.toLocalDateTime(
                jwtTokenService.parseClaims(refreshToken).getExpiration()
        );
        refreshTokenStoreService.save(user.getId(), refreshTokenId, refreshToken, refreshExpireTime);
        return new AuthTokenResponse(accessToken, refreshToken, "Bearer", UserView.from(user));
    }
}
