package com.xilespa.module.auth.validator;

import com.xilespa.common.exception.BusinessException;
import com.xilespa.common.exception.ErrorCode;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Kiểm tra chính sách mật khẩu theo NFR-SEC-03 (tối thiểu 10 ký tự, chặn mật khẩu phổ biến). */
@Component
@RequiredArgsConstructor
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 10;

    /** Danh sách đen các mật khẩu quá phổ biến, dễ đoán cần chặn theo NFR-SEC-03. */
    private static final Set<String> COMMON_PASSWORDS =
            Set.of(
                    "1234567890",
                    "password123",
                    "xile123456",
                    "admin123456",
                    "qwertyuiop",
                    "12345678901",
                    "xilespa123",
                    "0123456789",
                    "1111111111",
                    "1231231234",
                    "password1234",
                    "xile_dev_123",
                    "administrator");

    private final PasswordEncoder passwordEncoder;

    /**
     * Xác thực mật khẩu mới: độ dài tối thiểu 10 ký tự, không thuộc danh sách đen, và không trùng
     * mật khẩu cũ.
     *
     * @param newPassword Mật khẩu mới dạng plain-text.
     * @param currentPasswordHash Chuỗi băm mật khẩu hiện tại (nếu có để đối chiếu chống trùng).
     */
    public void validate(String newPassword, String currentPasswordHash) {
        if (newPassword == null || newPassword.length() < MIN_LENGTH) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Mật khẩu mới phải có tối thiểu " + MIN_LENGTH + " ký tự.");
        }

        String normalized = newPassword.trim().toLowerCase();
        if (COMMON_PASSWORDS.contains(normalized)) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Mật khẩu quá phổ biến và dễ đoán. Vui lòng chọn mật khẩu an toàn hơn.");
        }

        if (currentPasswordHash != null
                && passwordEncoder.matches(newPassword, currentPasswordHash)) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }
    }
}
