package com.xilespa.module.staff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xilespa.common.audit.AuditAction;
import com.xilespa.common.audit.AuditService;
import com.xilespa.common.exception.BusinessException;
import com.xilespa.common.exception.ErrorCode;
import com.xilespa.module.staff.dto.request.StaffRequest;
import com.xilespa.module.staff.dto.response.StaffResponse;
import com.xilespa.module.staff.entity.Staff;
import com.xilespa.module.staff.enums.StaffRole;
import com.xilespa.module.staff.mapper.StaffMapper;
import com.xilespa.module.staff.repository.StaffRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-15T03:00:00Z"), ZoneOffset.UTC);

    @Mock private StaffRepository staffRepository;
    @Mock private AuditService auditService;

    private StaffService service;

    @BeforeEach
    void setUp() {
        service =
                new StaffService(
                        staffRepository, Mappers.getMapper(StaffMapper.class), auditService, clock);
    }

    @Test
    void createTrimsSpacesAndRecordsAudit() {
        when(staffRepository.existsActiveByNormalizedName("Lan A", 0L)).thenReturn(false);
        when(staffRepository.save(any(Staff.class)))
                .thenAnswer(
                        inv -> {
                            Staff s = inv.getArgument(0);
                            ReflectionTestUtils.setField(s, "id", 5L);
                            return s;
                        });

        StaffResponse created = service.create(new StaffRequest("  Lan   A ", StaffRole.THERAPIST));

        assertThat(created.fullName()).isEqualTo("Lan A");
        assertThat(created.active()).isTrue();
        verify(auditService)
                .record(eq(AuditAction.CREATE), eq("staff"), eq(5L), any(), eq(created));
    }

    @Test
    void createWithActiveDuplicateNameIsRejected() {
        when(staffRepository.existsActiveByNormalizedName("Lan", 0L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new StaffRequest("Lan", StaffRole.THERAPIST)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STAFF_NAME_DUPLICATE);
        verify(staffRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void updateWithoutChangesWritesNoAudit() {
        Staff staff = existing(7L, "Lan", true);
        when(staffRepository.findById(7L)).thenReturn(Optional.of(staff));
        when(staffRepository.existsActiveByNormalizedName("Lan", 7L)).thenReturn(false);

        service.update(7L, new StaffRequest("Lan", StaffRole.THERAPIST));

        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void reactivatingWhenAnotherActiveStaffHasSameNameIsRejected() {
        Staff staff = existing(7L, "Lan", false);
        when(staffRepository.findById(7L)).thenReturn(Optional.of(staff));
        when(staffRepository.existsActiveByNormalizedName("Lan", 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.activate(7L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STAFF_NAME_DUPLICATE);
        assertThat(staff.isActive()).isFalse();
    }

    @Test
    void deactivateTwiceRecordsAuditOnce() {
        Staff staff = existing(7L, "Lan", true);
        when(staffRepository.findById(7L)).thenReturn(Optional.of(staff));

        service.deactivate(7L);
        service.deactivate(7L);

        verify(auditService).record(eq(AuditAction.DEACTIVATE), eq("staff"), eq(7L), any(), any());
    }

    @Test
    void getUnknownStaffIsNotFound() {
        when(staffRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void likePatternEscapesWildcards() {
        assertThat(StaffService.likePattern(null)).isEqualTo("%");
        assertThat(StaffService.likePattern("  ")).isEqualTo("%");
        assertThat(StaffService.likePattern(" Lan ")).isEqualTo("%lan%");
        assertThat(StaffService.likePattern("50%_a\\")).isEqualTo("%50\\%\\_a\\\\%");
    }

    private Staff existing(long id, String name, boolean active) {
        Staff staff = new Staff(name, StaffRole.THERAPIST, clock.instant());
        ReflectionTestUtils.setField(staff, "id", id);
        if (!active) {
            staff.deactivate(clock.instant());
        }
        return staff;
    }
}
