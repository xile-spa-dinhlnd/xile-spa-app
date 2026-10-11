package com.xilespa.module.catalog.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu tạo hoặc sửa một dịch vụ (FR-SVC-02, FR-SVC-03). Dùng chung cho POST và PUT vì hai thao
 * tác nhận cùng các trường. Tiền là số nguyên đồng (BR-01).
 *
 * @param durationMinutes thời lượng (phút), có thể để trống
 * @param tourFee tiền tour mặc định; để trống thì là 0
 */
public record ServiceItemRequest(
        @NotBlank(message = "Tên dịch vụ không được để trống")
                @Size(max = 150, message = "Tên dịch vụ tối đa 150 ký tự")
                String name,
        @NotNull(message = "Vui lòng chọn nhóm dịch vụ") Long groupId,
        @Positive(message = "Thời lượng phải lớn hơn 0 phút")
                @Max(value = 1440, message = "Thời lượng tối đa 1440 phút")
                Integer durationMinutes,
        @NotNull(message = "Vui lòng nhập giá niêm yết")
                @PositiveOrZero(message = "Giá niêm yết không được âm")
                @Max(value = 1_000_000_000L, message = "Giá niêm yết tối đa 1.000.000.000 đ")
                Long listPrice,
        @PositiveOrZero(message = "Tiền tour không được âm")
                @Max(value = 1_000_000_000L, message = "Tiền tour tối đa 1.000.000.000 đ")
                Long tourFee) {}
