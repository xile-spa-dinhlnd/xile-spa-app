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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Một lần đổi giá niêm yết của dịch vụ (FR-SVC-03), ánh xạ bảng {@code service_price_history}. Chỉ
 * thêm, không sửa. Tạo qua {@link ServiceItem#changePrice}.
 */
@Entity
@Table(name = "service_price_history")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServicePriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false, updatable = false)
    private ServiceItem service;

    @Column(name = "old_price", nullable = false, updatable = false)
    private long oldPrice;

    @Column(name = "new_price", nullable = false, updatable = false)
    private long newPrice;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    /** Id người đổi giá ({@code app_user.id}); lưu bằng id, không tham chiếu entity module auth. */
    @Column(name = "changed_by", nullable = false, updatable = false)
    private Long changedBy;

    ServicePriceHistory(
            ServiceItem service, long oldPrice, long newPrice, Instant changedAt, Long changedBy) {
        this.service = service;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
    }
}
