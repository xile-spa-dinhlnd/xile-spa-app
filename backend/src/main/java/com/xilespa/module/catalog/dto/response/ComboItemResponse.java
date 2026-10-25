package com.xilespa.module.catalog.dto.response;

public record ComboItemResponse(
        Long serviceId,
        String serviceName,
        boolean serviceActive,
        long listPrice,
        int quantity,
        long lineTotal) {}
