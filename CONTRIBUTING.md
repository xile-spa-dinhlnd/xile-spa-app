# Quy trình làm việc nhóm

Áp dụng cho cả người và AI. Nhóm hai người (Đình, Huy), Scrum rút gọn, sprint 2 tuần, task trên Trello.
Quy tắc code nằm ở `AGENTS.md`, `backend/AGENTS.md`, `frontend/AGENTS.md`; file này chỉ nói về **nhánh,
commit, Pull Request và nhịp làm việc**.

## 1. Luồng một card, từ đầu đến cuối

1. Kéo card từ `Sprint Backlog` sang `Đang làm`, gán tên mình. Mỗi người **tối đa 2 card** ở `Đang làm`.
2. Cập nhật `develop` rồi tạo nhánh cho card từ `develop` (mục 2).
3. Làm từng bước nhỏ, commit theo mục 3, đẩy nhánh lên GitHub ít nhất cuối mỗi buổi làm.
4. Mở Pull Request **vào `develop`** (mục 4). Chưa xong thì mở dạng **Draft** để Đình xem sớm.
5. Kéo card sang `Review (PR)`. PR của Huy: gán Đình review và nhắn. PR của Đình: tự review theo danh sách soát ở mục 4.
6. Sửa theo review, build và test chạy qua (CI xanh khi đã có CI), đủ điều kiện merge (mục 4) → **Squash and merge** vào `develop`, xóa nhánh.
7. Kéo card sang `Xong` khi đủ Definition of Done (`docs/backlog/sprint-0-1.md` mục 1), cập nhật cột
   "Cột Trello" trong file backlog.

Bị chặn (chờ người kia, chờ chủ tiệm trả lời OQ, lỗi môi trường): kéo card sang `Bị chặn`, ghi lý do trong
card và nhắn ngay, đừng ngồi chờ. Trong lúc đó nhận card khác.

## 2. Nhánh (Git Flow)

Nhóm dùng Git Flow để luyện tập quy trình có nhánh phát hành:

```
feature/*, fix/*, docs/*, chore/*  ──squash──▶  develop  ──release/*──merge──▶  main (+ tag vX.Y.Z)
                                                   ▲                                │
                                                   └──────── merge ngược ◀──────────┘
                                                                       hotfix/* ──merge──▶ main
```

| Nhánh       | Vai trò                                                         | Tách từ   | PR vào                     | Cách merge   |
| ----------- | --------------------------------------------------------------- | --------- | -------------------------- | ------------ |
| `main`      | Bản đã chốt, mỗi commit trên đó là một phiên bản có tag         | —         | —                          | —            |
| `develop`   | Nhánh làm việc hằng ngày, **nhánh mặc định** của repo           | `main`    | —                          | —            |
| `feature/*`, `fix/*`, `docs/*`, `chore/*` | Một card                          | `develop` | `develop`                  | Squash       |
| `release/*` | Chuẩn bị phát hành cuối sprint, chỉ sửa lỗi nhỏ                 | `develop` | `main`                     | Merge commit |
| `hotfix/*`  | Sửa gấp lỗi trên bản đã phát hành                               | `main`    | `main`                     | Merge commit |
| (merge ngược) | Đưa thay đổi của release/hotfix về lại `develop`              | —         | PR từ `main` vào `develop` | Merge commit |

Quy tắc:

- **Không ai đẩy thẳng lên `main` hay `develop`**; GitHub chặn bằng ruleset (mục 7). Mọi thay đổi qua PR.
- `develop` luôn build được và chạy được trên máy dev. `main` chỉ đổi khi phát hành (mục 6).
- Một card một nhánh, tách từ `develop` mới nhất:

  | Loại              | Mẫu tên                     | Ví dụ                         |
  | ----------------- | --------------------------- | ----------------------------- |
  | Tính năng         | `feature/<mã-card>-<mô-tả>` | `feature/S1-05-service-crud`  |
  | Sửa lỗi           | `fix/<mã-card>-<mô-tả>`     | `fix/S1-03-refresh-loop`      |
  | Tài liệu          | `docs/<mã-card>-<mô-tả>`    | `docs/S0-02-git-flow`         |
  | Hạ tầng, cấu hình | `chore/<mã-card>-<mô-tả>`   | `chore/S0-02-gitignore`       |
  | Phát hành         | `release/v<phiên-bản>`      | `release/v0.1.0`              |
  | Sửa gấp bản đã phát hành | `hotfix/v<phiên-bản>-<mô-tả>` | `hotfix/v0.1.1-closing-timezone` |

- Mô tả viết thường, không dấu, nối bằng `-`, tối đa khoảng 5 từ.
- Nhánh card sống **ngắn** (lý tưởng 1 đến 3 ngày). Card lớn hơn thì chia nhiều PR nhỏ trên nhiều nhánh
  (ví dụ `feature/S1-01-login`, `feature/S1-01-refresh`, `feature/S1-01-logout`).
- Bắt đầu một card:

  ```bash
  git switch develop
  git pull
  git switch -c feature/S1-05-service-crud
  ```

- Cập nhật nhánh card theo `develop` mỗi ngày bằng rebase:

  ```bash
  git fetch origin
  git rebase origin/develop
  git push --force-with-lease
  ```

  Chỉ `--force-with-lease` trên **nhánh card của mình**, không bao giờ trên `main`, `develop`, `release/*`
  hay nhánh người khác.

## 3. Commit

### Định dạng: Conventional Commits

```
<loại>(<phạm vi>): <mô tả ngắn>

<thân: vì sao đổi, điều gì chưa rõ — không bắt buộc>

Refs: <mã card>, <mã FR/BR nếu có>
```

**Loại:**

| Loại       | Khi nào                                                    |
| ---------- | ---------------------------------------------------------- |
| `feat`     | Thêm chức năng người dùng thấy được                        |
| `fix`      | Sửa lỗi                                                    |
| `refactor` | Đổi cấu trúc code, không đổi hành vi                       |
| `test`     | Chỉ thêm hoặc sửa test                                     |
| `docs`     | Chỉ tài liệu (SRS, ERD, ADR, README, AGENTS)               |
| `build`    | Gradle, npm, Dockerfile, phiên bản thư viện                |
| `ci`       | GitHub Actions                                             |
| `chore`    | Việc lặt vặt khác (`.gitignore`, `.editorconfig`, cấu hình) |
| `perf`     | Tăng hiệu năng                                             |
| `style`    | Chỉ format (Spotless, Prettier), không đổi logic           |
| `revert`   | Hoàn tác một commit trước                                  |

**Phạm vi:** tên module (`auth`, `catalog`, `customer`, `visit`, `expense`, `closing`, `dashboard`,
`staff`, `payroll`, `importer`, `sync`, `common`, `security`) hoặc phần hạ tầng (`db`, `deploy`, `ci`, `fe`, `be`, `docs`). Có thể bỏ trống nếu
thay đổi trải khắp repo.

**Mô tả ngắn:**

- Tiếng Việt có dấu, bắt đầu bằng động từ, viết thường, không chấm cuối, tối đa khoảng 72 ký tự.
- Nói **làm gì**, không nói "sửa code", "update", "wip".
- Thay đổi phá vỡ tương thích (đổi API mà frontend đang gọi): thêm `!` sau phạm vi, ví dụ
  `feat(catalog)!: đổi giá thành trường price_vnd`.

**Ví dụ tốt:**

```
feat(catalog): thêm API ngừng bán và bán lại dịch vụ
fix(auth): không tiết lộ email tồn tại khi đăng nhập sai
feat(db): thêm migration V3 cho bảng service_group
test(visit): kiểm tra không tạo giao dịch vào ngày đã chốt
docs(srs): cập nhật BR-04 theo câu trả lời OQ-06
build(be): nâng Spring Boot lên 4.1.2 trong version catalog
```

**Ví dụ không dùng:** `update`, `fix bug`, `wip`, `sửa linh tinh`, `final`, `final 2`.

### Khi nào commit

- **Mỗi commit là một bước nhỏ hoàn chỉnh**: build được, test liên quan chạy qua. Ví dụ một card S1-05 có
  thể là: entity + repository → service + test → controller + test tích hợp → audit.
- Commit **khi một bước nhỏ vừa chạy đúng**, đừng gom cả ngày vào một commit, cũng đừng commit từng dòng.
- **Đẩy nhánh lên GitHub cuối mỗi buổi làm**, kể cả chưa xong (để sao lưu và để người kia xem được).
  Nhánh của mình được phép có commit dở dang; khi merge vào `develop` sẽ squash nên lịch sử vẫn sạch.
- **Migration và code dùng nó đi cùng một PR.** Không merge migration "để đó".
- Không trộn việc khác vào commit: format lại cả file, đổi tên hàng loạt, sửa lỗi ngoài card → commit
  riêng hoặc card riêng.

### Không bao giờ commit

- `.env`, mật khẩu, khóa API, file service account Google, dữ liệu khách thật, file Excel của tiệm.
- Thư mục build (`build/`, `dist/`, `node_modules/`, `.gradle/`), file của IDE.
- Code bị comment bỏ, `console.log`/`System.out.println` để debug, test bị `@Disabled`/`skip`.
- Code do AI sinh mà mình **chưa đọc diff**. Người commit chịu trách nhiệm cho mọi dòng.

### Kiểm tra trước khi commit

```bash
cd backend && ./gradlew spotlessApply test
```

```bash
cd frontend && npm run lint && npm run typecheck && npm test
```

(Lệnh frontend sẽ cập nhật sau khi có khung S0-08. Test backend cần Docker Desktop đang chạy.)

## 4. Pull Request

- **Base đúng nhánh:** PR card vào `develop` (mặc định). Chỉ `release/*` và `hotfix/*` mới mở PR vào `main`.
- **Tiêu đề** theo đúng định dạng commit, thêm mã card ở cuối, vì khi squash tiêu đề PR thành commit
  trên `develop`: `feat(catalog): quản lý dịch vụ [S1-05]`.
- **Nội dung** theo `.github/pull_request_template.md`: card Trello, mã FR/BR, đã làm gì, cách thử,
  giả định còn chưa chắc, ảnh chụp màn hình (nếu có giao diện, chụp cả cỡ điện thoại).
- **Nhỏ:** cố gắng dưới khoảng 400 dòng thay đổi (không tính file sinh tự động, lock file). Lớn hơn thì
  chia nhỏ.
- **Tự review trước:** đọc lại toàn bộ diff trên GitHub trước khi gán người review.
- **Người duyệt: Đình là người duyệt duy nhất** (code owner, file `.github/CODEOWNERS`).
  - PR của Huy: Đình review và approve, rồi Huy bấm merge. Huy có thể bình luận vào PR của Đình nhưng
    approve của Huy không thay cho approve của Đình.
  - PR của Đình: GitHub không cho tự approve PR của chính mình, nên Đình merge bằng quyền bypass của
    ruleset (ô "Merge without waiting for requirements to be met"). **Trước khi bypass phải tự review**: đọc
    lại toàn bộ diff, đi hết danh sách soát bên dưới, ghi "Đã tự review" trong PR. Với phần nhạy cảm
    (đăng nhập, tiền, chốt sổ, migration), nên nhờ Huy đọc và bình luận dù không cần Huy approve.
- **Điều kiện merge** (ruleset của `develop` và `main`, mục 7): approve của Đình (hoặc Đình bypass với PR
  của chính mình), không còn bình luận chưa giải quyết, người viết đã chạy build + test ở máy. Từ khi có CI (card D-01): thêm CI xanh.
- **Cách merge:** PR card vào `develop` dùng **Squash and merge**. PR vào `main` (release, hotfix) và PR
  merge ngược `main` → `develop` dùng **Create a merge commit**, không squash (squash ở đây làm hai nhánh
  lệch lịch sử, lần phát hành sau sẽ xung đột). Người viết PR bấm merge; nhánh card, `release/*`, `hotfix/*`
  tự xóa sau khi merge.
- Thay đổi hành vi thì cập nhật tài liệu **trong cùng PR** (SRS, ERD, README).

### Review

- Đình review PR của Huy **trong vòng 1 ngày làm việc**. Không kịp thì nhắn báo.
- Đình ưu tiên review PR đang chờ trước khi bắt đầu card mới: PR chờ lâu làm cả nhóm chậm.
- Ghi tiền tố cho bình luận để rõ mức độ:
  - `bắt buộc:` phải sửa mới merge.
  - `gợi ý:` nên sửa, người viết quyết định.
  - `hỏi:` chưa hiểu, cần giải thích (có thể kèm sửa tài liệu).
- Danh sách soát khi review:
  - [ ] Tiền là số nguyên, không tự tính tiền ở giao diện.
  - [ ] Không sửa dữ liệu ngày đã chốt; sửa sai bằng bút toán điều chỉnh.
  - [ ] Thay đổi tài chính, danh mục có ghi `audit_log`.
  - [ ] Không có bí mật, không log mật khẩu, token.
  - [ ] Migration mới, không sửa migration cũ.
  - [ ] Có test cho logic nghiệp vụ, test chạy thật chứ không bị bỏ qua.
  - [ ] Đúng cấu trúc thư mục trong `backend/AGENTS.md` / `frontend/AGENTS.md`.
- Phần nhạy cảm (đăng nhập, tiền, chốt sổ, migration) review kỹ hơn, có thể kéo nhánh về chạy thử.

### Xung đột

- Ai merge **sau** thì người đó rebase lên `origin/develop` và xử lý xung đột.
- **Số migration Flyway**: trước khi tạo `V{n}__...`, nhắn người kia số mình dùng. Nếu `develop` đã có số đó
  thì đổi tên file của mình sang số kế tiếp trước khi merge (file của mình chưa chạy ở đâu ngoài máy dev).

## 5. Nhịp làm việc trong sprint

| Hoạt động                     | Khi nào                    | Làm gì                                                                                   |
| ----------------------------- | -------------------------- | ---------------------------------------------------------------------------------------- |
| Sprint planning               | Đầu sprint, khoảng 1 giờ   | Chọn card từ `Product Backlog`, chia việc, ước lượng điểm, chốt mục tiêu sprint         |
| Check-in hằng ngày (nhắn tin) | Mỗi ngày làm việc          | Ba dòng: hôm qua làm gì, hôm nay làm gì, đang bị chặn bởi gì                             |
| Sprint review                 | Cuối sprint                | Demo trên máy dev (sau này trên VPS); gom câu hỏi mở (OQ) hỏi chủ tiệm                  |
| Retro                         | Ngay sau review, 15–30 phút | Một điều giữ, một điều bỏ, một điều thử; ghi thành card hoặc sửa file quy tắc này       |

Lịch cụ thể chốt ở card S0-01.

**Chia việc:** theo tính năng, người nhận tính năng làm cả backend lẫn frontend của nó (ADR-0001). Phần
dùng chung (`common`, `shared`, cấu hình CI, Docker) đổi thì báo trước cho người kia.

**Học cùng nhau:** card DevOps hoặc phần một người chưa quen thì làm cặp (một người gõ, một người xem),
hoặc người kia review kỹ và hỏi lại cho đến khi hiểu.

**Câu hỏi nghiệp vụ:** không tự đoán. Ghi vào `docs/srs/07-open-questions.md` (mã OQ), gom hỏi chủ tiệm
theo lô ở buổi review, ghi lại câu trả lời.

## 6. Phát hành

**Phiên bản:** `v0.MINOR.PATCH`. Mỗi lần phát hành cuối sprint tăng MINOR (`v0.1.0` sau Sprint 0, `v0.2.0`
sau Sprint 1...); hotfix tăng PATCH (`v0.1.1`). Lên `v1.0.0` khi chủ tiệm bắt đầu dùng thật.

### Phát hành cuối sprint (release)

Làm sau buổi sprint review, khi mọi card của sprint đã vào `develop`. Đình làm và duyệt (bypass như PR của
mình, mục 4); Huy có thể cùng xem để học.

1. Tạo nhánh release từ `develop`:

   ```bash
   git switch develop
   git pull
   git switch -c release/v0.1.0
   git push -u origin release/v0.1.0
   ```

2. Chạy thử toàn bộ trên máy. Lỗi nhỏ thì sửa **ngay trên `release/v0.1.0`** (commit `fix: ...`).
   **Không** thêm tính năng mới vào nhánh release; tính năng mới vẫn vào `develop` như thường.
3. Mở PR `release/v0.1.0` → `main`, tiêu đề `release: v0.1.0`, nội dung liệt kê các card của sprint.
   Đình tự review → **Create a merge commit** (bypass, mục 4).
4. Gắn tag trên `main`:

   ```bash
   git switch main
   git pull
   git tag -a v0.1.0 -m "Phát hành v0.1.0: Sprint 0"
   git push origin v0.1.0
   ```

   Rồi tạo GitHub Release từ tag (Releases → Draft a new release → chọn tag → Generate release notes).
5. **Merge ngược** về `develop`: mở PR `main` → `develop`, tiêu đề `chore: merge ngược v0.1.0 về develop`,
   Đình duyệt → **Create a merge commit**. Bước này đưa các lỗi đã sửa ở bước 2 về `develop`; không làm thì lỗi
   quay lại ở lần phát hành sau.
6. Cập nhật máy: `git switch develop && git pull`.

### Sửa gấp bản đã phát hành (hotfix)

Chỉ dùng khi lỗi nằm trên bản đã phát hành và **không chờ được** đến release sau. Lỗi thường thì làm `fix/*`
từ `develop` như card bình thường.

1. Tách từ `main`:

   ```bash
   git switch main
   git pull
   git switch -c hotfix/v0.1.1-closing-timezone
   ```

2. Sửa, viết test chứng minh lỗi đã hết, commit `fix(...): ...`.
3. Mở PR `hotfix/...` → `main`. Hotfix của Huy cần Đình approve; của Đình thì tự review rồi bypass →
   **Create a merge commit**.
4. Gắn tag `v0.1.1` trên `main` như bước 4 ở trên, tạo GitHub Release.
5. **Merge ngược** `main` → `develop` như bước 5 ở trên. Bắt buộc, nếu không `develop` vẫn còn lỗi.

Khi đã có VPS (giai đoạn DevOps): bản trên `main` có tag mới được deploy lên production; `develop` sẽ là
nguồn cho môi trường thử (staging).

## 7. Cấu hình GitHub (đã bật, ghi lại để tra)

Chỉ Owner (Đình) sửa được các cấu hình này. Đổi gì thì cập nhật mục này trong cùng PR.

- **Default branch:** `develop`.
- **Pull Requests:** bật Allow merge commits và Allow squash merging (message mặc định: "Pull request title
  and description"); tắt Allow rebase merging; bật Automatically delete head branches.
- **Ruleset `protect-develop`** (target `develop`): Restrict deletions, Block force
  pushes, Require a pull request (1 approval, dismiss stale approvals, **require review from Code Owners**,
  require conversation resolution). Bypass list: chỉ Đình, chế độ **For pull requests only** (vẫn phải qua
  PR, không đẩy thẳng được, nhưng merge được PR của mình khi chưa có approve). Allowed merge methods: **Squash, Merge** (squash cho PR
  card, merge cho PR merge ngược).
- **Ruleset `protect-main`** (target `main`): như trên, cùng bypass list, Allowed merge methods: chỉ
  **Merge**.
- **Code owner:** `.github/CODEOWNERS` gán toàn bộ repo cho Đình.
- **Quyền:** team `core` (Đình, Huy) quyền Write. Base permissions của org: Read hoặc No permission. Bắt buộc
  2FA.
- **Code security:** Dependabot alerts, Dependabot security updates, Secret scanning, Push protection.
- **Chưa bật:** Require status checks (bật ở card D-01 khi có CI).
