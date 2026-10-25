package com.xilespa.module.staff.devdata;

import com.xilespa.module.staff.dto.request.StaffRequest;
import com.xilespa.module.staff.dto.response.StaffResponse;
import com.xilespa.module.staff.enums.StaffRole;
import com.xilespa.module.staff.service.StaffService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Nhân viên mẫu cho máy dev (backend/AGENTS.md mục "Dữ liệu mẫu cho máy dev"). Chỉ chạy khi {@code
 * xile.dev-data.enabled=true}. Đi qua {@link StaffService} để có kiểm tra và audit như dữ liệu
 * thật; nhân viên đã có cùng tên thì bỏ qua, nên chạy lại chỉ thêm phần còn thiếu. Tên là giả.
 */
@Component
@ConditionalOnProperty(name = "xile.dev-data.enabled", havingValue = "true")
@Order(10)
@RequiredArgsConstructor
public class StaffDevData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaffDevData.class);
    private static final int LOOKUP_SIZE = 100;

    /** {@code active = false}: người đã nghỉ, để thử bộ lọc và ô chọn người làm. */
    private record SampleStaff(String fullName, StaffRole role, boolean active) {}

    private static final List<SampleStaff> SAMPLES =
            List.of(
                    new SampleStaff("Chủ tiệm Xile", StaffRole.OWNER, true),
                    new SampleStaff("Nguyễn Thị Lan", StaffRole.THERAPIST, true),
                    new SampleStaff("Trần Thị Mai", StaffRole.THERAPIST, true),
                    new SampleStaff("Lê Văn Hùng", StaffRole.THERAPIST, true),
                    new SampleStaff("Phạm Thị Hoa", StaffRole.THERAPIST, false));

    private final StaffService staffService;

    @Override
    public void run(ApplicationArguments args) {
        int created = 0;
        for (SampleStaff sample : SAMPLES) {
            if (exists(sample.fullName())) {
                continue;
            }
            StaffResponse staff =
                    staffService.create(new StaffRequest(sample.fullName(), sample.role()));
            if (!sample.active()) {
                staffService.deactivate(staff.id());
            }
            created++;
        }
        log.info(
                "Dữ liệu mẫu nhân viên: thêm {} người, {} người đã có.",
                created,
                SAMPLES.size() - created);
    }

    private boolean exists(String fullName) {
        return staffService.search(fullName, null, null, 0, LOOKUP_SIZE).content().stream()
                .anyMatch(staff -> staff.fullName().equals(fullName));
    }
}
