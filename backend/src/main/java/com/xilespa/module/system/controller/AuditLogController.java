package com.xilespa.module.system.controller;

import com.xilespa.common.response.PageResponse;
import com.xilespa.module.system.dto.response.AuditLogResponse;
import com.xilespa.module.system.service.AuditLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Xem nhật ký thay đổi (FR-SYS-01). Chỉ đọc; nhật ký không sửa, không xóa qua API. */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    public PageResponse<AuditLogResponse> search(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditLogQueryService.search(entityType, entityId, page, size);
    }
}
