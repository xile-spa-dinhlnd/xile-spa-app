package com.xilespa.module.auth.service;

import com.xilespa.common.audit.AuditAction;
import com.xilespa.common.audit.AuditService;
import com.xilespa.module.auth.config.AuthSecurityProperties;
import com.xilespa.module.auth.repository.AppUserRepository;
import com.xilespa.module.auth.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service xử lý các thao tác ghi nhận bảo mật độc lập (chống rollback khi ném lỗi nghiệp vụ). Sử
 * dụng {@link Propagation#REQUIRES_NEW} để đảm bảo các thay đổi quan trọng (tăng bộ đếm sai, thu
 * hồi toàn bộ token khi bị xâm nhập) luôn được COMMIT vào CSDL ngay cả khi nghiệp vụ kết thúc bằng
 * ngoại lệ 401 Unauthorized.
 */
@Service
@RequiredArgsConstructor
public class AuthSecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(AuthSecurityAuditService.class);

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthSecurityProperties authSecurityProperties;
    private final AuditService auditService;

    /**
     * Ghi nhận một lần đăng nhập thất bại vào CSDL trong transaction độc lập (FR-AUTH-05). Đảm bảo
     * bộ đếm và trạng thái tạm khóa không bị rollback khi ném BusinessException 401.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedLogin(Long userId, Instant now) {
        userRepository
                .findById(userId)
                .ifPresent(
                        user -> {
                            boolean wasLocked =
                                    user.recordFailedLogin(
                                            authSecurityProperties.maxFailedAttempts(),
                                            authSecurityProperties.lockDuration(),
                                            now);
                            userRepository.save(user);

                            if (wasLocked) {
                                log.warn(
                                        "Tài khoản userId={} bị tạm khóa đến {} do đăng nhập sai {} lần",
                                        user.getId(),
                                        user.getLockedUntil(),
                                        user.getFailedLoginCount());
                                auditService.record(
                                        AuditAction.UPDATE,
                                        "app_user",
                                        user.getId(),
                                        Map.of(
                                                "email",
                                                user.getEmail(),
                                                "action",
                                                "ACCOUNT_LOCKED",
                                                "failedAttempts",
                                                user.getFailedLoginCount()),
                                        Map.of(
                                                "status",
                                                "LOCKED",
                                                "lockedUntil",
                                                String.valueOf(user.getLockedUntil())));
                            }
                        });
    }

    /**
     * Thu hồi toàn bộ Refresh Token của tài khoản khi phát hiện token cũ bị kẻ gian tái sử dụng
     * (FR-AUTH-02). Đảm bảo việc thu hồi được COMMIT ngay lập tức để bảo vệ tài khoản nạn nhân.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllUserTokensOnReuse(Long userId) {
        refreshTokenRepository.revokeAllActiveByUserId(userId);
    }
}
