<!--
Điền mọi mục trước khi gán Đình review. Ô nào không áp dụng thì vẫn tick và ghi "(không áp dụng)".
Quy tắc gốc: AGENTS.md (mục 4, 5), CONTRIBUTING.md (mục 3, 4), backend/AGENTS.md, frontend/AGENTS.md.
-->

## Thẻ

- Mã thẻ: <!-- S2-01. Gom nhiều thẻ thì liệt kê đủ: S2-01, S2-02 -->
- Trello: <!-- link card -->
- Yêu cầu liên quan: <!-- FR-SVC-01, BR-19, OQ-06 ... -->

## Đã làm gì

<!-- Gạch đầu dòng ngắn. PR gom nhiều thẻ thì chia mục theo từng thẻ. -->

-

## Cách thử

<!-- Các bước thử bằng Postman hoặc giao diện, kèm kết quả mong đợi và trường hợp biên. -->

1.

## Kết quả test ở máy

<!-- Dán số test chạy qua, ví dụ: "Backend 142/142, gồm test tích hợp. Frontend 59/59". Chưa chạy được phần nào thì ghi rõ lý do. -->

## Giả định, điểm chưa chắc

<!-- Chỗ nào tự quyết vì SRS chưa rõ, OQ nào liên quan, phần nào sửa ngoài phạm vi thẻ. Không có thì ghi "Không". -->

## Ảnh chụp màn hình

<!-- Có đổi giao diện: chụp cỡ máy tính và cỡ điện thoại. Không có thì ghi "Không". -->

## Tự kiểm tra

**Nhánh và PR**

- [ ] Base là `develop` (chỉ `release/*`, `hotfix/*` mới vào `main`)
- [ ] Tiêu đề theo Conventional Commits, tiếng Việt có dấu, mã thẻ ở cuối: `feat(catalog): quản lý dịch vụ [S1-05]`
- [ ] Mô tả commit khi squash có `Refs:` đủ mọi thẻ của PR
- [ ] PR nhỏ (khoảng dưới 400 dòng), hoặc đã ghi lý do ở mục "Giả định"
- [ ] Đã tự đọc lại toàn bộ diff trên GitHub, kể cả phần AI viết

**Build và test**

- [ ] Backend: `./gradlew spotlessApply test` qua, đã bật Docker Desktop để chạy cả test tích hợp
- [ ] Frontend: `npm run format:check && npm run lint && npm run typecheck && npm test` qua
- [ ] Có test cho logic nghiệp vụ mới; không `@Disabled`, không `skip`, không nới assert cho xanh
- [ ] CI xanh (khi repo đã có CI)

**Nghiệp vụ và dữ liệu**

- [ ] Tiền là số nguyên đồng (không `float`, `double`); giao diện không tự tính tiền
- [ ] Không sửa dữ liệu ngày đã chốt; sửa sai bằng bút toán điều chỉnh
- [ ] Thay đổi tài chính, danh mục có ghi `audit_log` trong cùng transaction; không xóa cứng
- [ ] Đổi schema bằng migration mới `V{n}__...`, không sửa migration cũ, đã báo số `V{n}` cho người kia
- [ ] Thời gian lấy từ `Clock` (`Asia/Ho_Chi_Minh`), không gọi `Instant.now()`, `LocalDate.now()` trực tiếp

**Bảo mật và cấu hình**

- [ ] Không có bí mật, `.env`, dữ liệu khách thật; không ghi mật khẩu, token ra log
- [ ] Phần đăng nhập, quên mật khẩu: thông báo lỗi và thời gian phản hồi không làm lộ email có tồn tại
- [ ] Khác biệt môi trường đặt bằng thuộc tính trong `application-{profile}.yml`, giá trị mặc định chỉ ở `application.yml`
- [ ] Không thêm thư viện ngoài ADR-0001; nếu thêm thì đã cập nhật ADR và nói rõ ở trên

**Code**

- [ ] Đúng cấu trúc thư mục và quy ước của `backend/AGENTS.md`, `frontend/AGENTS.md` (entity không setter, form React Hook Form + Zod...)
- [ ] Không còn code bị comment bỏ, `console.log`, `System.out.println`
- [ ] Giao diện dùng được trên điện thoại; lỗi từ backend gắn vào đúng ô theo mã lỗi

**Tài liệu trong cùng PR**

- [ ] SRS, ERD, README đã cập nhật nếu hành vi đổi
- [ ] Postman collection đã có request cho endpoint mới hoặc đã sửa
- [ ] File backlog đã chuyển trạng thái thẻ (Đang làm, Xong)
- [ ] File AGENTS đã cập nhật nếu đổi kiến trúc, quy ước, thư viện
- [ ] PR của Đình: đã ghi "Đã tự review" trước khi bypass
