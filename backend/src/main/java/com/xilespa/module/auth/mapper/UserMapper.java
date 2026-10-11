package com.xilespa.module.auth.mapper;

import com.xilespa.common.mapper.MapStructConfig;
import com.xilespa.module.auth.dto.response.UserResponse;
import com.xilespa.module.auth.entity.AppUser;
import org.mapstruct.Mapper;

/** Mapper chuyển đổi AppUser entity sang UserResponse DTO. */
@Mapper(config = MapStructConfig.class)
public interface UserMapper {

    UserResponse toResponse(AppUser user);
}
