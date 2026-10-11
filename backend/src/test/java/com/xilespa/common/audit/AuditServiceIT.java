package com.xilespa.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xilespa.module.auth.entity.AppUser;
import com.xilespa.module.auth.enums.UserRole;
import com.xilespa.module.auth.repository.AppUserRepository;
import com.xilespa.security.UserPrincipal;
import com.xilespa.support.IntegrationTest;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ghi nhật ký trên PostgreSQL thật. Bảng audit_log chỉ thêm (trigger chặn xóa), nên mỗi test dùng
 * entityType ngẫu nhiên để không ảnh hưởng nhau.
 */
@IntegrationTest
class AuditServiceIT {

    @Autowired AuditService auditService;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired AppUserRepository userRepository;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;

    private final String entityType = "test_" + UUID.randomUUID().toString().substring(0, 8);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsActorTimeAndMasksSensitiveFields() {
        // Người thực hiện là một app_user thật (khóa ngoại). Chạy trong transaction rồi rollback
        // để không để lại user hay nhật ký trỏ tới user (test khác có xóa toàn bộ app_user).
        tx.executeWithoutResult(
                status -> {
                    AppUser user =
                            userRepository.save(
                                    new AppUser(
                                            entityType + "@xilespa.vn",
                                            "hash",
                                            "Người thử",
                                            UserRole.OWNER,
                                            clock.instant()));
                    loginAs(user.getId());

                    auditService.record(
                            AuditAction.UPDATE,
                            entityType,
                            42,
                            Map.of("name", "Gội 30 phút", "price", 69000),
                            Map.of(
                                    "name",
                                    "Gội 30 phút",
                                    "price",
                                    79000,
                                    "passwordHash",
                                    "bí-mật",
                                    "nested",
                                    Map.of("refreshToken", "tok")));

                    AuditLog log = onlyLogOf(entityType);
                    assertThat(log.getActorId()).isEqualTo(user.getId());
                    assertThat(log.getAction()).isEqualTo(AuditAction.UPDATE);
                    assertThat(log.getEntityId()).isEqualTo("42");
                    assertThat(log.getOccurredAt()).isNotNull();
                    assertThat(log.getBeforeData()).contains("69000");
                    assertThat(log.getAfterData())
                            .contains("79000")
                            .doesNotContain("bí-mật", "tok")
                            .contains(AuditService.MASK);

                    status.setRollbackOnly();
                });
    }

    @Test
    void systemActionHasNoActor() {
        tx.executeWithoutResult(
                status ->
                        auditService.record(
                                AuditAction.CREATE, entityType, "1", null, Map.of("a", 1)));

        AuditLog log = onlyLogOf(entityType);
        assertThat(log.getActorId()).isNull();
        assertThat(log.getBeforeData()).isNull();
        assertThat(log.getAfterData()).isEqualToIgnoringWhitespace("{\"a\":1}");
    }

    @Test
    void logIsRolledBackTogetherWithTheBusinessChange() {
        tx.executeWithoutResult(
                status -> {
                    auditService.record(AuditAction.CREATE, entityType, "1", null, Map.of());
                    status.setRollbackOnly(); // nghiệp vụ thất bại
                });

        assertThat(countLogsOf(entityType)).isZero();
    }

    @Test
    void recordingOutsideATransactionFails() {
        assertThatThrownBy(
                        () ->
                                auditService.record(
                                        AuditAction.CREATE, entityType, "1", null, Map.of()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void auditLogIsAppendOnlyInTheDatabase() {
        tx.executeWithoutResult(
                status -> auditService.record(AuditAction.CREATE, entityType, "1", null, Map.of()));

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE audit_log SET action = 'UPDATE' WHERE entity_type = ?",
                                        entityType))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chỉ cho phép thêm");
    }

    private void loginAs(Long userId) {
        UserPrincipal principal = UserPrincipal.of(userId, "x@xilespa.vn", "X", "OWNER");
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()));
    }

    private AuditLog onlyLogOf(String type) {
        var logs = auditLogRepository.findByEntityType(type, PageRequest.of(0, 10)).getContent();
        assertThat(logs).hasSize(1);
        return logs.getFirst();
    }

    private long countLogsOf(String type) {
        return auditLogRepository.findByEntityType(type, PageRequest.of(0, 10)).getTotalElements();
    }
}
