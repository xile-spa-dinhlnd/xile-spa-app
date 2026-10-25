package com.xilespa.module.catalog.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ComboTest {

    private final Instant now = Instant.parse("2026-10-25T10:00:00Z");

    @Test
    void newComboIsActiveWithItems() {
        ServiceGroup group = new ServiceGroup("Massage", 1);
        ServiceItem s1 = new ServiceItem(group, "Massage cổ vai gáy", 30, 150_000L, 20_000L, now);
        ServiceItem s2 = new ServiceItem(group, "Gội đầu dưỡng sinh", 45, 200_000L, 30_000L, now);

        Map<ServiceItem, Integer> items = new LinkedHashMap<>();
        items.put(s1, 2);
        items.put(s2, 1);

        Combo combo = new Combo("Combo Thư Giãn", 450_000L, 50_000L, items, now);

        assertThat(combo.getName()).isEqualTo("Combo Thư Giãn");
        assertThat(combo.getPrice()).isEqualTo(450_000L);
        assertThat(combo.getTourFee()).isEqualTo(50_000L);
        assertThat(combo.isActive()).isTrue();
        assertThat(combo.getCreatedAt()).isEqualTo(now);
        assertThat(combo.getUpdatedAt()).isEqualTo(now);
        assertThat(combo.getItems()).hasSize(2);

        ComboItem item1 = combo.getItems().get(0);
        assertThat(item1.getCombo()).isSameAs(combo);
        assertThat(item1.getService()).isSameAs(s1);
        assertThat(item1.getQuantity()).isEqualTo(2);

        ComboItem item2 = combo.getItems().get(1);
        assertThat(item2.getCombo()).isSameAs(combo);
        assertThat(item2.getService()).isSameAs(s2);
        assertThat(item2.getQuantity()).isEqualTo(1);
    }
}
