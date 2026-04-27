package com.yupi.aicodehelper.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    /**
     * JWT secret. Should be at least 32 chars for HS256.
     */
    private String jwtSecret = "change-this-to-a-secure-secret-with-32-chars";

    /**
     * Access token expiration in seconds.
     */
    private long accessTokenExpireSeconds = 7200;

    /**
     * Refresh token expiration in seconds.
     */
    private long refreshTokenExpireSeconds = 604800;
}
