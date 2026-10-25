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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    /**
     * Cập nhật thông tin chi tiết của combo (FR-CMB-04). Trả về {@code true} nếu có thay đổi để cập
     * nhật {@code updatedAt}, {@code false} nếu giữ nguyên để không chạm thời gian cập nhật.
     */
    public boolean updateDetails(String name, long price, long tourFee, Instant now) {
        boolean changed =
                !Objects.equals(this.name, name) || this.price != price || this.tourFee != tourFee;
        if (!changed) {
            return false;
        }
        this.name = name;
        this.price = price;
        this.tourFee = tourFee;
        this.updatedAt = now;
        return true;
    }

    /**
     * Thay thế danh sách thành phần theo so sánh {@code serviceId} (FR-CMB-04). Giữ nguyên thực thể
     * cũ và cập nhật số lượng thay vì xóa hết rồi thêm lại để tránh xung đột unique index {@code
     * ux_combo_item}. Trả về {@code true} nếu thành phần hoặc số lượng có thay đổi.
     */
    public boolean replaceItems(Map<ServiceItem, Integer> newItems, Instant now) {
        boolean changed = false;

        Map<Long, ComboItem> existingItemsByServiceId = new LinkedHashMap<>();
        for (ComboItem item : this.items) {
            existingItemsByServiceId.put(item.getService().getId(), item);
        }

        Set<Long> newServiceIds = new HashSet<>();
        for (ServiceItem service : newItems.keySet()) {
            newServiceIds.add(service.getId());
        }

        Iterator<ComboItem> iterator = this.items.iterator();
        while (iterator.hasNext()) {
            ComboItem item = iterator.next();
            Long serviceId = item.getService().getId();
            if (!newServiceIds.contains(serviceId)) {
                iterator.remove();
                changed = true;
            }
        }

        for (Map.Entry<ServiceItem, Integer> entry : newItems.entrySet()) {
            ServiceItem service = entry.getKey();
            Long serviceId = service.getId();
            int newQuantity = entry.getValue();

            ComboItem existingItem = existingItemsByServiceId.get(serviceId);
            if (existingItem != null) {
                if (existingItem.changeQuantity(newQuantity)) {
                    changed = true;
                }
            } else {
                this.items.add(new ComboItem(this, service, newQuantity));
                changed = true;
            }
        }

        if (changed) {
            this.updatedAt = now;
        }
        return changed;
    }

    /**
     * Ngừng bán combo (FR-CMB-05, BR-19). Trả về {@code true} nếu trạng thái thay đổi từ đang bán
     * sang ngừng bán, {@code false} nếu đã ở trạng thái ngừng bán từ trước.
     */
    public boolean deactivate(Instant now) {
        if (!this.active) {
            return false;
        }
        this.active = false;
        this.updatedAt = now;
        return true;
    }

    /**
     * Bán lại combo (FR-CMB-05). Trả về {@code true} nếu trạng thái thay đổi từ ngừng bán sang đang
     * bán, {@code false} nếu đã ở trạng thái đang bán từ trước.
     */
    public boolean activate(Instant now) {
        if (this.active) {
            return false;
        }
        this.active = true;
        this.updatedAt = now;
        return true;
    }
}
