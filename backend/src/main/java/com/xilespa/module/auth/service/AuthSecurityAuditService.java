package com.xilespa.module.auth.service;

import com.xilespa.module.auth.repository.AppUserRepository;
import com.xilespa.module.auth.repository.RefreshTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
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

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final com.xilespa.module.auth.config.AuthSecurityProperties authSecurityProperties;

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
                            user.recordFailedLogin(
                                    authSecurityProperties.maxFailedAttempts(),
                                    authSecurityProperties.lockDuration(),
                                    now);
                            userRepository.save(user);
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
