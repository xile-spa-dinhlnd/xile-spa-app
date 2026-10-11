package com.xilespa.module.system.mapper;

import com.xilespa.common.audit.AuditLog;
import com.xilespa.module.system.dto.response.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Viết tay thay vì MapStruct (backend/AGENTS.md, quy tắc mapper số 4): cần đọc chuỗi JSON thành
 * {@link JsonNode} để trả về đúng dạng đối tượng, không phải chuỗi.
 */
@Component
@RequiredArgsConstructor
public class AuditLogMapper {

    private final ObjectMapper objectMapper;

    public AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getOccurredAt(),
                log.getActorId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                readJson(log.getBeforeData()),
                readJson(log.getAfterData()));
    }

    private JsonNode readJson(String json) {
        return json == null ? null : objectMapper.readTree(json);
    }
}
