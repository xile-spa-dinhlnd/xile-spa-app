package com.xilespa.module.auth.service;

import com.xilespa.common.exception.BusinessException;
import com.xilespa.common.exception.ErrorCode;
import com.xilespa.module.auth.dto.request.LoginRequest;
import com.xilespa.module.auth.dto.response.UserResponse;
import com.xilespa.module.auth.entity.AppUser;
import com.xilespa.module.auth.entity.RefreshToken;
import com.xilespa.module.auth.mapper.UserMapper;
import com.xilespa.module.auth.repository.AppUserRepository;
import com.xilespa.module.auth.repository.RefreshTokenRepository;
import com.xilespa.security.JwtProperties;
import com.xilespa.security.JwtService;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Xử lý nghiệp vụ đăng nhập, quản lý phiên và làm mới token (FR-AUTH-01, FR-AUTH-02). */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final Clock clock;

    public AuthService(
            AppUserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            UserMapper userMapper,
            Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userMapper = userMapper;
        this.clock = clock;
    }

    public record LoginResult(UserResponse user, String accessToken, String rawRefreshToken) {}

    public record RefreshResult(String accessToken, String rawRefreshToken) {}

    @Transactional
    public LoginResult login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        AppUser user =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.UNAUTHORIZED,
                                                "Email hoặc mật khẩu không đúng."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.setFailedLoginCount(user.getFailedLoginCount() + 1);
            userRepository.save(user);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Email hoặc mật khẩu không đúng.");
        }

        if (!user.isEnabled()) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED, "Tài khoản của bạn đã bị vô hiệu hóa.");
        }

        Instant now = clock.instant();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED, "Tài khoản đang bị tạm khóa. Vui lòng thử lại sau.");
        }

        user.setFailedLoginCount(0);
        user.setLastLoginAt(now);
        userRepository.save(user);

        String accessToken =
                jwtService.generateAccessToken(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRole().name());

        String rawRefreshToken = generateSecureRandomToken();
        String tokenHash = JwtService.hashToken(rawRefreshToken);
        Instant refreshExpiresAt = now.plus(jwtProperties.refreshTokenDuration());

        RefreshToken refreshToken = new RefreshToken(user, tokenHash, refreshExpiresAt);
        refreshTokenRepository.save(refreshToken);

        return new LoginResult(userMapper.toResponse(user), accessToken, rawRefreshToken);
    }

    @Transactional
    public RefreshResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
        }

        Instant now = clock.instant();
        String tokenHash = JwtService.hashToken(rawRefreshToken);
        RefreshToken token =
                refreshTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.UNAUTHORIZED,
                                                "Phiên đăng nhập không hợp lệ hoặc đã hết hạn."));

        // Phát hiện tái sử dụng Refresh Token đã bị thu hồi (chống trộm token)
        if (token.isRevoked()) {
            log.warn(
                    "Phát hiện tái sử dụng Refresh Token đã bị thu hồi cho userId={}. Tiến hành thu hồi toàn bộ phiên.",
                    token.getUser().getId());
            refreshTokenRepository.revokeAllActiveByUserId(token.getUser().getId());
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "Phiên đăng nhập không an toàn. Vui lòng đăng nhập lại.");
        }

        if (token.isExpired(now)) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }

        AppUser user = token.getUser();
        if (!user.isEnabled()) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED, "Tài khoản của bạn đã bị vô hiệu hóa.");
        }

        // Xoay vòng Refresh Token: thu hồi token cũ, cấp token mới
        token.revoke(now);
        refreshTokenRepository.save(token);

        String newRawRefreshToken = generateSecureRandomToken();
        String newTokenHash = JwtService.hashToken(newRawRefreshToken);
        Instant newRefreshExpiresAt = now.plus(jwtProperties.refreshTokenDuration());

        RefreshToken newToken = new RefreshToken(user, newTokenHash, newRefreshExpiresAt);
        refreshTokenRepository.save(newToken);

        String newAccessToken =
                jwtService.generateAccessToken(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRole().name());

        return new RefreshResult(newAccessToken, newRawRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        try {
            String tokenHash = JwtService.hashToken(rawRefreshToken);
            refreshTokenRepository
                    .findByTokenHash(tokenHash)
                    .ifPresent(token -> token.revoke(clock.instant()));
        } catch (Exception e) {
            log.debug("Lỗi khi thu hồi refresh token lúc đăng xuất: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        return userRepository
                .findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.NOT_FOUND,
                                        "Không tìm thấy thông tin người dùng."));
    }

    private String generateSecureRandomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
