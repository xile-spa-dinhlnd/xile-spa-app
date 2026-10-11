package com.xilespa.common.audit;

/** Loại thao tác được ghi nhật ký (cột {@code audit_log.action}). Thêm giá trị mới khi cần. */
public enum AuditAction {
    CREATE,
    UPDATE,
    DEACTIVATE,
    ACTIVATE,
    VOID
}
