package com.xilespa.module.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình bảo mật đăng nhập, chống dò mật khẩu và đặt lại mật khẩu (FR-AUTH-03, FR-AUTH-05,
 * NFR-SEC-05). Giá trị mặc định được cấu hình tập trung tại application.yml.
 *
 * @param maxFailedAttempts Số lần đăng nhập sai liên tiếp tối đa trước khi khóa tài khoản.
 * @param lockDuration Thời gian tạm khóa tài khoản.
 * @param passwordResetDuration Thời gian hiệu lực của token đặt lại mật khẩu.
 * @param resetPasswordBaseUrl Đường dẫn cơ sở frontend để đặt lại mật khẩu.
 */
@ConfigurationProperties(prefix = "xile.security.auth")
public record AuthSecurityProperties(
        int maxFailedAttempts,
        Duration lockDuration,
        Duration passwordResetDuration,
        String resetPasswordBaseUrl) {}
