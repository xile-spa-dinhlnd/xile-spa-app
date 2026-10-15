package com.xilespa.module.auth.controller;

import com.xilespa.module.auth.dto.request.ChangePasswordRequest;
import com.xilespa.module.auth.dto.request.LoginRequest;
import com.xilespa.module.auth.dto.response.UserResponse;
import com.xilespa.module.auth.service.AuthService;
import com.xilespa.module.auth.service.model.LoginResult;
import com.xilespa.module.auth.service.model.RefreshResult;
import com.xilespa.security.CookieHelper;
import com.xilespa.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller xử lý các endpoint xác thực /api/auth (FR-AUTH-01, FR-AUTH-02, FR-AUTH-04). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieHelper cookieHelper;

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletResponse response) {

        LoginResult result = authService.login(request);
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
        RefreshResult result = authService.refresh(rawRefreshToken);

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

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        String currentRawRefreshToken =
                CookieHelper.extractCookie(httpRequest, CookieHelper.REFRESH_TOKEN_COOKIE)
                        .orElse(null);
        authService.changePassword(principal.id(), currentRawRefreshToken, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody com.xilespa.module.auth.dto.request.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody com.xilespa.module.auth.dto.request.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
