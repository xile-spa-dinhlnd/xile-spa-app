package com.xilespa.module.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Thực thể Password Reset Token, ánh xạ bảng {@code password_reset_token} (V1__core_schema.sql).
 * Chỉ lưu băm SHA-256 của token, có thời hạn 30 phút và chỉ sử dụng đúng 1 lần (FR-AUTH-03).
 */
@Entity
@Table(name = "password_reset_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "token_hash", nullable = false, length = 128, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Khởi tạo token đặt lại mật khẩu mới.
     *
     * @param user Tài khoản yêu cầu.
     * @param tokenHash Chuỗi băm SHA-256 của token.
     * @param expiresAt Thời điểm hết hạn (mặc định +30 phút).
     * @param now Thời điểm tạo từ hệ thống.
     */
    public PasswordResetToken(AppUser user, String tokenHash, Instant expiresAt, Instant now) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = now;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(this.expiresAt);
    }

    public boolean isUsed() {
        return this.usedAt != null;
    }

    public boolean isValid(Instant now) {
        return !isUsed() && !isExpired(now);
    }

    /** Đánh dấu token đã được sử dụng (chỉ dùng một lần theo FR-AUTH-03). */
    public void markUsed(Instant now) {
        this.usedAt = now;
    }
}
