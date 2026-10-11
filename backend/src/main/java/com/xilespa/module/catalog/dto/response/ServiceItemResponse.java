package com.xilespa.module.catalog.dto.response;

import java.time.Instant;

/** Một dịch vụ trong bảng giá. Tiền là số nguyên đồng; giao diện chỉ định dạng, không tính. */
public record ServiceItemResponse(
        Long id,
        String name,
        Long groupId,
        String groupName,
        Integer durationMinutes,
        long listPrice,
        long tourFee,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
