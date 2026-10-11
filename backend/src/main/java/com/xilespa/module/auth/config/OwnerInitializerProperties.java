package com.xilespa.module.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình khởi tạo tài khoản chủ tiệm từ biến môi trường (FR-AUTH-07).
 *
 * @param enabled Bật/tắt việc tự động khởi tạo (mặc định true).
 * @param email Địa chỉ email của chủ tiệm.
 * @param password Mật khẩu ban đầu.
 * @param displayName Tên hiển thị (mặc định "Chủ tiệm Xile").
 */
@ConfigurationProperties(prefix = "xile.init.owner")
public record OwnerInitializerProperties(
        Boolean enabled, String email, String password, String displayName) {

    public OwnerInitializerProperties {
        if (enabled == null) {
            enabled = true;
        }
    }

    public boolean isConfigured() {
        return Boolean.TRUE.equals(enabled)
                && email != null
                && !email.isBlank()
                && password != null
                && !password.isBlank();
    }
}
