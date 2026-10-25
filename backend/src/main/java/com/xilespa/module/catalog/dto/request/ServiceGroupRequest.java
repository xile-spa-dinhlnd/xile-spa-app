package com.xilespa.module.catalog.dto.request;

import com.xilespa.common.validation.VisibleText;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Thân request tạo và sửa nhóm dịch vụ (FR-SVC-05). Dùng chung cho POST và PUT; PUT là thay thế
 * toàn bộ.
 */
public record ServiceGroupRequest(
        @VisibleText(message = "Tên nhóm dịch vụ không được để trống")
                @Size(max = 100, message = "Tên nhóm dịch vụ tối đa 100 ký tự")
                String name,
        @NotNull(message = "Thứ tự hiển thị không được để trống")
                @Min(value = 0, message = "Thứ tự hiển thị không được âm")
                Integer sortOrder) {}
