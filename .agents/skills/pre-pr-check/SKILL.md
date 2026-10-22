---
name: pre-pr-check
description: Chạy đủ kiểm tra trước khi mở Pull Request trong Xile Spa (format, lint, typecheck, test backend và frontend), tự soát checklist của mẫu PR, rồi soạn sẵn tiêu đề và mô tả PR. Dùng khi xong một card, trước khi người dùng commit lần cuối hoặc mở PR, hoặc khi người dùng gõ /pre-pr-check.
---

# /pre-pr-check: sẵn sàng mở PR

Skill này **không** commit, push hay mở PR. Kết quả là một báo cáo và bản nháp PR để người dùng dán vào GitHub.

## 1. Xem phạm vi thay đổi

```bash
git branch --show-current
git status
git diff --stat origin/develop...HEAD
git diff --stat
```

Liệt kê file đổi theo nhóm (backend, frontend, DB, tài liệu). Có file ngoài phạm vi card thì nêu ra.

## 2. Chạy kiểm tra (phần nào có đổi thì chạy phần đó; đổi cả hai thì chạy cả hai)

```bash
cd backend && ./gradlew spotlessApply test
cd frontend && npm run format && npm run lint && npm run typecheck && npm test
```

Ghi kết quả thật: số test qua, đỏ, bỏ qua. Test backend cần Docker đang chạy; không chạy được thì nói rõ, không
ghi là "đã qua". Có test đỏ thì dừng, phân tích bằng cách của `/fix-bug`, không báo sẵn sàng.

## 3. Tự soát

Đi qua từng ô trong mục "Checklist" của `.github/pull_request_template.md` và danh sách soát ở CONTRIBUTING.md
mục 4 ("Review"). Thêm:

- [ ] Card trong `docs/backlog/` đã "Xong" (hoặc vẫn "Đang làm" nếu PR là Draft).
- [ ] Endpoint mới hoặc sửa đã có trong Postman collection.
- [ ] Không còn `console.log`, `System.out.println`, code bị comment bỏ, TODO không có mã card.
- [ ] Không có bí mật, không sửa `.env`, không sửa migration cũ, không sửa file sinh tự động.
- [ ] Đổi quy ước hay cấu trúc thì đã cập nhật `AGENTS.md` tương ứng.

Ô nào chưa đạt thì ghi rõ và đề xuất sửa; sửa xong thì chạy lại bước 2.

## 4. Soạn PR

- **Tiêu đề:** `<loại>(<phạm vi>): <mô tả ngắn có dấu> [<mã thẻ>]` (CONTRIBUTING.md mục 3, 4). Ví dụ
  `feat(catalog): quản lý nhóm dịch vụ [S2-06]`.
- **Mô tả:** điền đúng các mục của `.github/pull_request_template.md` (Tóm tắt, Liên kết, Loại thay đổi, Thay đổi
  chính, Kiểm thử với số liệu thật, Ảnh chụp màn hình, Rủi ro và cách quay lại, Ghi chú cho người review,
  Checklist). Có sơ đồ từ `/plan` hoặc `/fix-bug` thì dán vào "Ghi chú cho người review".
- **Câu squash merge:** tiêu đề PR + dòng `Refs: <mọi mã card>, <FR/BR>`.

Trả bản nháp trong một khối markdown để người dùng chép.
