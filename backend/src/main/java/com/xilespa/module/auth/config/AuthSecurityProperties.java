package com.xilespa.module.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình bảo mật đăng nhập, chống dò mật khẩu và đặt lại mật khẩu (FR-AUTH-03, FR-AUTH-05,
 * NFR-SEC-05).
 *
 * @param maxFailedAttempts Số lần đăng nhập sai liên tiếp tối đa trước khi khóa tài khoản (mặc định
 *     5).
 * @param lockDuration Thời gian tạm khóa tài khoản (mặc định 15 phút).
 * @param passwordResetDuration Thời gian hiệu lực của token đặt lại mật khẩu (mặc định 30 phút).
 * @param resetPasswordBaseUrl Đường dẫn cơ sở frontend để đặt lại mật khẩu.
 */
@ConfigurationProperties(prefix = "xile.security.auth")
public record AuthSecurityProperties(
        Integer maxFailedAttempts,
        Duration lockDuration,
        Duration passwordResetDuration,
        String resetPasswordBaseUrl) {

    public AuthSecurityProperties {
        if (maxFailedAttempts == null || maxFailedAttempts <= 0) {
            maxFailedAttempts = 5;
        }
        if (lockDuration == null || lockDuration.isNegative() || lockDuration.isZero()) {
            lockDuration = Duration.ofMinutes(15);
        }
        if (passwordResetDuration == null
                || passwordResetDuration.isNegative()
                || passwordResetDuration.isZero()) {
            passwordResetDuration = Duration.ofMinutes(30);
        }
        if (resetPasswordBaseUrl == null || resetPasswordBaseUrl.isBlank()) {
            resetPasswordBaseUrl = "http://localhost:5173/reset-password";
        }
    }
}
