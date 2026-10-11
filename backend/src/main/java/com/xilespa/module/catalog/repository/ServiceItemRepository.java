package com.xilespa.module.catalog.repository;

import com.xilespa.module.catalog.entity.ServiceItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {

    /**
     * Tìm theo tên (không phân biệt hoa thường), lọc nhóm và trạng thái; tham số null là bỏ lọc.
     */
    @EntityGraph(attributePaths = "group")
    @Query(
            """
            SELECT s FROM ServiceItem s
            WHERE (:q IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:groupId IS NULL OR s.group.id = :groupId)
              AND (:active IS NULL OR s.active = :active)
            """)
    Page<ServiceItem> search(
            @Param("q") String q,
            @Param("groupId") Long groupId,
            @Param("active") Boolean active,
            Pageable pageable);

    /**
     * Có dịch vụ khác cùng tên chưa, so sánh bằng hàm {@code norm_name} của CSDL (bỏ khoảng trắng
     * thừa, không phân biệt hoa thường), đúng như unique index {@code ux_service_name}.
     */
    @Query(
            value =
                    "SELECT EXISTS (SELECT 1 FROM service WHERE norm_name(name) = norm_name(:name)"
                            + " AND id <> :excludeId)",
            nativeQuery = true)
    boolean existsByNormalizedName(@Param("name") String name, @Param("excludeId") long excludeId);
}
