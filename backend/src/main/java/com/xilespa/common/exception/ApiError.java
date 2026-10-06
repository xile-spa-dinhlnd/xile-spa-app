package com.xilespa.common.exception;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Định dạng lỗi chung của mọi API (S0-04). Mô tả cho frontend ở {@code backend/AGENTS.md}.
 *
 * @param code mã lỗi ổn định, xem {@link ErrorCode}
 * @param message thông báo tiếng Việt để hiển thị
 * @param fieldErrors lỗi theo từng ô nhập; rỗng nếu không phải lỗi kiểm tra dữ liệu
 * @param path đường dẫn API gây lỗi
 * @param timestamp thời điểm xảy ra lỗi (giờ Việt Nam)
 */
public record ApiError(
        String code,
        String message,
        List<FieldError> fieldErrors,
        String path,
        OffsetDateTime timestamp) {

    public record FieldError(String field, String message) {}
}
