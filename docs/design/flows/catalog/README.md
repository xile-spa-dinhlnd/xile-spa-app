# Luồng xử lý: catalog (phần chung)

Module `backend/.../module/catalog`; frontend `features/catalog`. Yêu cầu: FR-SVC-01 đến FR-SVC-05, FR-CMB-01 đến
FR-CMB-05. Bảng `service_group`, `service`, `service_price_history`, `combo`, `combo_item` (V1, dữ liệu mẫu 4 nhóm ở V2).

Chung cho mọi tính năng của module:

- **Tên không trùng**, không phân biệt hoa thường và khoảng trắng thừa: unique index trên `norm_name(name)` của CSDL
  (`ux_service_group_name`, `ux_service_name`). Service gộp khoảng trắng (`normalizeSpaces`), kiểm tra trước bằng
  `existsByNormalizedName(name, excludeId)` rồi `saveAndFlush` để bắt hai request cùng tên (lỗi unique → 409).
- **Không xóa cứng** (BR-19): không có endpoint `DELETE` (405). Ngừng dùng, ngừng bán là đánh dấu `active = false`;
  gọi lại khi đã ở trạng thái đó thì trả bản ghi như cũ, không ghi audit.
- **Nhật ký** (BR-18): mọi thay đổi gọi `AuditService.record` trong cùng transaction, `entityType` là tên bảng
  (`service_group`, `service`); sửa mà không đổi gì thì không ghi.
- **Nhóm ngừng dùng và dịch vụ** (OQ-21): không có dịch vụ đang bán nào nằm trong nhóm đã ngừng dùng. Ngừng dùng nhóm
  còn dịch vụ đang bán bị chặn (409 `SERVICE_GROUP_IN_USE`); bán lại dịch vụ trong nhóm đã ngừng dùng bị chặn (400
  `SERVICE_GROUP_NOT_FOUND`); tạo, sửa dịch vụ chỉ chọn được nhóm đang dùng.
- **Combo đang bán chỉ chứa dịch vụ đang bán** (OQ-22): ngừng bán dịch vụ còn nằm trong combo đang bán bị chặn (409
  `SERVICE_IN_ACTIVE_COMBO`); tạo, sửa, bán lại combo chỉ dùng được dịch vụ đang bán (400 `COMBO_SERVICE_UNAVAILABLE`).
  Tên combo không trùng theo `ux_combo_name`, cùng cách kiểm tra như tên dịch vụ.
- Tiền là `long` đồng (BR-01); thời điểm lấy từ `Clock`.

| Tính năng                       | File               |
| ------------------------------- | ------------------ |
| Bảng giá dịch vụ                | `service.md`       |
| Quản lý nhóm dịch vụ            | `service-group.md` |
| Combo                           | `combo.md`         |
