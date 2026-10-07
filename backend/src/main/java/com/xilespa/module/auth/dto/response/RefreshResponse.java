package com.xilespa.module.auth.dto.response;

/**
 * Kết quả làm mới phiên thành công chứa cặp token mới sau xoay vòng (FR-AUTH-02).
 *
 * @param accessToken chuỗi JWT access token mới
 * @param rawRefreshToken chuỗi refresh token mới để thiết lập cookie
 */
public record RefreshResponse(String accessToken, String rawRefreshToken) {}
