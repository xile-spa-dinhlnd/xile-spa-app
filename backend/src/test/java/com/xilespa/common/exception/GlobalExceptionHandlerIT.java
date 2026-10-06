package com.xilespa.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xilespa.support.IntegrationTest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@IntegrationTest
@Import(GlobalExceptionHandlerIT.TestController.class)
@WithMockUser
class GlobalExceptionHandlerIT {

    @Autowired MockMvc mockMvc;

    @Test
    void validationErrorListsEachField() throws Exception {
        mockMvc.perform(
                        post("/test/errors/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"\", \"price\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(
                        jsonPath("$.fieldErrors[?(@.field == 'name')].message")
                                .value("Tên không được để trống"))
                .andExpect(
                        jsonPath("$.fieldErrors[?(@.field == 'price')].message")
                                .value("Giá không được âm"));
    }

    @Test
    void unreadableBodyIsBadRequest() throws Exception {
        mockMvc.perform(
                        post("/test/errors/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void businessExceptionUsesItsCodeAndMessage() throws Exception {
        mockMvc.perform(get("/test/errors/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy dịch vụ"));
    }

    @Test
    void unknownPathIsNotFound() throws Exception {
        mockMvc.perform(get("/api/khong-ton-tai"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void unexpectedErrorDoesNotLeakDetails() throws Exception {
        mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Hệ thống gặp lỗi, vui lòng thử lại sau."));
    }

    record SampleRequest(
            @NotBlank(message = "Tên không được để trống") String name,
            @PositiveOrZero(message = "Giá không được âm") long price) {}

    @TestConfiguration
    @RestController
    static class TestController {

        @PostMapping("/test/errors/validate")
        void validate(@Valid @RequestBody SampleRequest request) {}

        @GetMapping("/test/errors/business")
        void business() {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy dịch vụ");
        }

        @GetMapping("/test/errors/unexpected")
        void unexpected() {
            throw new IllegalStateException("chi tiết nội bộ không được lộ ra");
        }
    }
}
