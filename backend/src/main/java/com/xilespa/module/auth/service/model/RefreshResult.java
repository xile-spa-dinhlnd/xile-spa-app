package com.xilespa.module.auth.service.model;

/**
 * Kết quả nội bộ của làm mới phiên (FR-AUTH-02). Chứa token thô nên KHÔNG BAO GIỜ trả thẳng ra API:
 * controller chỉ đặt token vào cookie httpOnly.
 */
public record RefreshResult(String accessToken, String rawRefreshToken) {}
