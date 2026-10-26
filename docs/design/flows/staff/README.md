# Luồng xử lý: staff (phần chung)

Module `backend/.../module/staff`; frontend `features/staff`. Yêu cầu: FR-STF-01. Bảng `staff` (V1); vai trò
`OWNER` (Chủ tiệm), `THERAPIST` (KTV) là enum `StaffRole`, nhãn tiếng Việt ở `features/staff/types.ts`.

Chung cho mọi tính năng của module:

- **Tên không trùng**, không phân biệt hoa thường và khoảng trắng thừa (`ux_staff_name`): service chuẩn hóa bằng
  `TextNormalizer.normalizeName` (`common/util`, S3-08), kiểm tra trước bằng `existsByNormalizedName(name, excludeId)`
  rồi `saveAndFlush` (lỗi unique → 409 `STAFF_NAME_DUPLICATE`).
- Không xóa cứng: nhân viên nghỉ là `active = false` ("đã nghỉ"), vẫn giữ trong lịch sử giao dịch.
- Mọi thay đổi gọi `AuditService.record` trong cùng transaction, `entityType = staff` (BR-18).
- Thời điểm lấy từ `Clock`.

| Tính năng           | File       |
| ------------------- | ---------- |
| Quản lý nhân viên   | `staff.md` |
