package com.xilespa.module.auth.service.model;

import com.xilespa.module.auth.dto.response.UserResponse;

/**
 * Kết quả nội bộ của đăng nhập (FR-AUTH-01). Chứa token thô nên KHÔNG BAO GIỜ trả thẳng ra API:
 * controller chỉ đặt token vào cookie httpOnly và trả {@link UserResponse}.
 */
public record LoginResult(UserResponse user, String accessToken, String rawRefreshToken) {}
