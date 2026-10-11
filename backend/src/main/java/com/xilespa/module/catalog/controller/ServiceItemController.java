package com.xilespa.module.catalog.controller;

import com.xilespa.common.response.PageResponse;
import com.xilespa.module.catalog.dto.request.ServiceItemRequest;
import com.xilespa.module.catalog.dto.response.ServiceItemResponse;
import com.xilespa.module.catalog.service.ServiceItemService;
import jakarta.validation.Valid;
import java.net.URI;
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

/** Bảng giá dịch vụ (FR-SVC-01 đến 04). Không có DELETE: dịch vụ chỉ ngừng bán (BR-19). */
@RestController
@RequestMapping("/api/catalog/services")
@RequiredArgsConstructor
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    @GetMapping
    public PageResponse<ServiceItemResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return serviceItemService.search(q, groupId, active, page, size);
    }

    @GetMapping("/{id}")
    public ServiceItemResponse get(@PathVariable Long id) {
        return serviceItemService.get(id);
    }

    @PostMapping
    public ResponseEntity<ServiceItemResponse> create(
            @Valid @RequestBody ServiceItemRequest request) {
        ServiceItemResponse created = serviceItemService.create(request);
        return ResponseEntity.created(URI.create("/api/catalog/services/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ServiceItemResponse update(
            @PathVariable Long id, @Valid @RequestBody ServiceItemRequest request) {
        return serviceItemService.update(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public ServiceItemResponse deactivate(@PathVariable Long id) {
        return serviceItemService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    public ServiceItemResponse activate(@PathVariable Long id) {
        return serviceItemService.activate(id);
    }
}
