package com.xilespa.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Lấy người dùng đang đăng nhập của request hiện tại (từ SecurityContext). */
public final class CurrentUser {

    private CurrentUser() {}

    /** Id người đang đăng nhập; rỗng nếu là tác vụ hệ thống (không có phiên đăng nhập). */
    public static Optional<Long> id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal.id());
        }
        return Optional.empty();
    }
}
