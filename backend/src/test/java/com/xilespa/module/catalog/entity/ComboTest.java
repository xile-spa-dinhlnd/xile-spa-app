package com.xilespa.module.catalog.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void updateChangesDetailsAndItems() {
        ServiceGroup group = new ServiceGroup("Massage", 1);
        ServiceItem s1 = new ServiceItem(group, "Massage cổ vai gáy", 30, 150_000L, 20_000L, now);
        ReflectionTestUtils.setField(s1, "id", 101L);
        ServiceItem s2 = new ServiceItem(group, "Gội đầu dưỡng sinh", 45, 200_000L, 30_000L, now);
        ReflectionTestUtils.setField(s2, "id", 102L);
        ServiceItem s3 = new ServiceItem(group, "Chăm sóc da mặt", 60, 250_000L, 35_000L, now);
        ReflectionTestUtils.setField(s3, "id", 103L);

        Map<ServiceItem, Integer> initialItems = new LinkedHashMap<>();
        initialItems.put(s1, 2);
        initialItems.put(s2, 1);
        Combo combo = new Combo("Combo Ban Đầu", 450_000L, 50_000L, initialItems, now);

        Instant updateTime = Instant.parse("2026-10-25T11:00:00Z");
        boolean detailsChanged = combo.updateDetails("Combo Mới", 500_000L, 60_000L, updateTime);

        Map<ServiceItem, Integer> newItems = new LinkedHashMap<>();
        newItems.put(s1, 3);
        newItems.put(s3, 1);
        boolean itemsChanged = combo.replaceItems(newItems, updateTime);

        assertThat(detailsChanged).isTrue();
        assertThat(itemsChanged).isTrue();
        assertThat(combo.getName()).isEqualTo("Combo Mới");
        assertThat(combo.getPrice()).isEqualTo(500_000L);
        assertThat(combo.getTourFee()).isEqualTo(60_000L);
        assertThat(combo.getUpdatedAt()).isEqualTo(updateTime);
        assertThat(combo.getItems()).hasSize(2);

        ComboItem item1 = combo.getItems().get(0);
        assertThat(item1.getService()).isSameAs(s1);
        assertThat(item1.getQuantity()).isEqualTo(3);

        ComboItem item2 = combo.getItems().get(1);
        assertThat(item2.getService()).isSameAs(s3);
        assertThat(item2.getQuantity()).isEqualTo(1);
    }

    @Test
    void unchangedDetailsDoNotTouchUpdatedAt() {
        ServiceGroup group = new ServiceGroup("Massage", 1);
        ServiceItem s1 = new ServiceItem(group, "Massage cổ vai gáy", 30, 150_000L, 20_000L, now);
        ReflectionTestUtils.setField(s1, "id", 101L);

        Map<ServiceItem, Integer> initialItems = new LinkedHashMap<>();
        initialItems.put(s1, 2);
        Combo combo = new Combo("Combo Thư Giãn", 450_000L, 50_000L, initialItems, now);

        Instant newTime = Instant.parse("2026-10-25T11:00:00Z");
        boolean detailsChanged = combo.updateDetails("Combo Thư Giãn", 450_000L, 50_000L, newTime);

        Map<ServiceItem, Integer> sameItems = new LinkedHashMap<>();
        sameItems.put(s1, 2);
        boolean itemsChanged = combo.replaceItems(sameItems, newTime);

        assertThat(detailsChanged).isFalse();
        assertThat(itemsChanged).isFalse();
        assertThat(combo.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void changingOnlyItemsTouchesUpdatedAt() {
        ServiceGroup group = new ServiceGroup("Massage", 1);
        ServiceItem s1 = new ServiceItem(group, "Massage cổ vai gáy", 30, 150_000L, 20_000L, now);
        ReflectionTestUtils.setField(s1, "id", 101L);

        Map<ServiceItem, Integer> initialItems = new LinkedHashMap<>();
        initialItems.put(s1, 2);
        Combo combo = new Combo("Combo Thư Giãn", 450_000L, 50_000L, initialItems, now);

        Instant newTime = Instant.parse("2026-10-25T11:00:00Z");
        boolean detailsChanged = combo.updateDetails("Combo Thư Giãn", 450_000L, 50_000L, newTime);

        Map<ServiceItem, Integer> changedItems = new LinkedHashMap<>();
        changedItems.put(s1, 3);
        boolean itemsChanged = combo.replaceItems(changedItems, newTime);

        assertThat(detailsChanged).isFalse();
        assertThat(itemsChanged).isTrue();
        assertThat(combo.getUpdatedAt()).isEqualTo(newTime);
    }

    @Test
    void removeAndReAddSameServiceKeepsItemRow() {
        ServiceGroup group = new ServiceGroup("Massage", 1);
        ServiceItem s1 = new ServiceItem(group, "Massage cổ vai gáy", 30, 150_000L, 20_000L, now);
        ReflectionTestUtils.setField(s1, "id", 101L);

        Map<ServiceItem, Integer> initialItems = new LinkedHashMap<>();
        initialItems.put(s1, 2);
        Combo combo = new Combo("Combo Thư Giãn", 450_000L, 50_000L, initialItems, now);
        ComboItem originalItem = combo.getItems().get(0);
        ReflectionTestUtils.setField(originalItem, "id", 999L);

        Instant newTime = Instant.parse("2026-10-25T11:00:00Z");
        Map<ServiceItem, Integer> updatedItems = new LinkedHashMap<>();
        updatedItems.put(s1, 5);
        boolean itemsChanged = combo.replaceItems(updatedItems, newTime);

        assertThat(itemsChanged).isTrue();
        assertThat(combo.getItems()).hasSize(1);
        ComboItem keptItem = combo.getItems().get(0);
        assertThat(keptItem).isSameAs(originalItem);
        assertThat(keptItem.getId()).isEqualTo(999L);
        assertThat(keptItem.getQuantity()).isEqualTo(5);
    }
}
