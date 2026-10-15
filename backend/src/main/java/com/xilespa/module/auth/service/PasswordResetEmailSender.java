package com.xilespa.module.auth.service;

/** Giao diện gửi thông báo/liên kết đặt lại mật khẩu qua email (FR-AUTH-03, OQ-10). */
public interface PasswordResetEmailSender {

    /**
     * Gửi email chứa liên kết đặt lại mật khẩu cho người dùng.
     *
     * @param toEmail Địa chỉ email người nhận.
     * @param resetLink Đường dẫn hoàn chỉnh chứa token đặt lại mật khẩu.
     */
    void sendResetEmail(String toEmail, String resetLink);
}
