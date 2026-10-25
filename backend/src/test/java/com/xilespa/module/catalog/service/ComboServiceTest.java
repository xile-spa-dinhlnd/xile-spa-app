package com.xilespa.module.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xilespa.common.audit.AuditAction;
import com.xilespa.common.audit.AuditService;
import com.xilespa.common.exception.BusinessException;
import com.xilespa.common.exception.ErrorCode;
import com.xilespa.module.catalog.dto.request.ComboItemRequest;
import com.xilespa.module.catalog.dto.request.ComboQuoteRequest;
import com.xilespa.module.catalog.dto.request.ComboRequest;
import com.xilespa.module.catalog.dto.response.ComboQuoteResponse;
import com.xilespa.module.catalog.dto.response.ComboResponse;
import com.xilespa.module.catalog.entity.Combo;
import com.xilespa.module.catalog.entity.ServiceGroup;
import com.xilespa.module.catalog.entity.ServiceItem;
import com.xilespa.module.catalog.mapper.CatalogMapper;
import com.xilespa.module.catalog.repository.ComboRepository;
import com.xilespa.module.catalog.repository.ServiceItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ComboServiceTest {

    private final Instant now = Instant.parse("2026-10-25T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @Mock private ComboRepository comboRepository;
    @Mock private ServiceItemRepository serviceItemRepository;
    @Mock private AuditService auditService;

    private ComboService service;
    private ServiceItem s1;
    private ServiceItem s2;

    @BeforeEach
    void setUp() {
        service =
                new ComboService(
                        comboRepository,
                        serviceItemRepository,
                        Mappers.getMapper(CatalogMapper.class),
                        auditService,
                        clock);

        ServiceGroup group = new ServiceGroup("Massage", 1);
        ReflectionTestUtils.setField(group, "id", 1L);

        s1 = new ServiceItem(group, "Massage cổ vai gáy", 30, 150_000L, 20_000L, now);
        ReflectionTestUtils.setField(s1, "id", 101L);

        s2 = new ServiceItem(group, "Gội đầu dưỡng sinh", 45, 200_000L, 30_000L, now);
        ReflectionTestUtils.setField(s2, "id", 102L);
    }

    // =========================================================================
    // 1. Tính tổng giá lẻ, tiết kiệm & quote
    // =========================================================================

    @Test
    void quoteComputesLineTotalsRetailTotalAndSavings() {
        when(serviceItemRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(s1, s2));

        ComboQuoteRequest request =
                new ComboQuoteRequest(
                        450_000L,
                        List.of(new ComboItemRequest(101L, 2), new ComboItemRequest(102L, 1)));

        ComboQuoteResponse response = service.quote(request);

        assertThat(response.retailTotal()).isEqualTo(500_000L);
        assertThat(response.savings()).isEqualTo(50_000L);
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).lineTotal()).isEqualTo(300_000L);
        assertThat(response.items().get(1).lineTotal()).isEqualTo(200_000L);
    }

    @Test
    void priceAboveRetailGivesNegativeSavings() {
        when(serviceItemRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(s1, s2));

        ComboQuoteRequest request =
                new ComboQuoteRequest(
                        550_000L,
                        List.of(new ComboItemRequest(101L, 2), new ComboItemRequest(102L, 1)));

        ComboQuoteResponse response = service.quote(request);

        assertThat(response.retailTotal()).isEqualTo(500_000L);
        assertThat(response.savings()).isEqualTo(-50_000L);
    }

    @Test
    void zeroPriceAndZeroListPrice() {
        ServiceGroup group = new ServiceGroup("Khác", 2);
        ServiceItem freeSvc = new ServiceItem(group, "Tư vấn miễn phí", 15, 0L, 0L, now);
        ReflectionTestUtils.setField(freeSvc, "id", 103L);
        when(serviceItemRepository.findAllById(List.of(103L))).thenReturn(List.of(freeSvc));

        ComboQuoteRequest request =
                new ComboQuoteRequest(0L, List.of(new ComboItemRequest(103L, 1)));

        ComboQuoteResponse response = service.quote(request);

        assertThat(response.retailTotal()).isEqualTo(0L);
        assertThat(response.savings()).isEqualTo(0L);
    }

    @Test
    void largestPriceAndQuantityDoNotOverflow() {
        ServiceGroup group = new ServiceGroup("VIP", 3);
        List<Long> ids = new ArrayList<>();
        List<ServiceItem> services = new ArrayList<>();
        List<ComboItemRequest> items = new ArrayList<>();

        for (long i = 1; i <= 20; i++) {
            ids.add(i);
            ServiceItem item =
                    new ServiceItem(group, "VIP Service " + i, 60, 1_000_000_000L, 0L, now);
            ReflectionTestUtils.setField(item, "id", i);
            services.add(item);
            items.add(new ComboItemRequest(i, 99));
        }
        when(serviceItemRepository.findAllById(ids)).thenReturn(services);

        ComboQuoteRequest request = new ComboQuoteRequest(1_000_000_000L, items);
        ComboQuoteResponse response = service.quote(request);

        // 20 * 99 * 1_000_000_000 = 1_980_000_000_000
        assertThat(response.retailTotal()).isEqualTo(1_980_000_000_000L);
        assertThat(response.savings()).isEqualTo(1_979_000_000_000L);
    }

    @Test
    void quoteReturnsTotalsWithoutSaving() {
        when(serviceItemRepository.findAllById(List.of(101L))).thenReturn(List.of(s1));

        ComboQuoteRequest request =
                new ComboQuoteRequest(null, List.of(new ComboItemRequest(101L, 1)));

        ComboQuoteResponse response = service.quote(request);

        assertThat(response.retailTotal()).isEqualTo(150_000L);
        assertThat(response.savings()).isNull();
        verify(comboRepository, never()).saveAndFlush(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void quoteWithEmptyItemsAndNoPrice() {
        ComboQuoteRequest request = new ComboQuoteRequest(null, Collections.emptyList());

        ComboQuoteResponse response = service.quote(request);

        assertThat(response.retailTotal()).isEqualTo(0L);
        assertThat(response.savings()).isNull();
        assertThat(response.items()).isEmpty();
        verify(serviceItemRepository, never()).findAllById(any());
    }

    @Test
    void quoteAllowsInactiveService() {
        s1.deactivate(now);
        when(serviceItemRepository.findAllById(List.of(101L))).thenReturn(List.of(s1));

        ComboQuoteRequest request =
                new ComboQuoteRequest(100_000L, List.of(new ComboItemRequest(101L, 1)));

        ComboQuoteResponse response = service.quote(request);

        assertThat(response.items().get(0).serviceActive()).isFalse();
        assertThat(response.retailTotal()).isEqualTo(150_000L);
        assertThat(response.savings()).isEqualTo(50_000L);
    }

    @Test
    void quoteWithDuplicateServiceIsRejected() {
        ComboQuoteRequest request =
                new ComboQuoteRequest(
                        100_000L,
                        List.of(new ComboItemRequest(101L, 1), new ComboItemRequest(101L, 2)));

        assertThatThrownBy(() -> service.quote(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMBO_ITEM_DUPLICATE);
        verify(serviceItemRepository, never()).findAllById(any());
    }

    @Test
    void quoteWithUnknownServiceIsRejected() {
        when(serviceItemRepository.findAllById(List.of(999L))).thenReturn(Collections.emptyList());

        ComboQuoteRequest request =
                new ComboQuoteRequest(100_000L, List.of(new ComboItemRequest(999L, 1)));

        assertThatThrownBy(() -> service.quote(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMBO_SERVICE_UNAVAILABLE);
    }

    // =========================================================================
    // 2. Tạo combo (create)
    // =========================================================================

    @Test
    void createTrimsSpacesAndRecordsAudit() {
        when(comboRepository.existsByNormalizedName("Combo Siêu Thư Giãn", 0L)).thenReturn(false);
        when(serviceItemRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(s1, s2));
        when(comboRepository.saveAndFlush(any(Combo.class)))
                .thenAnswer(
                        inv -> {
                            Combo c = inv.getArgument(0);
                            ReflectionTestUtils.setField(c, "id", 50L);
                            for (int i = 0; i < c.getItems().size(); i++) {
                                ReflectionTestUtils.setField(
                                        c.getItems().get(i), "id", (long) (i + 1));
                            }
                            return c;
                        });

        ComboRequest request =
                new ComboRequest(
                        "  Combo   Siêu   Thư   Giãn  ",
                        450_000L,
                        30_000L,
                        List.of(new ComboItemRequest(101L, 2), new ComboItemRequest(102L, 1)));

        ComboResponse created = service.create(request);

        assertThat(created.id()).isEqualTo(50L);
        assertThat(created.name()).isEqualTo("Combo Siêu Thư Giãn");
        assertThat(created.price()).isEqualTo(450_000L);
        assertThat(created.tourFee()).isEqualTo(30_000L);
        assertThat(created.active()).isTrue();
        assertThat(created.retailTotal()).isEqualTo(500_000L);
        assertThat(created.savings()).isEqualTo(50_000L);
        assertThat(created.items()).hasSize(2);

        verify(auditService)
                .record(eq(AuditAction.CREATE), eq("combo"), eq(50L), isNull(), eq(created));
    }

    @Test
    void missingTourFeeIsZero() {
        when(comboRepository.existsByNormalizedName("Combo Cơ Bản", 0L)).thenReturn(false);
        when(serviceItemRepository.findAllById(List.of(101L))).thenReturn(List.of(s1));
        when(comboRepository.saveAndFlush(any(Combo.class)))
                .thenAnswer(
                        inv -> {
                            Combo c = inv.getArgument(0);
                            ReflectionTestUtils.setField(c, "id", 51L);
                            return c;
                        });

        ComboRequest request =
                new ComboRequest(
                        "Combo Cơ Bản", 120_000L, null, List.of(new ComboItemRequest(101L, 1)));

        ComboResponse created = service.create(request);

        assertThat(created.tourFee()).isEqualTo(0L);
    }

    @Test
    void priceZeroIsAllowed() {
        when(comboRepository.existsByNormalizedName("Combo 0đ", 0L)).thenReturn(false);
        when(serviceItemRepository.findAllById(List.of(101L))).thenReturn(List.of(s1));
        when(comboRepository.saveAndFlush(any(Combo.class)))
                .thenAnswer(
                        inv -> {
                            Combo c = inv.getArgument(0);
                            ReflectionTestUtils.setField(c, "id", 52L);
                            return c;
                        });

        ComboRequest request =
                new ComboRequest("Combo 0đ", 0L, 0L, List.of(new ComboItemRequest(101L, 1)));

        ComboResponse created = service.create(request);

        assertThat(created.price()).isEqualTo(0L);
        assertThat(created.savings()).isEqualTo(150_000L);
    }

    @Test
    void priceAboveRetailIsAllowed() {
        when(comboRepository.existsByNormalizedName("Combo Đặc Biệt", 0L)).thenReturn(false);
        when(serviceItemRepository.findAllById(List.of(101L))).thenReturn(List.of(s1));
        when(comboRepository.saveAndFlush(any(Combo.class)))
                .thenAnswer(
                        inv -> {
                            Combo c = inv.getArgument(0);
                            ReflectionTestUtils.setField(c, "id", 53L);
                            return c;
                        });

        ComboRequest request =
                new ComboRequest(
                        "Combo Đặc Biệt", 200_000L, 0L, List.of(new ComboItemRequest(101L, 1)));

        ComboResponse created = service.create(request);

        assertThat(created.price()).isEqualTo(200_000L);
        assertThat(created.retailTotal()).isEqualTo(150_000L);
        assertThat(created.savings()).isEqualTo(-50_000L);
    }

    @Test
    void namesDifferingOnlyByCaseOrSpacesAreDuplicates() {
        when(comboRepository.existsByNormalizedName("combo test", 0L)).thenReturn(true);

        ComboRequest request =
                new ComboRequest(
                        "  combo   test  ", 100_000L, 0L, List.of(new ComboItemRequest(101L, 1)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMBO_NAME_DUPLICATE);
        verify(comboRepository, never()).saveAndFlush(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void createWithDuplicateNameIsRejected() {
        when(comboRepository.existsByNormalizedName("Combo Đã Có", 0L)).thenReturn(true);

        ComboRequest request =
                new ComboRequest(
                        "Combo Đã Có", 100_000L, 0L, List.of(new ComboItemRequest(101L, 1)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMBO_NAME_DUPLICATE);
        verify(comboRepository, never()).saveAndFlush(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void createWithDuplicateServiceIsRejected() {
        when(comboRepository.existsByNormalizedName("Combo Lặp", 0L)).thenReturn(false);

        ComboRequest request =
                new ComboRequest(
                        "Combo Lặp",
                        100_000L,
                        0L,
                        List.of(new ComboItemRequest(101L, 1), new ComboItemRequest(101L, 2)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMBO_ITEM_DUPLICATE);
        verify(comboRepository, never()).saveAndFlush(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void createWithInactiveServiceIsRejected() {
        s2.deactivate(now);
        when(comboRepository.existsByNormalizedName("Combo Có Dịch Vụ Ngừng", 0L))
                .thenReturn(false);
        when(serviceItemRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(s1, s2));

        ComboRequest request =
                new ComboRequest(
                        "Combo Có Dịch Vụ Ngừng",
                        300_000L,
                        0L,
                        List.of(new ComboItemRequest(101L, 1), new ComboItemRequest(102L, 1)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Dịch vụ đã ngừng bán: Gội đầu dưỡng sinh.")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMBO_SERVICE_UNAVAILABLE);
        verify(comboRepository, never()).saveAndFlush(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void createWithUnknownServiceIsRejected() {
        when(comboRepository.existsByNormalizedName("Combo Thiếu Dịch Vụ", 0L)).thenReturn(false);
        when(serviceItemRepository.findAllById(List.of(999L))).thenReturn(Collections.emptyList());

        ComboRequest request =
                new ComboRequest(
                        "Combo Thiếu Dịch Vụ",
                        100_000L,
                        0L,
                        List.of(new ComboItemRequest(999L, 1)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMBO_SERVICE_UNAVAILABLE);
        verify(comboRepository, never()).saveAndFlush(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    // =========================================================================
    // 3. Sửa combo (update)
    // =========================================================================

    @Test
    void updateChangesDetailsAndItems() {
        ServiceGroup group = new ServiceGroup("Massage", 1);
        ServiceItem s3 = new ServiceItem(group, "Chăm sóc da mặt", 60, 250_000L, 35_000L, now);
        ReflectionTestUtils.setField(s3, "id", 103L);

        Map<ServiceItem, Integer> initialItems = new LinkedHashMap<>();
        initialItems.put(s1, 2);
        initialItems.put(s2, 1);
        Combo combo = new Combo("Combo Ban Đầu", 450_000L, 50_000L, initialItems, now);
        ReflectionTestUtils.setField(combo, "id", 50L);

        when(comboRepository.findWithItemsById(50L)).thenReturn(Optional.of(combo));
        when(comboRepository.existsByNormalizedName("Combo Sửa", 50L)).thenReturn(false);
        when(serviceItemRepository.findAllById(List.of(101L, 103L))).thenReturn(List.of(s1, s3));
        when(comboRepository.saveAndFlush(any(Combo.class))).thenAnswer(inv -> inv.getArgument(0));

        ComboRequest request =
                new ComboRequest(
                        "Combo Sửa",
                        600_000L,
                        70_000L,
                        List.of(new ComboItemRequest(101L, 3), new ComboItemRequest(103L, 1)));

        ComboResponse response = service.update(50L, request);

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.name()).isEqualTo("Combo Sửa");
        assertThat(response.price()).isEqualTo(600_000L);
        assertThat(response.tourFee()).isEqualTo(70_000L);
        assertThat(response.items()).hasSize(2);
        // s1: 150_000 * 3 = 450_000; s3: 250_000 * 1 = 250_000 -> retail = 700_000, savings =
        // 100_000
        assertThat(response.retailTotal()).isEqualTo(700_000L);
        assertThat(response.savings()).isEqualTo(100_000L);

        verify(comboRepository).saveAndFlush(combo);

        ArgumentCaptor<ComboResponse> beforeCaptor = ArgumentCaptor.forClass(ComboResponse.class);
        ArgumentCaptor<ComboResponse> afterCaptor = ArgumentCaptor.forClass(ComboResponse.class);
        verify(auditService)
                .record(
                        eq(AuditAction.UPDATE),
                        eq("combo"),
                        eq(50L),
                        beforeCaptor.capture(),
                        afterCaptor.capture());

        ComboResponse before = beforeCaptor.getValue();
        ComboResponse after = afterCaptor.getValue();
        assertThat(before.name()).isEqualTo("Combo Ban Đầu");
        assertThat(before.items()).hasSize(2);
        assertThat(before.items().get(0).quantity()).isEqualTo(2);
        assertThat(after.name()).isEqualTo("Combo Sửa");
        assertThat(after.items()).hasSize(2);
        assertThat(after.items().get(0).quantity()).isEqualTo(3);
        assertThat(after.items().get(1).serviceId()).isEqualTo(103L);
    }

    @Test
    void updateWithoutChangesWritesNoAudit() {
        Map<ServiceItem, Integer> initialItems = new LinkedHashMap<>();
        initialItems.put(s1, 2);
        initialItems.put(s2, 1);
        Combo combo = new Combo("Combo Thư Giãn", 450_000L, 50_000L, initialItems, now);
        ReflectionTestUtils.setField(combo, "id", 50L);

        when(comboRepository.findWithItemsById(50L)).thenReturn(Optional.of(combo));
        when(comboRepository.existsByNormalizedName("Combo Thư Giãn", 50L)).thenReturn(false);
        when(serviceItemRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(s1, s2));

        ComboRequest request =
                new ComboRequest(
                        "Combo Thư Giãn",
                        450_000L,
                        50_000L,
                        List.of(new ComboItemRequest(101L, 2), new ComboItemRequest(102L, 1)));

        ComboResponse response = service.update(50L, request);

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.name()).isEqualTo("Combo Thư Giãn");
        verify(comboRepository, never()).saveAndFlush(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void updateWithDuplicateNameIsRejected() {
        Map<ServiceItem, Integer> initialItems = new LinkedHashMap<>();
        initialItems.put(s1, 1);
        Combo combo = new Combo("Combo Cũ", 100_000L, 0L, initialItems, now);
        ReflectionTestUtils.setField(combo, "id", 50L);

        when(comboRepository.findWithItemsById(50L)).thenReturn(Optional.of(combo));
        when(comboRepository.existsByNormalizedName("Combo Đã Có", 50L)).thenReturn(true);

        ComboRequest request =
                new ComboRequest(
                        "Combo Đã Có", 100_000L, 0L, List.of(new ComboItemRequest(101L, 1)));

        assertThatThrownBy(() -> service.update(50L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMBO_NAME_DUPLICATE);
        verify(comboRepository, never()).saveAndFlush(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void updateWithInactiveServiceIsRejected() {
        Map<ServiceItem, Integer> initialItems = new LinkedHashMap<>();
        initialItems.put(s1, 1);
        Combo combo = new Combo("Combo Cũ", 100_000L, 0L, initialItems, now);
        ReflectionTestUtils.setField(combo, "id", 50L);

        s2.deactivate(now);
        when(comboRepository.findWithItemsById(50L)).thenReturn(Optional.of(combo));
        when(comboRepository.existsByNormalizedName("Combo Cũ", 50L)).thenReturn(false);
        when(serviceItemRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(s1, s2));

        ComboRequest request =
                new ComboRequest(
                        "Combo Cũ",
                        200_000L,
                        0L,
                        List.of(new ComboItemRequest(101L, 1), new ComboItemRequest(102L, 1)));

        assertThatThrownBy(() -> service.update(50L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Dịch vụ đã ngừng bán: Gội đầu dưỡng sinh.")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMBO_SERVICE_UNAVAILABLE);
        verify(comboRepository, never()).saveAndFlush(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void updateUnknownComboIs404() {
        when(comboRepository.findWithItemsById(999L)).thenReturn(Optional.empty());

        ComboRequest request =
                new ComboRequest("Combo Mới", 100_000L, 0L, List.of(new ComboItemRequest(101L, 1)));

        assertThatThrownBy(() -> service.update(999L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(comboRepository, never()).saveAndFlush(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    // =========================================================================
    // 4. Ngừng bán & bán lại (deactivate & activate)
    // =========================================================================

    @Test
    void deactivateThenActivateRecordsAudit() {
        Combo combo = new Combo("Combo Thư Giãn", 450_000L, 50_000L, Map.of(s1, 1), now);
        ReflectionTestUtils.setField(combo, "id", 50L);

        when(comboRepository.findWithItemsById(50L)).thenReturn(Optional.of(combo));

        // Ngừng bán
        ComboResponse deactivated = service.deactivate(50L);
        assertThat(deactivated.active()).isFalse();
        verify(comboRepository).save(combo);
        verify(auditService)
                .record(eq(AuditAction.DEACTIVATE), eq("combo"), eq(50L), any(), eq(deactivated));

        // Bán lại
        ComboResponse activated = service.activate(50L);
        assertThat(activated.active()).isTrue();
        verify(auditService)
                .record(eq(AuditAction.ACTIVATE), eq("combo"), eq(50L), any(), eq(activated));
    }

    @Test
    void deactivateAndActivateAreIdempotent() {
        Combo combo = new Combo("Combo Thư Giãn", 450_000L, 50_000L, Map.of(s1, 1), now);
        ReflectionTestUtils.setField(combo, "id", 50L);

        when(comboRepository.findWithItemsById(50L)).thenReturn(Optional.of(combo));

        // Đang bán mà gọi activate -> không đổi, không ghi audit
        ComboResponse alreadyActive = service.activate(50L);
        assertThat(alreadyActive.active()).isTrue();
        verify(auditService, never()).record(any(), any(), any(), any(), any());

        // Ngừng bán lần 1 -> ghi audit
        service.deactivate(50L);
        verify(auditService).record(eq(AuditAction.DEACTIVATE), eq("combo"), eq(50L), any(), any());

        // Ngừng bán lần 2 -> không đổi, không ghi thêm audit
        ComboResponse alreadyInactive = service.deactivate(50L);
        assertThat(alreadyInactive.active()).isFalse();
        // Vẫn chỉ có 1 lần DEACTIVATE được ghi
        verify(auditService).record(eq(AuditAction.DEACTIVATE), eq("combo"), eq(50L), any(), any());
    }

    @Test
    void activatingComboWithInactiveServiceIsRejected() {
        s1.deactivate(now);
        Combo combo = new Combo("Combo Có Dịch Vụ Ngừng", 450_000L, 50_000L, Map.of(s1, 1), now);
        ReflectionTestUtils.setField(combo, "id", 50L);
        combo.deactivate(now);

        when(comboRepository.findWithItemsById(50L)).thenReturn(Optional.of(combo));

        assertThatThrownBy(() -> service.activate(50L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Dịch vụ đã ngừng bán: Massage cổ vai gáy.")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMBO_SERVICE_UNAVAILABLE);

        verify(comboRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void deactivateUnknownComboIs404() {
        when(comboRepository.findWithItemsById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void activateUnknownComboIs404() {
        when(comboRepository.findWithItemsById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activate(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }
}
