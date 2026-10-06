package com.xilespa.common.exception;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * Dịch mã SQLSTATE do trigger của cơ sở dữ liệu ném ra thành {@link ErrorCode} (S0-07). Trigger là
 * rào chắn cuối (docs/design/erd.md mục 5.5); service vẫn nên kiểm tra trước để báo lỗi sớm.
 *
 * <ul>
 *   <li>{@code XL001}: ngày đã chốt sổ, không được thêm, sửa, hủy (BR-07).
 *   <li>{@code XL002}: bảng chỉ thêm, hoặc cấm xóa cứng (BR-08, BR-09, BR-18).
 *   <li>{@code XL004}: không chốt sổ ngày tương lai (BR-06).
 * </ul>
 */
public final class DatabaseErrors {

    private static final Map<String, ErrorCode> BY_SQL_STATE =
            Map.of(
                    "XL001", ErrorCode.DAY_CLOSED,
                    "XL002", ErrorCode.RECORD_IMMUTABLE,
                    "XL004", ErrorCode.FUTURE_DAY_CLOSING);

    private DatabaseErrors() {}

    /**
     * Tìm {@link SQLException} trong chuỗi nguyên nhân và dịch mã của nó; rỗng nếu không phải mã
     * XL.
     */
    public static Optional<ErrorCode> translate(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SQLException sql && sql.getSQLState() != null) {
                ErrorCode code = BY_SQL_STATE.get(sql.getSQLState());
                if (code != null) {
                    return Optional.of(code);
                }
            }
        }
        return Optional.empty();
    }
}
