package com.xilespa.module.catalog.repository;

import com.xilespa.module.catalog.entity.ServiceGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceGroupRepository extends JpaRepository<ServiceGroup, Long> {

    List<ServiceGroup> findByActiveTrueOrderBySortOrderAscNameAsc();
}
