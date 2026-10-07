package com.xilespa.module.auth.controller;

import com.xilespa.module.auth.dto.request.LoginRequest;
import com.xilespa.module.auth.dto.response.LoginResponse;
import com.xilespa.module.auth.dto.response.RefreshResponse;
import com.xilespa.module.auth.dto.response.UserResponse;
import com.xilespa.module.auth.service.AuthService;
import com.xilespa.security.CookieHelper;
import com.xilespa.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller xử lý các endpoint xác thực /api/auth (FR-AUTH-01, FR-AUTH-02). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieHelper cookieHelper;

    public AuthController(AuthService authService, CookieHelper cookieHelper) {
        this.authService = authService;
        this.cookieHelper = cookieHelper;
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletResponse response) {

        LoginResponse result = authService.login(request);
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieHelper.createAccessTokenCookie(result.accessToken()).toString());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieHelper.createRefreshTokenCookie(result.rawRefreshToken()).toString());

        return ResponseEntity.ok(result.user());
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {

        String rawRefreshToken =
                CookieHelper.extractCookie(request, CookieHelper.REFRESH_TOKEN_COOKIE).orElse(null);
        RefreshResponse result = authService.refresh(rawRefreshToken);

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieHelper.createAccessTokenCookie(result.accessToken()).toString());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieHelper.createRefreshTokenCookie(result.rawRefreshToken()).toString());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {

        String rawRefreshToken =
                CookieHelper.extractCookie(request, CookieHelper.REFRESH_TOKEN_COOKIE).orElse(null);
        authService.logout(rawRefreshToken);

        response.addHeader(
                HttpHeaders.SET_COOKIE, cookieHelper.clearAccessTokenCookie().toString());
        response.addHeader(
                HttpHeaders.SET_COOKIE, cookieHelper.clearRefreshTokenCookie().toString());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal.id()));
    }
}
