package com.xilespa.module.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.xilespa.module.auth.enums.UserRole;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppUserTest {

    private final Instant now = Instant.parse("2026-10-12T10:00:00Z");

    @Test
    @DisplayName("Tăng bộ đếm sai nhưng chưa khóa khi chưa đạt ngưỡng tối đa")
    void recordFailedLogin_whenBelowThreshold_shouldIncrementCountWithoutLocking() {
        AppUser user = new AppUser("user@xilespa.vn", "hash", "User", UserRole.OWNER, now);

        user.recordFailedLogin(5, Duration.ofMinutes(15), now);
        user.recordFailedLogin(5, Duration.ofMinutes(15), now);

        assertThat(user.getFailedLoginCount()).isEqualTo(2);
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.isLocked(now)).isFalse();
    }

    @Test
    @DisplayName(
            "Khóa tài khoản lũy tiến (1 phút cho lần đầu chạm ngưỡng) khi đạt số lần sai tối đa")
    void recordFailedLogin_whenReachesThreshold_shouldLockAccountProgressively() {
        AppUser user = new AppUser("user@xilespa.vn", "hash", "User", UserRole.OWNER, now);

        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(5, Duration.ofMinutes(15), now);
        }

        // Khóa lũy tiến lần đầu: 1 phút (thay vì khóa cứng 15 phút)
        assertThat(user.getFailedLoginCount()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isEqualTo(now.plus(Duration.ofMinutes(1)));
        assertThat(user.isLocked(now)).isTrue();
        assertThat(user.isLocked(now.plus(Duration.ofSeconds(30)))).isTrue();
        assertThat(user.isLocked(now.plus(Duration.ofMinutes(2)))).isFalse();
    }

    @Test
    @DisplayName("Mở khóa bằng unlock() không làm thay đổi lastLoginAt")
    void unlock_shouldClearLockAndCountWithoutUpdatingLastLoginAt() {
        AppUser user = new AppUser("user@xilespa.vn", "hash", "User", UserRole.OWNER, now);
        user.lockUntil(now.plus(Duration.ofMinutes(15)), now);

        user.unlock(now);

        assertThat(user.getFailedLoginCount()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLastLoginAt()).isNull();
    }

    @Test
    @DisplayName("Reset bộ đếm và xóa thời gian khóa khi đăng nhập thành công")
    void recordSuccessfulLogin_shouldClearFailedCountAndLock() {
        AppUser user = new AppUser("user@xilespa.vn", "hash", "User", UserRole.OWNER, now);
        user.recordFailedLogin(5, Duration.ofMinutes(15), now);
        user.lockUntil(now.plus(Duration.ofMinutes(15)), now);

        user.recordSuccessfulLogin(now);

        assertThat(user.getFailedLoginCount()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLastLoginAt()).isEqualTo(now);
        assertThat(user.isLocked(now)).isFalse();
    }

    @Test
    @DisplayName("Bắt đầu chu kỳ đếm mới sau khi thời gian khóa trước đó đã hết hạn")
    void recordFailedLogin_afterLockExpired_shouldResetCountAndStartNewCycle() {
        AppUser user = new AppUser("user@xilespa.vn", "hash", "User", UserRole.OWNER, now);
        // Giả lập tài khoản bị khóa trong quá khứ (từ 30 phút trước, khóa 15 phút -> đã hết hạn 15
        // phút trước)
        Instant thirtyMinutesAgo = now.minus(Duration.ofMinutes(30));
        user.lockUntil(now.minus(Duration.ofMinutes(15)), thirtyMinutesAgo);

        // Lần sai tiếp theo ở thời điểm hiện tại
        user.recordFailedLogin(5, Duration.ofMinutes(15), now);

        assertThat(user.getFailedLoginCount()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.isLocked(now)).isFalse();
    }
}
