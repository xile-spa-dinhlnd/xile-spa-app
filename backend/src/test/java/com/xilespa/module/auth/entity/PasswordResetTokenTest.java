package com.xilespa.module.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.xilespa.module.auth.enums.UserRole;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordResetTokenTest {

    private final Instant now = Instant.parse("2026-10-12T10:00:00Z");

    @Test
    @DisplayName("Token hợp lệ khi chưa dùng và chưa hết hạn")
    void isValid_whenNotUsedAndNotExpired_returnsTrue() {
        AppUser user = new AppUser("user@xilespa.vn", "hash", "User", UserRole.OWNER, now);
        PasswordResetToken token =
                new PasswordResetToken(user, "token_hash", now.plus(Duration.ofMinutes(30)), now);

        assertThat(token.isExpired(now)).isFalse();
        assertThat(token.isUsed()).isFalse();
        assertThat(token.isValid(now)).isTrue();
    }

    @Test
    @DisplayName("Token không hợp lệ khi đã hết hạn 30 phút")
    void isExpired_whenPastExpiresAt_returnsTrue() {
        AppUser user = new AppUser("user@xilespa.vn", "hash", "User", UserRole.OWNER, now);
        PasswordResetToken token =
                new PasswordResetToken(user, "token_hash", now.plus(Duration.ofMinutes(30)), now);

        Instant after31Minutes = now.plus(Duration.ofMinutes(31));
        assertThat(token.isExpired(after31Minutes)).isTrue();
        assertThat(token.isValid(after31Minutes)).isFalse();
    }

    @Test
    @DisplayName("Token chỉ dùng được một lần: sau khi markUsed thì không còn hợp lệ")
    void markUsed_setsUsedAtAndMakesTokenInvalid() {
        AppUser user = new AppUser("user@xilespa.vn", "hash", "User", UserRole.OWNER, now);
        PasswordResetToken token =
                new PasswordResetToken(user, "token_hash", now.plus(Duration.ofMinutes(30)), now);

        token.markUsed(now.plus(Duration.ofMinutes(5)));

        assertThat(token.isUsed()).isTrue();
        assertThat(token.getUsedAt()).isEqualTo(now.plus(Duration.ofMinutes(5)));
        assertThat(token.isValid(now)).isFalse();
    }
}
