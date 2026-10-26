package com.xilespa.module.catalog.service;

import com.xilespa.common.audit.AuditAction;
import com.xilespa.common.audit.AuditService;
import com.xilespa.common.exception.BusinessException;
import com.xilespa.common.exception.ErrorCode;
import com.xilespa.common.response.PageRequests;
import com.xilespa.common.response.PageResponse;
import com.xilespa.common.util.TextNormalizer;
import com.xilespa.module.catalog.dto.request.ServiceItemRequest;
import com.xilespa.module.catalog.dto.response.ServiceItemResponse;
import com.xilespa.module.catalog.entity.ServiceGroup;
import com.xilespa.module.catalog.entity.ServiceItem;
import com.xilespa.module.catalog.mapper.CatalogMapper;
import com.xilespa.module.catalog.repository.ComboItemRepository;
import com.xilespa.module.catalog.repository.ServiceGroupRepository;
import com.xilespa.module.catalog.repository.ServiceItemRepository;
import com.xilespa.module.catalog.repository.ServicePriceHistoryRepository;
import com.xilespa.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý bảng giá dịch vụ (FR-SVC-01 đến 04). Mọi thay đổi ghi nhật ký trong cùng transaction
 * (BR-18). Không có xóa cứng (BR-19).
 */
@Service
@RequiredArgsConstructor
public class ServiceItemService {

    static final String AUDIT_ENTITY = "service";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_ACTIVE_COMBOS_IN_MESSAGE = 3;

    private final ServiceItemRepository serviceRepository;
    private final ServiceGroupRepository groupRepository;
    private final ServicePriceHistoryRepository priceHistoryRepository;
    private final ComboItemRepository comboItemRepository;
    private final CatalogMapper mapper;
    private final AuditService auditService;
    private final Clock clock;

    /**
     * Danh sách dịch vụ: tìm theo tên, lọc nhóm và trạng thái; xếp theo nhóm rồi tên (FR-SVC-01).
     */
    @Transactional(readOnly = true)
    public PageResponse<ServiceItemResponse> search(
            String q, Long groupId, Boolean active, int page, int size) {
        String namePattern = likePattern(q);
        PageRequest pageable =
                PageRequests.of(
                        page, size, MAX_PAGE_SIZE, Sort.by("group.sortOrder", "name", "id"));
        return PageResponse.from(
                serviceRepository.search(namePattern, groupId, active, pageable),
                mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ServiceItemResponse get(Long id) {
        return mapper.toResponse(findService(id));
    }

    /** Tạo dịch vụ (FR-SVC-02). */
    @Transactional
    public ServiceItemResponse create(ServiceItemRequest request) {
        String name = TextNormalizer.normalizeName(request.name());
        ensureNameAvailable(name, 0L);
        ServiceItem service =
                new ServiceItem(
                        findActiveGroup(request.groupId()),
                        name,
                        request.durationMinutes(),
                        request.listPrice(),
                        tourFeeOf(request),
                        clock.instant());
        saveChecked(service);

        ServiceItemResponse created = mapper.toResponse(service);
        auditService.record(AuditAction.CREATE, AUDIT_ENTITY, service.getId(), null, created);
        return created;
    }

    /** Sửa dịch vụ (FR-SVC-03). Đổi giá thì ghi thêm lịch sử giá. */
    @Transactional
    public ServiceItemResponse update(Long id, ServiceItemRequest request) {
        ServiceItem service = findService(id);
        ServiceItemResponse before = mapper.toResponse(service);

        String name = TextNormalizer.normalizeName(request.name());
        ensureNameAvailable(name, id);
        Instant now = clock.instant();
        service.updateDetails(
                findActiveGroup(request.groupId()),
                name,
                request.durationMinutes(),
                tourFeeOf(request),
                now);
        service.changePrice(request.listPrice(), currentUserId(), now)
                .ifPresent(priceHistoryRepository::save);
        saveChecked(service);

        ServiceItemResponse after = mapper.toResponse(service);
        if (!before.equals(after)) {
            auditService.record(AuditAction.UPDATE, AUDIT_ENTITY, id, before, after);
        }
        return after;
    }

    /**
     * Ngừng bán (FR-SVC-04, OQ-22). Gọi lại khi đã ngừng bán thì không đổi gì. Chặn ngừng bán nếu
     * dịch vụ đang nằm trong bất kỳ combo nào đang bán (409 SERVICE_IN_ACTIVE_COMBO).
     */
    @Transactional
    public ServiceItemResponse deactivate(Long id) {
        ServiceItem service = findService(id);
        if (service.isActive()) {
            List<String> activeComboNames = comboItemRepository.findActiveComboNamesByServiceId(id);
            if (!activeComboNames.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.SERVICE_IN_ACTIVE_COMBO,
                        "Dịch vụ đang có trong combo đang bán: "
                                + formatActiveComboNames(activeComboNames)
                                + ". Hãy ngừng bán các combo này hoặc bỏ dịch vụ khỏi combo trước.");
            }
        }

        ServiceItemResponse before = mapper.toResponse(service);
        if (service.deactivate(clock.instant())) {
            ServiceItemResponse after = mapper.toResponse(service);
            auditService.record(AuditAction.DEACTIVATE, AUDIT_ENTITY, id, before, after);
            return after;
        }
        return before;
    }

    private String formatActiveComboNames(List<String> activeComboNames) {
        if (activeComboNames.size() <= MAX_ACTIVE_COMBOS_IN_MESSAGE) {
            return String.join(", ", activeComboNames);
        }
        return String.join(", ", activeComboNames.subList(0, MAX_ACTIVE_COMBOS_IN_MESSAGE))
                + " và "
                + (activeComboNames.size() - MAX_ACTIVE_COMBOS_IN_MESSAGE)
                + " combo khác";
    }

    /**
     * Bán lại (FR-SVC-04). Gọi lại khi đang bán thì không đổi gì. Nhóm đã ngừng dùng thì không cho
     * bán lại (OQ-21).
     */
    @Transactional
    public ServiceItemResponse activate(Long id) {
        ServiceItem service = findService(id);
        if (!service.isActive() && !service.getGroup().isActive()) {
            throw new BusinessException(ErrorCode.SERVICE_GROUP_NOT_FOUND);
        }
        ServiceItemResponse before = mapper.toResponse(service);
        if (service.activate(clock.instant())) {
            ServiceItemResponse after = mapper.toResponse(service);
            auditService.record(AuditAction.ACTIVATE, AUDIT_ENTITY, id, before, after);
            return after;
        }
        return before;
    }

    private ServiceItem findService(Long id) {
        return serviceRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.NOT_FOUND, "Không tìm thấy dịch vụ."));
    }

    private ServiceGroup findActiveGroup(Long groupId) {
        return groupRepository
                .findById(groupId)
                .filter(ServiceGroup::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_GROUP_NOT_FOUND));
    }

    private void ensureNameAvailable(String name, long excludeId) {
        if (serviceRepository.existsByNormalizedName(name, excludeId)) {
            throw new BusinessException(ErrorCode.SERVICE_NAME_DUPLICATE);
        }
    }

    /** Lưu ngay để bắt trường hợp hai request cùng tạo một tên (unique index ux_service_name). */
    private void saveChecked(ServiceItem service) {
        try {
            serviceRepository.saveAndFlush(service);
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains("ux_service_name")) {
                throw new BusinessException(ErrorCode.SERVICE_NAME_DUPLICATE);
            }
            throw e;
        }
    }

    /** Mẫu LIKE viết thường, thoát ký tự đặc biệt % _ \\ của người dùng; rỗng thì lấy tất cả. */
    static String likePattern(String q) {
        if (q == null) {
            return "%";
        }
        String normalized = TextNormalizer.normalizeName(q);
        if (normalized.isEmpty()) {
            return "%";
        }
        String escaped =
                normalized
                        .toLowerCase(java.util.Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static long tourFeeOf(ServiceItemRequest request) {
        return request.tourFee() == null ? 0L : request.tourFee();
    }

    private static Long currentUserId() {
        return CurrentUser.id()
                .orElseThrow(
                        () -> new IllegalStateException("Đổi giá cần người dùng đang đăng nhập"));
    }
}
