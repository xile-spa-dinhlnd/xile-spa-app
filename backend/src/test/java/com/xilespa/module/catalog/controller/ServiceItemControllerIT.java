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
import com.xilespa.module.catalog.repository.ServicePriceHistoryRepository;
import com.xilespa.security.UserPrincipal;
import com.xilespa.support.IntegrationTest;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * API bảng giá dịch vụ trên PostgreSQL thật. Dịch vụ không xóa được (có nhật ký, lịch sử giá tham
 * chiếu), nên mỗi test đặt tên dịch vụ có tiền tố ngẫu nhiên và tìm theo tiền tố đó.
 */
@IntegrationTest
class ServiceItemControllerIT {

    private static final String TEST_USER_EMAIL = "catalog_it@xilespa.vn";

    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired ServiceGroupRepository groupRepository;
    @Autowired ServicePriceHistoryRepository priceHistoryRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired Clock clock;

    private final String prefix = "it" + UUID.randomUUID().toString().substring(0, 6) + " ";
    private RequestPostProcessor loggedIn;
    private long groupId;
    private long otherGroupId;

    @BeforeEach
    void setUp() {
        // User riêng của test này, vai trò STAFF để không ảnh hưởng việc khởi tạo chủ tiệm.
        // Không xóa vì nhật ký và lịch sử giá tham chiếu tới nó.
        AppUser user =
                userRepository
                        .findByEmailIgnoreCase(TEST_USER_EMAIL)
                        .orElseGet(
                                () ->
                                        userRepository.save(
                                                new AppUser(
                                                        TEST_USER_EMAIL,
                                                        "hash",
                                                        "Catalog IT",
                                                        UserRole.STAFF,
                                                        clock.instant())));
        UserPrincipal principal = UserPrincipal.of(user.getId(), TEST_USER_EMAIL, "IT", "STAFF");
        loggedIn =
                authentication(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()));
        var groups = groupRepository.findByActiveOrderBySortOrderAscNameAsc(true);
        groupId = groups.get(0).getId();
        otherGroupId = groups.get(1).getId();
    }

    @Test
    void createReturns201AndAuditLog() throws Exception {
        String body = json(prefix + "Gội 30 phút", groupId, 30, 69000, 10000L);

        String response =
                mockMvc.perform(send(post("/api/catalog/services"), body))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andExpect(jsonPath("$.name").value(prefix + "Gội 30 phút"))
                        .andExpect(jsonPath("$.groupId").value(groupId))
                        .andExpect(jsonPath("$.groupName").isNotEmpty())
                        .andExpect(jsonPath("$.listPrice").value(69000))
                        .andExpect(jsonPath("$.tourFee").value(10000))
                        .andExpect(jsonPath("$.active").value(true))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        Number id = JsonPath.read(response, "$.id");
        var logs =
                auditLogRepository.findByEntityTypeAndEntityId(
                        "service", id.toString(), PageRequest.of(0, 10));
        assertThat(logs.getContent()).hasSize(1);
        assertThat(logs.getContent().getFirst().getActorId()).isNotNull();
    }

    @Test
    void namesDifferingOnlyByCaseOrSpacesAreDuplicates() throws Exception {
        create(prefix + "GỘI 60P", 50000);

        mockMvc.perform(
                        send(
                                post("/api/catalog/services"),
                                json(prefix + " gội   60p ", groupId, null, 0, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SERVICE_NAME_DUPLICATE"))
                .andExpect(jsonPath("$.message").value("Tên dịch vụ đã tồn tại."));
    }

    @Test
    void invalidInputReturnsFieldErrors() throws Exception {
        mockMvc.perform(send(post("/api/catalog/services"), "{\"name\":\"\",\"listPrice\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'groupId')]").exists())
                .andExpect(
                        jsonPath("$.fieldErrors[?(@.field == 'listPrice')].message")
                                .value("Giá niêm yết không được âm"));
    }

    @Test
    void unknownGroupIsRejected() throws Exception {
        mockMvc.perform(
                        send(
                                post("/api/catalog/services"),
                                json(prefix + "X", 999999L, null, 0, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SERVICE_GROUP_NOT_FOUND"));
    }

    @Test
    void priceZeroAndMissingTourFeeAreAllowed() throws Exception {
        mockMvc.perform(
                        send(
                                post("/api/catalog/services"),
                                json(prefix + "Chưa có giá", groupId, null, 0, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listPrice").value(0))
                .andExpect(jsonPath("$.tourFee").value(0))
                .andExpect(jsonPath("$.durationMinutes").isEmpty());
    }

    @Test
    void updatingPriceWritesPriceHistory() throws Exception {
        long id = create(prefix + "Chăm sóc da", 200000);

        mockMvc.perform(
                        send(
                                put("/api/catalog/services/" + id),
                                json(prefix + "Chăm sóc da", otherGroupId, 60, 250000, 20000L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listPrice").value(250000))
                .andExpect(jsonPath("$.groupId").value(otherGroupId))
                .andExpect(jsonPath("$.durationMinutes").value(60));

        var history = priceHistoryRepository.findByServiceIdOrderByChangedAtDescIdDesc(id);
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().getOldPrice()).isEqualTo(200000);
        assertThat(history.getFirst().getNewPrice()).isEqualTo(250000);
        assertThat(auditActions(id)).containsExactly("UPDATE", "CREATE");
    }

    @Test
    void updateWithoutPriceChangeWritesNoHistory() throws Exception {
        long id = create(prefix + "Giãn cơ", 300000);

        mockMvc.perform(
                        send(
                                put("/api/catalog/services/" + id),
                                json(prefix + "Giãn cơ mới", groupId, null, 300000, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(prefix + "Giãn cơ mới"));

        assertThat(priceHistoryRepository.findByServiceIdOrderByChangedAtDescIdDesc(id)).isEmpty();
    }

    @Test
    void renamingToAnotherServicesNameIsDuplicate() throws Exception {
        create(prefix + "A", 1000);
        long b = create(prefix + "B", 1000);

        mockMvc.perform(
                        send(
                                put("/api/catalog/services/" + b),
                                json(prefix + "a", groupId, null, 1000, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SERVICE_NAME_DUPLICATE"));
    }

    @Test
    void deactivateThenActivate() throws Exception {
        long id = create(prefix + "Combo cũ", 1000);

        mockMvc.perform(post("/api/catalog/services/" + id + "/deactivate").with(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(post("/api/catalog/services/" + id + "/deactivate").with(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(post("/api/catalog/services/" + id + "/activate").with(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        assertThat(auditActions(id)).containsExactly("ACTIVATE", "DEACTIVATE", "CREATE");
    }

    @Test
    void searchFiltersByNameGroupAndStatus() throws Exception {
        create(prefix + "Gội thảo dược", 1000);
        long inactive = create(prefix + "Gội cũ", 1000);
        mockMvc.perform(post("/api/catalog/services/" + inactive + "/deactivate").with(loggedIn));

        mockMvc.perform(
                        get("/api/catalog/services")
                                .with(loggedIn)
                                .param("q", prefix.toUpperCase() + "gội"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(
                        get("/api/catalog/services")
                                .with(loggedIn)
                                .param("q", prefix)
                                .param("active", "true"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value(prefix + "Gội thảo dược"));
        mockMvc.perform(
                        get("/api/catalog/services")
                                .with(loggedIn)
                                .param("q", prefix)
                                .param("groupId", String.valueOf(otherGroupId)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(
                        get("/api/catalog/services")
                                .with(loggedIn)
                                .param("q", prefix)
                                .param("size", "1"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listWithoutSearchTermWorks() throws Exception {
        // Lỗi thật đã gặp: không có q thì PostgreSQL báo lower(bytea) và API trả 500.
        create(prefix + "Bất kỳ", 1000);

        mockMvc.perform(get("/api/catalog/services").with(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").isNumber());
        mockMvc.perform(get("/api/catalog/services").with(loggedIn).param("active", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void filtersWithoutSearchTermUseTheSameParamsAsTheUi() throws Exception {
        // Đúng các tổ hợp giao diện gửi: lọc nhóm hoặc trạng thái mà không có q, có page và size.
        long active = create(prefix + "Đang bán", 1000);
        long inactive = create(prefix + "Đã ngừng", 1000);
        mockMvc.perform(post("/api/catalog/services/" + inactive + "/deactivate").with(loggedIn));

        mockMvc.perform(
                        get("/api/catalog/services")
                                .with(loggedIn)
                                .param("groupId", String.valueOf(groupId))
                                .param("active", "true")
                                .param("page", "0")
                                .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + active + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + inactive + ")]").doesNotExist());
        mockMvc.perform(
                        get("/api/catalog/services")
                                .with(loggedIn)
                                .param("active", "false")
                                .param("page", "0")
                                .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + inactive + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + active + ")]").doesNotExist());
        // "Tất cả": không gửi active, thấy cả hai
        mockMvc.perform(
                        get("/api/catalog/services")
                                .with(loggedIn)
                                .param("q", prefix)
                                .param("page", "0")
                                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void wildcardCharactersInSearchAreLiteral() throws Exception {
        create(prefix + "Gội 30 phút", 1000);

        mockMvc.perform(get("/api/catalog/services").with(loggedIn).param("q", prefix + "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getUnknownServiceIs404() throws Exception {
        mockMvc.perform(get("/api/catalog/services/999999").with(loggedIn))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy dịch vụ."));
    }

    @Test
    void deleteIsNotSupported() throws Exception {
        long id = create(prefix + "Không xóa", 1000);

        mockMvc.perform(delete("/api/catalog/services/" + id).with(loggedIn))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(get("/api/catalog/services")).andExpect(status().isUnauthorized());
    }

    private long create(String name, long price) throws Exception {
        String response =
                mockMvc.perform(
                                send(
                                        post("/api/catalog/services"),
                                        json(name, groupId, null, price, null)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private java.util.List<String> auditActions(long id) {
        return auditLogRepository
                .findByEntityTypeAndEntityId(
                        "service",
                        String.valueOf(id),
                        PageRequest.of(
                                0,
                                10,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Order.desc(
                                                "occurredAt"),
                                        org.springframework.data.domain.Sort.Order.desc("id"))))
                .getContent()
                .stream()
                .map(l -> l.getAction().name())
                .toList();
    }

    private MockHttpServletRequestBuilder send(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(loggedIn).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String json(
            String name, Long groupId, Integer duration, long price, Long tourFee) {
        return """
                {"name": %s, "groupId": %s, "durationMinutes": %s, "listPrice": %d, "tourFee": %s}
                """
                .formatted(
                        name == null ? "null" : "\"" + name + "\"",
                        groupId,
                        duration,
                        price,
                        tourFee);
    }
}
