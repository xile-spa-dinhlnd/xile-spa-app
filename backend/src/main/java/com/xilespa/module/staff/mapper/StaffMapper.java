package com.xilespa.module.staff.mapper;

import com.xilespa.common.mapper.MapStructConfig;
import com.xilespa.module.staff.dto.response.StaffResponse;
import com.xilespa.module.staff.entity.Staff;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface StaffMapper {

    StaffResponse toResponse(Staff staff);
}
