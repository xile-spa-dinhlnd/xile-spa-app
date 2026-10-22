package com.xilespa.module.staff.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.xilespa.module.staff.enums.StaffRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StaffTest {

    private final Instant t0 = Instant.parse("2026-10-15T03:00:00Z");
    private final Instant t1 = Instant.parse("2026-10-16T03:00:00Z");

    @Test
    void newStaffIsActive() {
        Staff staff = new Staff("Lan", StaffRole.THERAPIST, t0);

        assertThat(staff.isActive()).isTrue();
        assertThat(staff.getCreatedAt()).isEqualTo(t0);
        assertThat(staff.getUpdatedAt()).isEqualTo(t0);
    }

    @Test
    void updateDetailsChangesNameAndRole() {
        Staff staff = new Staff("Lan", StaffRole.THERAPIST, t0);

        staff.updateDetails("Lan A", StaffRole.OWNER, t1);

        assertThat(staff.getFullName()).isEqualTo("Lan A");
        assertThat(staff.getRole()).isEqualTo(StaffRole.OWNER);
        assertThat(staff.getUpdatedAt()).isEqualTo(t1);
    }

    @Test
    void unchangedDetailsDoNotTouchUpdatedAt() {
        Staff staff = new Staff("Lan", StaffRole.THERAPIST, t0);

        staff.updateDetails("Lan", StaffRole.THERAPIST, t1);

        assertThat(staff.getUpdatedAt()).isEqualTo(t0);
    }

    @Test
    void deactivateAndActivateAreIdempotent() {
        Staff staff = new Staff("Lan", StaffRole.THERAPIST, t0);

        assertThat(staff.deactivate(t1)).isTrue();
        assertThat(staff.deactivate(t1)).isFalse();
        assertThat(staff.isActive()).isFalse();
        assertThat(staff.activate(t1)).isTrue();
        assertThat(staff.activate(t1)).isFalse();
        assertThat(staff.isActive()).isTrue();
    }
}
