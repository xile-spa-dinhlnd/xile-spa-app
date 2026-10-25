package com.xilespa.module.catalog.devdata;

import com.xilespa.module.catalog.dto.request.ComboItemRequest;
import com.xilespa.module.catalog.dto.request.ComboRequest;
import com.xilespa.module.catalog.dto.request.ServiceItemRequest;
import com.xilespa.module.catalog.dto.response.ServiceGroupResponse;
import com.xilespa.module.catalog.dto.response.ServiceItemResponse;
import com.xilespa.module.catalog.service.ComboService;
import com.xilespa.module.catalog.service.ServiceGroupService;
import com.xilespa.module.catalog.service.ServiceItemService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Bảng giá và combo mẫu cho máy dev (backend/AGENTS.md mục "Dữ liệu mẫu cho máy dev"). Chỉ chạy khi
 * {@code xile.dev-data.enabled=true}. Đi qua service của module để có kiểm tra, quy tắc OQ-21,
 * OQ-22 và audit như dữ liệu thật; bản ghi đã có cùng tên thì bỏ qua. Dịch vụ gắn vào bốn nhóm của
 * V2 theo tên; nhóm không còn (bị đổi tên, ngừng dùng) thì bỏ qua dịch vụ của nhóm đó.
 */
@Component
@ConditionalOnProperty(name = "xile.dev-data.enabled", havingValue = "true")
@Order(20)
@RequiredArgsConstructor
public class CatalogDevData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogDevData.class);
    private static final int LOOKUP_SIZE = 100;

    /** {@code active = false}: dịch vụ ngừng bán, để thử bộ lọc và chặn chọn trong combo. */
    private record SampleService(
            String group,
            String name,
            int durationMinutes,
            long listPrice,
            long tourFee,
            boolean active) {}

    private record SampleCombo(
            String name, long price, long tourFee, List<Map.Entry<String, Integer>> items) {}

    // Có một dịch vụ giá 0 (BR-01 cho phép, chưa có giá) và một dịch vụ ngừng bán.
    private static final List<SampleService> SERVICES =
            List.of(
                    new SampleService(
                            "Gội đầu dưỡng sinh",
                            "Gội đầu dưỡng sinh cơ bản",
                            45,
                            120_000L,
                            20_000L,
                            true),
                    new SampleService(
                            "Gội đầu dưỡng sinh", "Gội đầu thảo dược", 60, 180_000L, 30_000L, true),
                    new SampleService(
                            "Gội đầu dưỡng sinh",
                            "Gội đầu kèm massage cổ vai gáy",
                            75,
                            250_000L,
                            40_000L,
                            true),
                    new SampleService(
                            "Chăm sóc da", "Chăm sóc da mặt cơ bản", 60, 300_000L, 50_000L, true),
                    new SampleService(
                            "Chăm sóc da", "Đắp mặt nạ collagen", 30, 150_000L, 20_000L, true),
                    new SampleService("Chăm sóc da", "Tư vấn da", 15, 0L, 0L, true),
                    new SampleService(
                            "Stretching/Rehab", "Giãn cơ toàn thân", 60, 350_000L, 60_000L, true),
                    new SampleService(
                            "Stretching/Rehab", "Trị liệu cổ vai gáy", 45, 280_000L, 50_000L, true),
                    new SampleService(
                            "Stretching/Rehab", "Giãn cơ chân", 30, 200_000L, 30_000L, true),
                    new SampleService("Khác", "Xông hơi thảo dược", 30, 100_000L, 10_000L, true),
                    new SampleService(
                            "Khác", "Ngâm chân muối khoáng", 30, 90_000L, 10_000L, false));

    // Một combo rẻ hơn giá lẻ, một combo có số lượng 2, một combo đắt hơn giá lẻ (FR-CMB-03 cảnh
    // báo).
    private static final List<SampleCombo> COMBOS =
            List.of(
                    new SampleCombo(
                            "Combo Thư Giãn",
                            400_000L,
                            60_000L,
                            List.of(
                                    Map.entry("Gội đầu thảo dược", 1),
                                    Map.entry("Giãn cơ chân", 1),
                                    Map.entry("Xông hơi thảo dược", 1))),
                    new SampleCombo(
                            "Combo Giãn Cơ 2 Buổi",
                            600_000L,
                            100_000L,
                            List.of(Map.entry("Giãn cơ toàn thân", 2))),
                    new SampleCombo(
                            "Combo Da Sáng",
                            500_000L,
                            70_000L,
                            List.of(
                                    Map.entry("Chăm sóc da mặt cơ bản", 1),
                                    Map.entry("Đắp mặt nạ collagen", 1))));

    private final ServiceGroupService groupService;
    private final ServiceItemService serviceItemService;
    private final ComboService comboService;

    @Override
    public void run(ApplicationArguments args) {
        Map<String, Long> groupIdByName =
                groupService.list(true).stream()
                        .collect(
                                Collectors.toMap(
                                        ServiceGroupResponse::name, ServiceGroupResponse::id));
        Map<String, Long> serviceIdByName = new HashMap<>();
        int createdServices = seedServices(groupIdByName, serviceIdByName);
        int createdCombos = seedCombos(serviceIdByName);
        log.info(
                "Dữ liệu mẫu bảng giá: thêm {} dịch vụ, {} combo.", createdServices, createdCombos);
    }

    /** Ghi id của mọi dịch vụ mẫu (mới tạo hay đã có) vào {@code serviceIdByName} để ghép combo. */
    private int seedServices(Map<String, Long> groupIdByName, Map<String, Long> serviceIdByName) {
        int created = 0;
        for (SampleService sample : SERVICES) {
            Optional<ServiceItemResponse> existing = findService(sample.name());
            if (existing.isPresent()) {
                serviceIdByName.put(sample.name(), existing.get().id());
                continue;
            }
            Long groupId = groupIdByName.get(sample.group());
            if (groupId == null) {
                log.warn(
                        "Bỏ qua dịch vụ mẫu '{}': không có nhóm đang dùng '{}'.",
                        sample.name(),
                        sample.group());
                continue;
            }
            ServiceItemResponse service =
                    serviceItemService.create(
                            new ServiceItemRequest(
                                    sample.name(),
                                    groupId,
                                    sample.durationMinutes(),
                                    sample.listPrice(),
                                    sample.tourFee()));
            if (!sample.active()) {
                serviceItemService.deactivate(service.id());
            }
            serviceIdByName.put(sample.name(), service.id());
            created++;
        }
        return created;
    }

    private int seedCombos(Map<String, Long> serviceIdByName) {
        int created = 0;
        for (SampleCombo sample : COMBOS) {
            if (comboExists(sample.name())) {
                continue;
            }
            if (!sample.items().stream()
                    .allMatch(item -> serviceIdByName.containsKey(item.getKey()))) {
                log.warn("Bỏ qua combo mẫu '{}': thiếu dịch vụ thành phần.", sample.name());
                continue;
            }
            List<ComboItemRequest> items =
                    sample.items().stream()
                            .map(
                                    item ->
                                            new ComboItemRequest(
                                                    serviceIdByName.get(item.getKey()),
                                                    item.getValue()))
                            .toList();
            comboService.create(
                    new ComboRequest(sample.name(), sample.price(), sample.tourFee(), items));
            created++;
        }
        return created;
    }

    private Optional<ServiceItemResponse> findService(String name) {
        return serviceItemService.search(name, null, null, 0, LOOKUP_SIZE).content().stream()
                .filter(service -> service.name().equals(name))
                .findFirst();
    }

    private boolean comboExists(String name) {
        return comboService.search(name, null, 0, LOOKUP_SIZE).content().stream()
                .anyMatch(combo -> combo.name().equals(name));
    }
}
