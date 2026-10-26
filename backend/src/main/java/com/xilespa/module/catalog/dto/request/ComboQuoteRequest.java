package com.xilespa.module.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ComboQuoteRequest(
        @PositiveOrZero(message = "Giá combo không được âm")
                @Max(value = 1_000_000_000L, message = "Giá combo tối đa 1.000.000.000 đ")
                Long price,
        @Size(max = 20, message = "Combo tối đa 20 dịch vụ")
                List<@NotNull(message = "Dòng dịch vụ không được để trống") @Valid ComboItemRequest>
                        items) {}
