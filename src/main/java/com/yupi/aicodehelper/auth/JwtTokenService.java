package com.yupi.aicodehelper.auth;

import com.yupi.aicodehelper.config.properties.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {

    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_TOKEN_VERSION = "tokenVersion";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;

    private final AuthProperties authProperties;

    public JwtTokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.secretKey = Keys.hmacShaKeyFor(authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String username, String role, Long tokenVersion) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(authProperties.getAccessTokenExpireSeconds());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId, Long tokenVersion, String tokenId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(authProperties.getRefreshTokenExpireSeconds());
        return Jwts.builder()
                .id(tokenId)
                .subject(String.valueOf(userId))
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey)
                .compact();
    }

    public String generateTokenId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public boolean isRefreshToken(Claims claims) {
        return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
