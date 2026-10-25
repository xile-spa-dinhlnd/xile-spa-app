package com.xilespa.module.catalog.repository;

import com.xilespa.module.catalog.entity.Combo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComboRepository extends JpaRepository<Combo, Long> {

    /**
     * Tìm theo tên và trạng thái; {@code active} null là bỏ lọc (FR-CMB-01).
     *
     * @param namePattern mẫu LIKE đã viết thường, ví dụ {@code %gội%}; {@code %} là lấy tất cả.
     */
    @Query(
            """
            SELECT c FROM Combo c
            WHERE LOWER(c.name) LIKE :namePattern ESCAPE '\\'
              AND (:active IS NULL OR c.active = :active)
            """)
    Page<Combo> search(
            @Param("namePattern") String namePattern,
            @Param("active") Boolean active,
            Pageable pageable);

    /** Lấy danh sách combo theo các id kèm theo danh sách thành phần và dịch vụ tương ứng. */
    @Query(
            """
            SELECT DISTINCT c FROM Combo c
            LEFT JOIN FETCH c.items i
            LEFT JOIN FETCH i.service s
            WHERE c.id IN :ids
            """)
    List<Combo> findAllWithItemsByIdIn(@Param("ids") Collection<Long> ids);

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
