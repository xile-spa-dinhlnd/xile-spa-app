package com.xilespa.module.auth.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.xilespa.common.exception.BusinessException;
import com.xilespa.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyValidatorTest {

    @Mock private PasswordEncoder passwordEncoder;

    private PasswordPolicyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordPolicyValidator(passwordEncoder);
    }

    @Test
    @DisplayName("Ném lỗi khi mật khẩu mới null hoặc dưới 10 ký tự")
    void validate_whenTooShortOrNull_throwsException() {
        assertThatThrownBy(() -> validator.validate(null, "hash"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_POLICY_VIOLATION)
                .hasMessageContaining("tối thiểu 10 ký tự");

        assertThatThrownBy(() -> validator.validate("short123", "hash"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_POLICY_VIOLATION)
                .hasMessageContaining("tối thiểu 10 ký tự");
    }

    @Test
    @DisplayName("Ném lỗi khi mật khẩu dài hơn 72 byte UTF-8 (giới hạn cứng của BCrypt)")
    void validate_whenExceeds72Bytes_throwsException() {
        // Ký tự tiếng Việt có dấu 'ế' chiếm 3 byte trong UTF-8: 25 ký tự = 75 bytes > 72 bytes
        String longUtf8Password = "ế".repeat(25);
        assertThat(longUtf8Password.getBytes(StandardCharsets.UTF_8).length).isGreaterThan(72);

        assertThatThrownBy(() -> validator.validate(longUtf8Password, "hash"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_POLICY_VIOLATION)
                .hasMessageContaining("72 byte");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "1234567890",
                "password123",
                "xile123456",
                "admin123456",
                "qwertyuiop",
                "welcome1234",
                "xilespa@123"
            })
    @DisplayName("Ném lỗi khi mật khẩu thuộc danh sách đen phổ biến")
    void validate_whenCommonPassword_throwsException(String commonPassword) {
        assertThatThrownBy(() -> validator.validate(commonPassword, "oldHash"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_POLICY_VIOLATION)
                .hasMessageContaining("quá phổ biến");
    }

    @Test
    @DisplayName("Ném lỗi khi mật khẩu mới trùng với mật khẩu hiện tại")
    void validate_whenMatchesCurrentPassword_throwsException() {
        String newPassword = "MySecureNewPassword123";
        String currentHash = "$2a$10$oldHashValue";
        given(passwordEncoder.matches(newPassword, currentHash)).willReturn(true);

        assertThatThrownBy(() -> validator.validate(newPassword, currentHash))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_POLICY_VIOLATION)
                .hasMessageContaining("không được trùng");
    }

    @Test
    @DisplayName(
            "Hợp lệ khi mật khẩu đạt chuẩn tối thiểu 10 ký tự, không quá 72 byte, không phổ biến và khác mật khẩu cũ")
    void validate_whenValidPassword_success() {
        String newPassword = "MySecureNewPassword123";
        String currentHash = "$2a$10$oldHashValue";
        given(passwordEncoder.matches(newPassword, currentHash)).willReturn(false);

        assertThatCode(() -> validator.validate(newPassword, currentHash))
                .doesNotThrowAnyException();
    }
}
