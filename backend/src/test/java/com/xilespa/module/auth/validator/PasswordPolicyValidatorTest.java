package com.xilespa.module.auth.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.xilespa.common.exception.BusinessException;
import com.xilespa.common.exception.ErrorCode;
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
        // Arrange & Act & Assert
        assertThatThrownBy(() -> validator.validate(null, "hash"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST)
                .hasMessageContaining("tối thiểu 10 ký tự");

        assertThatThrownBy(() -> validator.validate("short123", "hash"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST)
                .hasMessageContaining("tối thiểu 10 ký tự");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "password123", "xile123456", "admin123456", "qwertyuiop"})
    @DisplayName("Ném lỗi khi mật khẩu thuộc danh sách đen phổ biến")
    void validate_whenCommonPassword_throwsException(String commonPassword) {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> validator.validate(commonPassword, "oldHash"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST)
                .hasMessageContaining("quá phổ biến");
    }

    @Test
    @DisplayName("Ném lỗi khi mật khẩu mới trùng với mật khẩu hiện tại")
    void validate_whenMatchesCurrentPassword_throwsException() {
        // Arrange
        String newPassword = "MySecureNewPassword123";
        String currentHash = "$2a$10$oldHashValue";
        given(passwordEncoder.matches(newPassword, currentHash)).willReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(newPassword, currentHash))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST)
                .hasMessageContaining("không được trùng");
    }

    @Test
    @DisplayName(
            "Hợp lệ khi mật khẩu đạt chuẩn tối thiểu 10 ký tự, không phổ biến và khác mật khẩu cũ")
    void validate_whenValidPassword_success() {
        // Arrange
        String newPassword = "MySecureNewPassword123";
        String currentHash = "$2a$10$oldHashValue";
        given(passwordEncoder.matches(newPassword, currentHash)).willReturn(false);

        // Act & Assert
        assertThatCode(() -> validator.validate(newPassword, currentHash))
                .doesNotThrowAnyException();
    }
}
