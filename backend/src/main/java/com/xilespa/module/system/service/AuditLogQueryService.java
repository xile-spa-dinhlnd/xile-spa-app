package com.xilespa.module.system.service;

import com.xilespa.common.audit.AuditLog;
import com.xilespa.common.audit.AuditLogRepository;
import com.xilespa.common.response.PageResponse;
import com.xilespa.module.system.dto.response.AuditLogResponse;
import com.xilespa.module.system.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc nhật ký thay đổi cho màn hình quản trị (FR-SYS-01). Chỉ đọc. */
@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    /** Mới nhất trước; lọc theo loại và mã đối tượng nếu có; tối đa 100 dòng mỗi trang. */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(
            String entityType, String entityId, int page, int size) {
        PageRequest pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.clamp(size, 1, MAX_PAGE_SIZE),
                        Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
        Page<AuditLog> result;
        if (entityType == null || entityType.isBlank()) {
            result = auditLogRepository.findAll(pageable);
        } else if (entityId == null || entityId.isBlank()) {
            result = auditLogRepository.findByEntityType(entityType, pageable);
        } else {
            result = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
        }
        return PageResponse.from(result, auditLogMapper::toResponse);
    }
}
