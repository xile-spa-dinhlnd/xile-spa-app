package com.xilespa.module.catalog.service;

import com.xilespa.common.audit.AuditAction;
import com.xilespa.common.audit.AuditService;
import com.xilespa.common.exception.BusinessException;
import com.xilespa.common.exception.ErrorCode;
import com.xilespa.module.catalog.dto.request.ServiceGroupRequest;
import com.xilespa.module.catalog.dto.response.ServiceGroupResponse;
import com.xilespa.module.catalog.entity.ServiceGroup;
import com.xilespa.module.catalog.mapper.CatalogMapper;
import com.xilespa.module.catalog.repository.ServiceGroupRepository;
import com.xilespa.module.catalog.repository.ServiceItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý nhóm dịch vụ (FR-SVC-05). Mọi thay đổi ghi nhật ký trong cùng transaction (BR-18). Không
 * có xóa cứng: ngừng dùng thì chỉ đánh dấu active = false (BR-19).
 */
@Service
@RequiredArgsConstructor
public class ServiceGroupService {

    static final String AUDIT_ENTITY = "service_group";

    private final ServiceGroupRepository groupRepository;
    private final ServiceItemRepository serviceRepository;
    private final CatalogMapper mapper;
    private final AuditService auditService;

    /**
     * Danh sách nhóm dịch vụ, sắp theo {@code sort_order ASC, name ASC}.
     *
     * @param active {@code null} là lấy tất cả (cả đang dùng và ngừng dùng), {@code true} là chỉ
     *     lấy nhóm đang dùng, {@code false} là chỉ lấy nhóm ngừng dùng.
     */
    @Transactional(readOnly = true)
    public List<ServiceGroupResponse> list(Boolean active) {
        List<ServiceGroup> groups =
                active == null
                        ? groupRepository.findAllByOrderBySortOrderAscNameAsc()
                        : groupRepository.findByActiveOrderBySortOrderAscNameAsc(active);
        return groups.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServiceGroupResponse get(Long id) {
        return mapper.toResponse(findGroup(id));
    }

    @Transactional
    public ServiceGroupResponse create(ServiceGroupRequest request) {
        String name = normalizeSpaces(request.name());
        ensureNameAvailable(name, 0L);

        ServiceGroup group = new ServiceGroup(name, request.sortOrder());
        saveChecked(group);

        ServiceGroupResponse created = mapper.toResponse(group);
        auditService.record(AuditAction.CREATE, AUDIT_ENTITY, group.getId(), null, created);
        return created;
    }

    @Transactional
    public ServiceGroupResponse update(Long id, ServiceGroupRequest request) {
        ServiceGroup group = findGroup(id);
        ServiceGroupResponse before = mapper.toResponse(group);

        String name = normalizeSpaces(request.name());
        ensureNameAvailable(name, id);
        group.update(name, request.sortOrder());
        saveChecked(group);

        ServiceGroupResponse after = mapper.toResponse(group);
        if (!before.equals(after)) {
            auditService.record(AuditAction.UPDATE, AUDIT_ENTITY, id, before, after);
        }
        return after;
    }

    /**
     * Ngừng dùng nhóm dịch vụ. Gọi lại khi đã ngừng dùng thì không đổi gì và không ghi audit. Nhóm
     * còn dịch vụ đang bán thì không cho ngừng dùng, vì sửa dịch vụ đó sẽ bị từ chối do nhóm đã
     * ngừng dùng (giả định của S2-06, ghi trong PR).
     */
    @Transactional
    public ServiceGroupResponse deactivate(Long id) {
        ServiceGroup group = findGroup(id);
        if (group.isActive() && serviceRepository.existsByGroupIdAndActiveTrue(id)) {
            throw new BusinessException(ErrorCode.SERVICE_GROUP_IN_USE);
        }
        ServiceGroupResponse before = mapper.toResponse(group);
        if (group.deactivate()) {
            ServiceGroupResponse after = mapper.toResponse(group);
            auditService.record(AuditAction.DEACTIVATE, AUDIT_ENTITY, id, before, after);
            return after;
        }
        return before;
    }

    /** Dùng lại nhóm dịch vụ. Gọi lại khi đang dùng thì không đổi gì và không ghi audit. */
    @Transactional
    public ServiceGroupResponse activate(Long id) {
        ServiceGroup group = findGroup(id);
        ServiceGroupResponse before = mapper.toResponse(group);
        if (group.activate()) {
            ServiceGroupResponse after = mapper.toResponse(group);
            auditService.record(AuditAction.ACTIVATE, AUDIT_ENTITY, id, before, after);
            return after;
        }
        return before;
    }

    private ServiceGroup findGroup(Long id) {
        return groupRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.NOT_FOUND, "Không tìm thấy nhóm dịch vụ."));
    }

    private void ensureNameAvailable(String name, long excludeId) {
        if (groupRepository.existsByNormalizedName(name, excludeId)) {
            throw new BusinessException(ErrorCode.SERVICE_GROUP_NAME_DUPLICATE);
        }
    }

    /**
     * Lưu ngay để bắt trường hợp hai request cùng tạo một tên (unique index ux_service_group_name).
     */
    private void saveChecked(ServiceGroup group) {
        try {
            groupRepository.saveAndFlush(group);
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage())
                    .contains("ux_service_group_name")) {
                throw new BusinessException(ErrorCode.SERVICE_GROUP_NAME_DUPLICATE);
            }
            throw e;
        }
    }

    /** Cắt đầu cuối và gộp khoảng trắng liên tiếp, khớp với cách CSDL so sánh tên. */
    private static String normalizeSpaces(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}
