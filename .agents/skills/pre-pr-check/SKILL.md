---
name: pre-pr-check
description: Chỉ chạy khi người dùng tự gõ /pre-pr-check; không tự gọi sau /implement, /fix-bug hay khi thấy code đã xong. Chạy đủ kiểm tra trước khi mở Pull Request trong Xile Spa (format, lint, typecheck, test), tự soát checklist, rồi là nơi duy nhất hướng dẫn git - câu commit, push, mở PR, squash merge, dọn nhánh - để người dùng tự chạy.
---

# /pre-pr-check: sẵn sàng mở PR và hướng dẫn git

- **Chỉ chạy khi người dùng gõ `/pre-pr-check`.** Không tự cho rằng code đã đúng và đã xong.
- Đây là **nơi duy nhất** hướng dẫn lệnh git (AGENTS.md mục 8). `/implement`, `/fix-bug`, `/self-review` chỉ gợi ý câu
  commit cho từng lần sửa; skill này soát lại các câu đã commit và soạn lệnh còn lại.
- Skill này không commit, push hay mở PR: chỉ soạn sẵn để người dùng tự chạy. Ngoại lệ: tạo nhánh ở bước 1.

## 1. Nhánh và phạm vi

```bash
git branch --show-current
git status --short
git log --format='%h %s' origin/develop..HEAD
git diff --stat origin/develop...HEAD
```

- Đang ở `develop` hoặc `main`: tạo nhánh theo mục "Nhánh" của `.agents/rules/writer-role.md` rồi làm tiếp. Đang ở
  nhánh của card khác: dừng và hỏi.
- Liệt kê file đổi theo nhóm (backend, frontend, DB, tài liệu). Có file ngoài phạm vi card thì nêu ra.
- **Kích thước** (CONTRIBUTING.md mục 4, "Nhỏ"): chỉ đếm code chính.

  ```bash
  git diff --numstat $(git merge-base origin/develop HEAD) -- backend/src/main frontend/src ':(exclude)frontend/src/**/*.test.ts' ':(exclude)frontend/src/**/*.test.tsx'
  ```

  Cộng thêm file mới chưa add. Quá khoảng 400 dòng, hoặc diff có việc của hơn một card, hoặc việc không thuộc card:
  ghi lên đầu báo cáo và đề xuất (tách việc ngoài card ra PR riêng; card quá lớn thì giữ một PR và ghi lý do trong
  "Ghi chú cho người review"). Không tự tách; người dùng quyết.
- **Test đi kèm code:** mỗi file code chính có đổi (`entity`, `service`, `controller`, `validator` ở backend;
  `schemas.ts`, component, page, hook, `shared/lib` ở frontend) phải có file test của nó đổi hoặc được thêm. Còn
  thiếu thì không báo sẵn sàng, đề xuất `/write-tests` cho đúng file đó. Chỉ đổi giao diện thuần (CSS, nhãn) thì ghi
  "không cần test mới" kèm lý do.

## 2. Chạy kiểm tra (phần nào có đổi thì chạy phần đó)

```bash
cd backend && ./gradlew spotlessApply test
cd frontend && npm run format && npm run lint && npm run typecheck && npm test
```

Luôn chạy thêm ở gốc repo (dưới một giây): `node tools/context-check/check.mjs`. Báo lỗi (đường dẫn ở mục 2 của
`AGENTS.md` không còn, file AGENTS quá giới hạn dòng, thiếu file nạp quy tắc) thì sửa tài liệu theo `AGENTS.md` mục 6,
không nới giới hạn. Đổi file trong `tools/` thì chạy thêm `node --test "tools/**/*.test.mjs"`.

Ghi kết quả thật: số test qua, đỏ, bỏ qua. Test backend cần Docker đang chạy; không chạy được thì nói rõ, không
ghi là "đã qua". Có test đỏ thì dừng, phân tích bằng cách của `/fix-bug`, không báo sẵn sàng.

## 3. Tự soát

Đi qua mục "Checklist" của `.github/pull_request_template.md` và danh sách soát ở CONTRIBUTING.md mục 4 ("Review").
Thêm:

- [ ] File luồng của tính năng có `Trạng thái: khớp code`, dòng lịch sử của card này, bảng "Hành vi và test" ghi tên
      test thật (mở từng test để chắc nó có và kiểm tra đúng điều ghi). Không tick chỉ vì test đang xanh.
- [ ] Mọi dòng ở mục "Ảnh hưởng" của file luồng đã làm; grep lại URL và tên method đã đổi (`AGENTS.md` mục 7).
- [ ] Card trong `docs/backlog/` đã "Xong" (hoặc vẫn "Đang làm" nếu PR là Draft).
- [ ] Endpoint mới hoặc sửa đã có trong Postman collection.
- [ ] Không còn `console.log`, `System.out.println`, code bị comment bỏ, TODO không có mã card.
- [ ] Không có bí mật, không sửa `.env`, không sửa migration cũ, không sửa file sinh tự động.
- [ ] File frontend đã sửa không còn `style={{}}` (trừ giá trị tính lúc chạy); CSS ở `.module.css` cạnh component.
- [ ] Đổi quy ước hay cấu trúc thì đã cập nhật `AGENTS.md` tương ứng.
- [ ] Câu commit đã có trên nhánh (bước 1) đúng CONTRIBUTING.md mục 3. Sai thì không viết lại lịch sử; ghi để sửa ở
      bước squash (mục 5).

Ô nào chưa đạt thì ghi rõ và đề xuất sửa; sửa xong thì chạy lại bước 2.

## 4. Soạn PR

- **Tiêu đề:** `<loại>(<phạm vi>): <mô tả ngắn có dấu> [<mã thẻ>]` (CONTRIBUTING.md mục 3, 4). Việc của card là
  `feat` (tính năng) hoặc `fix` (sửa lỗi đã có trên `develop`). Ví dụ `feat(catalog): quản lý nhóm dịch vụ [S2-06]`.
- **Mô tả:** điền đúng các mục của `.github/pull_request_template.md`, "Kiểm thử" có số liệu thật. "Ghi chú cho
  người review" có đường dẫn file luồng, giả định, và dòng "Đã tự review bằng /self-review ..." nếu người dùng đưa.
- **Câu squash:** tiêu đề PR, thân chỉ một dòng `Refs: <mã card>, <FR/BR/OQ>`.

Trả bản nháp trong một khối markdown để người dùng chép.

## 5. Hướng dẫn git

In đúng các lệnh cho trạng thái hiện tại, điền sẵn tên nhánh, danh sách file, câu commit:

1. **Commit** (bỏ qua nếu không còn gì chưa commit):

   ```bash
   git status
   git add <các file của card>        # liệt kê cụ thể, không "git add ." khi có file ngoài card
   git commit -m "<loại>(<phạm vi>): <mô tả tiếng Việt có dấu>" -m "Refs: <mã card>, <FR/BR/OQ>"
   ```

   Câu commit theo CONTRIBUTING.md mục 3: tiếng Việt có dấu, bắt đầu bằng động từ, không "update", "wip".
2. **Push:** `git push -u origin <nhánh>` (lần đầu), sau đó `git push`.
3. **Mở PR** vào `develop` trên GitHub, dán tiêu đề và mô tả ở bước 4. PR của Huy: gán Đình review. PR của Đình:
   merge bằng bypass sau khi `/self-review` sạch (CONTRIBUTING.md mục 4).
4. **Sửa sau review:** sửa bằng khối mà review đưa, commit theo bước 1, `git push`, bấm **Re-request review**.
5. **Merge:** chọn **Squash and merge**, không chọn "Create a merge commit". Ô tiêu đề dán tiêu đề PR; ô nội dung xóa
   phần GitHub tự ghép từ các commit, chỉ để dòng `Refs:` ở bước 4.
6. **Sau khi merge:** `git switch develop && git pull && git branch -D <nhánh>` (dùng `-D` vì squash làm git không
   nhận ra nhánh đã gộp).
