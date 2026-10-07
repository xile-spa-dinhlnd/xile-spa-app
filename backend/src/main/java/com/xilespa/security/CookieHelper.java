package com.xilespa.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Tiện ích tạo và xóa cookie httpOnly cho access_token và refresh_token (NFR-SEC-04). */
@Component
public class CookieHelper {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String REFRESH_TOKEN_PATH = "/api/auth";

    private final JwtProperties properties;

    public CookieHelper(JwtProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .path("/")
                .maxAge(properties.accessTokenDuration())
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String rawToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, rawToken)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .path(REFRESH_TOKEN_PATH)
                .maxAge(properties.refreshTokenDuration())
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .path(REFRESH_TOKEN_PATH)
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    public static Optional<String> extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(val -> val != null && !val.isBlank())
                .findFirst();
    }
}
