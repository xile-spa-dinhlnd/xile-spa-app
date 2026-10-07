package com.xilespa.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter đọc JWT Access Token từ cookie httpOnly và thiết lập SecurityContext (NFR-SEC-04). Nếu
 * không có cookie hoặc token không hợp lệ, tiếp tục chuỗi filter để Security xử lý 401 khi cần.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        CookieHelper.extractCookie(request, CookieHelper.ACCESS_TOKEN_COOKIE)
                .flatMap(jwtService::parseAndValidateToken)
                .ifPresent(
                        claims -> {
                            UserPrincipal principal =
                                    UserPrincipal.of(
                                            claims.userId(),
                                            claims.email(),
                                            claims.displayName(),
                                            claims.role());
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            principal, null, principal.getAuthorities());
                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        });

        filterChain.doFilter(request, response);
    }
}
