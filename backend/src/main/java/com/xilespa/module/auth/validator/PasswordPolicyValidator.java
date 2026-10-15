package com.xilespa.module.auth.validator;

import com.xilespa.common.exception.BusinessException;
import com.xilespa.common.exception.ErrorCode;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra chính sách mật khẩu theo NFR-SEC-03:
 *
 * <ul>
 *   <li>Tối thiểu 10 ký tự.
 *   <li>Tối đa 72 byte UTF-8 (giới hạn cứng của thuật toán BCrypt).
 *   <li>Chặn danh sách đen mật khẩu phổ biến (từ điển top 1000).
 *   <li>Không được trùng với mật khẩu hiện tại.
 * </ul>
 */
@Component
public class PasswordPolicyValidator {

    private static final Logger log = LoggerFactory.getLogger(PasswordPolicyValidator.class);
    private static final int MIN_LENGTH = 10;
    private static final int MAX_BYTES = 72;

    private static final Set<String> COMMON_PASSWORDS = loadCommonPasswords();

    private final PasswordEncoder passwordEncoder;

    public PasswordPolicyValidator(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Xác thực mật khẩu mới theo chính sách an toàn.
     *
     * @param newPassword Mật khẩu mới dạng plain-text.
     * @param currentPasswordHash Chuỗi băm mật khẩu hiện tại (nếu có để đối chiếu chống trùng).
     */
    public void validate(String newPassword, String currentPasswordHash) {
        if (newPassword == null || newPassword.length() < MIN_LENGTH) {
            throw new BusinessException(
                    ErrorCode.PASSWORD_POLICY_VIOLATION,
                    "Mật khẩu mới phải có tối thiểu " + MIN_LENGTH + " ký tự.");
        }

        if (newPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new BusinessException(
                    ErrorCode.PASSWORD_POLICY_VIOLATION,
                    "Mật khẩu không được dài quá 72 byte (khoảng 24-72 ký tự tùy dấu tiếng Việt).");
        }

        String normalized = newPassword.trim().toLowerCase();
        if (COMMON_PASSWORDS.contains(normalized)) {
            throw new BusinessException(
                    ErrorCode.PASSWORD_POLICY_VIOLATION,
                    "Mật khẩu quá phổ biến và dễ đoán. Vui lòng chọn mật khẩu an toàn hơn.");
        }

        if (currentPasswordHash != null
                && passwordEncoder.matches(newPassword, currentPasswordHash)) {
            throw new BusinessException(
                    ErrorCode.PASSWORD_POLICY_VIOLATION,
                    "Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }
    }

    private static Set<String> loadCommonPasswords() {
        Set<String> passwords = new HashSet<>();
        try (InputStream is =
                PasswordPolicyValidator.class.getResourceAsStream(
                        "/security/top-1000-passwords.txt")) {
            if (is != null) {
                try (BufferedReader reader =
                        new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim().toLowerCase();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                            passwords.add(trimmed);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể nạp danh sách top 1000 mật khẩu phổ biến từ resources", e);
        }

        // Bổ sung các mật khẩu dự phòng quan trọng
        passwords.addAll(
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
                        "administrator"));

        return Set.copyOf(passwords);
    }
}
