package com.xilespa.module.catalog.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceGroupTest {

    @Test
    void newServiceGroupIsActive() {
        ServiceGroup group = new ServiceGroup("Gội đầu", 10);

        assertThat(group.getName()).isEqualTo("Gội đầu");
        assertThat(group.getSortOrder()).isEqualTo(10);
        assertThat(group.isActive()).isTrue();
    }

    @Test
    void updateChangesNameAndSortOrder() {
        ServiceGroup group = new ServiceGroup("Gội đầu", 10);

        group.update("Gội đầu dưỡng sinh", 15);

        assertThat(group.getName()).isEqualTo("Gội đầu dưỡng sinh");
        assertThat(group.getSortOrder()).isEqualTo(15);
    }

    @Test
    void deactivateAndActivateAreIdempotent() {
        ServiceGroup group = new ServiceGroup("Gội đầu", 10);

        assertThat(group.deactivate()).isTrue();
        assertThat(group.deactivate()).isFalse();
        assertThat(group.isActive()).isFalse();

        assertThat(group.activate()).isTrue();
        assertThat(group.activate()).isFalse();
        assertThat(group.isActive()).isTrue();
    }
}
