package com.xilespa.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Xử lý tạo và xác thực Access Token (JWT HMAC-SHA256). Sử dụng {@link NimbusJwtEncoder} và {@link
 * NimbusJwtDecoder} của Spring Security OAuth2 JOSE (S2-04, ADR-0001). Băm refresh token: {@link
 * TokenHasher}.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final Clock clock;
    private final JwtProperties properties;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    @org.springframework.beans.factory.annotation.Autowired
    public JwtService(Clock clock, JwtProperties properties) {
        this(clock, null, properties);
    }

    public JwtService(Clock clock, ObjectMapper objectMapper, JwtProperties properties) {
        this.clock = clock;
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

        SecretKey secretKey =
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        JWKSource<SecurityContext> jwkSource = new ImmutableSecret<>(secretKey);
        this.jwtEncoder = new NimbusJwtEncoder(jwkSource);

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
        timestampValidator.setClock(clock);
        decoder.setJwtValidator(timestampValidator);
        this.jwtDecoder = decoder;
    }

    /** Sinh JWT Access Token (HMAC-SHA256) với thời hạn cấu hình. */
    public String generateAccessToken(Long userId, String email, String displayName, String role) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.accessTokenDuration());

        try {
            JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();

            JwtClaimsSet claims =
                    JwtClaimsSet.builder()
                            .subject(String.valueOf(userId))
                            .claim("email", email)
                            .claim("name", displayName)
                            .claim("role", role)
                            .issuedAt(now)
                            .expiresAt(expiresAt)
                            .build();

            return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
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

        try {
            Jwt jwt = jwtDecoder.decode(token);

            long userId = Long.parseLong(jwt.getSubject());
            String email = jwt.getClaimAsString("email");
            String displayName = jwt.getClaimAsString("name");
            String role = jwt.getClaimAsString("role");
            Instant expiresAt = jwt.getExpiresAt();

            return Optional.of(new JwtClaims(userId, email, displayName, role, expiresAt));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token không hợp lệ hoặc giải mã thất bại: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
