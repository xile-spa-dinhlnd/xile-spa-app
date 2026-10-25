package com.xilespa.module.catalog.controller;

import com.xilespa.module.catalog.dto.request.ComboQuoteRequest;
import com.xilespa.module.catalog.dto.request.ComboRequest;
import com.xilespa.module.catalog.dto.response.ComboQuoteResponse;
import com.xilespa.module.catalog.dto.response.ComboResponse;
import com.xilespa.module.catalog.service.ComboService;
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
import org.springframework.web.bind.annotation.RestController;

/** API quản lý combo dịch vụ (FR-CMB-01 đến 05). Không có DELETE: combo chỉ ngừng bán (BR-19). */
@RestController
@RequestMapping("/api/catalog/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    @GetMapping("/{id}")
    public ComboResponse get(@PathVariable Long id) {
        return comboService.get(id);
    }

    @PostMapping
    public ResponseEntity<ComboResponse> create(@Valid @RequestBody ComboRequest request) {
        ComboResponse created = comboService.create(request);
        return ResponseEntity.created(URI.create("/api/catalog/combos/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ComboResponse update(@PathVariable Long id, @Valid @RequestBody ComboRequest request) {
        return comboService.update(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public ComboResponse deactivate(@PathVariable Long id) {
        return comboService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    public ComboResponse activate(@PathVariable Long id) {
        return comboService.activate(id);
    }

    @PostMapping("/quote")
    public ComboQuoteResponse quote(@Valid @RequestBody ComboQuoteRequest request) {
        return comboService.quote(request);
    }
}
