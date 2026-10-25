package com.xilespa.module.catalog.mapper;

import com.xilespa.common.mapper.MapStructConfig;
import com.xilespa.module.catalog.dto.response.ComboItemResponse;
import com.xilespa.module.catalog.dto.response.ComboResponse;
import com.xilespa.module.catalog.dto.response.ServiceGroupResponse;
import com.xilespa.module.catalog.dto.response.ServiceItemResponse;
import com.xilespa.module.catalog.entity.Combo;
import com.xilespa.module.catalog.entity.ServiceGroup;
import com.xilespa.module.catalog.entity.ServiceItem;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface CatalogMapper {

    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "groupName", source = "group.name")
    ServiceItemResponse toResponse(ServiceItem service);

    ServiceGroupResponse toResponse(ServiceGroup group);

    @Mapping(target = "serviceId", source = "service.id")
    @Mapping(target = "serviceName", source = "service.name")
    @Mapping(target = "serviceActive", source = "service.active")
    @Mapping(target = "listPrice", source = "service.listPrice")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "lineTotal", source = "lineTotal")
    ComboItemResponse toItemResponse(ServiceItem service, int quantity, long lineTotal);

    @Mapping(target = "id", source = "combo.id")
    @Mapping(target = "name", source = "combo.name")
    @Mapping(target = "price", source = "combo.price")
    @Mapping(target = "tourFee", source = "combo.tourFee")
    @Mapping(target = "active", source = "combo.active")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "retailTotal", source = "retailTotal")
    @Mapping(target = "savings", source = "savings")
    @Mapping(target = "createdAt", source = "combo.createdAt")
    @Mapping(target = "updatedAt", source = "combo.updatedAt")
    ComboResponse toResponse(
            Combo combo, List<ComboItemResponse> items, long retailTotal, long savings);
}
