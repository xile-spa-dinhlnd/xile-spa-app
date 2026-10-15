package com.xilespa.module.auth.service;

import com.xilespa.common.audit.AuditAction;
import com.xilespa.common.audit.AuditService;
import com.xilespa.common.exception.BusinessException;
import com.xilespa.common.exception.ErrorCode;
import com.xilespa.module.auth.config.AuthSecurityProperties;
import com.xilespa.module.auth.dto.request.ChangePasswordRequest;
import com.xilespa.module.auth.dto.request.ForgotPasswordRequest;
import com.xilespa.module.auth.dto.request.LoginRequest;
import com.xilespa.module.auth.dto.request.ResetPasswordRequest;
import com.xilespa.module.auth.dto.response.UserResponse;
import com.xilespa.module.auth.entity.AppUser;
import com.xilespa.module.auth.entity.PasswordResetToken;
import com.xilespa.module.auth.entity.RefreshToken;
import com.xilespa.module.auth.mapper.UserMapper;
import com.xilespa.module.auth.repository.AppUserRepository;
import com.xilespa.module.auth.repository.PasswordResetTokenRepository;
import com.xilespa.module.auth.repository.RefreshTokenRepository;
import com.xilespa.module.auth.service.model.LoginResult;
import com.xilespa.module.auth.service.model.RefreshResult;
import com.xilespa.module.auth.validator.PasswordPolicyValidator;
import com.xilespa.security.JwtProperties;
import com.xilespa.security.JwtService;
import com.xilespa.security.TokenHasher;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xử lý nghiệp vụ đăng nhập, quản lý phiên, đổi mật khẩu và quên mật khẩu (FR-AUTH-01, FR-AUTH-02,
 * FR-AUTH-03, FR-AUTH-04).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Chuỗi băm BCrypt hợp lệ giả lập dùng để chống Timing Attack khi người dùng nhập email không
     * tồn tại trong hệ thống (NFR-SEC-02).
     */
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi59z6p/0kI2Xg9C4H9L5S6V7K8W9X.";

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthSecurityAuditService authSecurityAuditService;
    private final PasswordResetEmailSender passwordResetEmailSender;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthSecurityProperties authSecurityProperties;
    private final UserMapper userMapper;
    private final Clock clock;

    @Transactional
    public LoginResult login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        Instant now = clock.instant();

        Optional<AppUser> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            // Chống Timing Attack: Luôn gọi BCrypt matches với dummy hash để giữ thời gian phản hồi
            // đồng nhất
            passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "Email hoặc mật khẩu không đúng, hoặc tài khoản đang tạm khóa. Thử lại sau ít"
                            + " phút hoặc dùng Quên mật khẩu.");
        }

        AppUser user = userOpt.get();

        if (user.isLocked(now)) {
            // Chống Timing Attack: Luôn gọi BCrypt matches với dummy hash để giữ thời gian phản hồi
            // đồng nhất
            passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "Email hoặc mật khẩu không đúng, hoặc tài khoản đang tạm khóa. Thử lại sau ít"
                            + " phút hoặc dùng Quên mật khẩu.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // Ghi nhận số lần đăng nhập sai trong transaction độc lập (không bị rollback bởi
            // exception bên dưới)
            authSecurityAuditService.recordFailedLogin(user.getId(), now);
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "Email hoặc mật khẩu không đúng, hoặc tài khoản đang tạm khóa. Thử lại sau ít"
                            + " phút hoặc dùng Quên mật khẩu.");
        }

        if (!user.isEnabled()) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED, "Tài khoản của bạn đã bị vô hiệu hóa.");
        }

        user.recordSuccessfulLogin(now);
        userRepository.save(user);

        String accessToken =
                jwtService.generateAccessToken(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRole().name());

        String rawRefreshToken = generateSecureRandomToken();
        String tokenHash = TokenHasher.hash(rawRefreshToken);
        Instant refreshExpiresAt = now.plus(jwtProperties.refreshTokenDuration());

        RefreshToken refreshToken = new RefreshToken(user, tokenHash, refreshExpiresAt, now);
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
        String tokenHash = TokenHasher.hash(rawRefreshToken);
        RefreshToken token =
                refreshTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.UNAUTHORIZED,
                                                "Phiên đăng nhập không hợp lệ hoặc đã hết hạn."));

        // Phát hiện tái sử dụng Refresh Token đã bị thu hồi (chống trộm token, FR-AUTH-02)
        if (token.isRevoked()) {
            log.warn(
                    "Phát hiện tái sử dụng Refresh Token đã bị thu hồi cho userId={}. Tiến hành thu hồi toàn bộ phiên.",
                    token.getUser().getId());
            // Thu hồi toàn bộ phiên trong transaction độc lập để COMMIT ngay lập tức vào CSDL
            authSecurityAuditService.revokeAllUserTokensOnReuse(token.getUser().getId());
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
        String newTokenHash = TokenHasher.hash(newRawRefreshToken);
        Instant newRefreshExpiresAt = now.plus(jwtProperties.refreshTokenDuration());

        RefreshToken newToken = new RefreshToken(user, newTokenHash, newRefreshExpiresAt, now);
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
            String tokenHash = TokenHasher.hash(rawRefreshToken);
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

    /**
     * Đổi mật khẩu tài khoản đang đăng nhập (FR-AUTH-04).
     *
     * <p>Xác thực mật khẩu cũ, áp dụng chính sách bảo mật NFR-SEC-03, thu hồi toàn bộ phiên khác
     * nhưng giữ lại phiên hiện tại, và ghi nhật ký thay đổi (BR-18, không lưu mật khẩu thô).
     *
     * @param userId Id tài khoản đang đăng nhập.
     * @param currentRawRefreshToken Refresh token của phiên hiện tại (nếu có) để không thu hồi
     *     phiên này.
     * @param request Mật khẩu cũ và mật khẩu mới.
     */
    @Transactional
    public void changePassword(
            Long userId, String currentRawRefreshToken, ChangePasswordRequest request) {
        AppUser user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                "Không tìm thấy thông tin người dùng."));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    ErrorCode.PASSWORD_INCORRECT, "Mật khẩu hiện tại không chính xác.");
        }

        passwordPolicyValidator.validate(request.newPassword(), user.getPasswordHash());

        Instant now = clock.instant();
        user.changePassword(passwordEncoder.encode(request.newPassword()), now);
        userRepository.save(user);

        // Thu hồi toàn bộ phiên khác, chỉ giữ lại phiên hiện tại
        if (currentRawRefreshToken != null && !currentRawRefreshToken.isBlank()) {
            String currentTokenHash = TokenHasher.hash(currentRawRefreshToken);
            refreshTokenRepository.revokeAllActiveByUserIdExceptToken(
                    user.getId(), currentTokenHash);
        } else {
            refreshTokenRepository.revokeAllActiveByUserId(user.getId());
        }

        // Ghi audit log (BR-18): Tuyệt đối không ghi mật khẩu thô hay chuỗi băm
        auditService.record(
                AuditAction.UPDATE,
                "app_user",
                user.getId(),
                Map.of("email", user.getEmail(), "action", "CHANGE_PASSWORD"),
                Map.of("email", user.getEmail(), "status", "SUCCESS"));
    }

    /**
     * Yêu cầu gửi liên kết đặt lại mật khẩu qua email (FR-AUTH-03).
     *
     * <p>Chống dò email (Anti-Enumeration): Dù email có tồn tại hay không, phương thức đều kết thúc
     * thành công mà không làm lộ trạng thái tài khoản. Giới hạn tối đa 3 lần/15 phút và vô hiệu hóa
     * link cũ khi yêu cầu link mới.
     *
     * @param request Yêu cầu chứa email.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        Optional<AppUser> userOpt = userRepository.findByEmailIgnoreCase(email);

        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            log.debug("Yêu cầu quên mật khẩu cho email không tồn tại hoặc bị vô hiệu: {}", email);
            return;
        }

        AppUser user = userOpt.get();
        Instant now = clock.instant();

        // Giới hạn tần suất: Tối đa 3 lần yêu cầu trong vòng 15 phút (chống spam/lạm dụng)
        long recentRequests =
                passwordResetTokenRepository.countByUser_IdAndCreatedAtAfter(
                        user.getId(), now.minus(java.time.Duration.ofMinutes(15)));
        if (recentRequests >= 3) {
            log.warn(
                    "Tần suất yêu cầu quên mật khẩu vượt quá giới hạn (3 lần/15 phút) cho userId={}",
                    user.getId());
            return;
        }

        // Vô hiệu hóa toàn bộ link đặt lại mật khẩu còn hiệu lực trước đó của tài khoản
        passwordResetTokenRepository.invalidateAllActiveByUserId(user.getId(), now);

        String rawToken = generateSecureRandomToken();
        String tokenHash = TokenHasher.hash(rawToken);
        Instant expiresAt = now.plus(authSecurityProperties.passwordResetDuration());

        PasswordResetToken resetToken = new PasswordResetToken(user, tokenHash, expiresAt, now);
        passwordResetTokenRepository.save(resetToken);

        String resetLink = authSecurityProperties.resetPasswordBaseUrl() + "?token=" + rawToken;
        passwordResetEmailSender.sendResetEmail(user.getEmail(), resetLink);
    }

    /**
     * Đặt lại mật khẩu mới bằng token xác thực (FR-AUTH-03).
     *
     * <p>Xác thực tính hợp lệ và hạn dùng của token, áp dụng chính sách bảo mật NFR-SEC-03, cập
     * nhật mật khẩu mới, giải phóng khóa tài khoản (nếu có), thu hồi toàn bộ phiên đăng nhập cũ và
     * ghi audit log.
     *
     * @param request Yêu cầu chứa token và mật khẩu mới.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String rawToken = request.token().trim();
        String tokenHash = TokenHasher.hash(rawToken);
        Instant now = clock.instant();

        PasswordResetToken token =
                passwordResetTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.RESET_TOKEN_INVALID,
                                                "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết"
                                                        + " hạn."));

        if (!token.isValid(now)) {
            throw new BusinessException(
                    ErrorCode.RESET_TOKEN_INVALID,
                    "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
        }

        AppUser user = token.getUser();
        passwordPolicyValidator.validate(request.newPassword(), user.getPasswordHash());

        token.markUsed(now);
        passwordResetTokenRepository.save(token);

        user.changePassword(passwordEncoder.encode(request.newPassword()), now);
        // Giải phóng trạng thái khóa nếu tài khoản bị tạm khóa mà không cập nhật thời điểm đăng
        // nhập
        user.unlock(now);
        userRepository.save(user);

        // Sau khi đặt lại thành công, dọn sạch toàn bộ token đặt lại mật khẩu cũ của tài khoản
        passwordResetTokenRepository.deleteAllByUser_Id(user.getId());

        // Thu hồi toàn bộ phiên đăng nhập cũ (FR-AUTH-03)
        refreshTokenRepository.revokeAllActiveByUserId(user.getId());

        // Ghi nhật ký audit (BR-18): Tuyệt đối không ghi mật khẩu hay chuỗi băm
        auditService.record(
                AuditAction.UPDATE,
                "app_user",
                user.getId(),
                Map.of("email", user.getEmail(), "action", "RESET_PASSWORD"),
                Map.of("email", user.getEmail(), "status", "SUCCESS"));
    }

    private String generateSecureRandomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
