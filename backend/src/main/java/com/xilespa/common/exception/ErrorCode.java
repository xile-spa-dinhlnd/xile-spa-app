package com.xilespa.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Mã lỗi dùng chung cho mọi API. Frontend dựa vào {@code code} để xử lý, còn {@code message} để
 * hiển thị. Lỗi nghiệp vụ của từng module thêm mã mới vào đây.
 */
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ, vui lòng kiểm tra lại."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để tiếp tục."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Thao tác không được hỗ trợ."),
    CONFLICT(HttpStatus.CONFLICT, "Dữ liệu đã thay đổi hoặc bị trùng, vui lòng thử lại."),

    // Lỗi do trigger của cơ sở dữ liệu ném ra (docs/design/erd.md mục 5.5), xem DatabaseErrors.
    DAY_CLOSED(
            HttpStatus.CONFLICT,
            "Ngày này đã chốt sổ nên không thể thêm, sửa hay hủy dữ liệu. Muốn sửa sai, hãy ghi bút"
                    + " toán điều chỉnh."),
    RECORD_IMMUTABLE(
            HttpStatus.CONFLICT,
            "Dữ liệu này không được sửa hoặc xóa. Muốn sửa sai, hãy hủy bản ghi hoặc ghi bút toán"
                    + " điều chỉnh."),
    FUTURE_DAY_CLOSING(HttpStatus.BAD_REQUEST, "Không thể chốt sổ cho ngày trong tương lai."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống gặp lỗi, vui lòng thử lại sau.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
