package com.xilespa.module.system.dto.response;

import com.xilespa.common.audit.AuditAction;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * Một dòng nhật ký trả cho giao diện.
 *
 * @param actorId id người thực hiện; {@code null} là hệ thống
 * @param before giá trị trước khi đổi (JSON, đã che trường nhạy cảm)
 * @param after giá trị sau khi đổi (JSON, đã che trường nhạy cảm)
 */
public record AuditLogResponse(
        Long id,
        Instant occurredAt,
        Long actorId,
        AuditAction action,
        String entityType,
        String entityId,
        JsonNode before,
        JsonNode after) {}
