# Sổ tay dùng agent AI trong Xile Spa

Dành cho Đình và Huy. Quy tắc nằm ở `AGENTS.md` (mục 8 là vai trò từng agent); file này hướng dẫn **cách dùng**:
cài đặt, luồng làm việc, từng skill, hook.

## 1. Ai làm gì

| Agent                  | Người dùng | Việc                                                                     |
| ---------------------- | ---------- | ------------------------------------------------------------------------ |
| Antigravity            | Đình, Huy  | Lập kế hoạch, viết code, test, tài liệu của card. Git chỉ được đọc.      |
| Claude Code (máy Đình) | Đình       | Review PR của Huy (`/review-pr`), tự soát code của Đình (`/self-review`) |
| Claude app (claude.ai) | Đình       | Cuối sprint: release, retro, lập kế hoạch và chia card sprint sau        |

Một nguồn quy tắc: nội dung chỉ viết ở `AGENTS.md`, `backend/AGENTS.md`, `frontend/AGENTS.md`. Mỗi agent có một lớp
trỏ mỏng để nạp đúng lúc:

| File quy tắc         | Antigravity nạp qua                | Claude Code nạp qua                |
| -------------------- | ---------------------------------- | ---------------------------------- |
| `AGENTS.md`          | tự đọc ở gốc                       | `CLAUDE.md` (`@AGENTS.md`)         |
| `backend/AGENTS.md`  | `.agents/rules/backend.md` (glob)  | `backend/CLAUDE.md` (`@AGENTS.md`) |
| `frontend/AGENTS.md` | `.agents/rules/frontend.md` (glob) | `frontend/CLAUDE.md`               |

Quy tắc là hướng dẫn, agent vẫn có thể làm sai; chỉ những gì hook chặn (mục 5) mới chắc chắn. Phần riêng của
Antigravity ở `.agents/`; phần riêng của Claude Code ở `.claude/` (chỉ ở máy Đình, không commit).

## 2. Cài đặt

### Antigravity (Đình và Huy, mỗi máy làm một lần)

1. Mở **thư mục gốc repo** làm workspace (không mở riêng `backend/` hay `frontend/`). Antigravity chạy lệnh hook
   từ thư mục `.agents/` (nơi có `hooks.json`), nên lệnh trong đó viết `node ../tools/agent-hooks/...`.
2. Cần Node 22+ (đã có cho frontend) và `npm ci` trong `frontend/` (hook format dùng Prettier của frontend).
3. Rules, skills, hooks tự nạp từ `.agents/`. Kiểm tra trong panel **Customizations**:
   - Rules: `writer-role` (luôn bật), `backend`, `frontend` (bật khi đụng thư mục đó), `migrations` (bật khi đụng
     thư mục migration).
   - Skills: `plan`, `fix-bug`, `implement`, `write-tests`, `new-migration`, `pre-pr-check`.
   - Hooks: `xile-guard`, `xile-format`, `xile-stop`.
4. **Thử rào chắn:** nhờ agent chạy `git push --dry-run`. Phải bị chặn kèm lời nhắc tiếng Việt. Nếu lệnh chạy được
   thì hook chưa nạp: kiểm tra bước 1 và báo trong nhóm.
5. **Quyền chạy lệnh** (Settings → Permission Settings) không lưu được trong repo, mỗi người tự đặt giống nhau:
   - Chế độ chạy lệnh terminal: **Request Review** (hỏi trước, trừ danh sách cho phép).
   - Cho phép tự chạy: `./gradlew test`, `./gradlew spotlessApply`, `./gradlew build`, `npm run format`,
     `npm run lint`, `npm run typecheck`, `npm test`, `npx vitest run`, `git status`, `git log`, `git diff`,
     `git show`, `git blame`.
   - Không bao giờ tự chạy: `git push`, `git commit`, `git checkout`, `git reset`, `rm`, `docker compose down`.
     Hook vẫn chặn các lệnh nguy hiểm kể cả khi đặt sai ở đây.

Workflows (`.agents/workflows/`) của Antigravity đã ngừng chạy từ 19/10/2026; dự án chỉ dùng Skills.

### Claude Code (chỉ Đình)

Bộ `.claude/` (settings, skill `review-pr`, `self-review`) giữ riêng ở máy Đình, có README cài đặt đi kèm. Cần
GitHub CLI đã đăng nhập (`gh auth login`) để đọc PR và đăng review. Claude Code chạy bằng extension trong
Antigravity hoặc lệnh `claude` ở terminal, mở tại gốc repo.

## 3. Luồng làm việc

### 3.1 Một card, từ đầu đến merge

```mermaid
flowchart TD
    A[Người làm card: tạo nhánh feature/S2-06-...] --> B[Antigravity: /plan-task, có mục Chia PR]
    B --> C{Duyệt kế hoạch?}
    C -- cần sửa --> B
    C -- duyệt --> D[Antigravity: /implement phần của PR hiện tại]
    D --> D2[Mỗi tầng: code rồi /write-tests, test xanh mới sang tầng sau]
    D2 --> D3[Bảng đối chiếu hành vi và test; còn ô trống thì quay lại /write-tests]
    D3 --> E[Antigravity dừng. Người làm card: đọc diff, git add, git commit]
    E --> F{Xong phần của PR này?}
    F -- chưa --> D
    F -- xong --> G[Người làm card gõ /pre-pr-check: đo kích thước, file thiếu test]
    G --> H{Ai làm card?}
    H -- Huy --> I[Huy: git push, mở PR bằng bản nháp]
    I --> J[Đình: Claude Code /review-pr số]
    J --> K{Có nhận xét bắt buộc?}
    K -- có --> L[Huy: sửa bằng Antigravity, commit, push] --> J
    K -- không --> M[Đình approve, Huy Squash and merge]
    H -- Đình --> N[Đình: Claude Code /self-review]
    N --> O{Còn cần sửa?}
    O -- có --> D
    O -- không --> P[Đình: push, mở PR ghi Đã tự review, Squash and merge]
```

Ghi nhớ:

- AI không commit, không push, không đổi nhánh. Người làm card làm mọi lệnh git (CONTRIBUTING.md mục 8).
- Sửa file trên `develop` được, nhưng commit và push chỉ trên nhánh card. Còn ở `develop` hay `main` thì skill và
  hook Stop nhắc tạo nhánh trước khi commit; `/pre-pr-check` không báo sẵn sàng (AGENTS.md mục 5 bước 1).
- Trong lúc Antigravity đang sửa file, không đổi nhánh.
- Mỗi bước của `/implement` có một câu commit gợi ý: commit ngay khi bước đó xanh.

### 3.2 Sửa lỗi

```mermaid
flowchart LR
    A[Báo lỗi: bước làm, mong đợi, thực tế] --> B[Antigravity: /fix-bug]
    B --> C[Test đỏ chứng minh lỗi]
    C --> D[Nguyên nhân ở file:dòng, sơ đồ cũ và mới nếu là lỗi luồng]
    D --> E{Duyệt cách sửa?}
    E -- duyệt --> F[Sửa, test đỏ chuyển xanh, chạy cả bộ]
    E -- chưa đủ bằng chứng --> B
    F --> G[Commit fix, tiếp như mục 3.1 từ /pre-pr-check]
```

Không tái hiện được lỗi thì Antigravity dừng và hỏi thêm, không sửa "phòng hờ". Lỗi trên bản đã phát hành thì làm
nhánh `hotfix/*` theo CONTRIBUTING.md mục 6.

### 3.3 Cuối sprint (Claude app)

1. **Sprint review và retro:** Đình và Huy demo cho nhau. Kết quả retro (giữ, bỏ, thử) gửi vào dự án trên claude.ai.
2. **Release:** Claude app hướng dẫn theo CONTRIBUTING.md mục 6 (`release/vX.Y.0` → `main`, tag, merge ngược).
3. **Lập kế hoạch sprint sau:** Claude app tổng kết sprint vừa xong từ GitHub (PR đã merge, card còn dở, điểm đã
   làm), Đình chốt phạm vi; Claude viết `docs/backlog/sprint-*.md`, cập nhật `docs/roadmap.md`, chia card cho Đình
   và Huy, mở PR tài liệu vào `develop`. Đình dán card lên Trello.
4. Lỗi AI lặp lại trong sprint: thêm **một dòng** vào `AGENTS.md` (mục 4 hoặc 7) theo AGENTS.md mục 6.

## 4. Skill

### Antigravity (`.agents/skills/`)

Gõ `/<tên>` trong khung chat, hoặc nói việc cần làm; agent tự chọn skill theo mô tả.

| Skill            | Khi nào                        | Kết quả                                                                                                                     |
| ---------------- | ------------------------------ | --------------------------------------------------------------------------------------------------------------------------- |
| `/plan-task`     | Bắt đầu card, tính năng        | Kế hoạch: mục tiêu, ý tưởng, sơ đồ Mermaid, file sẽ sửa, test; dừng chờ duyệt                                               |
| `/fix-bug`       | Có lỗi, test đỏ chưa rõ vì sao | Test đỏ, nguyên nhân `file:dòng`, sơ đồ luồng cũ/mới, cách sửa; dừng chờ duyệt                                              |
| `/implement`     | Kế hoạch đã duyệt              | Kiểm tra nhánh; làm phần của PR hiện tại, mỗi tầng có `/write-tests`; bảng đối chiếu hành vi và test; dừng cho người commit |
| `/write-tests`   | Viết, bổ sung test             | Bảng ca test đủ sáu nhóm (AGENTS.md mục 10), test đã thử làm đỏ, kết quả chạy                                               |
| `/new-migration` | Đổi schema                     | `V{n}__...sql` mới, entity, ERD, test schema                                                                                |
| `/pre-pr-check`  | Xong card, trước khi push      | Kiểm tra nhánh, kích thước PR, file code thiếu test; format, lint, test; checklist; bản nháp PR và lệnh git                 |

Câu mẫu:

- `/plan-task S2-06` hoặc "Lập kế hoạch cho card S2-06 quản lý nhóm dịch vụ".
- `/fix-bug Lưu nhân viên trùng tên với người đã nghỉ vẫn báo lỗi 409, mong đợi là lưu được.`
- "Kế hoạch ổn, `/implement` đi, làm tới hết backend thì dừng cho mình commit."
- `/pre-pr-check`

Sơ đồ trong kế hoạch là Mermaid: dán vào mục "Ghi chú cho người review" của PR, GitHub hiển thị thành hình.

### Claude Code (máy Đình)

| Skill             | Khi nào                        | Kết quả                                                                                                                                      |
| ----------------- | ------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------- |
| `/review-pr <số>` | PR của Huy cần review          | Chạy test trên worktree riêng, bản nháp nhận xét `bắt buộc:`/`gợi ý:`/`hỏi:`; Đình đồng ý thì đăng lên GitHub (Request changes hoặc Comment) |
| `/self-review`    | Code của Đình, trước khi mở PR | Báo cáo cần sửa / nên sửa, kèm lời nhắn sửa để dán vào Antigravity; khi sạch thì soạn PR có dòng "Đã tự review"                              |

Claude không approve và không merge; nhận xét đăng bằng tài khoản của Đình nên luôn có bước Đình đọc trước.

### Claude app

Skill `plan-sprint` trong tài khoản claude.ai của Đình: gọi khi bắt đầu lập kế hoạch sprint mới (mục 3.3).

## 5. Hook

Mã nguồn: `tools/agent-hooks/` (Node, không cần cài thêm). Test: `node --test tools/agent-hooks/hooks.test.mjs`.

| Hook         | Khi nào                       | Làm gì                                                                                                                                                                                                                                                    |
| ------------ | ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `guard.mjs`  | Trước mỗi lệnh, đọc, sửa file | Chặn git ngoài nhóm chỉ đọc; `gh` làm đổi GitHub; xóa hàng loạt; `docker compose down -v`; đọc, sửa `.env`, khóa; sửa migration đã có trên `develop`; sửa file sinh tự động; sửa cấu hình hook                                                            |
| `format.mjs` | Sau khi Antigravity sửa file  | Chạy Prettier trên đúng file frontend vừa sửa                                                                                                                                                                                                             |
| `stop.mjs`   | Khi Antigravity kết thúc lượt | File Java đổi thì chạy `./gradlew spotlessApply` một lần; có code đổi thì nhắc tự soát test, Postman, backlog, báo cáo, kèm danh sách file code đổi mà chưa có test đổi theo, và nhắc tạo nhánh nếu đang ở `develop`/`main` (mỗi trạng thái nhắc một lần) |

Claude Code dùng chung `guard.mjs` (được thêm `git fetch`, `git worktree`, đăng review bằng `gh`; vẫn không
approve, merge, push).

**Khi bị chặn:** agent sẽ dừng và ghi lệnh cần chạy. Nếu đúng là việc của người thì tự chạy lệnh đó. Nếu thấy hook
chặn nhầm, sửa `tools/agent-hooks/` bằng tay, thêm test vào `hooks.test.mjs`, mở PR; đừng tắt hook. Agent không
được tự sửa thư mục này.

Sau khi agent ghi file, Prettier có thể đổi định dạng file đó; agent cần đọc lại file trước khi sửa tiếp (bình thường,
không phải lỗi).

## 6. Hỏi nhanh

- **Agent làm sai cùng một kiểu nhiều lần?** Thêm một dòng vào `AGENTS.md` mục 4 hoặc mục 7, trong PR của card đang làm.
- **Muốn đổi skill?** Sửa `.agents/skills/<tên>/SKILL.md` trong một PR riêng (`docs` hoặc `chore`), ghi lý do.
- **Antigravity hỏi có chạy lệnh git không?** Chỉ đồng ý với `status`, `log`, `diff`, `show`, `blame`, `branch`.
- **Test backend đỏ hàng loạt vì không kết nối được database?** Mở Docker Desktop rồi chạy lại.
