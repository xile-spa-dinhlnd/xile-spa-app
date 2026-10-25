package com.xilespa.module.catalog.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ComboItemTest {

    private final Instant now = Instant.parse("2026-10-25T10:00:00Z");

    @Test
    void createComboItemHoldsFields() {
        ServiceGroup group = new ServiceGroup("Massage", 1);
        ServiceItem service = new ServiceItem(group, "Massage chân", 30, 100_000L, 10_000L, now);
        Combo combo = new Combo("Combo Test", 100_000L, 0L, null, now);

        ComboItem item = new ComboItem(combo, service, 2);

        assertThat(item.getCombo()).isSameAs(combo);
        assertThat(item.getService()).isSameAs(service);
        assertThat(item.getQuantity()).isEqualTo(2);
    }
}
