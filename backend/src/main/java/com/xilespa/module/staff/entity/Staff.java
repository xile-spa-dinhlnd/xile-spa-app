package com.xilespa.module.staff.entity;

import com.xilespa.module.staff.enums.StaffRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Nhân viên thực hiện dịch vụ, ánh xạ bảng {@code staff} (V1). Không xóa cứng: nhân viên nghỉ thì
 * chỉ đánh dấu {@code active = false}, vẫn giữ trong lịch sử giao dịch (FR-STF-01).
 *
 * <p>Cột {@code user_id} (liên kết tài khoản đăng nhập) chưa dùng ở MVP nên không ánh xạ.
 */
@Entity
@Table(name = "staff")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private StaffRole role;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Staff(String fullName, StaffRole role, Instant now) {
        this.fullName = fullName;
        this.role = role;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Sửa tên và vai trò. Chỉ cập nhật {@code updatedAt} khi thực sự có thay đổi. */
    public void updateDetails(String fullName, StaffRole role, Instant now) {
        if (this.fullName.equals(fullName) && this.role == role) {
            return;
        }
        this.fullName = fullName;
        this.role = role;
        this.updatedAt = now;
    }

    /** Cho nghỉ việc. Trả về false nếu đã nghỉ từ trước. */
    public boolean deactivate(Instant now) {
        if (!active) {
            return false;
        }
        this.active = false;
        this.updatedAt = now;
        return true;
    }

    /** Cho làm lại. Trả về false nếu đang làm sẵn. */
    public boolean activate(Instant now) {
        if (active) {
            return false;
        }
        this.active = true;
        this.updatedAt = now;
        return true;
    }
}
