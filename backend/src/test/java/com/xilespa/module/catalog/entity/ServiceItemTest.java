package com.xilespa.module.catalog.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ServiceItemTest {

    private final Instant t0 = Instant.parse("2026-10-11T03:00:00Z");
    private final Instant t1 = Instant.parse("2026-10-12T03:00:00Z");

    private ServiceItem newService() {
        ServiceGroup group = new ServiceGroup() {};
        ReflectionTestUtils.setField(group, "id", 1L);
        return new ServiceItem(group, "Gội 30 phút", 30, 69_000, 10_000, t0);
    }

    @Test
    void changingPriceCreatesHistoryWithOldAndNewPrice() {
        ServiceItem service = newService();

        var history = service.changePrice(79_000, 7L, t1);

        assertThat(history).isPresent();
        assertThat(history.get().getOldPrice()).isEqualTo(69_000);
        assertThat(history.get().getNewPrice()).isEqualTo(79_000);
        assertThat(history.get().getChangedBy()).isEqualTo(7L);
        assertThat(history.get().getChangedAt()).isEqualTo(t1);
        assertThat(service.getListPrice()).isEqualTo(79_000);
        assertThat(service.getUpdatedAt()).isEqualTo(t1);
    }

    @Test
    void samePriceCreatesNoHistory() {
        ServiceItem service = newService();

        assertThat(service.changePrice(69_000, 7L, t1)).isEmpty();
        assertThat(service.getUpdatedAt()).isEqualTo(t0);
    }

    @Test
    void unchangedDetailsDoNotTouchUpdatedAt() {
        ServiceItem service = newService();

        service.updateDetails(service.getGroup(), "Gội 30 phút", 30, 10_000, t1);

        assertThat(service.getUpdatedAt()).isEqualTo(t0);
    }

    @Test
    void deactivateAndActivateAreIdempotent() {
        ServiceItem service = newService();

        assertThat(service.deactivate(t1)).isTrue();
        assertThat(service.deactivate(t1)).isFalse();
        assertThat(service.isActive()).isFalse();
        assertThat(service.activate(t1)).isTrue();
        assertThat(service.activate(t1)).isFalse();
        assertThat(service.isActive()).isTrue();
    }
}
