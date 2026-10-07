package com.xilespa.module.auth.dto.response;

/**
 * Kết quả đăng nhập thành công chứa thông tin người dùng và cặp token (FR-AUTH-01).
 *
 * @param user thông tin người dùng
 * @param accessToken chuỗi JWT access token ngắn hạn
 * @param rawRefreshToken chuỗi refresh token thô để thiết lập cookie
 */
public record LoginResponse(UserResponse user, String accessToken, String rawRefreshToken) {}
