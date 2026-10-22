package com.xilespa.module.staff.controller;

import com.xilespa.common.response.PageResponse;
import com.xilespa.module.staff.dto.request.StaffRequest;
import com.xilespa.module.staff.dto.response.StaffResponse;
import com.xilespa.module.staff.enums.StaffRole;
import com.xilespa.module.staff.service.StaffService;
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

/** Nhân viên (FR-STF-01). Không có DELETE: nhân viên nghỉ thì chỉ đánh dấu đã nghỉ. */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public PageResponse<StaffResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StaffRole role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return staffService.search(q, role, active, page, size);
    }

    @GetMapping("/{id}")
    public StaffResponse get(@PathVariable Long id) {
        return staffService.get(id);
    }

    @PostMapping
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody StaffRequest request) {
        StaffResponse created = staffService.create(request);
        return ResponseEntity.created(URI.create("/api/staff/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public StaffResponse update(@PathVariable Long id, @Valid @RequestBody StaffRequest request) {
        return staffService.update(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public StaffResponse deactivate(@PathVariable Long id) {
        return staffService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    public StaffResponse activate(@PathVariable Long id) {
        return staffService.activate(id);
    }
}
