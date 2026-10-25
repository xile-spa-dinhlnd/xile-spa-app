package com.xilespa.module.staff.devdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xilespa.common.response.PageResponse;
import com.xilespa.module.staff.dto.request.StaffRequest;
import com.xilespa.module.staff.dto.response.StaffResponse;
import com.xilespa.module.staff.enums.StaffRole;
import com.xilespa.module.staff.service.StaffService;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffDevDataTest {

    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");

    @Mock private StaffService staffService;

    private StaffDevData devData;
    private final AtomicLong nextId = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        devData = new StaffDevData(staffService);
    }

    @Test
    void emptyDatabaseGetsAllSampleStaffAndOneLeftStaff() {
        givenSearchReturns(List.of());
        givenCreateAssignsIds();

        devData.run(null);

        ArgumentCaptor<StaffRequest> created = ArgumentCaptor.forClass(StaffRequest.class);
        verify(staffService, times(5)).create(created.capture());
        assertThat(created.getAllValues())
                .extracting(StaffRequest::fullName)
                .containsExactly(
                        "Chủ tiệm Xile",
                        "Nguyễn Thị Lan",
                        "Trần Thị Mai",
                        "Lê Văn Hùng",
                        "Phạm Thị Hoa");
        assertThat(created.getAllValues().getFirst().role()).isEqualTo(StaffRole.OWNER);
        verify(staffService).deactivate(5L);
    }

    @Test
    void existingStaffAreNotCreatedAgain() {
        when(staffService.search(anyString(), isNull(), isNull(), anyInt(), anyInt()))
                .thenAnswer(inv -> page(List.of(staff(1L, inv.getArgument(0)))));

        devData.run(null);

        verify(staffService, never()).create(any());
        verify(staffService, never()).deactivate(anyLong());
    }

    @Test
    void similarNameFromSearchDoesNotCountAsExisting() {
        givenSearchReturns(List.of());
        when(staffService.search(eq("Nguyễn Thị Lan"), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(page(List.of(staff(99L, "Nguyễn Thị Lan Anh"))));
        givenCreateAssignsIds();

        devData.run(null);

        verify(staffService).create(new StaffRequest("Nguyễn Thị Lan", StaffRole.THERAPIST));
    }

    private void givenSearchReturns(List<StaffResponse> content) {
        when(staffService.search(anyString(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(page(content));
    }

    private void givenCreateAssignsIds() {
        when(staffService.create(any()))
                .thenAnswer(
                        inv -> {
                            StaffRequest request = inv.getArgument(0);
                            return new StaffResponse(
                                    nextId.getAndIncrement(),
                                    request.fullName(),
                                    request.role(),
                                    true,
                                    NOW,
                                    NOW);
                        });
    }

    private static StaffResponse staff(Long id, String fullName) {
        return new StaffResponse(id, fullName, StaffRole.THERAPIST, true, NOW, NOW);
    }

    private static PageResponse<StaffResponse> page(List<StaffResponse> content) {
        return new PageResponse<>(content, 0, 100, content.size(), 1);
    }
}
