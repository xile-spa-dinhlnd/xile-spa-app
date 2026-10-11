package com.xilespa.module.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.xilespa.common.audit.AuditAction;
import com.xilespa.common.audit.AuditService;
import com.xilespa.module.auth.config.OwnerInitializerProperties;
import com.xilespa.module.auth.entity.AppUser;
import com.xilespa.module.auth.enums.UserRole;
import com.xilespa.module.auth.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class OwnerAccountInitializerTest {

    private final java.time.Clock clock =
            java.time.Clock.fixed(
                    java.time.Instant.parse("2026-10-11T03:00:00Z"), java.time.ZoneOffset.UTC);

    @Mock private AppUserRepository userRepository;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private AuditService auditService;

    @Test
    @DisplayName("Bỏ qua khi cấu hình enabled = false")
    void run_whenDisabled_shouldDoNothing() {
        // Arrange
        OwnerInitializerProperties properties =
                new OwnerInitializerProperties(false, "owner@xilespa.vn", "secret123", "Chủ tiệm");
        OwnerAccountInitializer initializer =
                new OwnerAccountInitializer(
                        userRepository, passwordEncoder, properties, clock, auditService);

        // Act
        initializer.run(new DefaultApplicationArguments());

        // Assert
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Bỏ qua khi không cấu hình email hoặc mật khẩu")
    void run_whenNotConfigured_shouldDoNothing() {
        // Arrange
        OwnerInitializerProperties properties = new OwnerInitializerProperties(true, "", "", "");
        OwnerAccountInitializer initializer =
                new OwnerAccountInitializer(
                        userRepository, passwordEncoder, properties, clock, auditService);

        // Act
        initializer.run(new DefaultApplicationArguments());

        // Assert
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Bỏ qua khi đã tồn tại tài khoản có vai trò OWNER")
    void run_whenOwnerAlreadyExists_shouldSkip() {
        // Arrange
        OwnerInitializerProperties properties =
                new OwnerInitializerProperties(true, "owner@xilespa.vn", "secret123", "Chủ tiệm");
        when(userRepository.existsByRole(UserRole.OWNER)).thenReturn(true);
        OwnerAccountInitializer initializer =
                new OwnerAccountInitializer(
                        userRepository, passwordEncoder, properties, clock, auditService);

        // Act
        initializer.run(new DefaultApplicationArguments());

        // Assert
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Bỏ qua khi email đã tồn tại trong bảng app_user")
    void run_whenEmailAlreadyExists_shouldSkip() {
        // Arrange
        OwnerInitializerProperties properties =
                new OwnerInitializerProperties(true, "owner@xilespa.vn", "secret123", "Chủ tiệm");
        when(userRepository.existsByRole(UserRole.OWNER)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("owner@xilespa.vn")).thenReturn(true);
        OwnerAccountInitializer initializer =
                new OwnerAccountInitializer(
                        userRepository, passwordEncoder, properties, clock, auditService);

        // Act
        initializer.run(new DefaultApplicationArguments());

        // Assert
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Khởi tạo thành công tài khoản chủ tiệm khi cấu hình hợp lệ và chưa có chủ tiệm")
    void run_whenValidConfigAndNoOwner_shouldCreateOwner() {
        // Arrange
        OwnerInitializerProperties properties =
                new OwnerInitializerProperties(
                        true, " Owner@XileSpa.vn ", "my_password_123", " Đình ");
        when(userRepository.existsByRole(UserRole.OWNER)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("owner@xilespa.vn")).thenReturn(false);
        when(passwordEncoder.encode("my_password_123")).thenReturn("hashed_password_xyz");
        OwnerAccountInitializer initializer =
                new OwnerAccountInitializer(
                        userRepository, passwordEncoder, properties, clock, auditService);

        // Act
        initializer.run(new DefaultApplicationArguments());

        // Assert
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());

        AppUser saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("owner@xilespa.vn");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed_password_xyz");
        assertThat(saved.getDisplayName()).isEqualTo("Đình");
        assertThat(saved.getRole()).isEqualTo(UserRole.OWNER);
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getFailedLoginCount()).isZero();

        // Ghi nhật ký tạo tài khoản, không kèm mật khẩu hay hash (BR-18, NFR-SEC-02)
        ArgumentCaptor<Object> afterCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditService)
                .record(
                        eq(AuditAction.CREATE),
                        eq("app_user"),
                        any(),
                        isNull(),
                        afterCaptor.capture());
        assertThat(afterCaptor.getValue().toString())
                .contains("owner@xilespa.vn")
                .doesNotContain("hashed_password_xyz", "my_password_123");
    }
}
