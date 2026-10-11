package com.xilespa.module.auth.dto.response;

import com.xilespa.module.auth.enums.UserRole;

/** DTO trả về thông tin người dùng đã xác thực. */
public record UserResponse(Long id, String email, String displayName, UserRole role) {}
