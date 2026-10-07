package com.xilespa.module.auth.entity;

/**
 * Vai trò người dùng trong hệ thống (khớp với ràng buộc ck_app_user_role ở CSDL). - OWNER: Chủ
 * tiệm, toàn quyền xem và nhập dữ liệu. - STAFF: Nhân viên / KTV (dự kiến ở giai đoạn sau).
 */
public enum UserRole {
    OWNER,
    STAFF
}
