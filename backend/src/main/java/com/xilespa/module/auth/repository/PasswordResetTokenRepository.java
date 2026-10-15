package com.xilespa.module.auth.repository;

import com.xilespa.module.auth.entity.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Repository truy xuất Password Reset Token từ bảng {@code password_reset_token}. */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    long countByUser_IdAndCreatedAtAfter(Long userId, Instant after);

    @Modifying
    @Query(
            "UPDATE PasswordResetToken t SET t.usedAt = :now WHERE t.user.id = :userId AND t.usedAt IS NULL")
    void invalidateAllActiveByUserId(Long userId, Instant now);

    void deleteAllByUser_Id(Long userId);
}
