package com.xilespa.module.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Yêu cầu đặt lại mật khẩu với token xác thực (FR-AUTH-03, NFR-SEC-03).
 *
 * @param token Token xác thực được gửi qua liên kết email.
 * @param newPassword Mật khẩu mới (tối thiểu 10 ký tự, chặn mật khẩu phổ biến).
 */
public record ResetPasswordRequest(
        @NotBlank(message = "Mã xác thực không được để trống") String token,
        @NotBlank(message = "Mật khẩu mới không được để trống")
                @Size(min = 10, message = "Mật khẩu mới phải có tối thiểu 10 ký tự")
                String newPassword) {}
