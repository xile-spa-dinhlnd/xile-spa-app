package com.xilespa.module.staff.controller;

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
import com.xilespa.security.UserPrincipal;
import com.xilespa.support.IntegrationTest;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
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
 * API nhân viên trên PostgreSQL thật. Nhân viên không xóa được, nên mỗi test đặt tên có tiền tố
 * ngẫu nhiên và tìm theo tiền tố đó.
 */
@IntegrationTest
class StaffControllerIT {

    private static final String TEST_USER_EMAIL = "staff_it@xilespa.vn";

    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired Clock clock;

    private final String prefix = "it" + UUID.randomUUID().toString().substring(0, 6) + " ";
    private RequestPostProcessor loggedIn;

    @BeforeEach
    void setUp() {
        // User riêng của test này, vai trò STAFF để không ảnh hưởng việc khởi tạo chủ tiệm.
        AppUser user =
                userRepository
                        .findByEmailIgnoreCase(TEST_USER_EMAIL)
                        .orElseGet(
                                () ->
                                        userRepository.save(
                                                new AppUser(
                                                        TEST_USER_EMAIL,
                                                        "hash",
                                                        "Staff IT",
                                                        UserRole.STAFF,
                                                        clock.instant())));
        UserPrincipal principal = UserPrincipal.of(user.getId(), TEST_USER_EMAIL, "IT", "STAFF");
        loggedIn =
                authentication(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()));
    }

    @Test
    void createReturns201AndAuditLog() throws Exception {
        String response =
                mockMvc.perform(send(post("/api/staff"), json(prefix + "Lan", "THERAPIST")))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andExpect(jsonPath("$.fullName").value(prefix + "Lan"))
                        .andExpect(jsonPath("$.role").value("THERAPIST"))
                        .andExpect(jsonPath("$.active").value(true))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        long id = ((Number) JsonPath.read(response, "$.id")).longValue();
        assertThat(auditActions(id)).containsExactly("CREATE");
    }

    @Test
    void activeNamesDifferingOnlyByCaseOrSpacesAreDuplicates() throws Exception {
        create(prefix + "Lan Anh", "THERAPIST");

        mockMvc.perform(send(post("/api/staff"), json(prefix + " LAN   anh ", "OWNER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STAFF_NAME_DUPLICATE"))
                .andExpect(jsonPath("$.message").value("Đã có nhân viên đang làm trùng tên."));
    }

    @Test
    void nameOfStaffWhoLeftCanBeReusedButNotReactivatedTwice() throws Exception {
        long left = create(prefix + "Hoa", "THERAPIST");
        mockMvc.perform(post("/api/staff/" + left + "/deactivate").with(loggedIn))
                .andExpect(status().isOk());

        // Người đã nghỉ không chặn tên: thêm người mới cùng tên được
        create(prefix + "Hoa", "THERAPIST");

        // Nhưng cho người cũ làm lại thì trùng với người đang làm
        mockMvc.perform(post("/api/staff/" + left + "/activate").with(loggedIn))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STAFF_NAME_DUPLICATE"));
    }

    @Test
    void invalidInputReturnsFieldErrors() throws Exception {
        mockMvc.perform(send(post("/api/staff"), "{\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'fullName')]").exists())
                .andExpect(
                        jsonPath("$.fieldErrors[?(@.field == 'role')].message")
                                .value("Vui lòng chọn vai trò"));
    }

    @Test
    void unknownRoleIsBadRequest() throws Exception {
        mockMvc.perform(send(post("/api/staff"), json(prefix + "X", "MANAGER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateChangesNameAndRoleAndWritesAudit() throws Exception {
        long id = create(prefix + "Mai", "THERAPIST");

        mockMvc.perform(send(put("/api/staff/" + id), json(prefix + "Mai B", "OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value(prefix + "Mai B"))
                .andExpect(jsonPath("$.role").value("OWNER"));

        assertThat(auditActions(id)).containsExactly("UPDATE", "CREATE");
    }

    @Test
    void renamingToAnotherActiveStaffsNameIsDuplicate() throws Exception {
        create(prefix + "A", "THERAPIST");
        long b = create(prefix + "B", "THERAPIST");

        mockMvc.perform(send(put("/api/staff/" + b), json(prefix + "a", "THERAPIST")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STAFF_NAME_DUPLICATE"));
    }

    @Test
    void deactivateThenActivate() throws Exception {
        long id = create(prefix + "Thu", "THERAPIST");

        mockMvc.perform(post("/api/staff/" + id + "/deactivate").with(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(post("/api/staff/" + id + "/deactivate").with(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(post("/api/staff/" + id + "/activate").with(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        assertThat(auditActions(id)).containsExactly("ACTIVATE", "DEACTIVATE", "CREATE");
    }

    @Test
    void searchFiltersByNameRoleAndStatus() throws Exception {
        create(prefix + "Ngọc", "THERAPIST");
        create(prefix + "Chủ", "OWNER");
        long left = create(prefix + "Nga", "THERAPIST");
        mockMvc.perform(post("/api/staff/" + left + "/deactivate").with(loggedIn));

        mockMvc.perform(get("/api/staff").with(loggedIn).param("q", prefix.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
        mockMvc.perform(
                        get("/api/staff")
                                .with(loggedIn)
                                .param("q", prefix + "ng")
                                .param("active", "true"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value(prefix + "Ngọc"));
        mockMvc.perform(get("/api/staff").with(loggedIn).param("q", prefix).param("role", "OWNER"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].role").value("OWNER"));
        // Người đang làm xếp trước người đã nghỉ
        mockMvc.perform(get("/api/staff").with(loggedIn).param("q", prefix))
                .andExpect(jsonPath("$.content[2].active").value(false));
    }

    @Test
    void listWithoutSearchTermWorks() throws Exception {
        create(prefix + "Bất kỳ", "THERAPIST");

        mockMvc.perform(get("/api/staff").with(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").isNumber());
        mockMvc.perform(get("/api/staff").with(loggedIn).param("active", "false"))
                .andExpect(status().isOk());
    }

    @Test
    void getUnknownStaffIs404() throws Exception {
        mockMvc.perform(get("/api/staff/999999").with(loggedIn))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void deleteIsNotSupported() throws Exception {
        long id = create(prefix + "Không xóa", "THERAPIST");

        mockMvc.perform(delete("/api/staff/" + id).with(loggedIn))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(get("/api/staff")).andExpect(status().isUnauthorized());
    }

    private long create(String fullName, String role) throws Exception {
        String response =
                mockMvc.perform(send(post("/api/staff"), json(fullName, role)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private List<String> auditActions(long id) {
        return auditLogRepository
                .findByEntityTypeAndEntityId(
                        "staff",
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

    private static String json(String fullName, String role) {
        return """
                {"fullName": "%s", "role": "%s"}
                """
                .formatted(fullName, role);
    }
}
