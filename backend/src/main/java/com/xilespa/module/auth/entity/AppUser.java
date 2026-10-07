package com.xilespa.module.auth.entity;

import com.xilespa.module.auth.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Thực thể người dùng ứng dụng, ánh xạ bảng {@code app_user} (V1__core_schema.sql). Áp dụng Rich
 * Domain Model: đóng gói dữ liệu, chỉ thay đổi trạng thái qua các phương thức nghiệp vụ.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.OWNER;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public AppUser() {}

    public AppUser(String email, String passwordHash, String displayName, UserRole role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.enabled = true;
        this.failedLoginCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // --- Domain Methods (Hành vi nghiệp vụ) ---

    /** Ghi nhận một lần đăng nhập thất bại (chống dò mật khẩu, FR-AUTH-05). */
    public void recordFailedLogin(Instant now) {
        this.failedLoginCount++;
        this.updatedAt = now;
    }

    /** Ghi nhận đăng nhập thành công: xóa bộ đếm lỗi và cập nhật thời điểm đăng nhập gần nhất. */
    public void recordSuccessfulLogin(Instant now) {
        this.failedLoginCount = 0;
        this.lastLoginAt = now;
        this.updatedAt = now;
    }

    /** Đổi mật khẩu tài khoản (FR-AUTH-04). */
    public void changePassword(String newPasswordHash, Instant now) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = now;
    }

    /** Cập nhật tên hiển thị người dùng. */
    public void updateDisplayName(String newDisplayName, Instant now) {
        this.displayName = newDisplayName;
        this.updatedAt = now;
    }

    /** Tạm khóa tài khoản đến một thời điểm nhất định. */
    public void lockUntil(Instant lockTime, Instant now) {
        this.lockedUntil = lockTime;
        this.updatedAt = now;
    }

    public void enable(Instant now) {
        this.enabled = true;
        this.updatedAt = now;
    }

    public void disable(Instant now) {
        this.enabled = false;
        this.updatedAt = now;
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
