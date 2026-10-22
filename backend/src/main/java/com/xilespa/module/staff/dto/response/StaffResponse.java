package com.xilespa.module.staff.dto.response;

import com.xilespa.module.staff.enums.StaffRole;
import java.time.Instant;

/** Một nhân viên. {@code active = false} nghĩa là đã nghỉ. */
public record StaffResponse(
        Long id,
        String fullName,
        StaffRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
