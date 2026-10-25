package com.xilespa.module.catalog.dto.response;

import java.time.Instant;
import java.util.List;

public record ComboResponse(
        Long id,
        String name,
        long price,
        long tourFee,
        boolean active,
        List<ComboItemResponse> items,
        long retailTotal,
        long savings,
        Instant createdAt,
        Instant updatedAt) {}
