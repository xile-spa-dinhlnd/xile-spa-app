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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Thực thể người dùng ứng dụng, ánh xạ bảng {@code app_user} (V1__core_schema.sql). Áp dụng Rich
 * Domain Model: đóng gói dữ liệu, chỉ thay đổi trạng thái qua các phương thức nghiệp vụ. Lombok chỉ
 * sinh getter và constructor rỗng cho JPA (quy tắc trong {@code backend/AGENTS.md}).
 */
@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Tạo tài khoản mới.
     *
     * @param now thời điểm tạo, lấy từ {@link java.time.Clock} của hệ thống (không dùng
     *     Instant.now())
     */
    public AppUser(
            String email, String passwordHash, String displayName, UserRole role, Instant now) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.enabled = true;
        this.failedLoginCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // --- Domain Methods (Hành vi nghiệp vụ) ---

    /** Kiểm tra xem tài khoản có đang bị khóa tạm thời tại thời điểm xét hay không (FR-AUTH-05). */
    public boolean isLocked(Instant now) {
        return this.lockedUntil != null && this.lockedUntil.isAfter(now);
    }

    /**
     * Ghi nhận một lần đăng nhập thất bại và tạm khóa tài khoản nếu vượt quá ngưỡng (FR-AUTH-05).
     *
     * @param maxFailedAttempts Số lần sai tối đa được phép.
     * @param lockDuration Thời gian tạm khóa.
     * @param now Thời điểm xảy ra.
     */
    public void recordFailedLogin(
            int maxFailedAttempts, java.time.Duration lockDuration, Instant now) {
        if (this.lockedUntil != null && !this.lockedUntil.isAfter(now)) {
            this.failedLoginCount = 0;
            this.lockedUntil = null;
        }

        this.failedLoginCount++;
        if (this.failedLoginCount >= maxFailedAttempts) {
            this.lockedUntil = now.plus(lockDuration);
        }
        this.updatedAt = now;
    }

    /** Ghi nhận một lần đăng nhập thất bại với tham số thời điểm mặc định. */
    public void recordFailedLogin(Instant now) {
        recordFailedLogin(5, java.time.Duration.ofMinutes(15), now);
    }

    /**
     * Ghi nhận đăng nhập thành công: xóa bộ đếm lỗi, giải phóng trạng thái khóa và cập nhật thời
     * điểm đăng nhập gần nhất.
     */
    public void recordSuccessfulLogin(Instant now) {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
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
}
