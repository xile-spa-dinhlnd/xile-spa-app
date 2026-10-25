package com.xilespa.module.catalog.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Một gói dịch vụ (combo) trong bảng giá, ánh xạ bảng {@code combo} (V1). Không xóa cứng: chỉ ngừng
 * bán (BR-19). Tiền là số nguyên đồng (BR-01).
 */
@Entity
@Table(name = "combo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Combo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "price", nullable = false)
    private long price;

    @Column(name = "tour_fee", nullable = false)
    private long tourFee;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ComboItem> items = new ArrayList<>();

    public Combo(
            String name, long price, long tourFee, Map<ServiceItem, Integer> items, Instant now) {
        this.name = name;
        this.price = price;
        this.tourFee = tourFee;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
        items.forEach((svc, qty) -> this.items.add(new ComboItem(this, svc, qty)));
    }
}
