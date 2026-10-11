package com.xilespa.module.system.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xilespa.common.audit.AuditAction;
import com.xilespa.common.audit.AuditService;
import com.xilespa.support.IntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class AuditLogControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired AuditService auditService;
    @Autowired TransactionTemplate tx;

    /** audit_log chỉ thêm, không dọn được; mỗi test dùng loại đối tượng riêng. */
    private final String entityType = "it_" + UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void seed() {
        // 3 dòng theo thứ tự thời gian: tạo dịch vụ 1, tạo dịch vụ 2, đổi giá dịch vụ 1
        record(AuditAction.CREATE, "1", null, Map.of("price", 69000));
        record(AuditAction.CREATE, "2", null, Map.of("price", 120000));
        record(AuditAction.UPDATE, "1", Map.of("price", 69000), Map.of("price", 79000));
    }

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser
    void listsNewestFirstWithJsonValues() throws Exception {
        mockMvc.perform(get("/api/audit-logs").param("entityType", entityType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].action").value("UPDATE"))
                .andExpect(jsonPath("$.content[0].entityId").value("1"))
                .andExpect(jsonPath("$.content[0].before.price").value(69000))
                .andExpect(jsonPath("$.content[0].after.price").value(79000))
                .andExpect(jsonPath("$.content[2].action").value("CREATE"))
                .andExpect(jsonPath("$.content[2].before").isEmpty());
    }

    @Test
    @WithMockUser
    void filtersByEntityId() throws Exception {
        mockMvc.perform(
                        get("/api/audit-logs")
                                .param("entityType", entityType)
                                .param("entityId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].after.price").value(120000));
    }

    @Test
    @WithMockUser
    void paginates() throws Exception {
        mockMvc.perform(
                        get("/api/audit-logs")
                                .param("entityType", entityType)
                                .param("page", "1")
                                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @WithMockUser
    void pageSizeIsCappedAt100() throws Exception {
        mockMvc.perform(
                        get("/api/audit-logs")
                                .param("entityType", entityType)
                                .param("size", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    private void record(AuditAction action, String id, Object before, Object after) {
        tx.executeWithoutResult(
                status -> auditService.record(action, entityType, id, before, after));
    }
}
