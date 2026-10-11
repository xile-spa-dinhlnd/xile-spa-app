package com.xilespa.module.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO yêu cầu đăng nhập tài khoản (FR-AUTH-01). */
public record LoginRequest(
        @NotBlank(message = "Email không được để trống")
                @Email(message = "Email không đúng định dạng")
                String email,
        @NotBlank(message = "Mật khẩu không được để trống") String password) {}
