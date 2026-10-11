package com.xilespa.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình bảo mật JWT và thời hạn phiên (FR-AUTH-02, NFR-SEC-04).
 *
 * @param secret Khóa bí mật ký JWT (HMAC-SHA256, tối thiểu 256 bits).
 * @param accessTokenDuration Thời hạn sống của Access Token (mặc định 15 phút).
 * @param refreshTokenDuration Thời hạn sống của Refresh Token (mặc định 7 ngày).
 * @param cookieSecure Cờ Secure cho cookie (false trên dev HTTP, true trên production HTTPS).
 */
@ConfigurationProperties(prefix = "xile.security.jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenDuration,
        Duration refreshTokenDuration,
        Boolean cookieSecure) {

    public JwtProperties {
        if (accessTokenDuration == null) {
            accessTokenDuration = Duration.ofMinutes(15);
        }
        if (refreshTokenDuration == null) {
            refreshTokenDuration = Duration.ofDays(7);
        }
        if (cookieSecure == null) {
            cookieSecure = false;
        }
    }
}
