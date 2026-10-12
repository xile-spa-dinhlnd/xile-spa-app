package com.xilespa.module.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xilespa.common.exception.BusinessException;
import com.xilespa.common.exception.ErrorCode;
import com.xilespa.module.auth.dto.request.LoginRequest;
import com.xilespa.module.auth.dto.response.UserResponse;
import com.xilespa.module.auth.entity.AppUser;
import com.xilespa.module.auth.entity.RefreshToken;
import com.xilespa.module.auth.enums.UserRole;
import com.xilespa.module.auth.mapper.UserMapper;
import com.xilespa.module.auth.repository.AppUserRepository;
import com.xilespa.module.auth.repository.RefreshTokenRepository;
import com.xilespa.module.auth.service.model.LoginResult;
import com.xilespa.module.auth.service.model.RefreshResult;
import com.xilespa.security.JwtProperties;
import com.xilespa.security.JwtService;
import com.xilespa.security.TokenHasher;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private com.xilespa.module.auth.repository.PasswordResetTokenRepository
            passwordResetTokenRepository;

    @Mock private AuthSecurityAuditService authSecurityAuditService;
    @Mock private com.xilespa.module.auth.service.PasswordResetEmailSender passwordResetEmailSender;
    @Mock private com.xilespa.module.auth.validator.PasswordPolicyValidator passwordPolicyValidator;
    @Mock private com.xilespa.common.audit.AuditService auditService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserMapper userMapper;

    private final Instant now = Instant.parse("2026-10-07T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final JwtProperties properties =
            new JwtProperties("secret-key", Duration.ofMinutes(15), Duration.ofDays(7), false);
    private final com.xilespa.module.auth.config.AuthSecurityProperties authSecurityProperties =
            new com.xilespa.module.auth.config.AuthSecurityProperties(
                    5,
                    Duration.ofMinutes(15),
                    Duration.ofMinutes(30),
                    "http://localhost:5173/reset-password");

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService =
                new AuthService(
                        userRepository,
                        refreshTokenRepository,
                        passwordResetTokenRepository,
                        authSecurityAuditService,
                        passwordResetEmailSender,
                        passwordPolicyValidator,
                        auditService,
                        passwordEncoder,
                        jwtService,
                        properties,
                        authSecurityProperties,
                        userMapper,
                        clock);
    }

    @Test
    @DisplayName("Đăng nhập thành công khi đúng email và mật khẩu")
    void login_whenValidCredentials_shouldSucceed() {
        // Arrange
        LoginRequest request = new LoginRequest("owner@xilespa.vn", "password123");
        AppUser user =
                new AppUser("owner@xilespa.vn", "encoded_hash", "Chủ tiệm", UserRole.OWNER, now);
        ReflectionTestUtils.setField(user, "id", 1L);

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
    @DisplayName("Đăng nhập thất bại khi không tìm thấy email (gọi băm giả chống Timing Attack)")
    void login_whenUserNotFound_shouldCallDummyHashAndThrowUnauthorized() {
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

        // Xác nhận đã gọi BCrypt matches với dummy hash để giữ thời gian phản hồi cố định
        verify(passwordEncoder).matches(eq("password123"), any(String.class));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai mật khẩu và gọi audit service ghi nhận số lần sai")
    void login_whenWrongPassword_shouldRecordFailedAttemptAndThrowUnauthorized() {
        // Arrange
        LoginRequest request = new LoginRequest("owner@xilespa.vn", "wrong_password");
        AppUser user =
                new AppUser("owner@xilespa.vn", "encoded_hash", "Chủ tiệm", UserRole.OWNER, now);
        ReflectionTestUtils.setField(user, "id", 10L);

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

        verify(authSecurityAuditService).recordFailedLogin(10L, now);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tài khoản bị vô hiệu hóa")
    void login_whenUserDisabled_shouldThrowUnauthorized() {
        // Arrange
        LoginRequest request = new LoginRequest("owner@xilespa.vn", "password123");
        AppUser user =
                new AppUser("owner@xilespa.vn", "encoded_hash", "Chủ tiệm", UserRole.OWNER, now);
        user.disable(now);

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
    @DisplayName(
            "Đăng nhập thất bại khi tài khoản đang bị tạm khóa (gọi băm giả chống Timing Attack)")
    void login_whenAccountLocked_shouldCallDummyHashAndThrowUnauthorized() {
        // Arrange
        LoginRequest request = new LoginRequest("owner@xilespa.vn", "password123");
        AppUser user =
                new AppUser("owner@xilespa.vn", "encoded_hash", "Chủ tiệm", UserRole.OWNER, now);
        user.lockUntil(now.plus(Duration.ofMinutes(15)), now);

        when(userRepository.findByEmailIgnoreCase("owner@xilespa.vn"))
                .thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> {
                            BusinessException be = (BusinessException) e;
                            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                            assertThat(be.getMessage()).contains("tạm khóa");
                        });

        // Xác nhận đã gọi dummy BCrypt để giữ thời gian phản hồi cố định
        verify(passwordEncoder).matches(eq("password123"), any(String.class));
        verify(authSecurityAuditService, never()).recordFailedLogin(any(), any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đăng nhập thành công khi thời gian tạm khóa đã hết hạn")
    void login_whenLockExpired_andValidCredentials_shouldSucceedAndResetLock() {
        // Arrange
        LoginRequest request = new LoginRequest("owner@xilespa.vn", "password123");
        AppUser user =
                new AppUser("owner@xilespa.vn", "encoded_hash", "Chủ tiệm", UserRole.OWNER, now);
        ReflectionTestUtils.setField(user, "id", 1L);
        // Khóa ở quá khứ (đã hết hạn)
        user.lockUntil(now.minus(Duration.ofMinutes(1)), now);
        ReflectionTestUtils.setField(user, "failedLoginCount", 5);

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
        assertThat(user.getFailedLoginCount()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLastLoginAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Làm mới token thành công và xoay vòng Refresh Token (Token Rotation)")
    void refresh_whenValidToken_shouldRotateAndReturnNewTokens() {
        // Arrange
        String rawToken = "valid-raw-refresh-token";
        String tokenHash = TokenHasher.hash(rawToken);

        AppUser user = new AppUser("owner@xilespa.vn", "hash", "Chủ tiệm", UserRole.OWNER, now);
        ReflectionTestUtils.setField(user, "id", 1L);

        RefreshToken oldToken =
                new RefreshToken(user, tokenHash, now.plus(Duration.ofDays(1)), now);
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
    @DisplayName(
            "Phát hiện tái sử dụng token đã thu hồi -> gọi audit service thu hồi toàn bộ phiên")
    void refresh_whenTokenAlreadyRevoked_shouldCallAuditToRevokeAllUserTokens() {
        // Arrange
        String rawToken = "already-revoked-token";
        String tokenHash = TokenHasher.hash(rawToken);

        AppUser user = new AppUser("owner@xilespa.vn", "hash", "Chủ tiệm", UserRole.OWNER, now);
        ReflectionTestUtils.setField(user, "id", 99L);

        RefreshToken revokedToken =
                new RefreshToken(user, tokenHash, now.plus(Duration.ofDays(1)), now);
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

        verify(authSecurityAuditService).revokeAllUserTokensOnReuse(99L);
    }

    @Test
    @DisplayName("Làm mới token thất bại khi token đã hết hạn")
    void refresh_whenTokenExpired_shouldThrowUnauthorized() {
        // Arrange
        String rawToken = "expired-token";
        String tokenHash = TokenHasher.hash(rawToken);

        AppUser user = new AppUser("owner@xilespa.vn", "hash", "Chủ tiệm", UserRole.OWNER, now);
        RefreshToken expiredToken = new RefreshToken(user, tokenHash, now.minusSeconds(10), now);

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
        String tokenHash = TokenHasher.hash(rawToken);

        AppUser user = new AppUser("owner@xilespa.vn", "hash", "Chủ tiệm", UserRole.OWNER, now);
        RefreshToken token = new RefreshToken(user, tokenHash, now.plus(Duration.ofDays(1)), now);

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        // Act
        authService.logout(rawToken);

        // Assert
        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại khi người dùng không tồn tại")
    void changePassword_whenUserNotFound_throwsNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        var request =
                new com.xilespa.module.auth.dto.request.ChangePasswordRequest(
                        "oldPass", "NewSecurePass123");

        // Act & Assert
        assertThatThrownBy(() -> authService.changePassword(999L, "raw-token", request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại khi mật khẩu hiện tại không đúng")
    void changePassword_whenOldPasswordIncorrect_throwsBadRequest() {
        // Arrange
        AppUser user = new AppUser("owner@xilespa.vn", "old_hash", "Chủ tiệm", UserRole.OWNER, now);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongOldPass", "old_hash")).thenReturn(false);

        var request =
                new com.xilespa.module.auth.dto.request.ChangePasswordRequest(
                        "wrongOldPass", "NewSecurePass123");

        // Act & Assert
        assertThatThrownBy(() -> authService.changePassword(1L, "raw-token", request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST)
                .hasMessageContaining("Mật khẩu hiện tại không chính xác");
    }

    @Test
    @DisplayName(
            "Đổi mật khẩu thành công: cập nhật hash, thu hồi các phiên khác và ghi nhật ký audit")
    void changePassword_whenSuccess_updatesPasswordAndRevokesOtherTokens() {
        // Arrange
        AppUser user = new AppUser("owner@xilespa.vn", "old_hash", "Chủ tiệm", UserRole.OWNER, now);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword123", "old_hash")).thenReturn(true);
        when(passwordEncoder.encode("NewSecurePassword123")).thenReturn("new_encoded_hash");

        String currentRawToken = "current-device-token";
        String currentTokenHash = TokenHasher.hash(currentRawToken);
        var request =
                new com.xilespa.module.auth.dto.request.ChangePasswordRequest(
                        "OldPassword123", "NewSecurePassword123");

        // Act
        authService.changePassword(1L, currentRawToken, request);

        // Assert
        verify(passwordPolicyValidator).validate("NewSecurePassword123", "old_hash");
        verify(userRepository).save(user);
        assertThat(user.getPasswordHash()).isEqualTo("new_encoded_hash");
        verify(refreshTokenRepository).revokeAllActiveByUserIdExceptToken(1L, currentTokenHash);
        verify(auditService)
                .record(
                        eq(com.xilespa.common.audit.AuditAction.UPDATE),
                        eq("app_user"),
                        eq(1L),
                        any(),
                        any());
    }

    @Test
    @DisplayName("Đổi mật khẩu thành công khi không có currentRefreshToken: thu hồi toàn bộ phiên")
    void changePassword_whenNoCurrentToken_revokesAllTokens() {
        // Arrange
        AppUser user = new AppUser("owner@xilespa.vn", "old_hash", "Chủ tiệm", UserRole.OWNER, now);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword123", "old_hash")).thenReturn(true);
        when(passwordEncoder.encode("NewSecurePassword123")).thenReturn("new_encoded_hash");

        var request =
                new com.xilespa.module.auth.dto.request.ChangePasswordRequest(
                        "OldPassword123", "NewSecurePassword123");

        // Act
        authService.changePassword(1L, null, request);

        // Assert
        verify(refreshTokenRepository).revokeAllActiveByUserId(1L);
    }

    @Test
    @DisplayName(
            "Quên mật khẩu: khi email không tồn tại thì không ném lỗi và không gửi email (chống dò email)")
    void forgotPassword_whenEmailNotFound_doesNotThrowAndDoesNotSendEmail() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("unknown@xilespa.vn"))
                .thenReturn(Optional.empty());
        var request =
                new com.xilespa.module.auth.dto.request.ForgotPasswordRequest("unknown@xilespa.vn");

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> authService.forgotPassword(request));
        verify(passwordResetEmailSender, never()).sendResetEmail(any(), any());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Quên mật khẩu: khi email hợp lệ thì tạo token 30 phút và gọi email sender")
    void forgotPassword_whenEmailValid_savesTokenAndSendsEmail() {
        // Arrange
        AppUser user = new AppUser("owner@xilespa.vn", "hash", "Chủ tiệm", UserRole.OWNER, now);
        when(userRepository.findByEmailIgnoreCase("owner@xilespa.vn"))
                .thenReturn(Optional.of(user));
        var request =
                new com.xilespa.module.auth.dto.request.ForgotPasswordRequest("owner@xilespa.vn");

        // Act
        authService.forgotPassword(request);

        // Assert
        verify(passwordResetTokenRepository).save(any());
        verify(passwordResetEmailSender)
                .sendResetEmail(
                        eq("owner@xilespa.vn"),
                        org.mockito.ArgumentMatchers.contains(
                                "http://localhost:5173/reset-password?token="));
    }

    @Test
    @DisplayName("Đặt lại mật khẩu: khi token không tìm thấy thì ném lỗi BAD_REQUEST")
    void resetPassword_whenTokenNotFound_throwsBadRequest() {
        // Arrange
        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        var request =
                new com.xilespa.module.auth.dto.request.ResetPasswordRequest(
                        "invalid-raw-token", "NewPassword123!");

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST)
                .hasMessageContaining("Liên kết đặt lại mật khẩu không hợp lệ");
    }

    @Test
    @DisplayName("Đặt lại mật khẩu: khi token đã dùng thì ném lỗi BAD_REQUEST")
    void resetPassword_whenTokenAlreadyUsed_throwsBadRequest() {
        // Arrange
        AppUser user = new AppUser("owner@xilespa.vn", "hash", "Chủ tiệm", UserRole.OWNER, now);
        var token =
                new com.xilespa.module.auth.entity.PasswordResetToken(
                        user, "hash", now.plus(Duration.ofMinutes(30)), now);
        token.markUsed(now.minus(Duration.ofMinutes(1)));

        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        var request =
                new com.xilespa.module.auth.dto.request.ResetPasswordRequest(
                        "used-token", "NewPassword123!");

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST)
                .hasMessageContaining("Liên kết đặt lại mật khẩu không hợp lệ");
    }

    @Test
    @DisplayName(
            "Đặt lại mật khẩu thành công: đổi pass, mở khóa tài khoản, thu hồi toàn bộ phiên và ghi audit")
    void resetPassword_whenValid_succeedsAndRevokesAllSessions() {
        // Arrange
        AppUser user = new AppUser("owner@xilespa.vn", "old_hash", "Chủ tiệm", UserRole.OWNER, now);
        ReflectionTestUtils.setField(user, "id", 1L);
        user.recordFailedLogin(5, Duration.ofMinutes(15), now); // Giả lập tài khoản đang bị khóa

        var token =
                new com.xilespa.module.auth.entity.PasswordResetToken(
                        user, "token_hash", now.plus(Duration.ofMinutes(30)), now);

        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("BrandNewPassword123!")).thenReturn("new_encoded_hash");

        var request =
                new com.xilespa.module.auth.dto.request.ResetPasswordRequest(
                        "valid-raw-token", "BrandNewPassword123!");

        // Act
        authService.resetPassword(request);

        // Assert
        verify(passwordPolicyValidator).validate("BrandNewPassword123!", "old_hash");
        assertThat(token.isUsed()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("new_encoded_hash");
        assertThat(user.isLocked(now)).isFalse(); // Tài khoản được giải phóng khóa
        verify(refreshTokenRepository).revokeAllActiveByUserId(1L);
        verify(auditService)
                .record(
                        eq(com.xilespa.common.audit.AuditAction.UPDATE),
                        eq("app_user"),
                        eq(1L),
                        any(),
                        any());
    }
}
