package com.xilespa.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.xilespa.common.audit.AuditLogRepository;
import com.xilespa.module.auth.entity.AppUser;
import com.xilespa.module.auth.enums.UserRole;
import com.xilespa.module.auth.repository.AppUserRepository;
import com.xilespa.module.catalog.entity.ServiceItem;
import com.xilespa.module.catalog.repository.ServiceGroupRepository;
import com.xilespa.module.catalog.repository.ServiceItemRepository;
import com.xilespa.security.UserPrincipal;
import com.xilespa.support.IntegrationTest;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Kiểm tra các ca biên và chặn đầu vào bất thường theo thiết kế S3-08. Bao gồm kiểm tra kiểu dữ
 * liệu chặt, định dạng body, mã hóa dữ liệu và xử lý lỗi luôn trả về JSON.
 */
@IntegrationTest
class InputHandlingIT {

    private static final String TEST_USER_EMAIL = "input_hardening_it@xilespa.vn";

    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired ServiceGroupRepository groupRepository;
    @Autowired ServiceItemRepository serviceItemRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired Clock clock;

    private final String prefix = "ih" + UUID.randomUUID().toString().substring(0, 6) + " ";
    private RequestPostProcessor loggedIn;
    private long groupId;

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
                                                        "Input Hardening IT",
                                                        UserRole.STAFF,
                                                        clock.instant())));
        UserPrincipal principal = UserPrincipal.of(user.getId(), TEST_USER_EMAIL, "IT", "STAFF");
        loggedIn =
                authentication(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()));

        var groups = groupRepository.findByActiveOrderBySortOrderAscNameAsc(true);
        groupId = groups.get(0).getId();
    }

    // --- 1. Kiểu chặt (#1, #7) ---

    @Test
    void integerPriceIsAccepted() throws Exception {
        String body =
                """
                {
                  "name": "%sDịch vụ giá chuẩn",
                  "groupId": %d,
                  "durationMinutes": 30,
                  "listPrice": 100000,
                  "tourFee": 10000
                }
                """
                        .formatted(prefix, groupId);

        mockMvc.perform(
                        post("/api/catalog/services")
                                .with(loggedIn)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listPrice").value(100000));
    }

    @Test
    void decimalPriceIsRejectedOnPriceField() throws Exception {
        String body =
                """
                {
                  "name": "%sDịch vụ giá thập phân",
                  "groupId": %d,
                  "durationMinutes": 30,
                  "listPrice": 1.7,
                  "tourFee": 10000
                }
                """
                        .formatted(prefix, groupId);

        mockMvc.perform(
                        post("/api/catalog/services")
                                .with(loggedIn)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("listPrice"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Phải là số nguyên"));
    }

    @Test
    void stringNumberIsRejected() throws Exception {
        String body =
                """
                {
                  "name": "%sDịch vụ giá chuỗi",
                  "groupId": %d,
                  "durationMinutes": 30,
                  "listPrice": "100000",
                  "tourFee": 10000
                }
                """
                        .formatted(prefix, groupId);

        mockMvc.perform(
                        post("/api/catalog/services")
                                .with(loggedIn)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("listPrice"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Phải là số nguyên"));
    }

    @Test
    void exponentNumberIsRejected() throws Exception {
        String body =
                """
                {
                  "name": "%sDịch vụ giá mũ",
                  "groupId": %d,
                  "durationMinutes": 30,
                  "listPrice": 1e5,
                  "tourFee": 10000
                }
                """
                        .formatted(prefix, groupId);

        mockMvc.perform(
                        post("/api/catalog/services")
                                .with(loggedIn)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("listPrice"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Phải là số nguyên"));
    }

    @Test
    void unknownEnumIsRejectedOnRoleField() throws Exception {
        String body =
                """
                {
                  "fullName": "%sNhân viên test",
                  "role": "ADMIN"
                }
                """
                        .formatted(prefix);

        mockMvc.perform(
                        post("/api/staff")
                                .with(loggedIn)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("role"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Giá trị không hợp lệ"));
    }

    @Test
    void decimalQuantityInNestedListPointsToItemField() throws Exception {
        String body =
                """
                {
                  "price": 100000,
                  "items": [
                    {
                      "serviceId": 1,
                      "quantity": 2.9
                    }
                  ]
                }
                """;

        mockMvc.perform(
                        post("/api/catalog/combos/quote")
                                .with(loggedIn)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("items[0].quantity"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Phải là số nguyên"));
    }

    @Test
    void typeErrorDoesNotLeakJacksonDetails() throws Exception {
        String body =
                """
                {
                  "name": "%sDịch vụ kiểm tra rò rỉ",
                  "groupId": %d,
                  "durationMinutes": 30,
                  "listPrice": "khong_phai_so",
                  "tourFee": 10000
                }
                """
                        .formatted(prefix, groupId);

        String response =
                mockMvc.perform(
                                post("/api/catalog/services")
                                        .with(loggedIn)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                        .andExpect(
                                jsonPath("$.message")
                                        .value("Dữ liệu không hợp lệ, vui lòng kiểm tra lại."))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response).doesNotContain("tools.jackson");
        assertThat(response).doesNotContain("com.fasterxml.jackson");
        assertThat(response).doesNotContain("MismatchedInputException");
        assertThat(response).doesNotContain("Cannot deserialize");
    }

    @Test
    void decimalPriceCreatesNothing() throws Exception {
        String name = prefix + "Dịch vụ không được tạo";
        String body =
                """
                {
                  "name": "%s",
                  "groupId": %d,
                  "durationMinutes": 30,
                  "listPrice": 1.7,
                  "tourFee": 10000
                }
                """
                        .formatted(name, groupId);

        mockMvc.perform(
                        post("/api/catalog/services")
                                .with(loggedIn)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());

        assertThat(serviceItemRepository.existsByNormalizedName(name, 0L)).isFalse();
        assertThat(auditLogRepository.findByEntityType("service", Pageable.unpaged()).getContent())
                .noneMatch(a -> a.getAfterData() != null && a.getAfterData().contains(name));
    }

    // --- 2. Body, định dạng (#5) ---

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(
                        post("/api/catalog/services")
                                .with(loggedIn)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Yêu cầu không hợp lệ."));
    }

    @Test
    void emptyBodyIsBadRequest() throws Exception {
        mockMvc.perform(
                        post("/api/catalog/services")
                                .with(loggedIn)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Yêu cầu không hợp lệ."));
    }

    @Test
    void textPlainBodyIs415() throws Exception {
        mockMvc.perform(
                        post("/api/catalog/services")
                                .with(loggedIn)
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("name=test"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value("Dữ liệu gửi lên phải ở dạng JSON."));
    }

    @Test
    void missingContentTypeIs415() throws Exception {
        mockMvc.perform(
                        post("/api/catalog/services")
                                .with(loggedIn)
                                .content("{\"name\": \"test\"}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value("Dữ liệu gửi lên phải ở dạng JSON."));
    }

    @Test
    void nonNumericIdIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/catalog/services/abc").with(loggedIn))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void numberBeyondLongIsBadRequest() throws Exception {
        String name = prefix + "Dịch vụ số vượt Long";
        String body =
                """
                {
                  "name": "%s",
                  "groupId": %d,
                  "durationMinutes": 30,
                  "listPrice": 99999999999999999999,
                  "tourFee": 10000
                }
                """
                        .formatted(name, groupId);

        mockMvc.perform(
                        post("/api/catalog/services")
                                .with(loggedIn)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        assertThat(serviceItemRepository.existsByNormalizedName(name, 0L)).isFalse();
    }

    @Test
    void xmlAcceptOnMissingResourceNeverEscapes() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/services/999999")
                                .with(loggedIn)
                                .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", startsWith("application/json")))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void xmlAcceptOnListIs406() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/services")
                                .with(loggedIn)
                                .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(header().string("Content-Type", startsWith("application/json")))
                .andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"))
                .andExpect(jsonPath("$.message").value("Hệ thống chỉ trả dữ liệu dạng JSON."));
    }

    @Test
    void unknownFieldsCannotSetIdOrActive() throws Exception {
        String name = prefix + "Dịch vụ trường lạ";
        String body =
                """
                {
                  "id": 999999,
                  "active": false,
                  "name": "%s",
                  "groupId": %d,
                  "durationMinutes": 30,
                  "listPrice": 100000,
                  "tourFee": 10000
                }
                """
                        .formatted(name, groupId);

        String res =
                mockMvc.perform(
                                post("/api/catalog/services")
                                        .with(loggedIn)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").value(not(999999)))
                        .andExpect(jsonPath("$.active").value(true))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        long createdId = ((Number) JsonPath.read(res, "$.id")).longValue();
        assertThat(createdId).isNotEqualTo(999999L);
        ServiceItem created = serviceItemRepository.findById(createdId).orElseThrow();
        assertThat(created.isActive()).isTrue();
    }
}
