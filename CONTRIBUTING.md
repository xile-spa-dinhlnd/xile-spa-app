# Quy trình làm việc nhóm

Áp dụng cho cả người và AI. Nhóm hai người (Đình, Huy), Scrum rút gọn, sprint 2 tuần, task trên Trello.
Quy tắc code nằm ở `AGENTS.md`, `backend/AGENTS.md`, `frontend/AGENTS.md`; file này chỉ nói về **nhánh,
commit, Pull Request và nhịp làm việc**.

## 1. Luồng một card, từ đầu đến cuối

1. Kéo card từ `Sprint Backlog` sang `Đang làm`, gán tên mình. Mỗi người **tối đa 2 card** ở `Đang làm`.
2. Cập nhật `main` rồi tạo nhánh cho card (mục 2).
3. Làm từng bước nhỏ, commit theo mục 3, đẩy nhánh lên GitHub ít nhất cuối mỗi buổi làm.
4. Mở Pull Request (mục 4). Chưa xong thì mở dạng **Draft** để người kia xem sớm.
5. Kéo card sang `Review (PR)`, nhắn người kia.
6. Sửa theo review, build và test chạy qua (CI xanh khi đã có CI), có approve → **Squash and merge**, xóa nhánh.
7. Kéo card sang `Xong` khi đủ Definition of Done (`docs/backlog/sprint-0-1.md` mục 1), cập nhật cột
   "Cột Trello" trong file backlog.

Bị chặn (chờ người kia, chờ chủ tiệm trả lời OQ, lỗi môi trường): kéo card sang `Bị chặn`, ghi lý do trong
card và nhắn ngay, đừng ngồi chờ. Trong lúc đó nhận card khác.

## 2. Nhánh

- `main` luôn chạy được và deploy được. **Không ai đẩy thẳng lên `main`**, mọi thay đổi qua PR.
- Một card một nhánh, tách từ `main` mới nhất:

  | Loại              | Mẫu tên                    | Ví dụ                           |
  | ----------------- | -------------------------- | ------------------------------- |
  | Tính năng         | `feature/<mã-card>-<mô-tả>` | `feature/S1-05-service-crud`    |
  | Sửa lỗi           | `fix/<mã-card>-<mô-tả>`     | `fix/S1-03-refresh-loop`        |
  | Tài liệu          | `docs/<mã-card>-<mô-tả>`    | `docs/S0-03-agents-structure`   |
  | Hạ tầng, cấu hình | `chore/<mã-card>-<mô-tả>`   | `chore/S0-02-gitignore`         |
  | Sửa gấp bản chạy thật | `hotfix/<mô-tả>`        | `hotfix/closing-timezone`       |

- Mô tả viết thường, không dấu, nối bằng `-`, tối đa khoảng 5 từ.
- Nhánh sống **ngắn** (lý tưởng 1 đến 3 ngày). Card lớn hơn thì chia nhiều PR nhỏ trên nhiều nhánh
  (ví dụ `feature/S1-01-login`, `feature/S1-01-refresh`, `feature/S1-01-logout`).
- Cập nhật nhánh theo `main` mỗi ngày bằng rebase:

  ```bash
  git fetch origin
  git rebase origin/main
  git push --force-with-lease
  ```

  Chỉ `--force-with-lease` trên **nhánh của mình**, không bao giờ trên `main` hay nhánh người khác.

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
build(be): khóa phiên bản Spring Boot 3.3 trong version catalog
```

**Ví dụ không dùng:** `update`, `fix bug`, `wip`, `sửa linh tinh`, `final`, `final 2`.

### Khi nào commit

- **Mỗi commit là một bước nhỏ hoàn chỉnh**: build được, test liên quan chạy qua. Ví dụ một card S1-05 có
  thể là: entity + repository → service + test → controller + test tích hợp → audit.
- Commit **khi một bước nhỏ vừa chạy đúng**, đừng gom cả ngày vào một commit, cũng đừng commit từng dòng.
- **Đẩy nhánh lên GitHub cuối mỗi buổi làm**, kể cả chưa xong (để sao lưu và để người kia xem được).
  Nhánh của mình được phép có commit dở dang; khi merge sẽ squash nên `main` vẫn sạch.
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

(Lệnh chính xác sẽ cập nhật sau khi có khung S0-04, S0-08.)

## 4. Pull Request

- **Tiêu đề** theo đúng định dạng commit, thêm mã card ở cuối, vì khi squash tiêu đề PR thành commit
  trên `main`: `feat(catalog): quản lý dịch vụ [S1-05]`.
- **Nội dung** theo `.github/pull_request_template.md`: card Trello, mã FR/BR, đã làm gì, cách thử,
  giả định còn chưa chắc, ảnh chụp màn hình (nếu có giao diện, chụp cả cỡ điện thoại).
- **Nhỏ:** cố gắng dưới khoảng 400 dòng thay đổi (không tính file sinh tự động, lock file). Lớn hơn thì
  chia nhỏ.
- **Tự review trước:** đọc lại toàn bộ diff trên GitHub trước khi gán người review.
- **Điều kiện merge** (bật bảo vệ nhánh `main` ở S0-02): 1 approve của người kia, không còn
  bình luận chưa giải quyết, người viết đã chạy build + test ở máy. Từ khi có CI (card D-01): thêm CI xanh.
- **Cách merge:** luôn **Squash and merge**, người viết PR bấm merge, rồi xóa nhánh.
- Thay đổi hành vi thì cập nhật tài liệu **trong cùng PR** (SRS, ERD, README).

### Review

- Người được gán review **trong vòng 1 ngày làm việc**. Không kịp thì nhắn báo.
- Ưu tiên review PR của người kia trước khi bắt đầu card mới: PR chờ lâu làm cả nhóm chậm.
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

- Ai merge **sau** thì người đó rebase và xử lý xung đột.
- **Số migration Flyway**: trước khi tạo `V{n}__...`, nhắn người kia số mình dùng. Nếu `main` đã có số đó
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

- Mỗi lần deploy lên VPS từ `main` thì gắn tag `v0.<sprint>.<lần>` (ví dụ `v0.2.0`) và ghi vài dòng thay
  đổi vào GitHub Release.
- Lỗi trên bản chạy thật: nhánh `hotfix/...` từ `main`, PR vẫn cần người kia approve (có thể review
  nhanh qua tin nhắn), merge xong deploy lại và gắn tag mới.
