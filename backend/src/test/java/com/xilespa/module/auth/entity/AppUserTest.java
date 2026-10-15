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
    @DisplayName("Khóa lũy tiến tăng dần 1, 2, 4, 8, 15 phút khi tiếp tục sai sau mỗi lần hết hạn")
    void recordFailedLogin_progressiveLockoutAcrossTime() {
        AppUser user = new AppUser("user@xilespa.vn", "hash", "User", UserRole.OWNER, now);
        Duration maxLock = Duration.ofMinutes(15);
        Instant currentTime = now;

        // Sai 4 lần đầu: chưa khóa
        for (int i = 1; i <= 4; i++) {
            boolean locked = user.recordFailedLogin(5, maxLock, currentTime);
            assertThat(locked).isFalse();
            assertThat(user.getFailedLoginCount()).isEqualTo(i);
            assertThat(user.getLockedUntil()).isNull();
        }

        // Lần 5: khóa 1 phút (2^0)
        boolean locked5 = user.recordFailedLogin(5, maxLock, currentTime);
        assertThat(locked5).isTrue();
        assertThat(user.getFailedLoginCount()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isEqualTo(currentTime.plus(Duration.ofMinutes(1)));
        assertThat(user.isLocked(currentTime)).isTrue();

        // Tiến đồng hồ qua 1 phút (hết hạn khóa)
        currentTime = currentTime.plus(Duration.ofMinutes(1)).plusSeconds(1);
        assertThat(user.isLocked(currentTime)).isFalse();

        // Lần 6: khóa 2 phút (2^1)
        boolean locked6 = user.recordFailedLogin(5, maxLock, currentTime);
        assertThat(locked6).isTrue();
        assertThat(user.getFailedLoginCount()).isEqualTo(6);
        assertThat(user.getLockedUntil()).isEqualTo(currentTime.plus(Duration.ofMinutes(2)));

        // Tiến đồng hồ qua 2 phút
        currentTime = currentTime.plus(Duration.ofMinutes(2)).plusSeconds(1);
        assertThat(user.isLocked(currentTime)).isFalse();

        // Lần 7: khóa 4 phút (2^2)
        boolean locked7 = user.recordFailedLogin(5, maxLock, currentTime);
        assertThat(locked7).isTrue();
        assertThat(user.getFailedLoginCount()).isEqualTo(7);
        assertThat(user.getLockedUntil()).isEqualTo(currentTime.plus(Duration.ofMinutes(4)));

        // Tiến đồng hồ qua 4 phút
        currentTime = currentTime.plus(Duration.ofMinutes(4)).plusSeconds(1);
        assertThat(user.isLocked(currentTime)).isFalse();

        // Lần 8: khóa 8 phút (2^3)
        boolean locked8 = user.recordFailedLogin(5, maxLock, currentTime);
        assertThat(locked8).isTrue();
        assertThat(user.getFailedLoginCount()).isEqualTo(8);
        assertThat(user.getLockedUntil()).isEqualTo(currentTime.plus(Duration.ofMinutes(8)));

        // Tiến đồng hồ qua 8 phút
        currentTime = currentTime.plus(Duration.ofMinutes(8)).plusSeconds(1);
        assertThat(user.isLocked(currentTime)).isFalse();

        // Lần 9: chạm trần tối đa 15 phút (2^4 = 16 > 15)
        boolean locked9 = user.recordFailedLogin(5, maxLock, currentTime);
        assertThat(locked9).isTrue();
        assertThat(user.getFailedLoginCount()).isEqualTo(9);
        assertThat(user.getLockedUntil()).isEqualTo(currentTime.plus(Duration.ofMinutes(15)));

        // Lần 10: vẫn bị giới hạn tối đa 15 phút
        currentTime = currentTime.plus(Duration.ofMinutes(15)).plusSeconds(1);
        boolean locked10 = user.recordFailedLogin(5, maxLock, currentTime);
        assertThat(locked10).isTrue();
        assertThat(user.getFailedLoginCount()).isEqualTo(10);
        assertThat(user.getLockedUntil()).isEqualTo(currentTime.plus(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName(
            "Bộ đếm sai không tự về 0 theo thời gian, chỉ về 0 khi đăng nhập đúng hoặc đặt lại")
    void recordFailedLogin_afterLongGap_shouldKeepCount() {
        AppUser user = new AppUser("user@xilespa.vn", "hash", "User", UserRole.OWNER, now);
        Duration maxLock = Duration.ofMinutes(15);

        // Sai 3 lần tại thời điểm now
        user.recordFailedLogin(5, maxLock, now);
        user.recordFailedLogin(5, maxLock, now);
        user.recordFailedLogin(5, maxLock, now);

        // Lần sai tiếp theo sau 25 giờ vẫn cộng dồn
        user.recordFailedLogin(5, maxLock, now.plus(Duration.ofHours(25)));

        assertThat(user.getFailedLoginCount()).isEqualTo(4);
        assertThat(user.getLockedUntil()).isNull();
    }
}
