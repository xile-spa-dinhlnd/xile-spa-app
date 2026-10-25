package com.xilespa.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;

/** Chuyển mọi exception thành {@link ApiError}. Không để lộ chi tiết kỹ thuật ra ngoài. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ApiErrorFactory errors;
    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ApiErrorFactory errors, ObjectMapper objectMapper) {
        this.errors = errors;
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(BusinessException.class)
    void handleBusiness(
            BusinessException ex, HttpServletRequest request, HttpServletResponse response) {
        respond(ex.getErrorCode(), ex.getMessage(), List.of(), request, response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    void handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request,
            HttpServletResponse response) {
        List<ApiError.FieldError> fieldErrors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(e -> new ApiError.FieldError(e.getField(), e.getDefaultMessage()))
                        .toList();
        respond(
                ErrorCode.VALIDATION_FAILED,
                ErrorCode.VALIDATION_FAILED.defaultMessage(),
                fieldErrors,
                request,
                response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    void handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.debug("Lỗi đọc JSON body: {}", ex.getMessage());
        MismatchedInputException mismatch = findMismatchedInputException(ex);
        if (mismatch != null && mismatch.getPath() != null && !mismatch.getPath().isEmpty()) {
            String field = buildFieldPath(mismatch.getPath());
            String message = determineTypeMismatchMessage(mismatch.getTargetType());
            respond(
                    ErrorCode.VALIDATION_FAILED,
                    ErrorCode.VALIDATION_FAILED.defaultMessage(),
                    List.of(new ApiError.FieldError(field, message)),
                    request,
                    response);
            return;
        }
        respond(ErrorCode.BAD_REQUEST, request, response);
    }

    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    void handleBadRequest(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        respond(ErrorCode.BAD_REQUEST, request, response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    void handleNotFound(HttpServletRequest request, HttpServletResponse response) {
        respond(ErrorCode.NOT_FOUND, request, response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    void handleMethodNotAllowed(HttpServletRequest request, HttpServletResponse response) {
        respond(ErrorCode.METHOD_NOT_ALLOWED, request, response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    void handleUnsupportedMediaType(HttpServletRequest request, HttpServletResponse response) {
        respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, request, response);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    void handleNotAcceptable(HttpServletRequest request, HttpServletResponse response) {
        respond(ErrorCode.NOT_ACCEPTABLE, request, response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    void handleAccessDenied(HttpServletRequest request, HttpServletResponse response) {
        respond(ErrorCode.FORBIDDEN, request, response);
    }

    /**
     * Lỗi từ cơ sở dữ liệu. Các mã do trigger ném ra (XL001, XL002, XL004) là lỗi nghiệp vụ có chủ
     * đích, trả thông báo tiếng Việt; các lỗi còn lại coi là lỗi hệ thống.
     */
    @ExceptionHandler({DataAccessException.class, TransactionSystemException.class})
    void handleDatabase(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        DatabaseErrors.translate(ex)
                .ifPresentOrElse(
                        code -> {
                            log.info(
                                    "Từ chối bởi ràng buộc dữ liệu {} tại {} {}",
                                    code,
                                    request.getMethod(),
                                    request.getRequestURI());
                            respond(code, request, response);
                        },
                        () -> handleUnexpected(ex, request, response));
    }

    @ExceptionHandler(Exception.class)
    void handleUnexpected(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        log.error("Lỗi không mong đợi tại {} {}", request.getMethod(), request.getRequestURI(), ex);
        respond(ErrorCode.INTERNAL_ERROR, request, response);
    }

    private void respond(ErrorCode code, HttpServletRequest request, HttpServletResponse response) {
        respond(code, code.defaultMessage(), List.of(), request, response);
    }

    private void respond(
            ErrorCode code,
            String message,
            List<ApiError.FieldError> fieldErrors,
            HttpServletRequest request,
            HttpServletResponse response) {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            objectMapper.writeValue(
                    response.getOutputStream(),
                    errors.create(code, message, fieldErrors, request.getRequestURI()));
        } catch (IOException e) {
            log.error("Không thể ghi lỗi ApiError ra response", e);
        }
    }

    private MismatchedInputException findMismatchedInputException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof MismatchedInputException mismatched) {
                return mismatched;
            }
            current = current.getCause();
        }
        return null;
    }

    private String buildFieldPath(List<JacksonException.Reference> path) {
        StringBuilder sb = new StringBuilder();
        for (JacksonException.Reference ref : path) {
            if (ref.getPropertyName() != null) {
                if (!sb.isEmpty()) {
                    sb.append(".");
                }
                sb.append(ref.getPropertyName());
            } else if (ref.getIndex() >= 0) {
                sb.append("[").append(ref.getIndex()).append("]");
            }
        }
        return sb.toString();
    }

    private String determineTypeMismatchMessage(Class<?> targetType) {
        if (targetType != null
                && (Number.class.isAssignableFrom(targetType)
                        || targetType == long.class
                        || targetType == int.class
                        || targetType == short.class
                        || targetType == byte.class
                        || targetType == double.class
                        || targetType == float.class)) {
            return "Phải là số nguyên";
        }
        if (targetType != null
                && (targetType.isEnum() || Enum.class.isAssignableFrom(targetType))) {
            return "Giá trị không hợp lệ";
        }
        return "Sai kiểu dữ liệu";
    }
}
