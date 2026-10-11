package com.xilespa.security;

import java.time.Instant;

/** Dữ liệu trích xuất từ JWT Access Token. */
public record JwtClaims(
        Long userId, String email, String displayName, String role, Instant expiresAt) {}
