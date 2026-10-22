package com.xilespa.module.staff.service;

import com.xilespa.common.audit.AuditAction;
import com.xilespa.common.audit.AuditService;
import com.xilespa.common.exception.BusinessException;
import com.xilespa.common.exception.ErrorCode;
import com.xilespa.common.response.PageResponse;
import com.xilespa.module.staff.dto.request.StaffRequest;
import com.xilespa.module.staff.dto.response.StaffResponse;
import com.xilespa.module.staff.entity.Staff;
import com.xilespa.module.staff.enums.StaffRole;
import com.xilespa.module.staff.mapper.StaffMapper;
import com.xilespa.module.staff.repository.StaffRepository;
import java.time.Clock;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý nhân viên (FR-STF-01). Mọi thay đổi ghi nhật ký trong cùng transaction (BR-18). Không có
 * xóa cứng: nhân viên nghỉ thì chỉ đánh dấu đã nghỉ.
 *
 * <p>Hai nhân viên đang làm không được trùng tên (so sánh không phân biệt hoa thường và khoảng
 * trắng thừa), để khi nhập giao dịch không chọn nhầm người.
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    static final String AUDIT_ENTITY = "staff";
    private static final int MAX_PAGE_SIZE = 100;

    private final StaffRepository staffRepository;
    private final StaffMapper mapper;
    private final AuditService auditService;
    private final Clock clock;

    /** Danh sách nhân viên: tìm theo tên, lọc vai trò và trạng thái; người đang làm lên trước. */
    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> search(
            String q, StaffRole role, Boolean active, int page, int size) {
        PageRequest pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.clamp(size, 1, MAX_PAGE_SIZE),
                        Sort.by(
                                Sort.Order.desc("active"),
                                Sort.Order.asc("fullName"),
                                Sort.Order.asc("id")));
        return PageResponse.from(
                staffRepository.search(likePattern(q), role, active, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StaffResponse get(Long id) {
        return mapper.toResponse(findStaff(id));
    }

    @Transactional
    public StaffResponse create(StaffRequest request) {
        String fullName = normalizeSpaces(request.fullName());
        ensureNameAvailable(fullName, 0L);
        Staff staff = new Staff(fullName, request.role(), clock.instant());
        staffRepository.save(staff);

        StaffResponse created = mapper.toResponse(staff);
        auditService.record(AuditAction.CREATE, AUDIT_ENTITY, staff.getId(), null, created);
        return created;
    }

    @Transactional
    public StaffResponse update(Long id, StaffRequest request) {
        Staff staff = findStaff(id);
        StaffResponse before = mapper.toResponse(staff);

        String fullName = normalizeSpaces(request.fullName());
        if (staff.isActive()) {
            ensureNameAvailable(fullName, id);
        }
        staff.updateDetails(fullName, request.role(), clock.instant());

        StaffResponse after = mapper.toResponse(staff);
        if (!before.equals(after)) {
            auditService.record(AuditAction.UPDATE, AUDIT_ENTITY, id, before, after);
        }
        return after;
    }

    /** Cho nghỉ việc. Gọi lại khi đã nghỉ thì không đổi gì. */
    @Transactional
    public StaffResponse deactivate(Long id) {
        Staff staff = findStaff(id);
        StaffResponse before = mapper.toResponse(staff);
        if (staff.deactivate(clock.instant())) {
            StaffResponse after = mapper.toResponse(staff);
            auditService.record(AuditAction.DEACTIVATE, AUDIT_ENTITY, id, before, after);
            return after;
        }
        return before;
    }

    /**
     * Cho làm lại. Gọi lại khi đang làm thì không đổi gì. Không cho làm lại nếu đã có người đang
     * làm trùng tên.
     */
    @Transactional
    public StaffResponse activate(Long id) {
        Staff staff = findStaff(id);
        StaffResponse before = mapper.toResponse(staff);
        if (staff.isActive()) {
            return before;
        }
        ensureNameAvailable(staff.getFullName(), id);
        staff.activate(clock.instant());
        StaffResponse after = mapper.toResponse(staff);
        auditService.record(AuditAction.ACTIVATE, AUDIT_ENTITY, id, before, after);
        return after;
    }

    private Staff findStaff(Long id) {
        return staffRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.NOT_FOUND, "Không tìm thấy nhân viên."));
    }

    private void ensureNameAvailable(String fullName, long excludeId) {
        if (staffRepository.existsActiveByNormalizedName(fullName, excludeId)) {
            throw new BusinessException(ErrorCode.STAFF_NAME_DUPLICATE);
        }
    }

    /** Mẫu LIKE viết thường, thoát ký tự đặc biệt % _ \\ của người dùng; rỗng thì lấy tất cả. */
    static String likePattern(String q) {
        if (q == null || q.isBlank()) {
            return "%";
        }
        String escaped =
                q.trim()
                        .toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /** Cắt đầu cuối và gộp khoảng trắng liên tiếp, khớp với cách CSDL so sánh tên. */
    private static String normalizeSpaces(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}
