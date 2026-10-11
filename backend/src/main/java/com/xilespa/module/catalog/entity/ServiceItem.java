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
import java.time.Instant;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Một dịch vụ trong bảng giá, ánh xạ bảng {@code service} (V1). Tên class là ServiceItem để không
 * trùng với annotation {@code @Service} của Spring. Không xóa cứng: chỉ ngừng bán (BR-19). Tiền là
 * số nguyên đồng (BR-01).
 */
@Entity
@Table(name = "service")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ServiceGroup group;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Thời lượng (phút); có thể chưa có. */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /** Giá niêm yết (đồng), ≥ 0. Có thể là 0 khi chưa có giá. */
    @Column(name = "list_price", nullable = false)
    private long listPrice;

    /** Tiền tour mặc định (đồng), ≥ 0. */
    @Column(name = "tour_fee", nullable = false)
    private long tourFee;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ServiceItem(
            ServiceGroup group,
            String name,
            Integer durationMinutes,
            long listPrice,
            long tourFee,
            Instant now) {
        this.group = group;
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.listPrice = listPrice;
        this.tourFee = tourFee;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Sửa thông tin mô tả (không gồm giá). Giao dịch cũ đã chụp lại tên nên không bị ảnh hưởng
     * (BR-03). Chỉ cập nhật {@code updatedAt} khi thực sự có thay đổi.
     */
    public void updateDetails(
            ServiceGroup group, String name, Integer durationMinutes, long tourFee, Instant now) {
        boolean changed =
                !this.group.getId().equals(group.getId())
                        || !this.name.equals(name)
                        || !java.util.Objects.equals(this.durationMinutes, durationMinutes)
                        || this.tourFee != tourFee;
        if (!changed) {
            return;
        }
        this.group = group;
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.tourFee = tourFee;
        this.updatedAt = now;
    }

    /**
     * Đổi giá niêm yết (FR-SVC-03). Trả về bản ghi lịch sử cần lưu, hoặc rỗng nếu giá không đổi.
     * Giao dịch cũ giữ giá tại thời điểm bán (BR-03).
     */
    public Optional<ServicePriceHistory> changePrice(long newPrice, Long changedBy, Instant now) {
        if (newPrice == this.listPrice) {
            return Optional.empty();
        }
        ServicePriceHistory history =
                new ServicePriceHistory(this, this.listPrice, newPrice, now, changedBy);
        this.listPrice = newPrice;
        this.updatedAt = now;
        return Optional.of(history);
    }

    /** Ngừng bán (FR-SVC-04, BR-19). Trả về false nếu đã ngừng bán từ trước. */
    public boolean deactivate(Instant now) {
        if (!active) {
            return false;
        }
        this.active = false;
        this.updatedAt = now;
        return true;
    }

    /** Bán lại. Trả về false nếu đang bán sẵn. */
    public boolean activate(Instant now) {
        if (active) {
            return false;
        }
        this.active = true;
        this.updatedAt = now;
        return true;
    }
}
