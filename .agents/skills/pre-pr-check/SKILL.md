---
name: pre-pr-check
description: Chạy đủ kiểm tra trước khi mở Pull Request trong Xile Spa (format, lint, typecheck, test backend và frontend), tự soát checklist của mẫu PR, kiểm tra nhánh, rồi soạn sẵn tiêu đề, mô tả PR và các lệnh git để người dùng tự chạy. Dùng khi xong một card, trước khi người dùng commit lần cuối hoặc mở PR, hoặc khi người dùng gõ /pre-pr-check.
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

Kiểm tra nhánh: phải là nhánh tách từ `develop`, tên đúng mẫu `feature|fix|docs|chore/<mã-card>-<mô-tả>`, hoặc
`docs|chore/<mô-tả>` cho việc quy trình không có card (CONTRIBUTING.md mục 2). Đang ở `develop` hoặc `main` thì
ghi lên đầu báo cáo, đưa lệnh `git switch -c <loại>/<mã-card>-<mô-tả>` (thay đổi chưa commit đi theo sang nhánh
mới) và không báo sẵn sàng.

Liệt kê file đổi theo nhóm (backend, frontend, DB, tài liệu). Có file ngoài phạm vi card thì nêu ra.

**Kích thước PR** (CONTRIBUTING.md mục 4, "Nhỏ"):

```bash
git diff --numstat $(git merge-base origin/develop HEAD) -- . ':(exclude)frontend/package-lock.json' ':(exclude)docs/postman/*.json' ':(exclude)docs/design/diagrams/*'
git status --short   # file mới chưa add cũng tính
```

Tổng số dòng thêm và xóa quá khoảng 400, hoặc diff có việc của hơn một card, hoặc có việc không thuộc card
(sửa quy trình, AGENTS không liên quan) thì ghi lên đầu báo cáo và đề xuất cách tách: nhóm file nào vào PR nào, tên
nhánh từng PR. Không tự tách; người dùng quyết.

**Test đi kèm code:** với mỗi file code chính có đổi (`entity`, `service`, `controller`, `validator` ở backend;
`schemas.ts`, component, page, hook, `shared/lib` ở frontend), phải có file test của nó cũng đổi hoặc được thêm
(`XxxTest`, `XxxControllerIT`, `Xxx.test.tsx`, `schemas.test.ts`). Liệt kê các file thiếu test thành bảng; còn
thiếu thì không báo sẵn sàng, đề xuất chạy `/write-tests` cho đúng file đó. Chỉ đổi giao diện thuần (CSS, nhãn)
thì ghi "không cần test mới" kèm lý do.

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
- [ ] Test đủ các nhóm ca của `AGENTS.md` mục 10 cho mọi hành vi mới hoặc sửa: đọc bảng đối chiếu hành vi và
      test mà `/implement` để lại (bước 7), mở từng test được ghi để chắc nó có thật và kiểm tra đúng điều ghi.
      Không có bảng thì tự lập. Không tick ô này chỉ vì test đang xanh.
- [ ] File frontend đã sửa không còn `style={{}}` (trừ giá trị tính lúc chạy); CSS ở `.module.css` cạnh component.
- [ ] Đổi quy ước hay cấu trúc thì đã cập nhật `AGENTS.md` tương ứng.
- [ ] Thêm hoặc đổi luồng có rẽ nhánh nghiệp vụ thì `docs/design/flows/<module>.md` khớp với code.

Ô nào chưa đạt thì ghi rõ và đề xuất sửa; sửa xong thì chạy lại bước 2.

## 4. Soạn PR

- **Tiêu đề:** `<loại>(<phạm vi>): <mô tả ngắn có dấu> [<mã thẻ>]` (CONTRIBUTING.md mục 3, 4). Ví dụ
  `feat(catalog): quản lý nhóm dịch vụ [S2-06]`.
- **Mô tả:** điền đúng các mục của `.github/pull_request_template.md` (Tóm tắt, Liên kết, Loại thay đổi, Thay đổi
  chính, Kiểm thử với số liệu thật, Ảnh chụp màn hình, Rủi ro và cách quay lại, Ghi chú cho người review,
  Checklist). Có sơ đồ từ `/plan-task` hoặc `/fix-bug` thì dán vào "Ghi chú cho người review".
- **Câu squash merge:** tiêu đề PR + dòng `Refs: <mọi mã card>, <FR/BR>`.

Trả bản nháp trong một khối markdown để người dùng chép.

## 5. Lệnh git cho người dùng

Agent không chạy git ghi. Sau bản nháp PR, in sẵn các lệnh để người dùng tự chạy, điền đúng tên nhánh, danh sách
file và câu commit (Conventional Commits, CONTRIBUTING.md mục 3):

```bash
git status
git add <các file của card>        # liệt kê cụ thể, không dùng "git add ." khi có file ngoài card
git commit -m "<loại>(<phạm vi>): <mô tả>" -m "Refs: <mã card>, <FR/BR>"
git push -u origin <nhánh>
```

Rồi mở PR vào `develop` trên GitHub bằng bản nháp ở bước 4. Sau khi squash merge (CONTRIBUTING.md mục 1):
`git switch develop && git pull && git branch -D <nhánh>`. Dùng `-D` vì squash merge làm git không nhận ra nhánh
đã gộp.
