package com.xilespa.module.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Một dịch vụ thành phần trong combo, ánh xạ bảng {@code combo_item} (V1). */
@Entity
@Table(name = "combo_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComboItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "combo_id", nullable = false)
    private Combo combo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceItem service;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public ComboItem(Combo combo, ServiceItem service, int quantity) {
        this.combo = combo;
        this.service = service;
        this.quantity = quantity;
    }
}
