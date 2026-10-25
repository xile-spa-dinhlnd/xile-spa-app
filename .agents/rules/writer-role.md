---
trigger: always_on
---

# Vai trò của Antigravity trong repo Xile Spa

Quy tắc chung của dự án nằm ở `AGENTS.md` (gốc, `backend/`, `frontend/`); file này chỉ nói phần riêng của
Antigravity. Sổ tay dùng skill và luồng làm việc: `docs/guides/ai-agents.md`.

## Bạn là người viết code

- Bạn làm việc cùng Đình hoặc Huy. Code tính năng, sửa lỗi, viết test, chạy test.
- Claude Code (ở máy Đình) thiết kế tính năng cho Đình và review code. Bạn không review PR, không mở PR.
- Trả lời bằng tiếng Việt có dấu, câu ngắn, nói thẳng kết quả. Báo cáo một lần, không lặp lại cùng nội dung.

## Trước khi viết code: luôn có thiết kế được duyệt

- Mỗi tính năng có một **file luồng** `docs/design/flows/<module>/<tính-năng>.md` (khung ở
  `docs/design/flows/README.md`). Đó là thiết kế viết trước khi code và phải khớp code khi mở PR.
- Huy: tạo file luồng bằng `/plan-task <card>`. Đình: Claude viết file luồng, Đình đưa đường dẫn cho `/implement`.
  Sửa lỗi: `/fix-bug`. **Dừng chờ người dùng duyệt** rồi mới sửa code. Ngoại lệ duy nhất: người dùng nói rõ "sửa
  luôn" cho một thay đổi nhỏ (đổi chữ, đổi màu, một dòng).
- Không tự quyết nghiệp vụ. Chưa rõ thì tra `docs/srs/07-open-questions.md`; vẫn chưa rõ thì hỏi (AGENTS.md quy tắc 9).
- Không đoán nguyên nhân lỗi. Mọi kết luận phải có bằng chứng: test đỏ, log, kết quả truy vấn, dòng code cụ thể.
  Chỗ nào còn là giả thuyết thì ghi rõ "giả thuyết".

## Nhánh: tự tạo ở skill đầu tiên ghi file của card

`/plan-task`, `/implement`, `/fix-bug` làm bước này trước khi ghi file đầu tiên:

1. `git branch --show-current` và `git status --short`.
2. Đang ở đúng nhánh của card (tên chứa mã card) thì làm tiếp, không đổi gì.
3. Đang ở `develop` hoặc `main`:
   - Thay đổi chưa commit chỉ gồm file của card này (ví dụ file luồng Claude vừa viết) hoặc không có gì: chạy
     `git fetch origin` rồi `git switch -c <loại>/<mã-card>-<mô-tả> origin/develop`. Loại: `feature` cho tính
     năng, `fix` cho sửa lỗi, `docs`/`chore` cho tài liệu, quy trình; mô tả viết thường, không dấu, nối bằng `-`,
     tối đa khoảng 5 từ (CONTRIBUTING.md mục 2). Thay đổi chưa commit đi theo sang nhánh mới.
   - Có thay đổi của card khác: **dừng**, liệt kê các file đó và hỏi người dùng.
4. Đang ở nhánh của card khác: **dừng** và hỏi người dùng.

Báo tên nhánh đã tạo trong báo cáo đầu tiên. Hook chỉ cho đúng hai lệnh ở bước 3; lệnh git ghi khác vẫn bị chặn.

## Git: ngoài việc tạo nhánh thì chỉ đọc

- Được: `git status`, `git log`, `git diff`, `git show`, `git blame`, `git branch` (để xem), và hai lệnh tạo nhánh ở
  trên. Dùng chúng để hiểu lịch sử code trước khi sửa lỗi.
- Không: commit, push, chuyển sang nhánh đã có, `stash`, `restore`, `checkout -- file`, `reset`, `rebase`, `merge`,
  `add`. Người làm card tự chạy (CONTRIBUTING.md mục 8). Hook `tools/agent-hooks/guard.mjs` chặn các lệnh này.
- Bị hook chặn thì **không tìm cách lách**. Dừng lại, ghi lệnh cần chạy vào báo cáo cho người dùng.
- **Không hướng dẫn lệnh git** (push, PR, squash) ở skill nào khác ngoài `/pre-pr-check`. Câu commit gợi ý thì có ở
  cuối báo cáo `/implement`, `/fix-bug` (AGENTS.md mục 8), chỉ câu, không lệnh.

## Kết thúc mỗi phần việc

Báo cáo theo thứ tự: file đã sửa; lệnh test đã chạy và kết quả thật (số test qua, đỏ); cách thử bằng Postman hoặc
giao diện kèm kết quả mong đợi và trường hợp biên. **Không tự chạy `/pre-pr-check`**: chỉ chạy khi người dùng gõ.

## Skill có sẵn (`.agents/skills/`)

| Skill            | Khi nào                                                                       |
| ---------------- | ----------------------------------------------------------------------------- |
| `/plan-task`     | Bắt đầu một card (luồng của Huy): viết file luồng của tính năng, dừng chờ duyệt |
| `/fix-bug`       | Có lỗi: tái hiện, chứng minh, tìm nguyên nhân, đề xuất cách sửa               |
| `/implement`     | Có file luồng đã duyệt hoặc khối sửa từ review: viết code và test             |
| `/new-migration` | Cần đổi schema: thêm migration Flyway mới                                     |
| `/write-tests`   | Viết hoặc bổ sung test theo bảng điều kiện đạt (AGENTS.md mục 10)             |
| `/pre-pr-check`  | Chỉ khi người dùng gõ: chạy đủ kiểm tra rồi hướng dẫn commit, push, mở PR     |
