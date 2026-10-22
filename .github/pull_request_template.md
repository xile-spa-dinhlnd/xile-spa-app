<!--
Tiêu đề PR: <loại>(<phạm vi>): <mô tả ngắn có dấu> [<mã thẻ>]
Ví dụ:      feat(catalog): quản lý dịch vụ [S1-05]
Base: develop (chỉ release/* và hotfix/* mới vào main). Quy tắc đầy đủ: CONTRIBUTING.md mục 3, 4.
Các dòng hướng dẫn dạng chú thích như thế này không hiện ra khi xem PR. Mục không áp dụng thì ghi "Không".
-->

## Tóm tắt

<!-- 1 đến 3 câu: PR giải quyết vấn đề gì và vì sao cần. -->

## Liên kết

- Thẻ: <!-- S2-01 (link Trello). Gom nhiều thẻ thì liệt kê đủ. -->
- Yêu cầu: <!-- FR-SVC-01, BR-19, OQ-06, ADR-0003 ... -->

## Loại thay đổi

- [ ] `feat`: chức năng mới
- [ ] `fix`: sửa lỗi
- [ ] `refactor` / `perf`: đổi cấu trúc hoặc hiệu năng, không đổi hành vi
- [ ] `test` / `docs` / `build` / `ci` / `chore`
- [ ] **Breaking change**: đổi API mà frontend đang gọi, đổi schema, đổi cấu hình hoặc biến môi trường

## Thay đổi chính

<!-- Gạch đầu dòng, nhóm theo backend / frontend / DB / tài liệu. PR gom nhiều thẻ thì chia theo thẻ. -->

-

## Kiểm thử

<!-- Ghi kết quả thật, ví dụ "Backend 142/142 (gồm test tích hợp), Frontend 59/59". Chưa chạy được phần nào thì nói lý do. -->

- Tự động:
- Thủ công (Postman hoặc giao diện, kèm kết quả mong đợi):
  1.

## Ảnh chụp màn hình

<!-- Có đổi giao diện: chụp cả cỡ máy tính và điện thoại, nên có trước / sau. -->

| Trước | Sau |
| ----- | --- |
|       |     |

## Rủi ro và cách quay lại

<!-- Phần nào dễ hỏng, ảnh hưởng dữ liệu nào, có migration không, cần đặt biến môi trường gì khi deploy. -->

## Ghi chú cho người review

<!-- Giả định tự quyết vì SRS chưa rõ, chỗ còn chưa chắc, phần sửa ngoài phạm vi thẻ, file nên xem kỹ. -->

## Checklist

- [ ] Đã tự đọc lại toàn bộ diff (kể cả phần AI viết); PR nhỏ, khoảng dưới 400 dòng hoặc đã giải thích
- [ ] Build và test qua ở máy: `./gradlew spotlessApply test` (bật Docker) và `npm run format:check && npm run lint && npm run typecheck && npm test`; CI xanh nếu có
- [ ] Có test cho logic nghiệp vụ mới; không `@Disabled`, không `skip`, không nới assert
- [ ] Quy tắc nghiệp vụ (AGENTS.md mục 4): tiền là số nguyên đồng, không sửa ngày đã chốt, ghi `audit_log` cùng transaction, không xóa cứng, thời gian qua `Clock`
- [ ] Đổi schema bằng migration mới `V{n}__...`, không sửa migration cũ
- [ ] Không có bí mật, `.env`, dữ liệu khách thật; không log mật khẩu, token; phần đăng nhập không làm lộ email có tồn tại
- [ ] Đúng cấu trúc và quy ước trong `backend/AGENTS.md`, `frontend/AGENTS.md`; thư viện mới đã ghi ADR-0001
- [ ] Đã cập nhật trong cùng PR: SRS / ERD / README nếu hành vi đổi, Postman nếu đổi endpoint, trạng thái thẻ trong backlog, AGENTS nếu đổi quy ước
