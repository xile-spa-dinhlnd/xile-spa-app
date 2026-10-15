package com.xilespa.module.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Yêu cầu gửi liên kết đặt lại mật khẩu (FR-AUTH-03).
 *
 * @param email Địa chỉ email của tài khoản cần đặt lại mật khẩu.
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email không được để trống")
                @Email(message = "Email không đúng định dạng")
                String email) {}
