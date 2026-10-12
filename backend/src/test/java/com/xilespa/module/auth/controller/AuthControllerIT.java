package com.xilespa.module.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xilespa.module.auth.entity.AppUser;
import com.xilespa.module.auth.entity.PasswordResetToken;
import com.xilespa.module.auth.enums.UserRole;
import com.xilespa.module.auth.repository.AppUserRepository;
import com.xilespa.module.auth.repository.PasswordResetTokenRepository;
import com.xilespa.module.auth.repository.RefreshTokenRepository;
import com.xilespa.security.CookieHelper;
import com.xilespa.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@IntegrationTest
class AuthControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        cleanUp();

        testUser =
                userRepository
                        .findByEmailIgnoreCase("auth_it@xilespa.vn")
                        .orElseGet(
                                () ->
                                        userRepository.save(
                                                new AppUser(
                                                        "auth_it@xilespa.vn",
                                                        passwordEncoder.encode("SecretPass123"),
                                                        "Chủ tiệm IT",
                                                        UserRole.OWNER,
                                                        java.time.Instant.now())));
        testUser.changePassword(passwordEncoder.encode("SecretPass123"), java.time.Instant.now());
        testUser.recordSuccessfulLogin(java.time.Instant.now());
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    @DisplayName("Đăng nhập thành công: trả 200, thông tin user và đặt 2 cookie httpOnly")
    void login_success_setsCookies() throws Exception {
        String loginPayload =
                """
                {
                    "email": "auth_it@xilespa.vn",
                    "password": "SecretPass123"
                }
                """;

        MvcResult mvcResult =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(loginPayload))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(testUser.getId()))
                        .andExpect(jsonPath("$.email").value("auth_it@xilespa.vn"))
                        .andExpect(jsonPath("$.displayName").value("Chủ tiệm IT"))
                        .andExpect(jsonPath("$.role").value("OWNER"))
                        .andExpect(cookie().exists(CookieHelper.ACCESS_TOKEN_COOKIE))
                        .andExpect(cookie().httpOnly(CookieHelper.ACCESS_TOKEN_COOKIE, true))
                        .andExpect(cookie().exists(CookieHelper.REFRESH_TOKEN_COOKIE))
                        .andExpect(cookie().httpOnly(CookieHelper.REFRESH_TOKEN_COOKIE, true))
                        .andReturn();

        var setCookies = mvcResult.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookies).isNotEmpty();
        assertThat(setCookies)
                .allMatch(c -> c.contains("SameSite=Strict") && c.contains("HttpOnly"));

        assertThat(refreshTokenRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Đăng nhập sai mật khẩu: trả 401 UNAUTHORIZED, thông báo chung")
    void login_wrongPassword_returns401() throws Exception {
        String loginPayload =
                """
                {
                    "email": "auth_it@xilespa.vn",
                    "password": "wrong_password"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng."))
                .andExpect(cookie().doesNotExist(CookieHelper.ACCESS_TOKEN_COOKIE));

        // Kiểm tra trực tiếp CSDL: failed_login_count phải được tăng lên 1 (không bị rollback bởi
        // exception 401)
        AppUser reloadedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(reloadedUser.getFailedLoginCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Khóa tài khoản sau 5 lần sai liên tiếp và từ chối các lần đăng nhập tiếp theo")
    void login_when5ConsecutiveFailedAttempts_shouldLockAccount() throws Exception {
        String wrongPayload =
                """
                {
                    "email": "auth_it@xilespa.vn",
                    "password": "wrong_password"
                }
                """;

        String correctPayload =
                """
                {
                    "email": "auth_it@xilespa.vn",
                    "password": "SecretPass123"
                }
                """;

        // Thực hiện 5 lần sai liên tiếp
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(wrongPayload))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng."));
        }

        // Kiểm tra CSDL: tài khoản đã bị khóa
        AppUser lockedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(lockedUser.getFailedLoginCount()).isEqualTo(5);
        assertThat(lockedUser.getLockedUntil()).isNotNull();
        assertThat(lockedUser.getLockedUntil()).isAfter(java.time.Instant.now());

        // Lần thứ 6: Dù gõ ĐÚNG mật khẩu cũng bị chặn vì tài khoản đang bị tạm khóa
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(correctPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(
                        jsonPath("$.message")
                                .value("Tài khoản đang bị tạm khóa. Vui lòng thử lại sau."));

        // Giả lập thời gian tạm khóa đã hết hạn (chỉnh lockedUntil về quá khứ)
        lockedUser.lockUntil(
                java.time.Instant.now().minus(java.time.Duration.ofMinutes(1)),
                java.time.Instant.now());
        userRepository.save(lockedUser);

        // Sau khi hết hạn khóa, nhập đúng mật khẩu thì thành công và tự động giải phóng khóa
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(correctPayload))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(CookieHelper.ACCESS_TOKEN_COOKIE));

        AppUser restoredUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(restoredUser.getFailedLoginCount()).isZero();
        assertThat(restoredUser.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("Làm mới token thành công: xoay vòng Refresh Token và cấp cookie mới")
    void refresh_success_rotatesCookies() throws Exception {
        // 1. Đăng nhập để lấy cookie
        String loginPayload =
                """
                {
                    "email": "auth_it@xilespa.vn",
                    "password": "SecretPass123"
                }
                """;

        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(loginPayload))
                        .andExpect(status().isOk())
                        .andReturn();

        Cookie refreshTokenCookie =
                loginResult.getResponse().getCookie(CookieHelper.REFRESH_TOKEN_COOKIE);
        assertThat(refreshTokenCookie).isNotNull();

        // 2. Gọi refresh với cookie
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(CookieHelper.ACCESS_TOKEN_COOKIE))
                .andExpect(cookie().exists(CookieHelper.REFRESH_TOKEN_COOKIE));

        // Token cũ đã bị revoked, tổng cộng có 2 token trong DB
        assertThat(refreshTokenRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName(
            "Phát hiện tái sử dụng token đã thu hồi: trả về 401 và thu hồi toàn bộ token của user vào CSDL")
    void refresh_whenTokenRevoked_returns401AndRevokesAllTokensInDb() throws Exception {
        // 1. Đăng nhập lấy token 1
        String loginPayload =
                """
                {
                    "email": "auth_it@xilespa.vn",
                    "password": "SecretPass123"
                }
                """;

        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(loginPayload))
                        .andExpect(status().isOk())
                        .andReturn();

        Cookie stolenTokenCookie =
                loginResult.getResponse().getCookie(CookieHelper.REFRESH_TOKEN_COOKIE);

        // 2. Refresh hợp lệ lần 1 -> token 1 bị revoke, cấp token 2
        MvcResult rotateResult =
                mockMvc.perform(post("/api/auth/refresh").cookie(stolenTokenCookie))
                        .andExpect(status().isOk())
                        .andReturn();

        // 3. Kẻ gian cố tình dùng lại token 1 đã bị revoke
        mockMvc.perform(post("/api/auth/refresh").cookie(stolenTokenCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(
                        jsonPath("$.message")
                                .value("Phiên đăng nhập không an toàn. Vui lòng đăng nhập lại."));

        // Kiểm tra trực tiếp CSDL: tất cả token của user phải ở trạng thái revoked (không bị
        // rollback bởi 401)
        var allTokens = refreshTokenRepository.findAll();
        assertThat(allTokens).isNotEmpty();
        assertThat(allTokens).allMatch(com.xilespa.module.auth.entity.RefreshToken::isRevoked);
    }

    @Test
    @DisplayName(
            "Lấy thông tin người dùng hiện tại qua GET /api/auth/me thành công khi có cookie access_token")
    void getMe_whenAuthenticated_returns200() throws Exception {
        // Đăng nhập lấy access_token
        String loginPayload =
                """
                {
                    "email": "auth_it@xilespa.vn",
                    "password": "SecretPass123"
                }
                """;

        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(loginPayload))
                        .andExpect(status().isOk())
                        .andReturn();

        Cookie accessTokenCookie =
                loginResult.getResponse().getCookie(CookieHelper.ACCESS_TOKEN_COOKIE);
        assertThat(accessTokenCookie).isNotNull();

        // Gọi /api/auth/me
        mockMvc.perform(get("/api/auth/me").cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.email").value("auth_it@xilespa.vn"))
                .andExpect(jsonPath("$.displayName").value("Chủ tiệm IT"));
    }

    @Test
    @DisplayName("GET /api/auth/me trả về 401 khi không có cookie access_token")
    void getMe_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Đăng xuất thành công: xóa 2 cookie (Max-Age=0) và thu hồi Refresh Token")
    void logout_clearsCookies() throws Exception {
        // 1. Đăng nhập
        String loginPayload =
                """
                {
                    "email": "auth_it@xilespa.vn",
                    "password": "SecretPass123"
                }
                """;

        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(loginPayload))
                        .andExpect(status().isOk())
                        .andReturn();

        Cookie refreshTokenCookie =
                loginResult.getResponse().getCookie(CookieHelper.REFRESH_TOKEN_COOKIE);

        // 2. Đăng xuất
        mockMvc.perform(post("/api/auth/logout").cookie(refreshTokenCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(CookieHelper.ACCESS_TOKEN_COOKIE, 0))
                .andExpect(cookie().maxAge(CookieHelper.REFRESH_TOKEN_COOKIE, 0));
    }

    @Test
    @DisplayName("POST /api/auth/change-password trả về 401 khi chưa đăng nhập")
    void changePassword_whenUnauthenticated_returns401() throws Exception {
        String payload =
                """
                {
                    "oldPassword": "SecretPass123",
                    "newPassword": "BrandNewSecurePassword123"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("POST /api/auth/change-password trả về 400 khi mật khẩu cũ không đúng")
    void changePassword_whenWrongOldPassword_returns400() throws Exception {
        // 1. Đăng nhập lấy access_token
        String loginPayload =
                """
                {
                    "email": "auth_it@xilespa.vn",
                    "password": "SecretPass123"
                }
                """;

        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(loginPayload))
                        .andExpect(status().isOk())
                        .andReturn();

        Cookie accessTokenCookie =
                loginResult.getResponse().getCookie(CookieHelper.ACCESS_TOKEN_COOKIE);

        // 2. Đổi mật khẩu với mật khẩu cũ sai
        String changePayload =
                """
                {
                    "oldPassword": "WrongOldPassword",
                    "newPassword": "BrandNewSecurePassword123"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .cookie(accessTokenCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(changePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Mật khẩu hiện tại không chính xác."));
    }

    @Test
    @DisplayName(
            "POST /api/auth/change-password thành công: đổi mật khẩu, thu hồi phiên khác nhưng giữ phiên hiện tại")
    void changePassword_whenSuccess_updatesPasswordAndRevokesOtherSessions() throws Exception {
        // 1. Đăng nhập phiên 1
        String loginPayload =
                """
                {
                    "email": "auth_it@xilespa.vn",
                    "password": "SecretPass123"
                }
                """;

        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(loginPayload))
                        .andExpect(status().isOk())
                        .andReturn();

        Cookie accessTokenCookie =
                loginResult.getResponse().getCookie(CookieHelper.ACCESS_TOKEN_COOKIE);
        Cookie refreshTokenCookie =
                loginResult.getResponse().getCookie(CookieHelper.REFRESH_TOKEN_COOKIE);

        // 2. Tạo thêm 1 refresh token giả lập phiên ở thiết bị thứ hai
        String otherDeviceTokenHash =
                com.xilespa.security.TokenHasher.hash("other-device-raw-token");
        var otherToken =
                new com.xilespa.module.auth.entity.RefreshToken(
                        testUser,
                        otherDeviceTokenHash,
                        java.time.Instant.now().plus(java.time.Duration.ofDays(7)),
                        java.time.Instant.now());
        otherToken = refreshTokenRepository.save(otherToken);

        // 3. Thực hiện đổi mật khẩu ở phiên 1
        String changePayload =
                """
                {
                    "oldPassword": "SecretPass123",
                    "newPassword": "BrandNewSecurePassword123"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .cookie(accessTokenCookie)
                                .cookie(refreshTokenCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(changePayload))
                .andExpect(status().isOk());

        // 4. Kiểm tra CSDL: Phiên 2 đã bị thu hồi, phiên 1 vẫn còn hiệu lực
        var reloadedOtherToken = refreshTokenRepository.findById(otherToken.getId()).orElseThrow();
        assertThat(reloadedOtherToken.isRevoked()).isTrue();

        String currentTokenHash =
                com.xilespa.security.TokenHasher.hash(refreshTokenCookie.getValue());
        var reloadedCurrentToken =
                refreshTokenRepository.findByTokenHash(currentTokenHash).orElseThrow();
        assertThat(reloadedCurrentToken.isRevoked()).isFalse();

        // 5. Kiểm tra mật khẩu mới có thể đăng nhập được
        String newLoginPayload =
                """
                {
                    "email": "auth_it@xilespa.vn",
                    "password": "BrandNewSecurePassword123"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(newLoginPayload))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName(
            "Quên mật khẩu: Luôn trả về 200 (chống user enumeration), chỉ lưu token khi user tồn tại")
    void forgotPassword_alwaysReturns200_andSavesTokenOnlyForExistingUser() throws Exception {
        // 1. Email tồn tại
        String existingEmailPayload =
                """
                {
                    "email": "auth_it@xilespa.vn"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(existingEmailPayload))
                .andExpect(status().isOk());

        assertThat(passwordResetTokenRepository.count()).isEqualTo(1);
        var token = passwordResetTokenRepository.findAll().get(0);
        assertThat(token.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(token.isUsed()).isFalse();

        // 2. Email không tồn tại -> Vẫn trả về 200 (chống enumeration), không sinh thêm token
        String nonExistingEmailPayload =
                """
                {
                    "email": "nobody@xilespa.vn"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(nonExistingEmailPayload))
                .andExpect(status().isOk());

        assertThat(passwordResetTokenRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Đặt lại mật khẩu thành công: mở khóa tài khoản, thu hồi toàn bộ token phiên cũ, đổi mật khẩu mới")
    void resetPassword_whenValidToken_resetsPassword_unlocksUser_andRevokesAllRefreshTokens()
            throws Exception {
        // 1. Tạo Refresh token đang hoạt động cho user
        String tokenHash = com.xilespa.security.TokenHasher.hash("active-refresh-token-session");
        var activeToken =
                new com.xilespa.module.auth.entity.RefreshToken(
                        testUser,
                        tokenHash,
                        java.time.Instant.now().plus(java.time.Duration.ofDays(7)),
                        java.time.Instant.now());
        refreshTokenRepository.save(activeToken);

        // 2. Giả lập user đang bị khóa do nhập sai mật khẩu nhiều lần
        testUser.lockUntil(
                java.time.Instant.now().plus(java.time.Duration.ofMinutes(15)),
                java.time.Instant.now());
        userRepository.save(testUser);

        // 3. Tạo PasswordResetToken hợp lệ
        String rawResetToken = "valid-reset-token-xyz-123456789";
        String resetTokenHash = com.xilespa.security.TokenHasher.hash(rawResetToken);
        var resetToken =
                new PasswordResetToken(
                        testUser,
                        resetTokenHash,
                        java.time.Instant.now().plus(java.time.Duration.ofMinutes(30)),
                        java.time.Instant.now());
        passwordResetTokenRepository.save(resetToken);

        // 4. Gọi POST /api/auth/reset-password
        String resetPayload =
                """
                {
                    "token": "%s",
                    "newPassword": "ValidBrandNewPass123"
                }
                """
                        .formatted(rawResetToken);

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(resetPayload))
                .andExpect(status().isOk());

        // 5. Kiểm tra CSDL: Token reset đã được đánh dấu là đã sử dụng
        var reloadedResetToken =
                passwordResetTokenRepository.findByTokenHash(resetTokenHash).orElseThrow();
        assertThat(reloadedResetToken.isUsed()).isTrue();

        // 6. Kiểm tra user đã được mở khóa hoàn toàn
        var reloadedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(reloadedUser.getLockedUntil()).isNull();
        assertThat(reloadedUser.getFailedLoginCount()).isZero();

        // 7. Toàn bộ refresh token cũ đã bị thu hồi
        var reloadedRefreshToken =
                refreshTokenRepository.findById(activeToken.getId()).orElseThrow();
        assertThat(reloadedRefreshToken.isRevoked()).isTrue();

        // 8. Đăng nhập thành công với mật khẩu mới
        String loginPayload =
                """
                {
                    "email": "auth_it@xilespa.vn",
                    "password": "ValidBrandNewPass123"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(CookieHelper.ACCESS_TOKEN_COOKIE));
    }

    @Test
    @DisplayName("Đặt lại mật khẩu thất bại khi token không hợp lệ hoặc đã hết hạn")
    void resetPassword_whenInvalidOrExpiredToken_returns400() throws Exception {
        // Token không tồn tại
        String invalidPayload =
                """
                {
                    "token": "non-existent-token",
                    "newPassword": "ValidBrandNewPass123"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidPayload))
                .andExpect(status().isBadRequest());

        // Token đã hết hạn
        String rawExpiredToken = "expired-token-abc";
        String expiredHash = com.xilespa.security.TokenHasher.hash(rawExpiredToken);
        var expiredToken =
                new PasswordResetToken(
                        testUser,
                        expiredHash,
                        java.time.Instant.now().minus(java.time.Duration.ofMinutes(1)),
                        java.time.Instant.now());
        passwordResetTokenRepository.save(expiredToken);

        String expiredPayload =
                """
                {
                    "token": "%s",
                    "newPassword": "ValidBrandNewPass123"
                }
                """
                        .formatted(rawExpiredToken);

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(expiredPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Đặt lại mật khẩu thất bại khi mật khẩu mới vi phạm chính sách độ dài")
    void resetPassword_whenPasswordViolatesPolicy_returns400() throws Exception {
        String shortPasswordPayload =
                """
                {
                    "token": "any-token",
                    "newPassword": "short"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(shortPasswordPayload))
                .andExpect(status().isBadRequest());
    }

    /**
     * Chỉ xóa dữ liệu refresh token và password reset token của chính test này. Không xóa app_user
     * vì audit_log có khóa ngoại tham chiếu tới user (trigger chặn xóa audit_log).
     */
    private void cleanUp() {
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
    }
}
