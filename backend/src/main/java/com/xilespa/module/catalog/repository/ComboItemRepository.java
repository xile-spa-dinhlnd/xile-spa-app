package com.xilespa.module.catalog.repository;

import com.xilespa.module.catalog.entity.ComboItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComboItemRepository extends JpaRepository<ComboItem, Long> {

    /** Tìm danh sách tên các combo đang bán có chứa dịch vụ này, xếp theo tên combo (OQ-22). */
    @Query(
            """
            SELECT c.name FROM ComboItem ci
            JOIN ci.combo c
            WHERE ci.service.id = :serviceId AND c.active = true
            ORDER BY c.name ASC
            """)
    List<String> findActiveComboNamesByServiceId(@Param("serviceId") Long serviceId);
}
