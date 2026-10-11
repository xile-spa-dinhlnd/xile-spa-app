package com.xilespa.module.catalog.repository;

import com.xilespa.module.catalog.entity.ServicePriceHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicePriceHistoryRepository extends JpaRepository<ServicePriceHistory, Long> {

    List<ServicePriceHistory> findByServiceIdOrderByChangedAtDescIdDesc(Long serviceId);
}
