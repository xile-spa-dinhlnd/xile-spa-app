package com.xilespa.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Băm refresh token thô bằng SHA-256 (hex) để lưu vào CSDL, không lưu token thô (NFR-SEC-04).
 * Refresh token là chuỗi ngẫu nhiên 256 bit, không phải JWT, nên tách khỏi {@link JwtService}. Dùng
 * SHA-256 thay vì bcrypt vì cần cùng một kết quả để tra cứu token theo giá trị băm.
 */
public final class TokenHasher {

    private TokenHasher() {}

    public static String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token không được để trống khi băm");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Thuật toán SHA-256 không khả dụng", e);
        }
    }
}
