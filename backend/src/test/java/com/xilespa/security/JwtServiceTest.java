package com.xilespa.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

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
        jwtService = new JwtService(clock, new ObjectMapper(), properties);
    }

    @Test
    @DisplayName("Sinh và giải mã token thành công khi token hợp lệ")
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
    @DisplayName("Từ chối khi token đã hết hạn")
    void parseAndValidateToken_whenExpired_shouldReturnEmpty() {
        // Arrange: Token sinh ở thời điểm fixedNow (hết hạn sau 15p)
        String token = jwtService.generateAccessToken(1L, "owner@xilespa.vn", "Chủ tiệm", "OWNER");

        // Đồng hồ dịch chuyển tới 20 phút sau
        Clock futureClock = Clock.fixed(fixedNow.plus(Duration.ofMinutes(20)), ZoneOffset.UTC);
        JwtService futureJwtService = new JwtService(futureClock, new ObjectMapper(), properties);

        // Act
        Optional<JwtClaims> claimsOpt = futureJwtService.parseAndValidateToken(token);

        // Assert
        assertThat(claimsOpt).isEmpty();
    }

    @Test
    @DisplayName("Băm token SHA-256 nhất quán và đúng định dạng hex 64 ký tự")
    void hashToken_sha256Hex() {
        // Arrange
        String rawToken = "raw-refresh-token-123456";

        // Act
        String hash1 = JwtService.hashToken(rawToken);
        String hash2 = JwtService.hashToken(rawToken);

        // Assert
        assertThat(hash1).hasSize(64);
        assertThat(hash1).isEqualTo(hash2);
        assertThat(JwtService.hashToken("different-token")).isNotEqualTo(hash1);
    }

    @Test
    @DisplayName("Ném lỗi khi secret để trống")
    void constructor_whenSecretBlank_shouldThrowException() {
        JwtProperties emptyProps =
                new JwtProperties("", Duration.ofMinutes(15), Duration.ofDays(7), false);
        assertThatThrownBy(() -> new JwtService(clock, new ObjectMapper(), emptyProps))
                .isInstanceOf(IllegalStateException.class);
    }
}
