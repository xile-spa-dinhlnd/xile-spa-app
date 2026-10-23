package com.xilespa.module.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Nhóm dịch vụ (FR-SVC-05), ánh xạ bảng {@code service_group}. Không có xóa cứng: ngừng dùng thì
 * chỉ đánh dấu active = false (BR-19).
 */
@Entity
@Table(name = "service_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    public ServiceGroup(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
        this.active = true;
    }

    public void update(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }

    /** Ngừng dùng nhóm. Trả về true nếu trạng thái thay đổi, false nếu đã ngừng dùng từ trước. */
    public boolean deactivate() {
        if (!this.active) {
            return false;
        }
        this.active = false;
        return true;
    }

    /** Dùng lại nhóm. Trả về true nếu trạng thái thay đổi, false nếu đang dùng từ trước. */
    public boolean activate() {
        if (this.active) {
            return false;
        }
        this.active = true;
        return true;
    }
}
