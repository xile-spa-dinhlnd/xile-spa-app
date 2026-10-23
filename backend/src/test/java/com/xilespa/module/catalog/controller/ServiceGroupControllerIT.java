package com.xilespa.module.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.xilespa.common.audit.AuditLogRepository;
import com.xilespa.module.auth.entity.AppUser;
import com.xilespa.module.auth.enums.UserRole;
import com.xilespa.module.auth.repository.AppUserRepository;
import com.xilespa.module.catalog.repository.ServiceGroupRepository;
import com.xilespa.module.catalog.repository.ServiceItemRepository;
import com.xilespa.security.UserPrincipal;
import com.xilespa.support.IntegrationTest;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * API nhóm dịch vụ (FR-SVC-05) trên PostgreSQL thật. Nhóm dịch vụ không xóa cứng, nên mỗi test tạo
 * tên có tiền tố ngẫu nhiên để tránh đụng độ và tự dọn dữ liệu của chính mình sau mỗi test.
 */
@IntegrationTest
class ServiceGroupControllerIT {

    private static final String TEST_USER_EMAIL = "group_it@xilespa.vn";

    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired ServiceGroupRepository groupRepository;
    @Autowired ServiceItemRepository serviceRepository;
    @Autowired Clock clock;

    private final String prefix = "it" + UUID.randomUUID().toString().substring(0, 6) + " ";
    private RequestPostProcessor loggedIn;

    @AfterEach
    void tearDown() {
        serviceRepository.deleteAll(
                serviceRepository.findAll().stream()
                        .filter(s -> s.getName().startsWith(prefix))
                        .toList());
        groupRepository.deleteAll(
                groupRepository.findAll().stream()
                        .filter(g -> g.getName().startsWith(prefix))
                        .toList());
    }

    @BeforeEach
    void setUp() {
        AppUser user =
                userRepository
                        .findByEmailIgnoreCase(TEST_USER_EMAIL)
                        .orElseGet(
                                () ->
                                        userRepository.save(
                                                new AppUser(
                                                        TEST_USER_EMAIL,
                                                        "hash",
                                                        "Group IT",
                                                        UserRole.STAFF,
                                                        clock.instant())));
        UserPrincipal principal = UserPrincipal.of(user.getId(), TEST_USER_EMAIL, "IT", "STAFF");
        loggedIn =
                authentication(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()));
    }

    @Test
    void listsGroupsInOrder() throws Exception {
        create(prefix + "Nhóm A", 501);
        create(prefix + "Nhóm B", 502);

        String response =
                mockMvc.perform(get("/api/catalog/service-groups").with(loggedIn))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[?(@.name == 'Gội đầu dưỡng sinh')]").exists())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<Number> sortOrders = JsonPath.read(response, "$[*].sortOrder");
        for (int i = 0; i < sortOrders.size() - 1; i++) {
            assertThat(sortOrders.get(i).intValue())
                    .isLessThanOrEqualTo(sortOrders.get(i + 1).intValue());
        }
    }

    @Test
    void listsActiveOrInactiveGroups() throws Exception {
        long activeId = create(prefix + "Nhóm Active", 50);
        long inactiveId = create(prefix + "Nhóm Inactive", 51);
        mockMvc.perform(
                        post("/api/catalog/service-groups/" + inactiveId + "/deactivate")
                                .with(loggedIn))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/catalog/service-groups").with(loggedIn).param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + activeId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + inactiveId + ")]").doesNotExist());

        mockMvc.perform(get("/api/catalog/service-groups").with(loggedIn).param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + activeId + ")]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == " + inactiveId + ")]").exists());
    }

    @Test
    void listWithoutActiveFilterIncludesInactiveGroups() throws Exception {
        long inactiveId = create(prefix + "Nhóm Ngừng", 52);
        mockMvc.perform(
                        post("/api/catalog/service-groups/" + inactiveId + "/deactivate")
                                .with(loggedIn))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/catalog/service-groups").with(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + inactiveId + ")].active").value(false));
    }

    @Test
    void activeGroupsKeepSeedOrder() throws Exception {
        String response =
                mockMvc.perform(
                                get("/api/catalog/service-groups")
                                        .with(loggedIn)
                                        .param("active", "true"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<String> names = JsonPath.read(response, "$[*].name");
        assertThat(names)
                .containsSubsequence(
                        "Gội đầu dưỡng sinh", "Chăm sóc da", "Stretching/Rehab", "Khác");
    }

    @Test
    void getReturnsGroupDetails() throws Exception {
        long id = create(prefix + "Chi Tiết", 45);

        mockMvc.perform(get("/api/catalog/service-groups/" + id).with(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(prefix + "Chi Tiết"))
                .andExpect(jsonPath("$.sortOrder").value(45))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getUnknownGroupIs404() throws Exception {
        mockMvc.perform(get("/api/catalog/service-groups/999999").with(loggedIn))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void createReturns201AndAuditLog() throws Exception {
        String response =
                mockMvc.perform(send(post("/api/catalog/service-groups"), json(prefix + "Mới", 60)))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andExpect(jsonPath("$.name").value(prefix + "Mới"))
                        .andExpect(jsonPath("$.sortOrder").value(60))
                        .andExpect(jsonPath("$.active").value(true))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        long id = ((Number) JsonPath.read(response, "$.id")).longValue();
        assertThat(auditActions(id)).containsExactly("CREATE");
    }

    @Test
    void namesDifferingOnlyByCaseOrSpacesAreDuplicates() throws Exception {
        create(prefix + "Spa Trị Liệu", 70);

        mockMvc.perform(
                        send(
                                post("/api/catalog/service-groups"),
                                json("  " + prefix.toUpperCase() + "  spa   trị liệu ", 75)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SERVICE_GROUP_NAME_DUPLICATE"))
                .andExpect(jsonPath("$.message").value("Tên nhóm dịch vụ đã tồn tại."));
    }

    @Test
    void validationFailsOnBlankOrInvalidData() throws Exception {
        mockMvc.perform(send(post("/api/catalog/service-groups"), json("", 10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));

        mockMvc.perform(send(post("/api/catalog/service-groups"), json("   ", 10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));

        mockMvc.perform(send(post("/api/catalog/service-groups"), json("a".repeat(101), 10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));

        mockMvc.perform(send(post("/api/catalog/service-groups"), json("Hợp lệ", -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sortOrder"));

        mockMvc.perform(send(post("/api/catalog/service-groups"), "{\"name\": \"Hợp lệ\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sortOrder"));
    }

    @Test
    void updateModifiesDetailsAndAuditLog() throws Exception {
        long id = create(prefix + "Ban Đầu", 80);

        mockMvc.perform(
                        send(
                                put("/api/catalog/service-groups/" + id),
                                json(prefix + "Cập Nhật", 85)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(prefix + "Cập Nhật"))
                .andExpect(jsonPath("$.sortOrder").value(85));

        assertThat(auditActions(id)).containsExactly("UPDATE", "CREATE");
    }

    @Test
    void updateKeepsOwnNameWithoutDuplicateError() throws Exception {
        long id = create(prefix + "Giữ Nguyên", 90);

        mockMvc.perform(
                        send(
                                put("/api/catalog/service-groups/" + id),
                                json(prefix + "Giữ Nguyên", 95)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortOrder").value(95));
    }

    @Test
    void updateWithDuplicateNameOfAnotherGroupIsConflict() throws Exception {
        create(prefix + "Nhóm Một", 100);
        long id2 = create(prefix + "Nhóm Hai", 101);

        mockMvc.perform(
                        send(
                                put("/api/catalog/service-groups/" + id2),
                                json(prefix + "Nhóm Một", 101)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SERVICE_GROUP_NAME_DUPLICATE"));
    }

    @Test
    void deactivateAndActivateChangesActiveAndRecordsAudit() throws Exception {
        long id = create(prefix + "Trạng Thái", 110);

        mockMvc.perform(post("/api/catalog/service-groups/" + id + "/deactivate").with(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Gọi lại khi đã inactive không đổi gì
        mockMvc.perform(post("/api/catalog/service-groups/" + id + "/deactivate").with(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/catalog/service-groups/" + id + "/activate").with(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        // Gọi lại khi đã active không đổi gì
        mockMvc.perform(post("/api/catalog/service-groups/" + id + "/activate").with(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        assertThat(auditActions(id)).containsExactly("ACTIVATE", "DEACTIVATE", "CREATE");
    }

    @Test
    void deactivateGroupWithActiveServiceIsConflict() throws Exception {
        long id = create(prefix + "Còn Dịch Vụ", 115);
        mockMvc.perform(
                        send(
                                post("/api/catalog/services"),
                                """
                                {"name": "%s", "groupId": %d, "listPrice": 100000}
                                """
                                        .formatted(prefix + "Dịch vụ trong nhóm", id)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/catalog/service-groups/" + id + "/deactivate").with(loggedIn))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SERVICE_GROUP_IN_USE"));

        mockMvc.perform(get("/api/catalog/service-groups/" + id).with(loggedIn))
                .andExpect(jsonPath("$.active").value(true));
        assertThat(auditActions(id)).containsExactly("CREATE");
    }

    @Test
    void deleteIsNotSupported() throws Exception {
        long id = create(prefix + "Không Xóa", 120);

        mockMvc.perform(delete("/api/catalog/service-groups/" + id).with(loggedIn))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(get("/api/catalog/service-groups")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/catalog/service-groups")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/catalog/service-groups/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/catalog/service-groups/1/deactivate"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/catalog/service-groups/1/activate"))
                .andExpect(status().isUnauthorized());
    }

    private long create(String name, int sortOrder) throws Exception {
        String response =
                mockMvc.perform(send(post("/api/catalog/service-groups"), json(name, sortOrder)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private List<String> auditActions(long id) {
        return auditLogRepository
                .findByEntityTypeAndEntityId(
                        "service_group",
                        String.valueOf(id),
                        PageRequest.of(
                                0,
                                10,
                                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))))
                .getContent()
                .stream()
                .map(l -> l.getAction().name())
                .toList();
    }

    private MockHttpServletRequestBuilder send(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(loggedIn).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String json(String name, int sortOrder) {
        return """
                {"name": "%s", "sortOrder": %d}
                """
                .formatted(name, sortOrder);
    }
}
