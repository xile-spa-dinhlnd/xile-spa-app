# Checklist cho mỗi endpoint mới hoặc sửa

Nguồn: `backend/AGENTS.md`, `frontend/AGENTS.md`, `AGENTS.md` mục 4. Đi hết danh sách trước khi báo xong.

## Backend

- [ ] URL `/api/{module}/{tài-nguyên}` số nhiều, kebab-case; hành động nghiệp vụ là `POST .../{id}/<hành-động>`.
- [ ] Thay đổi dữ liệu không dùng `GET`.
- [ ] Controller chỉ nhận/trả DTO, có `@Valid`, không gọi repository, không dựng `ResponseEntity` lỗi.
- [ ] Request DTO là `record`, annotation kiểm tra có `message` tiếng Việt; Response DTO là `record`.
- [ ] Mapper `@Mapper(config = MapStructConfig.class)`; trường nhạy cảm (id, trạng thái, giá đã chốt, `created_by`)
      đánh dấu `ignore = true` và gán trong service; không tính toán trong mapper.
- [ ] Service có `@Transactional`; đổi dữ liệu tài chính hoặc danh mục thì gọi `AuditService.record` với DTO,
      không truyền entity.
- [ ] Tiền là `long`; thời điểm lấy từ `Clock`; ngày làm việc là `LocalDate` người dùng chọn.
- [ ] Ghi giao dịch theo ngày: kiểm tra ngày đã chốt trước, dùng `lock_business_day(d)` khi cần (ERD 5.5).
- [ ] Lỗi nghiệp vụ mới thêm vào `ErrorCode` (mã, HTTP status, thông báo tiếng Việt).
- [ ] Module khác chỉ dùng qua `service/` và `dto/` của nó; tham chiếu bằng id.
- [ ] Danh sách có phân trang trả `PageResponse<T>`, trang bắt đầu từ 0.
- [ ] Endpoint yêu cầu đăng nhập (mặc định); không log mật khẩu, token.

## Test

- [ ] Đủ sáu nhóm ca của `AGENTS.md` mục 10 (dùng `/write-tests`).
- [ ] Test đơn vị cho từng nhánh nghiệp vụ của service (thành công và từng lỗi).
- [ ] `...ControllerIT` với `@IntegrationTest`: 2xx, 400 (validation, có `fieldErrors`), 401, 404, 409 nếu có.
- [ ] Test IT tạo dữ liệu có tiền tố riêng, không `deleteAll()` bảng dùng chung, không đếm toàn bảng `audit_log`.
- [ ] Không `@Disabled`, không nới assert.

## Frontend

- [ ] Gọi API qua `shared/api/client.ts`; query key trong `api.ts`; sau khi sửa thì `invalidateQueries`.
- [ ] Kiểu trong `types.ts` khớp DTO; schema Zod khớp kiểm tra của backend.
- [ ] Lỗi backend gắn vào ô bằng `setError` (mã lỗi nghiệp vụ và `fieldErrors`); lỗi khác `message.error`.
- [ ] Tiền chỉ định dạng bằng `formatMoney`, nhập bằng `MoneyInput`; không tự tính tiền.
- [ ] Máy tính dùng `Table`, điện thoại dùng `Card`; có trạng thái trống, lỗi tải có nút "Thử lại".
- [ ] Style bằng `<Component>.module.css` và biến trong `src/styles/tokens.css`; không `style={{}}`, không mã màu cứng.

## Tài liệu

- [ ] Request Postman (thành công và lỗi chính) có kiểm tra mã trạng thái.
- [ ] SRS, ERD cập nhật nếu hành vi hoặc schema đổi.
