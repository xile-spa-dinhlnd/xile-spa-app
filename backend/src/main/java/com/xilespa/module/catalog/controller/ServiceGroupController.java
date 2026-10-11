package com.xilespa.module.catalog.controller;

import com.xilespa.module.catalog.dto.response.ServiceGroupResponse;
import com.xilespa.module.catalog.service.ServiceItemService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Nhóm dịch vụ đang dùng, cho ô chọn ở giao diện. Thêm, sửa nhóm: FR-SVC-05 (Sprint 2). */
@RestController
@RequestMapping("/api/catalog/service-groups")
@RequiredArgsConstructor
public class ServiceGroupController {

    private final ServiceItemService serviceItemService;

    @GetMapping
    public List<ServiceGroupResponse> list() {
        return serviceItemService.listGroups();
    }
}
