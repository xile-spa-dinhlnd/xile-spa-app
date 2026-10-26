package com.xilespa.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.ClassUtils;
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
import tools.jackson.databind.exc.MismatchedInputException;

/** Chuyển mọi exception thành {@link ApiError}. Không để lộ chi tiết kỹ thuật ra ngoài. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ApiErrorFactory errors;

    public GlobalExceptionHandler(ApiErrorFactory errors) {
        this.errors = errors;
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return respond(ex.getErrorCode(), ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(e -> new ApiError.FieldError(e.getField(), e.getDefaultMessage()))
                        .toList();
        return respond(
                ErrorCode.VALIDATION_FAILED,
                ErrorCode.VALIDATION_FAILED.defaultMessage(),
                fieldErrors,
                request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.debug("Lỗi đọc JSON body: {}", ex.getMessage());
        MismatchedInputException mismatch = findMismatchedInputException(ex);
        if (mismatch != null && mismatch.getPath() != null && !mismatch.getPath().isEmpty()) {
            String field = buildFieldPath(mismatch.getPath());
            String message = determineTypeMismatchMessage(mismatch.getTargetType());
            return respond(
                    ErrorCode.VALIDATION_FAILED,
                    ErrorCode.VALIDATION_FAILED.defaultMessage(),
                    List.of(new ApiError.FieldError(field, message)),
                    request);
        }
        return respond(ErrorCode.BAD_REQUEST, request);
    }

    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
        return respond(ErrorCode.BAD_REQUEST, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNotFound(HttpServletRequest request) {
        return respond(ErrorCode.NOT_FOUND, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> handleMethodNotAllowed(HttpServletRequest request) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> handleUnsupportedMediaType(HttpServletRequest request) {
        return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ApiError> handleNotAcceptable(HttpServletRequest request) {
        return respond(ErrorCode.NOT_ACCEPTABLE, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(HttpServletRequest request) {
        return respond(ErrorCode.FORBIDDEN, request);
    }

    /**
     * Lỗi từ cơ sở dữ liệu. Các mã do trigger ném ra (XL001, XL002, XL004) là lỗi nghiệp vụ có chủ
     * đích, trả thông báo tiếng Việt; các lỗi còn lại coi là lỗi hệ thống.
     */
    @ExceptionHandler({DataAccessException.class, TransactionSystemException.class})
    ResponseEntity<ApiError> handleDatabase(Exception ex, HttpServletRequest request) {
        return DatabaseErrors.translate(ex)
                .map(
                        code -> {
                            log.info(
                                    "Từ chối bởi ràng buộc dữ liệu {} tại {} {}",
                                    code,
                                    request.getMethod(),
                                    request.getRequestURI());
                            return respond(code, request);
                        })
                .orElseGet(() -> handleUnexpected(ex, request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Lỗi không mong đợi tại {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(ErrorCode.INTERNAL_ERROR, request);
    }

    private ResponseEntity<ApiError> respond(ErrorCode code, HttpServletRequest request) {
        return respond(code, code.defaultMessage(), List.of(), request);
    }

    private ResponseEntity<ApiError> respond(
            ErrorCode code,
            String message,
            List<ApiError.FieldError> fieldErrors,
            HttpServletRequest request) {
        return ResponseEntity.status(code.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errors.create(code, message, fieldErrors, request.getRequestURI()));
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
        if (targetType != null) {
            Class<?> resolved = ClassUtils.resolvePrimitiveIfNecessary(targetType);
            if (Number.class.isAssignableFrom(resolved)) {
                return "Phải là số nguyên";
            }
            if (Enum.class.isAssignableFrom(resolved)) {
                return "Giá trị không hợp lệ";
            }
        }
        return "Sai kiểu dữ liệu";
    }
}
