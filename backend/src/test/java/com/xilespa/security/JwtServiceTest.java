package com.xilespa.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtServiceTest {

    private final Instant fixedNow = Instant.parse("2026-10-07T10:00:00Z");
    private Clock clock;
    private JwtProperties properties;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        properties =
                new JwtProperties(
                        "my-super-secret-key-that-is-at-least-256-bits-long-for-test",
                        Duration.ofMinutes(15),
                        Duration.ofDays(7),
                        false);
        jwtService = new JwtService(clock, properties);
    }

    @Test
    @DisplayName("Sinh và giải mã token thành công khi token hợp lệ có issuer xile-spa")
    void generateAndValidateToken_success() {
        // Arrange & Act
        String token = jwtService.generateAccessToken(1L, "owner@xilespa.vn", "Chủ tiệm", "OWNER");
        Optional<JwtClaims> claimsOpt = jwtService.parseAndValidateToken(token);

        // Assert
        assertThat(claimsOpt).isPresent();
        JwtClaims claims = claimsOpt.get();
        assertThat(claims.userId()).isEqualTo(1L);
        assertThat(claims.email()).isEqualTo("owner@xilespa.vn");
        assertThat(claims.displayName()).isEqualTo("Chủ tiệm");
        assertThat(claims.role()).isEqualTo("OWNER");
        assertThat(claims.expiresAt()).isEqualTo(fixedNow.plus(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("Từ chối khi token bị giả mạo chữ ký")
    void parseAndValidateToken_whenSignatureTampered_shouldReturnEmpty() {
        // Arrange
        String token = jwtService.generateAccessToken(1L, "owner@xilespa.vn", "Chủ tiệm", "OWNER");
        String tamperedToken = token.substring(0, token.lastIndexOf('.') + 1) + "invalid_signature";

        // Act
        Optional<JwtClaims> claimsOpt = jwtService.parseAndValidateToken(tamperedToken);

        // Assert
        assertThat(claimsOpt).isEmpty();
    }

    @Test
    @DisplayName("Từ chối khi token có issuer không hợp lệ")
    void parseAndValidateToken_whenIssuerInvalid_shouldReturnEmpty() {
        // Arrange: Tạo token với issuer giả mạo
        NimbusJwtEncoder customEncoder =
                new NimbusJwtEncoder(
                        new ImmutableSecret<SecurityContext>(
                                new SecretKeySpec(
                                        properties.secret().getBytes(StandardCharsets.UTF_8),
                                        "HmacSHA256")));
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer("malicious-issuer")
                        .subject("1")
                        .claim("email", "owner@xilespa.vn")
                        .claim("name", "Chủ tiệm")
                        .claim("role", "OWNER")
                        .issuedAt(fixedNow)
                        .expiresAt(fixedNow.plus(Duration.ofMinutes(15)))
                        .build();
        String fakeToken =
                customEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        // Act
        Optional<JwtClaims> claimsOpt = jwtService.parseAndValidateToken(fakeToken);

        // Assert
        assertThat(claimsOpt).isEmpty();
    }

    @Test
    @DisplayName("Từ chối khi token đã hết hạn")
    void parseAndValidateToken_whenExpired_shouldReturnEmpty() {
        // Arrange: Token sinh ở thời điểm fixedNow (hết hạn sau 15p)
        String token = jwtService.generateAccessToken(1L, "owner@xilespa.vn", "Chủ tiệm", "OWNER");

        // Đồng hồ dịch chuyển tới 20 phút sau
        Clock futureClock = Clock.fixed(fixedNow.plus(Duration.ofMinutes(20)), ZoneOffset.UTC);
        JwtService futureJwtService = new JwtService(futureClock, properties);

        // Act
        Optional<JwtClaims> claimsOpt = futureJwtService.parseAndValidateToken(token);

        // Assert
        assertThat(claimsOpt).isEmpty();
    }

    @Test
    @DisplayName("Ném lỗi khi secret để trống")
    void constructor_whenSecretBlank_shouldThrowException() {
        JwtProperties emptyProps =
                new JwtProperties("", Duration.ofMinutes(15), Duration.ofDays(7), false);
        assertThatThrownBy(() -> new JwtService(clock, emptyProps))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("không được để trống");
    }

    @Test
    @DisplayName("Ném lỗi khi secret ngắn hơn 32 bytes (256 bits)")
    void constructor_whenSecretTooShort_shouldThrowException() {
        JwtProperties shortProps =
                new JwtProperties(
                        "short-secret-under-32-bytes",
                        Duration.ofMinutes(15),
                        Duration.ofDays(7),
                        false);
        assertThatThrownBy(() -> new JwtService(clock, shortProps))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ít nhất 32 bytes");
    }
}
