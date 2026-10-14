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
     * Áp dụng khóa lũy tiến: 1 phút, 2 phút, 4 phút... tối đa theo cấu hình.
     *
     * @param maxFailedAttempts Số lần sai tối đa được phép.
     * @param maxLockDuration Thời gian tạm khóa tối đa.
     * @param now Thời điểm xảy ra.
     * @return true nếu tài khoản vừa bị chuyển sang trạng thái tạm khóa.
     */
    public boolean recordFailedLogin(
            int maxFailedAttempts, java.time.Duration maxLockDuration, Instant now) {
        if (this.lockedUntil != null && !this.lockedUntil.isAfter(now)) {
            this.failedLoginCount = 0;
            this.lockedUntil = null;
        }

        this.failedLoginCount++;
        boolean justLocked = false;
        if (this.failedLoginCount >= maxFailedAttempts) {
            int attemptsOver = this.failedLoginCount - maxFailedAttempts;
            long minutes = Math.min((long) Math.pow(2, attemptsOver), maxLockDuration.toMinutes());
            if (minutes < 1) {
                minutes = 1;
            }
            this.lockedUntil = now.plus(java.time.Duration.ofMinutes(minutes));
            justLocked = true;
        }
        this.updatedAt = now;
        return justLocked;
    }

    /**
     * Mở khóa tài khoản và thiết lập lại bộ đếm đăng nhập sai mà không thay đổi thời điểm đăng nhập
     * cuối.
     */
    public void unlock(Instant now) {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.updatedAt = now;
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
