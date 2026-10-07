package com.xilespa.module.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xilespa.common.exception.BusinessException;
import com.xilespa.common.exception.ErrorCode;
import com.xilespa.module.auth.dto.request.LoginRequest;
import com.xilespa.module.auth.dto.response.UserResponse;
import com.xilespa.module.auth.entity.AppUser;
import com.xilespa.module.auth.entity.RefreshToken;
import com.xilespa.module.auth.entity.UserRole;
import com.xilespa.module.auth.mapper.UserMapper;
import com.xilespa.module.auth.repository.AppUserRepository;
import com.xilespa.module.auth.repository.RefreshTokenRepository;
import com.xilespa.module.auth.service.AuthService.LoginResult;
import com.xilespa.module.auth.service.AuthService.RefreshResult;
import com.xilespa.security.JwtProperties;
import com.xilespa.security.JwtService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserMapper userMapper;

    private final Instant now = Instant.parse("2026-10-07T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final JwtProperties properties =
            new JwtProperties("secret-key", Duration.ofMinutes(15), Duration.ofDays(7), false);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService =
                new AuthService(
                        userRepository,
                        refreshTokenRepository,
                        passwordEncoder,
                        jwtService,
                        properties,
                        userMapper,
                        clock);
    }

    @Test
    @DisplayName("Đăng nhập thành công khi đúng email và mật khẩu")
    void login_whenValidCredentials_shouldSucceed() {
        // Arrange
        LoginRequest request = new LoginRequest("owner@xilespa.vn", "password123");
        AppUser user = new AppUser("owner@xilespa.vn", "encoded_hash", "Chủ tiệm", UserRole.OWNER);
        user.setId(1L);

        when(userRepository.findByEmailIgnoreCase("owner@xilespa.vn"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_hash")).thenReturn(true);
        when(jwtService.generateAccessToken(1L, "owner@xilespa.vn", "Chủ tiệm", "OWNER"))
                .thenReturn("access.jwt.token");
        when(userMapper.toResponse(user))
                .thenReturn(new UserResponse(1L, "owner@xilespa.vn", "Chủ tiệm", UserRole.OWNER));

        // Act
        LoginResult result = authService.login(request);

        // Assert
        assertThat(result.accessToken()).isEqualTo("access.jwt.token");
        assertThat(result.rawRefreshToken()).isNotBlank();
        assertThat(result.user().email()).isEqualTo("owner@xilespa.vn");
        assertThat(user.getFailedLoginCount()).isZero();
        assertThat(user.getLastLoginAt()).isEqualTo(now);

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi không tìm thấy email (báo lỗi chung không lộ thông tin)")
    void login_whenUserNotFound_shouldThrowUnauthorized() {
        // Arrange
        LoginRequest request = new LoginRequest("unknown@xilespa.vn", "password123");
        when(userRepository.findByEmailIgnoreCase("unknown@xilespa.vn"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> {
                            BusinessException be = (BusinessException) e;
                            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                            assertThat(be.getMessage())
                                    .isEqualTo("Email hoặc mật khẩu không đúng.");
                        });

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai mật khẩu và tăng số lần đăng nhập sai")
    void login_whenWrongPassword_shouldIncrementFailedCountAndThrowUnauthorized() {
        // Arrange
        LoginRequest request = new LoginRequest("owner@xilespa.vn", "wrong_password");
        AppUser user = new AppUser("owner@xilespa.vn", "encoded_hash", "Chủ tiệm", UserRole.OWNER);
        user.setFailedLoginCount(1);

        when(userRepository.findByEmailIgnoreCase("owner@xilespa.vn"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "encoded_hash")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> {
                            BusinessException be = (BusinessException) e;
                            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                            assertThat(be.getMessage())
                                    .isEqualTo("Email hoặc mật khẩu không đúng.");
                        });

        assertThat(user.getFailedLoginCount()).isEqualTo(2);
        verify(userRepository).save(user);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tài khoản bị vô hiệu hóa")
    void login_whenUserDisabled_shouldThrowUnauthorized() {
        // Arrange
        LoginRequest request = new LoginRequest("owner@xilespa.vn", "password123");
        AppUser user = new AppUser("owner@xilespa.vn", "encoded_hash", "Chủ tiệm", UserRole.OWNER);
        user.setEnabled(false);

        when(userRepository.findByEmailIgnoreCase("owner@xilespa.vn"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_hash")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> {
                            BusinessException be = (BusinessException) e;
                            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                            assertThat(be.getMessage()).contains("vô hiệu hóa");
                        });
    }

    @Test
    @DisplayName("Làm mới token thành công và xoay vòng Refresh Token (Token Rotation)")
    void refresh_whenValidToken_shouldRotateAndReturnNewTokens() {
        // Arrange
        String rawToken = "valid-raw-refresh-token";
        String tokenHash = JwtService.hashToken(rawToken);

        AppUser user = new AppUser("owner@xilespa.vn", "hash", "Chủ tiệm", UserRole.OWNER);
        user.setId(1L);

        RefreshToken oldToken = new RefreshToken(user, tokenHash, now.plus(Duration.ofDays(1)));
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(oldToken));
        when(jwtService.generateAccessToken(1L, "owner@xilespa.vn", "Chủ tiệm", "OWNER"))
                .thenReturn("new.access.token");

        // Act
        RefreshResult result = authService.refresh(rawToken);

        // Assert
        assertThat(result.accessToken()).isEqualTo("new.access.token");
        assertThat(result.rawRefreshToken()).isNotBlank();
        assertThat(oldToken.isRevoked()).isTrue();
        assertThat(oldToken.getRevokedAt()).isEqualTo(now);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(tokenCaptor.capture());
        java.util.List<RefreshToken> savedTokens = tokenCaptor.getAllValues();
        assertThat(savedTokens.get(0)).isSameAs(oldToken);
        assertThat(savedTokens.get(0).isRevoked()).isTrue();

        RefreshToken newToken = savedTokens.get(1);
        assertThat(newToken.getUser()).isEqualTo(user);
        assertThat(newToken.isValid(now)).isTrue();
    }

    @Test
    @DisplayName("Phát hiện tái sử dụng token đã thu hồi -> thu hồi toàn bộ phiên của người dùng")
    void refresh_whenTokenAlreadyRevoked_shouldRevokeAllUserTokens() {
        // Arrange
        String rawToken = "already-revoked-token";
        String tokenHash = JwtService.hashToken(rawToken);

        AppUser user = new AppUser("owner@xilespa.vn", "hash", "Chủ tiệm", UserRole.OWNER);
        user.setId(99L);

        RefreshToken revokedToken = new RefreshToken(user, tokenHash, now.plus(Duration.ofDays(1)));
        revokedToken.revoke(now.minusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(revokedToken));

        // Act & Assert
        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> {
                            BusinessException be = (BusinessException) e;
                            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                            assertThat(be.getMessage()).contains("không an toàn");
                        });

        verify(refreshTokenRepository).revokeAllActiveByUserId(99L);
    }

    @Test
    @DisplayName("Làm mới token thất bại khi token đã hết hạn")
    void refresh_whenTokenExpired_shouldThrowUnauthorized() {
        // Arrange
        String rawToken = "expired-token";
        String tokenHash = JwtService.hashToken(rawToken);

        AppUser user = new AppUser("owner@xilespa.vn", "hash", "Chủ tiệm", UserRole.OWNER);
        RefreshToken expiredToken = new RefreshToken(user, tokenHash, now.minusSeconds(10));

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(expiredToken));

        // Act & Assert
        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> {
                            BusinessException be = (BusinessException) e;
                            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                            assertThat(be.getMessage()).contains("hết hạn");
                        });
    }

    @Test
    @DisplayName("Đăng xuất thu hồi Refresh Token trong CSDL")
    void logout_shouldRevokeTokenInDatabase() {
        // Arrange
        String rawToken = "logout-raw-token";
        String tokenHash = JwtService.hashToken(rawToken);

        AppUser user = new AppUser("owner@xilespa.vn", "hash", "Chủ tiệm", UserRole.OWNER);
        RefreshToken token = new RefreshToken(user, tokenHash, now.plus(Duration.ofDays(1)));

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        // Act
        authService.logout(rawToken);

        // Assert
        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isEqualTo(now);
    }
}
