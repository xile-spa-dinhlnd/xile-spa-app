package com.xilespa.module.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.xilespa.module.catalog.entity.ServiceGroup;
import com.xilespa.module.catalog.entity.ServiceItem;
import com.xilespa.module.catalog.repository.ComboRepository;
import com.xilespa.module.catalog.repository.ServiceGroupRepository;
import com.xilespa.module.catalog.repository.ServiceItemRepository;
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
 * Test tích hợp API combo trên PostgreSQL thật (FR-CMB-01 đến 03, BR-01, BR-18, BR-19). Dữ liệu
 * test có tiền tố ngẫu nhiên để không ảnh hưởng test khác.
 */
@IntegrationTest
class ComboControllerIT {

    private static final String TEST_USER_EMAIL = "combo_it@xilespa.vn";

    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired ServiceGroupRepository groupRepository;
    @Autowired ServiceItemRepository serviceItemRepository;
    @Autowired ComboRepository comboRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired Clock clock;

    private final String prefix = "cb" + UUID.randomUUID().toString().substring(0, 6) + " ";
    private RequestPostProcessor loggedIn;
    private ServiceItem service1;
    private ServiceItem service2;

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
                                                        "Combo IT",
                                                        UserRole.STAFF,
                                                        clock.instant())));
        UserPrincipal principal = UserPrincipal.of(user.getId(), TEST_USER_EMAIL, "IT", "STAFF");
        loggedIn =
                authentication(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()));

        var groups = groupRepository.findByActiveOrderBySortOrderAscNameAsc(true);
        ServiceGroup group = groups.get(0);

        service1 =
                serviceItemRepository.save(
                        new ServiceItem(
                                group,
                                prefix + "Dịch vụ 1",
                                30,
                                100_000L,
                                10_000L,
                                clock.instant()));
        service2 =
                serviceItemRepository.save(
                        new ServiceItem(
                                group,
                                prefix + "Dịch vụ 2",
                                45,
                                200_000L,
                                20_000L,
                                clock.instant()));
    }

    // =========================================================================
    // 1. Tính thử (quote)
    // =========================================================================

    @Test
    void quoteReturnsTotalsWithoutSaving() throws Exception {
        String items =
                """
                [
                    {"serviceId": %d, "quantity": 1},
                    {"serviceId": %d, "quantity": 1}
                ]
                """
                        .formatted(service1.getId(), service2.getId());
        String body = quoteJson(250_000L, items);

        mockMvc.perform(send(post("/api/catalog/combos/quote"), body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retailTotal").value(300_000))
                .andExpect(jsonPath("$.savings").value(50_000))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(100_000))
                .andExpect(jsonPath("$.items[1].lineTotal").value(200_000));

        // Không lưu combo nào
        assertThat(comboRepository.findAll().stream().anyMatch(c -> c.getName().startsWith(prefix)))
                .isFalse();
    }

    @Test
    void quoteWithInvalidItemReturnsFieldErrors() throws Exception {
        // Số lượng 0, thiếu serviceId
        String items =
                """
                [
                    {"serviceId": %d, "quantity": 0},
                    {"quantity": 1}
                ]
                """
                        .formatted(service1.getId());
        String body = quoteJson(100_000L, items);

        mockMvc.perform(send(post("/api/catalog/combos/quote"), body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'items[0].quantity')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'items[1].serviceId')]").exists());

        // 21 dòng (vượt quá 20)
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 21; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"serviceId\": 1, \"quantity\": 1}");
        }
        sb.append("]");
        mockMvc.perform(send(post("/api/catalog/combos/quote"), quoteJson(100_000L, sb.toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'items')]").exists());
    }

    @Test
    void quoteWithDuplicateServiceIsRejected() throws Exception {
        String items =
                """
                [
                    {"serviceId": %d, "quantity": 1},
                    {"serviceId": %d, "quantity": 2}
                ]
                """
                        .formatted(service1.getId(), service1.getId());

        mockMvc.perform(send(post("/api/catalog/combos/quote"), quoteJson(100_000L, items)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMBO_ITEM_DUPLICATE"))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Mỗi dịch vụ chỉ được có một dòng trong combo, hãy tăng số"
                                                + " lượng."));
    }

    @Test
    void quoteWithUnknownServiceIsRejected() throws Exception {
        String items =
                """
                [
                    {"serviceId": 999999, "quantity": 1}
                ]
                """;

        mockMvc.perform(send(post("/api/catalog/combos/quote"), quoteJson(100_000L, items)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMBO_SERVICE_UNAVAILABLE"));
    }

    // =========================================================================
    // 2. Tạo combo (create)
    // =========================================================================

    @Test
    void createReturns201AndAuditLog() throws Exception {
        String items =
                """
                [
                    {"serviceId": %d, "quantity": 2},
                    {"serviceId": %d, "quantity": 1}
                ]
                """
                        .formatted(service1.getId(), service2.getId());
        String body = comboJson(prefix + "Combo Vip", 350_000L, 25_000L, items);

        String response =
                mockMvc.perform(send(post("/api/catalog/combos"), body))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andExpect(jsonPath("$.name").value(prefix + "Combo Vip"))
                        .andExpect(jsonPath("$.price").value(350_000))
                        .andExpect(jsonPath("$.tourFee").value(25_000))
                        .andExpect(jsonPath("$.active").value(true))
                        .andExpect(jsonPath("$.retailTotal").value(400_000))
                        .andExpect(jsonPath("$.savings").value(50_000))
                        .andExpect(jsonPath("$.items.length()").value(2))
                        .andExpect(jsonPath("$.items[0].lineTotal").value(200_000))
                        .andExpect(jsonPath("$.items[1].lineTotal").value(200_000))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        Number id = JsonPath.read(response, "$.id");
        var logs =
                auditLogRepository.findByEntityTypeAndEntityId(
                        "combo", id.toString(), PageRequest.of(0, 10));
        assertThat(logs.getContent()).hasSize(1);
        assertThat(logs.getContent().getFirst().getActorId()).isNotNull();
        assertThat(logs.getContent().getFirst().getAction().name()).isEqualTo("CREATE");
    }

    @Test
    void invalidInputReturnsFieldErrors() throws Exception {
        // Tên trống, giá âm, items rỗng
        mockMvc.perform(send(post("/api/catalog/combos"), comboJson("", -1L, -5L, "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'price')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'tourFee')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'items')]").exists());

        // Tên quá 150 ký tự
        String longName = "A".repeat(151);
        String validItem = "[{\"serviceId\": " + service1.getId() + ", \"quantity\": 1}]";
        mockMvc.perform(
                        send(
                                post("/api/catalog/combos"),
                                comboJson(longName, 100_000L, 0L, validItem)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());

        // Tên chỉ có khoảng trắng
        mockMvc.perform(
                        send(
                                post("/api/catalog/combos"),
                                comboJson("   ", 100_000L, 0L, validItem)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());

        // Giá trống
        mockMvc.perform(
                        send(
                                post("/api/catalog/combos"),
                                comboJson("Combo Giá Trống", null, 0L, validItem)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'price')]").exists());

        // Số lượng 0
        String qtyZero = "[{\"serviceId\": " + service1.getId() + ", \"quantity\": 0}]";
        mockMvc.perform(
                        send(
                                post("/api/catalog/combos"),
                                comboJson("Combo Qty 0", 100_000L, 0L, qtyZero)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'items[0].quantity')]").exists());

        // Số lượng 100
        String qtyHundred = "[{\"serviceId\": " + service1.getId() + ", \"quantity\": 100}]";
        mockMvc.perform(
                        send(
                                post("/api/catalog/combos"),
                                comboJson("Combo Qty 100", 100_000L, 0L, qtyHundred)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'items[0].quantity')]").exists());

        // Thiếu serviceId
        String missingServiceId = "[{\"quantity\": 1}]";
        mockMvc.perform(
                        send(
                                post("/api/catalog/combos"),
                                comboJson("Combo Thiếu Svc", 100_000L, 0L, missingServiceId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'items[0].serviceId')]").exists());
    }

    @Test
    void namesDifferingOnlyByCaseOrSpacesAreDuplicates() throws Exception {
        String items = "[{\"serviceId\": " + service1.getId() + ", \"quantity\": 1}]";
        createCombo(prefix + "COMBO THƯ GIÃN", 100_000L, items);

        mockMvc.perform(
                        send(
                                post("/api/catalog/combos"),
                                comboJson(prefix + "  combo   thư   giãn  ", 100_000L, 0L, items)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMBO_NAME_DUPLICATE"))
                .andExpect(jsonPath("$.message").value("Tên combo đã tồn tại."));
    }

    @Test
    void createWithDuplicateNameIsRejected() throws Exception {
        String items = "[{\"serviceId\": " + service1.getId() + ", \"quantity\": 1}]";
        createCombo(prefix + "Combo Trùng", 100_000L, items);

        mockMvc.perform(
                        send(
                                post("/api/catalog/combos"),
                                comboJson(prefix + "Combo Trùng", 120_000L, 0L, items)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMBO_NAME_DUPLICATE"));
    }

    @Test
    void createWithDuplicateServiceIsRejected() throws Exception {
        String items =
                """
                [
                    {"serviceId": %d, "quantity": 1},
                    {"serviceId": %d, "quantity": 1}
                ]
                """
                        .formatted(service1.getId(), service1.getId());

        mockMvc.perform(
                        send(
                                post("/api/catalog/combos"),
                                comboJson(prefix + "Combo Lặp Svc", 100_000L, 0L, items)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMBO_ITEM_DUPLICATE"));
    }

    @Test
    void createWithInactiveServiceIsRejected() throws Exception {
        service2.deactivate(clock.instant());
        serviceItemRepository.save(service2);

        String items =
                """
                [
                    {"serviceId": %d, "quantity": 1},
                    {"serviceId": %d, "quantity": 1}
                ]
                """
                        .formatted(service1.getId(), service2.getId());

        mockMvc.perform(
                        send(
                                post("/api/catalog/combos"),
                                comboJson(prefix + "Combo Svc Ngừng", 200_000L, 0L, items)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMBO_SERVICE_UNAVAILABLE"))
                .andExpect(
                        jsonPath("$.message")
                                .value("Dịch vụ đã ngừng bán: " + service2.getName() + "."));
    }

    @Test
    void createWithUnknownServiceIsRejected() throws Exception {
        String items = "[{\"serviceId\": 999999, \"quantity\": 1}]";

        mockMvc.perform(
                        send(
                                post("/api/catalog/combos"),
                                comboJson(prefix + "Combo Lạ", 100_000L, 0L, items)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMBO_SERVICE_UNAVAILABLE"));
    }

    // =========================================================================
    // 3. Xem một combo (get)
    // =========================================================================

    @Test
    void getReturnsComboWithItems() throws Exception {
        String items =
                """
                [
                    {"serviceId": %d, "quantity": 2},
                    {"serviceId": %d, "quantity": 1}
                ]
                """
                        .formatted(service1.getId(), service2.getId());
        long comboId = createCombo(prefix + "Combo Xem", 320_000L, items);

        mockMvc.perform(send(get("/api/catalog/combos/" + comboId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(comboId))
                .andExpect(jsonPath("$.name").value(prefix + "Combo Xem"))
                .andExpect(jsonPath("$.price").value(320_000))
                .andExpect(jsonPath("$.retailTotal").value(400_000))
                .andExpect(jsonPath("$.savings").value(80_000))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].serviceId").value(service1.getId()))
                .andExpect(jsonPath("$.items[0].serviceName").value(service1.getName()))
                .andExpect(jsonPath("$.items[0].serviceActive").value(true))
                .andExpect(jsonPath("$.items[0].listPrice").value(service1.getListPrice()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(200_000))
                .andExpect(jsonPath("$.items[1].serviceId").value(service2.getId()))
                .andExpect(jsonPath("$.items[1].serviceName").value(service2.getName()))
                .andExpect(jsonPath("$.items[1].serviceActive").value(true))
                .andExpect(jsonPath("$.items[1].listPrice").value(service2.getListPrice()))
                .andExpect(jsonPath("$.items[1].quantity").value(1))
                .andExpect(jsonPath("$.items[1].lineTotal").value(200_000));
    }

    @Test
    void getShowsCurrentServicePriceAfterPriceChange() throws Exception {
        String items = "[{\"serviceId\": " + service1.getId() + ", \"quantity\": 1}]";
        long comboId = createCombo(prefix + "Combo Đổi Giá Svc", 80_000L, items);

        // Ban đầu service1 listPrice = 100.000 -> retailTotal = 100.000, savings = 20.000
        mockMvc.perform(send(get("/api/catalog/combos/" + comboId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retailTotal").value(100_000))
                .andExpect(jsonPath("$.savings").value(20_000));

        // Đổi giá service1 lên 160.000 (giá combo 80.000 giữ nguyên)
        service1.changePrice(160_000L, 1L, clock.instant());
        serviceItemRepository.save(service1);

        // retailTotal cập nhật theo giá mới: 160.000, savings = 80.000
        mockMvc.perform(send(get("/api/catalog/combos/" + comboId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retailTotal").value(160_000))
                .andExpect(jsonPath("$.savings").value(80_000))
                .andExpect(jsonPath("$.price").value(80_000));
    }

    @Test
    void getUnknownComboIs404() throws Exception {
        mockMvc.perform(send(get("/api/catalog/combos/999999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy combo."));
    }

    // =========================================================================
    // 4. Quyền & không xóa cứng
    // =========================================================================

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(get("/api/catalog/combos/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/api/catalog/combos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/api/catalog/combos/quote")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteIsNotSupported() throws Exception {
        mockMvc.perform(send(delete("/api/catalog/combos/1")))
                .andExpect(status().isMethodNotAllowed());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private long createCombo(String name, long price, String itemsJson) throws Exception {
        String body = comboJson(name, price, 0L, itemsJson);
        String response =
                mockMvc.perform(send(post("/api/catalog/combos"), body))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private MockHttpServletRequestBuilder send(
            MockHttpServletRequestBuilder builder, String content) {
        return builder.with(loggedIn).contentType(MediaType.APPLICATION_JSON).content(content);
    }

    private MockHttpServletRequestBuilder send(MockHttpServletRequestBuilder builder) {
        return builder.with(loggedIn);
    }

    private static String comboJson(String name, Long price, Long tourFee, String itemsJson) {
        return """
                {
                    "name": %s,
                    "price": %s,
                    "tourFee": %s,
                    "items": %s
                }
                """
                .formatted(
                        name == null ? "null" : "\"" + name + "\"",
                        price == null ? "null" : price,
                        tourFee == null ? "null" : tourFee,
                        itemsJson);
    }

    private static String quoteJson(Long price, String itemsJson) {
        return """
                {
                    "price": %s,
                    "items": %s
                }
                """
                .formatted(price == null ? "null" : price, itemsJson);
    }
}
