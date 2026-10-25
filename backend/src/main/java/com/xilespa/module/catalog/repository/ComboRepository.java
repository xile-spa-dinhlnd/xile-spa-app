package com.xilespa.module.catalog.repository;

import com.xilespa.module.catalog.entity.Combo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComboRepository extends JpaRepository<Combo, Long> {

    /**
     * Có combo khác cùng tên chưa, so sánh bằng hàm {@code norm_name} của CSDL (bỏ khoảng trắng
     * thừa, không phân biệt hoa thường), đúng như unique index {@code ux_combo_name}.
     */
    @Query(
            value =
                    "SELECT EXISTS (SELECT 1 FROM combo WHERE norm_name(name) = norm_name(:name)"
                            + " AND id <> :excludeId)",
            nativeQuery = true)
    boolean existsByNormalizedName(@Param("name") String name, @Param("excludeId") long excludeId);

    /** Tìm combo kèm theo danh sách thành phần và dịch vụ tương ứng. */
    @Query(
            """
            SELECT c FROM Combo c
            LEFT JOIN FETCH c.items i
            LEFT JOIN FETCH i.service s
            WHERE c.id = :id
            """)
    Optional<Combo> findWithItemsById(@Param("id") Long id);
}
