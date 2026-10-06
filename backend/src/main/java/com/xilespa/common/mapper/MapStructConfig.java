package com.xilespa.common.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * Cấu hình chung cho mọi mapper MapStruct: {@code @Mapper(config = MapStructConfig.class)}. Quên
 * map một trường của đối tượng đích thì build báo lỗi. Mapper chỉ chép dữ liệu, không chứa nghiệp
 * vụ (xem {@code backend/AGENTS.md}).
 */
@MapperConfig(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MapStructConfig {}
