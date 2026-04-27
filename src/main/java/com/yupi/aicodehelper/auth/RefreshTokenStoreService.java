package com.yupi.aicodehelper.auth;

import com.yupi.aicodehelper.entity.UserRefreshToken;
import com.yupi.aicodehelper.repository.UserRefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class RefreshTokenStoreService {

    private final UserRefreshTokenRepository userRefreshTokenRepository;

    public RefreshTokenStoreService(UserRefreshTokenRepository userRefreshTokenRepository) {
        this.userRefreshTokenRepository = userRefreshTokenRepository;
    }

    @Transactional
    public void save(Long userId, String tokenId, String refreshToken, LocalDateTime expiresAt) {
        UserRefreshToken entity = new UserRefreshToken();
        entity.setUserId(userId);
        entity.setTokenId(tokenId);
        entity.setTokenHash(hashToken(refreshToken));
        entity.setExpiresAt(expiresAt);
        userRefreshTokenRepository.save(entity);
    }

    public boolean isValidActiveToken(String tokenId, String refreshToken) {
        Optional<UserRefreshToken> tokenOptional = userRefreshTokenRepository.findByTokenIdAndRevokedAtIsNull(tokenId);
        if (tokenOptional.isEmpty()) {
            return false;
        }
        UserRefreshToken token = tokenOptional.get();
        return token.getExpiresAt().isAfter(LocalDateTime.now())
                && token.getTokenHash().equals(hashToken(refreshToken));
    }

    @Transactional
    public void revokeToken(String tokenId) {
        userRefreshTokenRepository.findByTokenIdAndRevokedAtIsNull(tokenId).ifPresent(token -> {
            token.setRevokedAt(LocalDateTime.now());
            userRefreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void revokeAllByUserId(Long userId) {
        userRefreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
    }

    public LocalDateTime toLocalDateTime(java.util.Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
