package com.xilespa.security;

import com.xilespa.common.exception.ApiErrorFactory;
import com.xilespa.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Trả 401/403 theo định dạng {@code ApiError}. Lỗi ở tầng security xảy ra trước controller nên
 * không đi qua {@code GlobalExceptionHandler}.
 */
@Component
class ApiErrorResponseWriter {

    private final ApiErrorFactory errors;
    private final ObjectMapper objectMapper;

    ApiErrorResponseWriter(ApiErrorFactory errors, ObjectMapper objectMapper) {
        this.errors = errors;
        this.objectMapper = objectMapper;
    }

    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> write(request, response, ErrorCode.UNAUTHORIZED);
    }

    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> write(request, response, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code)
            throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getOutputStream(), errors.create(code, request.getRequestURI()));
    }
}
