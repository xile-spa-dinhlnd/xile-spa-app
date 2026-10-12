package com.xilespa.module.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Yêu cầu đổi mật khẩu khi đã đăng nhập (FR-AUTH-04, NFR-SEC-03).
 *
 * @param oldPassword Mật khẩu hiện tại.
 * @param newPassword Mật khẩu mới (tối thiểu 10 ký tự, chặn mật khẩu phổ biến).
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Mật khẩu hiện tại không được để trống") String oldPassword,
        @NotBlank(message = "Mật khẩu mới không được để trống")
                @Size(min = 10, message = "Mật khẩu mới phải có tối thiểu 10 ký tự")
                String newPassword) {}
