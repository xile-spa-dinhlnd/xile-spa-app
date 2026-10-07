package com.xilespa.module.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xilespa.module.auth.entity.AppUser;
import com.xilespa.module.auth.entity.UserRole;
import com.xilespa.module.auth.repository.AppUserRepository;
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
    @Autowired private PasswordEncoder passwordEncoder;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser =
                new AppUser(
                        "auth_it@xilespa.vn",
                        passwordEncoder.encode("SecretPass123"),
                        "Chủ tiệm IT",
                        UserRole.OWNER);
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
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
                .andExpect(cookie().httpOnly(CookieHelper.REFRESH_TOKEN_COOKIE, true));

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
}
