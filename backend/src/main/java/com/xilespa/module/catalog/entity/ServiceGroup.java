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
 * Nhóm dịch vụ, ánh xạ bảng {@code service_group} (V1, dữ liệu mẫu ở V2). Quản lý nhóm (thêm, sửa)
 * làm ở FR-SVC-05; hiện chỉ đọc.
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
}
