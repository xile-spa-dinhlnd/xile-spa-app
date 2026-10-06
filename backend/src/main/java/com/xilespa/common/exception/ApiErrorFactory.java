package com.xilespa.common.exception;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

/** Tạo {@link ApiError} ở một chỗ, dùng chung cho tầng MVC và tầng security. */
@Component
public class ApiErrorFactory {

    private final Clock clock;

    public ApiErrorFactory(Clock clock) {
        this.clock = clock;
    }

    public ApiError create(ErrorCode code, String path) {
        return create(code, code.defaultMessage(), List.of(), path);
    }

    public ApiError create(
            ErrorCode code, String message, List<ApiError.FieldError> fieldErrors, String path) {
        return new ApiError(code.name(), message, fieldErrors, path, OffsetDateTime.now(clock));
    }
}
