package com.xilespa.module.catalog.controller;

import com.xilespa.module.catalog.dto.request.ServiceGroupRequest;
import com.xilespa.module.catalog.dto.response.ServiceGroupResponse;
import com.xilespa.module.catalog.service.ServiceGroupService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nhóm dịch vụ (FR-SVC-05). Không có DELETE: nhóm không còn dùng thì chỉ đánh dấu ngừng dùng
 * (BR-19).
 */
@RestController
@RequestMapping("/api/catalog/service-groups")
@RequiredArgsConstructor
public class ServiceGroupController {

    private final ServiceGroupService serviceGroupService;

    @GetMapping
    public List<ServiceGroupResponse> list(@RequestParam(required = false) Boolean active) {
        return serviceGroupService.list(active);
    }

    @GetMapping("/{id}")
    public ServiceGroupResponse get(@PathVariable Long id) {
        return serviceGroupService.get(id);
    }

    @PostMapping
    public ResponseEntity<ServiceGroupResponse> create(
            @Valid @RequestBody ServiceGroupRequest request) {
        ServiceGroupResponse created = serviceGroupService.create(request);
        return ResponseEntity.created(URI.create("/api/catalog/service-groups/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ServiceGroupResponse update(
            @PathVariable Long id, @Valid @RequestBody ServiceGroupRequest request) {
        return serviceGroupService.update(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public ServiceGroupResponse deactivate(@PathVariable Long id) {
        return serviceGroupService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    public ServiceGroupResponse activate(@PathVariable Long id) {
        return serviceGroupService.activate(id);
    }
}
