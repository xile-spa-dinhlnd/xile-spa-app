package com.xilespa.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenHasherTest {

    @Test
    @DisplayName("Băm SHA-256 nhất quán, dạng hex 64 ký tự, khác token thì khác kết quả")
    void hashIsDeterministicSha256Hex() {
        String hash1 = TokenHasher.hash("raw-refresh-token-123456");
        String hash2 = TokenHasher.hash("raw-refresh-token-123456");

        assertThat(hash1).hasSize(64).matches("[0-9a-f]{64}").isEqualTo(hash2);
        assertThat(TokenHasher.hash("different-token")).isNotEqualTo(hash1);
    }

    @Test
    @DisplayName("Token rỗng thì báo lỗi")
    void blankTokenIsRejected() {
        assertThatThrownBy(() -> TokenHasher.hash(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
