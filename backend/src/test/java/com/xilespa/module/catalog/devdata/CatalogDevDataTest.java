package com.xilespa.module.catalog.devdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xilespa.common.response.PageResponse;
import com.xilespa.module.catalog.dto.request.ComboItemRequest;
import com.xilespa.module.catalog.dto.request.ComboRequest;
import com.xilespa.module.catalog.dto.request.ServiceItemRequest;
import com.xilespa.module.catalog.dto.response.ComboResponse;
import com.xilespa.module.catalog.dto.response.ServiceGroupResponse;
import com.xilespa.module.catalog.dto.response.ServiceItemResponse;
import com.xilespa.module.catalog.service.ComboService;
import com.xilespa.module.catalog.service.ServiceGroupService;
import com.xilespa.module.catalog.service.ServiceItemService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogDevDataTest {

    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");
    private static final List<ServiceGroupResponse> SEED_GROUPS =
            List.of(
                    new ServiceGroupResponse(1L, "Gội đầu dưỡng sinh", 10, true),
                    new ServiceGroupResponse(2L, "Chăm sóc da", 20, true),
                    new ServiceGroupResponse(3L, "Stretching/Rehab", 30, true),
                    new ServiceGroupResponse(4L, "Khác", 99, true));

    @Mock private ServiceGroupService groupService;
    @Mock private ServiceItemService serviceItemService;
    @Mock private ComboService comboService;

    private CatalogDevData devData;
    private final AtomicLong nextId = new AtomicLong(100);
    private final Map<String, Long> createdServiceIds = new HashMap<>();

    @BeforeEach
    void setUp() {
        devData = new CatalogDevData(groupService, serviceItemService, comboService);
    }

    @Test
    void emptyDatabaseGetsServicesCombosAndOneStoppedService() {
        when(groupService.list(true)).thenReturn(SEED_GROUPS);
        givenNoServicesAndNoCombos();
        givenServiceCreateAssignsIds();

        devData.run(null);

        verify(serviceItemService, times(11)).create(any());
        verify(serviceItemService).deactivate(createdServiceIds.get("Ngâm chân muối khoáng"));
        ArgumentCaptor<ComboRequest> combos = ArgumentCaptor.forClass(ComboRequest.class);
        verify(comboService, times(3)).create(combos.capture());
        ComboRequest relax = combos.getAllValues().getFirst();
        assertThat(relax.name()).isEqualTo("Combo Thư Giãn");
        assertThat(relax.price()).isEqualTo(400_000L);
        assertThat(relax.items())
                .containsExactly(
                        new ComboItemRequest(createdServiceIds.get("Gội đầu thảo dược"), 1),
                        new ComboItemRequest(createdServiceIds.get("Giãn cơ chân"), 1),
                        new ComboItemRequest(createdServiceIds.get("Xông hơi thảo dược"), 1));
        assertThat(combos.getAllValues().get(1).items())
                .containsExactly(
                        new ComboItemRequest(createdServiceIds.get("Giãn cơ toàn thân"), 2));
    }

    @Test
    void servicesAreCreatedInTheirSeedGroupWithSamplePrice() {
        when(groupService.list(true)).thenReturn(SEED_GROUPS);
        givenNoServicesAndNoCombos();
        givenServiceCreateAssignsIds();

        devData.run(null);

        verify(serviceItemService).create(new ServiceItemRequest("Tư vấn da", 2L, 15, 0L, 0L));
        verify(serviceItemService)
                .create(new ServiceItemRequest("Giãn cơ toàn thân", 3L, 60, 350_000L, 60_000L));
    }

    @Test
    void existingServicesAreReusedForCombosAndExistingCombosAreSkipped() {
        when(groupService.list(true)).thenReturn(SEED_GROUPS);
        when(serviceItemService.search(anyString(), isNull(), isNull(), anyInt(), anyInt()))
                .thenAnswer(inv -> page(List.of(service(7L, inv.getArgument(0)))));
        when(comboService.search(anyString(), isNull(), anyInt(), anyInt()))
                .thenAnswer(
                        inv ->
                                inv.getArgument(0).equals("Combo Thư Giãn")
                                        ? page(List.of(combo(inv.getArgument(0))))
                                        : page(List.<ComboResponse>of()));

        devData.run(null);

        verify(serviceItemService, never()).create(any());
        verify(serviceItemService, never()).deactivate(any());
        ArgumentCaptor<ComboRequest> combos = ArgumentCaptor.forClass(ComboRequest.class);
        verify(comboService, times(2)).create(combos.capture());
        assertThat(combos.getAllValues())
                .extracting(ComboRequest::name)
                .containsExactly("Combo Giãn Cơ 2 Buổi", "Combo Da Sáng");
        assertThat(combos.getAllValues().getFirst().items())
                .containsExactly(new ComboItemRequest(7L, 2));
    }

    @Test
    void missingGroupSkipsItsServicesAndCombosThatNeedThem() {
        when(groupService.list(true)).thenReturn(SEED_GROUPS.subList(0, 3));
        givenNoServicesAndNoCombos();
        givenServiceCreateAssignsIds();

        devData.run(null);

        verify(serviceItemService, times(9)).create(any());
        ArgumentCaptor<ComboRequest> combos = ArgumentCaptor.forClass(ComboRequest.class);
        verify(comboService, times(2)).create(combos.capture());
        assertThat(combos.getAllValues())
                .extracting(ComboRequest::name)
                .doesNotContain("Combo Thư Giãn");
    }

    private void givenNoServicesAndNoCombos() {
        when(serviceItemService.search(anyString(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(page(List.<ServiceItemResponse>of()));
        when(comboService.search(anyString(), isNull(), anyInt(), anyInt()))
                .thenReturn(page(List.<ComboResponse>of()));
    }

    private void givenServiceCreateAssignsIds() {
        when(serviceItemService.create(any()))
                .thenAnswer(
                        inv -> {
                            ServiceItemRequest request = inv.getArgument(0);
                            long id = nextId.getAndIncrement();
                            createdServiceIds.put(request.name(), id);
                            return service(id, request.name());
                        });
    }

    private static ServiceItemResponse service(Long id, String name) {
        return new ServiceItemResponse(id, name, 1L, "Nhóm", 30, 100_000L, 0L, true, NOW, NOW);
    }

    private static ComboResponse combo(String name) {
        return new ComboResponse(1L, name, 100_000L, 0L, true, List.of(), 0L, -100_000L, NOW, NOW);
    }

    private static <T> PageResponse<T> page(List<T> content) {
        return new PageResponse<>(content, 0, 100, content.size(), 1);
    }
}
