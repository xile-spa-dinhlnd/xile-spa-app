package com.xilespa.module.catalog.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ComboItemRequest(
        @NotNull(message = "Vui lòng chọn dịch vụ") Long serviceId,
        @NotNull(message = "Vui lòng nhập số lượng")
                @Min(value = 1, message = "Số lượng tối thiểu là 1")
                @Max(value = 99, message = "Số lượng tối đa là 99")
                Integer quantity) {}
