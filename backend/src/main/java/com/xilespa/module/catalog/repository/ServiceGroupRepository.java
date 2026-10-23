package com.xilespa.module.catalog.repository;

import com.xilespa.module.catalog.entity.ServiceGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceGroupRepository extends JpaRepository<ServiceGroup, Long> {

    List<ServiceGroup> findAllByOrderBySortOrderAscNameAsc();

    List<ServiceGroup> findByActiveOrderBySortOrderAscNameAsc(boolean active);

    /**
     * Kiểm tra có nhóm khác cùng tên chưa, so sánh bằng hàm {@code norm_name} của CSDL (bỏ khoảng
     * trắng thừa, không phân biệt hoa thường), đúng như unique index {@code ux_service_group_name}.
     */
    @Query(
            value =
                    "SELECT EXISTS (SELECT 1 FROM service_group WHERE norm_name(name) = norm_name(:name)"
                            + " AND id <> :excludeId)",
            nativeQuery = true)
    boolean existsByNormalizedName(@Param("name") String name, @Param("excludeId") long excludeId);
}
