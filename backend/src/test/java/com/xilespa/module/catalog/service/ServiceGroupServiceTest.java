package com.xilespa.module.catalog.service;

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
import com.xilespa.module.catalog.dto.request.ServiceGroupRequest;
import com.xilespa.module.catalog.dto.response.ServiceGroupResponse;
import com.xilespa.module.catalog.entity.ServiceGroup;
import com.xilespa.module.catalog.mapper.CatalogMapper;
import com.xilespa.module.catalog.repository.ServiceGroupRepository;
import com.xilespa.module.catalog.repository.ServiceItemRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ServiceGroupServiceTest {

    @Mock private ServiceGroupRepository groupRepository;
    @Mock private ServiceItemRepository serviceRepository;
    @Mock private AuditService auditService;

    private ServiceGroupService service;

    @BeforeEach
    void setUp() {
        service =
                new ServiceGroupService(
                        groupRepository,
                        serviceRepository,
                        Mappers.getMapper(CatalogMapper.class),
                        auditService);
    }

    private ServiceGroup existing(Long id, String name, int sortOrder, boolean active) {
        ServiceGroup group = new ServiceGroup(name, sortOrder);
        ReflectionTestUtils.setField(group, "id", id);
        ReflectionTestUtils.setField(group, "active", active);
        return group;
    }

    @Test
    void listReturnsAllGroupsWhenActiveIsNull() {
        ServiceGroup g1 = existing(1L, "Gội đầu", 10, true);
        ServiceGroup g2 = existing(2L, "Chăm sóc da", 20, false);
        when(groupRepository.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of(g1, g2));

        List<ServiceGroupResponse> result = service.list(null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Gội đầu");
        assertThat(result.get(0).active()).isTrue();
        assertThat(result.get(1).name()).isEqualTo("Chăm sóc da");
        assertThat(result.get(1).active()).isFalse();
    }

    @Test
    void listReturnsActiveGroupsWhenActiveIsTrue() {
        ServiceGroup g1 = existing(1L, "Gội đầu", 10, true);
        when(groupRepository.findByActiveOrderBySortOrderAscNameAsc(true)).thenReturn(List.of(g1));

        List<ServiceGroupResponse> result = service.list(true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Gội đầu");
    }

    @Test
    void listReturnsInactiveGroupsWhenActiveIsFalse() {
        ServiceGroup g2 = existing(2L, "Chăm sóc da", 20, false);
        when(groupRepository.findByActiveOrderBySortOrderAscNameAsc(false)).thenReturn(List.of(g2));

        List<ServiceGroupResponse> result = service.list(false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Chăm sóc da");
    }

    @Test
    void getReturnsGroup() {
        ServiceGroup g = existing(1L, "Gội đầu", 10, true);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(g));

        ServiceGroupResponse response = service.get(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Gội đầu");
        assertThat(response.sortOrder()).isEqualTo(10);
        assertThat(response.active()).isTrue();
    }

    @Test
    void getWhenNotFoundThrowsNotFound() {
        when(groupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void createTrimsSpacesAndRecordsAudit() {
        when(groupRepository.existsByNormalizedName("Gội đầu dưỡng sinh", 0L)).thenReturn(false);
        when(groupRepository.saveAndFlush(any(ServiceGroup.class)))
                .thenAnswer(
                        inv -> {
                            ServiceGroup g = inv.getArgument(0);
                            ReflectionTestUtils.setField(g, "id", 5L);
                            return g;
                        });

        ServiceGroupResponse created =
                service.create(new ServiceGroupRequest("  Gội đầu   dưỡng sinh  ", 15));

        assertThat(created.name()).isEqualTo("Gội đầu dưỡng sinh");
        assertThat(created.sortOrder()).isEqualTo(15);
        assertThat(created.active()).isTrue();
        verify(auditService)
                .record(eq(AuditAction.CREATE), eq("service_group"), eq(5L), any(), eq(created));
    }

    @Test
    void createWithDuplicateNameThrowsDuplicate() {
        when(groupRepository.existsByNormalizedName("Gội đầu", 0L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new ServiceGroupRequest("Gội đầu", 10)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SERVICE_GROUP_NAME_DUPLICATE);

        verify(groupRepository, never()).saveAndFlush(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void updateModifiesDetailsAndRecordsAudit() {
        ServiceGroup g = existing(5L, "Gội đầu", 10, true);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(g));
        when(groupRepository.existsByNormalizedName("Gội đầu chuyên sâu", 5L)).thenReturn(false);

        ServiceGroupResponse updated =
                service.update(5L, new ServiceGroupRequest("  Gội đầu   chuyên sâu ", 20));

        assertThat(updated.name()).isEqualTo("Gội đầu chuyên sâu");
        assertThat(updated.sortOrder()).isEqualTo(20);
        verify(auditService)
                .record(eq(AuditAction.UPDATE), eq("service_group"), eq(5L), any(), eq(updated));
    }

    @Test
    void updateWithDuplicateNameThrowsDuplicate() {
        ServiceGroup g = existing(5L, "Gội đầu", 10, true);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(g));
        when(groupRepository.existsByNormalizedName("Chăm sóc da", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, new ServiceGroupRequest("Chăm sóc da", 20)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SERVICE_GROUP_NAME_DUPLICATE);

        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void updateWithoutChangesWritesNoAudit() {
        ServiceGroup g = existing(5L, "Gội đầu", 10, true);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(g));
        when(groupRepository.existsByNormalizedName("Gội đầu", 5L)).thenReturn(false);

        service.update(5L, new ServiceGroupRequest("Gội đầu", 10));

        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void deactivateRecordsAudit() {
        ServiceGroup g = existing(5L, "Gội đầu", 10, true);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(g));

        ServiceGroupResponse deactivated = service.deactivate(5L);

        assertThat(deactivated.active()).isFalse();
        verify(auditService)
                .record(
                        eq(AuditAction.DEACTIVATE),
                        eq("service_group"),
                        eq(5L),
                        any(),
                        eq(deactivated));
    }

    @Test
    void deactivateAlreadyInactiveDoesNotRecordAudit() {
        ServiceGroup g = existing(5L, "Gội đầu", 10, false);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(g));

        ServiceGroupResponse response = service.deactivate(5L);

        assertThat(response.active()).isFalse();
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void deactivateGroupWithActiveServicesIsRejected() {
        ServiceGroup g = existing(5L, "Gội đầu", 10, true);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(g));
        when(serviceRepository.existsByGroupIdAndActiveTrue(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.deactivate(5L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SERVICE_GROUP_IN_USE);

        assertThat(g.isActive()).isTrue();
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void activateRecordsAudit() {
        ServiceGroup g = existing(5L, "Gội đầu", 10, false);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(g));

        ServiceGroupResponse activated = service.activate(5L);

        assertThat(activated.active()).isTrue();
        verify(auditService)
                .record(
                        eq(AuditAction.ACTIVATE),
                        eq("service_group"),
                        eq(5L),
                        any(),
                        eq(activated));
    }

    @Test
    void activateAlreadyActiveDoesNotRecordAudit() {
        ServiceGroup g = existing(5L, "Gội đầu", 10, true);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(g));

        ServiceGroupResponse response = service.activate(5L);

        assertThat(response.active()).isTrue();
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }
}
