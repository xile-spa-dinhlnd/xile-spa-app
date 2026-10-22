package com.xilespa.module.staff.dto.request;

import com.xilespa.module.staff.enums.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dữ liệu tạo hoặc sửa một nhân viên (FR-STF-01). Dùng chung cho POST và PUT. */
public record StaffRequest(
        @NotBlank(message = "Tên nhân viên không được để trống")
                @Size(max = 100, message = "Tên nhân viên tối đa 100 ký tự")
                String fullName,
        @NotNull(message = "Vui lòng chọn vai trò") StaffRole role) {}
