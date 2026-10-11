package com.xilespa.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Một dòng nhật ký thay đổi, ánh xạ bảng {@code audit_log} (V1). Chỉ thêm, không sửa, không xóa
 * (BR-18): {@link Immutable} ở tầng JPA và trigger {@code trg_audit_log_append_only} ở CSDL. Người
 * thực hiện lưu bằng id ({@code actorId}), không tham chiếu entity của module auth.
 */
@Entity
@Table(name = "audit_log")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    /** Id người thực hiện ({@code app_user.id}); {@code null} nghĩa là hệ thống tự làm. */
    @Column(name = "actor_id", updatable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40, updatable = false)
    private AuditAction action;

    /** Tên bảng của đối tượng bị thay đổi, ví dụ {@code app_user}, {@code service}. */
    @Column(name = "entity_type", nullable = false, length = 60, updatable = false)
    private String entityType;

    @Column(name = "entity_id", length = 64, updatable = false)
    private String entityId;

    /** Giá trị trước khi đổi (chuỗi JSON đã che trường nhạy cảm); {@code null} khi tạo mới. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", updatable = false)
    private String beforeData;

    /** Giá trị sau khi đổi (chuỗi JSON đã che trường nhạy cảm). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", updatable = false)
    private String afterData;

    public AuditLog(
            Instant occurredAt,
            Long actorId,
            AuditAction action,
            String entityType,
            String entityId,
            String beforeData,
            String afterData) {
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.beforeData = beforeData;
        this.afterData = afterData;
    }
}
