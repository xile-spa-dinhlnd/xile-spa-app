package com.xilespa.module.staff.repository;

import com.xilespa.module.staff.entity.Staff;
import com.xilespa.module.staff.enums.StaffRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    /**
     * Tìm theo tên, lọc vai trò và trạng thái; {@code role}, {@code active} null là bỏ lọc.
     *
     * @param namePattern mẫu LIKE đã viết thường, ví dụ {@code %lan%}; {@code %} là lấy tất cả.
     *     Không truyền null: PostgreSQL không đoán được kiểu tham số chuỗi null (lỗi lower(bytea)).
     */
    @Query(
            """
            SELECT s FROM Staff s
            WHERE LOWER(s.fullName) LIKE :namePattern ESCAPE '\\'
              AND (:role IS NULL OR s.role = :role)
              AND (:active IS NULL OR s.active = :active)
            """)
    Page<Staff> search(
            @Param("namePattern") String namePattern,
            @Param("role") StaffRole role,
            @Param("active") Boolean active,
            Pageable pageable);

    /**
     * Có nhân viên ĐANG LÀM khác cùng tên chưa, so sánh bằng hàm {@code norm_name} của CSDL (bỏ
     * khoảng trắng thừa, không phân biệt hoa thường), giống cách so tên dịch vụ.
     */
    @Query(
            value =
                    "SELECT EXISTS (SELECT 1 FROM staff WHERE active AND norm_name(full_name) ="
                            + " norm_name(:name) AND id <> :excludeId)",
            nativeQuery = true)
    boolean existsActiveByNormalizedName(
            @Param("name") String name, @Param("excludeId") long excludeId);
}
