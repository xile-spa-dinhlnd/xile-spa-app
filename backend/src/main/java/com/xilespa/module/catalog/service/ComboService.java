package com.xilespa.module.catalog.service;

import com.xilespa.common.audit.AuditAction;
import com.xilespa.common.audit.AuditService;
import com.xilespa.common.exception.BusinessException;
import com.xilespa.common.exception.ErrorCode;
import com.xilespa.module.catalog.dto.request.ComboItemRequest;
import com.xilespa.module.catalog.dto.request.ComboQuoteRequest;
import com.xilespa.module.catalog.dto.request.ComboRequest;
import com.xilespa.module.catalog.dto.response.ComboItemResponse;
import com.xilespa.module.catalog.dto.response.ComboQuoteResponse;
import com.xilespa.module.catalog.dto.response.ComboResponse;
import com.xilespa.module.catalog.entity.Combo;
import com.xilespa.module.catalog.entity.ServiceItem;
import com.xilespa.module.catalog.mapper.CatalogMapper;
import com.xilespa.module.catalog.repository.ComboRepository;
import com.xilespa.module.catalog.repository.ServiceItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý combo bảng giá (FR-CMB-01 đến 05). Mọi thay đổi ghi nhật ký trong cùng transaction
 * (BR-18). Không có xóa cứng (BR-19).
 */
@Service
@RequiredArgsConstructor
public class ComboService {

    static final String AUDIT_ENTITY = "combo";

    private final ComboRepository comboRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final CatalogMapper mapper;
    private final AuditService auditService;
    private final Clock clock;

    private record ItemLine(ServiceItem service, int quantity) {}

    private record LineCalculation(List<ComboItemResponse> items, long retailTotal) {}

    /** Xem chi tiết một combo kèm thành phần và tổng giá lẻ, tiết kiệm (FR-CMB-01). */
    @Transactional(readOnly = true)
    public ComboResponse get(Long id) {
        return toResponse(findComboWithItems(id));
    }

    /** Tạo combo mới kèm các dịch vụ thành phần (FR-CMB-02). */
    @Transactional
    public ComboResponse create(ComboRequest request) {
        String name = normalizeSpaces(request.name());
        ensureNameAvailable(name, 0L);
        checkDuplicateServiceItems(request.items());

        Map<Long, ServiceItem> serviceMap = findAndValidateActiveServices(request.items());

        Map<ServiceItem, Integer> itemMap = new LinkedHashMap<>();
        for (ComboItemRequest itemReq : request.items()) {
            itemMap.put(serviceMap.get(itemReq.serviceId()), itemReq.quantity());
        }

        long tourFee = request.tourFee() == null ? 0L : request.tourFee();
        Combo combo = new Combo(name, request.price(), tourFee, itemMap, clock.instant());
        saveChecked(combo);

        ComboResponse created = toResponse(combo);
        auditService.record(AuditAction.CREATE, AUDIT_ENTITY, combo.getId(), null, created);
        return created;
    }

    /**
     * Cập nhật thông tin và thành phần của combo (FR-CMB-04). So thành phần theo serviceId: giữ
     * nguyên dòng cũ nếu cùng serviceId và chỉ cập nhật số lượng, bỏ dòng mất và thêm dòng mới. Nếu
     * không có thay đổi nào, không cập nhật updatedAt và không ghi audit log.
     */
    @Transactional
    public ComboResponse update(Long id, ComboRequest request) {
        Combo combo = findComboWithItems(id);
        ComboResponse before = toResponse(combo);

        String name = normalizeSpaces(request.name());
        ensureNameAvailable(name, id);
        checkDuplicateServiceItems(request.items());

        Map<Long, ServiceItem> serviceMap = findAndValidateActiveServices(request.items());
        Map<ServiceItem, Integer> itemMap = new LinkedHashMap<>();
        for (ComboItemRequest itemReq : request.items()) {
            itemMap.put(serviceMap.get(itemReq.serviceId()), itemReq.quantity());
        }

        long tourFee = request.tourFee() == null ? 0L : request.tourFee();
        Instant now = clock.instant();

        boolean detailsChanged = combo.updateDetails(name, request.price(), tourFee, now);
        boolean itemsChanged = combo.replaceItems(itemMap, now);

        if (detailsChanged || itemsChanged) {
            saveChecked(combo);
            ComboResponse after = toResponse(combo);
            auditService.record(AuditAction.UPDATE, AUDIT_ENTITY, combo.getId(), before, after);
            return after;
        }

        return before;
    }

    /**
     * Tính thử tổng giá lẻ và mức tiết kiệm (FR-CMB-03). Không lưu vào DB, không ghi audit. Cho
     * phép dịch vụ đã ngừng bán tham gia tính thử nếu dịch vụ đó tồn tại.
     */
    @Transactional(readOnly = true)
    public ComboQuoteResponse quote(ComboQuoteRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            Long savings = request.price() == null ? null : Math.subtractExact(0L, request.price());
            return new ComboQuoteResponse(Collections.emptyList(), 0L, savings);
        }

        checkDuplicateServiceItems(request.items());
        Map<Long, ServiceItem> serviceMap = findServices(request.items());
        List<ItemLine> lines =
                request.items().stream()
                        .map(req -> new ItemLine(serviceMap.get(req.serviceId()), req.quantity()))
                        .toList();

        LineCalculation calc = calculateLines(lines);
        Long savings =
                request.price() == null
                        ? null
                        : Math.subtractExact(calc.retailTotal(), request.price());
        return new ComboQuoteResponse(calc.items(), calc.retailTotal(), savings);
    }

    private LineCalculation calculateLines(List<ItemLine> lines) {
        List<ComboItemResponse> itemResponses = new ArrayList<>(lines.size());
        long retailTotal = 0L;

        for (ItemLine line : lines) {
            long lineTotal =
                    Math.multiplyExact(line.service().getListPrice(), (long) line.quantity());
            retailTotal = Math.addExact(retailTotal, lineTotal);
            itemResponses.add(mapper.toItemResponse(line.service(), line.quantity(), lineTotal));
        }

        return new LineCalculation(itemResponses, retailTotal);
    }

    private Combo findComboWithItems(Long id) {
        return comboRepository
                .findWithItemsById(id)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy combo."));
    }

    private void ensureNameAvailable(String name, long excludeId) {
        if (comboRepository.existsByNormalizedName(name, excludeId)) {
            throw new BusinessException(ErrorCode.COMBO_NAME_DUPLICATE);
        }
    }

    private void checkDuplicateServiceItems(List<ComboItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        long distinctCount = items.stream().map(ComboItemRequest::serviceId).distinct().count();
        if (distinctCount != items.size()) {
            throw new BusinessException(ErrorCode.COMBO_ITEM_DUPLICATE);
        }
    }

    private Map<Long, ServiceItem> findServices(List<ComboItemRequest> items) {
        List<Long> serviceIds = items.stream().map(ComboItemRequest::serviceId).toList();
        Map<Long, ServiceItem> serviceMap =
                serviceItemRepository.findAllById(serviceIds).stream()
                        .collect(Collectors.toMap(ServiceItem::getId, Function.identity()));

        if (serviceMap.size() != serviceIds.size()) {
            throw new BusinessException(
                    ErrorCode.COMBO_SERVICE_UNAVAILABLE,
                    "Dịch vụ không tồn tại hoặc đã ngừng bán.");
        }

        return serviceMap;
    }

    private Map<Long, ServiceItem> findAndValidateActiveServices(List<ComboItemRequest> items) {
        Map<Long, ServiceItem> serviceMap = findServices(items);

        List<String> inactiveNames =
                items.stream()
                        .map(item -> serviceMap.get(item.serviceId()))
                        .filter(service -> !service.isActive())
                        .map(ServiceItem::getName)
                        .toList();

        if (!inactiveNames.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.COMBO_SERVICE_UNAVAILABLE,
                    "Dịch vụ đã ngừng bán: " + String.join(", ", inactiveNames) + ".");
        }

        return serviceMap;
    }

    private ComboResponse toResponse(Combo combo) {
        List<ItemLine> lines =
                combo.getItems().stream()
                        .map(item -> new ItemLine(item.getService(), item.getQuantity()))
                        .toList();
        LineCalculation calc = calculateLines(lines);
        long savings = Math.subtractExact(calc.retailTotal(), combo.getPrice());
        return mapper.toResponse(combo, calc.items(), calc.retailTotal(), savings);
    }

    private void saveChecked(Combo combo) {
        try {
            comboRepository.saveAndFlush(combo);
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains("ux_combo_name")) {
                throw new BusinessException(ErrorCode.COMBO_NAME_DUPLICATE);
            }
            throw e;
        }
    }

    private static String normalizeSpaces(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}
