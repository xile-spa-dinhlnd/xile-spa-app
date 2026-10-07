package com.xilespa.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Xử lý tạo và xác thực Access Token (JWT HMAC-SHA256) và băm Refresh Token (SHA-256). Sử dụng JDK
 * JCA chuẩn và Jackson 3, không cần dependency bổ sung (ADR-0001, NFR-SEC-04).
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final Base64.Encoder B64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_URL_DECODER = Base64.getUrlDecoder();

    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final JwtProperties properties;
    private final SecretKeySpec secretKeySpec;

    public JwtService(Clock clock, ObjectMapper objectMapper, JwtProperties properties) {
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.properties = properties;

        String secret = properties.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Cấu hình jwt.secret không được để trống (xile.security.jwt.secret).");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT secret key phải có độ dài ít nhất 32 bytes (256 bits) cho thuật toán HMAC-SHA256 (NFR-SEC-04).");
        }
        this.secretKeySpec =
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    /** Sinh JWT Access Token (HMAC-SHA256) với thời hạn cấu hình. */
    public String generateAccessToken(Long userId, String email, String displayName, String role) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.accessTokenDuration());

        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            String headerJson = objectMapper.writeValueAsString(header);
            String headerEncoded =
                    B64_URL_ENCODER.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", String.valueOf(userId));
            payload.put("email", email);
            payload.put("name", displayName);
            payload.put("role", role);
            payload.put("iat", now.getEpochSecond());
            payload.put("exp", expiresAt.getEpochSecond());

            String payloadJson = objectMapper.writeValueAsString(payload);
            String payloadEncoded =
                    B64_URL_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signingInput = headerEncoded + "." + payloadEncoded;
            byte[] signatureBytes = sign(signingInput);
            String signatureEncoded = B64_URL_ENCODER.encodeToString(signatureBytes);

            return signingInput + "." + signatureEncoded;
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi khi sinh access token JWT", e);
        }
    }

    /**
     * Xác thực chữ ký và kiểm tra hạn dùng của token. Trả về rỗng nếu token không hợp lệ hoặc hết
     * hạn.
     */
    public Optional<JwtClaims> parseAndValidateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        try {
            String signingInput = parts[0] + "." + parts[1];
            byte[] expectedSignature = sign(signingInput);
            byte[] actualSignature = B64_URL_DECODER.decode(parts[2]);

            // So sánh độ dài không đổi (constant-time) chống tấn công timing attack
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                return Optional.empty();
            }

            byte[] payloadBytes = B64_URL_DECODER.decode(parts[1]);
            JsonNode payloadNode = objectMapper.readTree(payloadBytes);

            long expSeconds = payloadNode.path("exp").asLong(0);
            Instant expiresAt = Instant.ofEpochSecond(expSeconds);
            if (expiresAt.isBefore(clock.instant())) {
                return Optional.empty();
            }

            long userId = Long.parseLong(payloadNode.path("sub").asText());
            String email = payloadNode.path("email").asText();
            String displayName = payloadNode.path("name").asText();
            String role = payloadNode.path("role").asText();

            return Optional.of(new JwtClaims(userId, email, displayName, role, expiresAt));
        } catch (Exception e) {
            log.debug("Token không hợp lệ hoặc giải mã thất bại: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Băm chuỗi Refresh Token thô bằng SHA-256 dạng hex để lưu trữ an toàn trong CSDL. */
    public static String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token không được để trống khi băm");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Thuật toán SHA-256 không khả dụng", e);
        }
    }

    private byte[] sign(String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(secretKeySpec);
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }
}
