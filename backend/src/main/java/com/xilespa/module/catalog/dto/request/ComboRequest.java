package com.xilespa.module.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ComboRequest(
        @NotBlank(message = "Tên combo không được để trống")
                @Size(max = 150, message = "Tên combo tối đa 150 ký tự")
                String name,
        @NotNull(message = "Vui lòng nhập giá combo")
                @PositiveOrZero(message = "Giá combo không được âm")
                @Max(value = 1_000_000_000L, message = "Giá combo tối đa 1.000.000.000 đ")
                Long price,
        @PositiveOrZero(message = "Tiền tour không được âm")
                @Max(value = 1_000_000_000L, message = "Tiền tour tối đa 1.000.000.000 đ")
                Long tourFee,
        @NotEmpty(message = "Combo phải có ít nhất một dịch vụ")
                @Size(max = 20, message = "Combo tối đa 20 dịch vụ")
                @Valid
                List<ComboItemRequest> items) {}
