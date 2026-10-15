package com.xilespa.module.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xilespa.common.audit.AuditAction;
import com.xilespa.common.audit.AuditService;
import com.xilespa.module.auth.config.AuthSecurityProperties;
import com.xilespa.module.auth.entity.AppUser;
import com.xilespa.module.auth.enums.UserRole;
import com.xilespa.module.auth.repository.AppUserRepository;
import com.xilespa.module.auth.repository.RefreshTokenRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthSecurityAuditServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AuditService auditService;

    private final AuthSecurityProperties properties =
            new AuthSecurityProperties(
                    5,
                    Duration.ofMinutes(15),
                    Duration.ofMinutes(30),
                    "http://localhost:5173/reset-password");

    private AuthSecurityAuditService authSecurityAuditService;
    private final Instant now = Instant.parse("2026-10-14T10:00:00Z");

    @BeforeEach
    void setUp() {
        authSecurityAuditService =
                new AuthSecurityAuditService(
                        userRepository, refreshTokenRepository, properties, auditService);
    }

    @Test
    @DisplayName("Ghi nhận đăng nhập sai khi chưa đạt ngưỡng khóa: tăng bộ đếm, không ghi audit")
    void recordFailedLogin_whenNotReachedThreshold_incrementsCountWithoutAudit() {
        // Arrange
        AppUser user = new AppUser("user@xilespa.vn", "hash", "User", UserRole.OWNER, now);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        authSecurityAuditService.recordFailedLogin(1L, now);

        // Assert
        assertThat(user.getFailedLoginCount()).isEqualTo(1);
        assertThat(user.isLocked(now)).isFalse();
        verify(userRepository).save(user);
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Ghi nhận đăng nhập sai khi chạm ngưỡng khóa: khóa tài khoản và ghi audit log")
    void recordFailedLogin_whenThresholdReached_locksAccountAndRecordsAudit() {
        // Arrange
        AppUser user = new AppUser("user@xilespa.vn", "hash", "User", UserRole.OWNER, now);
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "failedLoginCount", 4);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        authSecurityAuditService.recordFailedLogin(1L, now);

        // Assert
        assertThat(user.getFailedLoginCount()).isEqualTo(5);
        assertThat(user.isLocked(now)).isTrue();
        verify(userRepository).save(user);
        verify(auditService).record(eq(AuditAction.UPDATE), eq("app_user"), eq(1L), any(), any());
    }

    @Test
    @DisplayName("Thu hồi toàn bộ token khi phát hiện token cũ bị tái sử dụng")
    void revokeAllUserTokensOnReuse_callsRepository() {
        // Act
        authSecurityAuditService.revokeAllUserTokensOnReuse(1L);

        // Assert
        verify(refreshTokenRepository).revokeAllActiveByUserId(1L);
    }
}
