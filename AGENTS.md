# AGENTS.md — quy tắc chung cho AI làm việc trong repo này

File này là **nguồn quy tắc duy nhất** cho mọi công cụ AI (Claude Code, Antigravity, hoặc công cụ khác).
Antigravity tự đọc file này (và `AGENTS.md` trong thư mục con); `CLAUDE.md` chỉ trỏ về đây. Phần riêng của từng
agent nằm ở mục 8. Nếu quy tắc ở nơi khác lệch với file này thì file này đúng.

Giữ file ngắn. Quy tắc nghiệp vụ nằm ở SRS, cấu trúc dữ liệu nằm ở migration và `docs/design/erd.md`;
ở đây **chỉ trỏ đường, không chép lại**.

## 1. Dự án là gì

Hệ thống quản trị cho tiệm Spa & Giãn cơ Xile (một tiệm nhỏ, người dùng chính là chủ tiệm, dùng trên
máy tính, điện thoại và iPad). Thay cho quy trình Excel theo tháng. Phạm vi: dịch vụ và combo, sổ doanh
thu hằng ngày có chốt sổ, CRM khách hàng, lương/tiền tour, và đồng bộ một chiều ra Google Sheets.

Nhóm hai người: **Đình** và **Huy**, chia việc theo tính năng, Scrum rút gọn, sprint 2 tuần, task trên Trello.

## 2. Đọc tài liệu nào cho loại việc nào

Đừng đọc cả `docs/`. Theo bảng này:

| Việc đang làm                                           | Đọc trước                                                                                                                     |
| ------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| Bất cứ việc gì                                          | file này, `README.md`                                                                                                         |
| Tiền, giảm giá, doanh thu, chốt sổ, bút toán điều chỉnh | `docs/srs/04-business-rules.md`, `docs/design/erd.md` (mục 5 và 6), `docs/adr/0003-closed-day-corrections-via-adjustments.md` |
| Một API hoặc một màn hình mới                           | mục tương ứng trong `docs/srs/03-functional-requirements.md` (theo mã FR)                                                     |
| Bảng, cột, index, migration                             | `docs/design/erd.md`, rồi `backend/src/main/resources/db/migration/`                                                          |
| Lựa chọn công nghệ, thư viện                            | `docs/adr/0001-tech-stack.md`                                                                                                 |
| Google Sheets, nguồn dữ liệu gốc                        | `docs/adr/0002-source-of-truth-and-sheets-mirror.md`                                                                          |
| Thuật ngữ, tên trường                                   | `docs/srs/06-data-dictionary.md`                                                                                              |
| Chưa rõ nghiệp vụ, thấy mâu thuẫn                       | `docs/srs/07-open-questions.md` (mã OQ) — **hỏi lại, đừng tự quyết**                                                          |
| Card đang làm, phạm vi sprint                           | `docs/backlog/sprint-0-1.md`                                                                                                  |
| Phiên bản nào chứa gì, mốc tới v1.0.0                   | `docs/roadmap.md`                                                                                                             |
| Nhánh, commit, Pull Request, nhịp sprint                | `CONTRIBUTING.md`                                                                                                             |
| Thử API bằng Postman                                    | `docs/postman/` (collection và environment dev)                                                                               |
| Dùng skill, hook của agent; luồng làm việc với AI       | `docs/guides/ai-agents.md`                                                                                                    |

Quy tắc riêng của từng phần: `backend/AGENTS.md`, `frontend/AGENTS.md`.

## 3. Công nghệ

Java 21 · Spring Boot 4 · Gradle (Kotlin DSL) · Spring Data JPA · Spring Security (JWT trong cookie
`httpOnly`) · Flyway · PostgreSQL · React + TypeScript (Vite) · Ant Design · TanStack Query ·
JUnit 5 + Testcontainers · Vitest · Docker Compose · GitHub Actions.

Chi tiết và lý do: `docs/adr/0001-tech-stack.md`. **Muốn thêm thư viện hoặc đổi cách làm thì đối chiếu
ADR-0001 trước và nói rõ trong PR**, đừng tự thêm dependency.

### Lệnh thường dùng

Cần JDK 21, Node 22+, Docker Desktop **đang chạy** (database dev và test tích hợp dùng Testcontainers).

| Việc                       | Backend (trong `backend/`)                                                             | Frontend (trong `frontend/`)                                      |
| -------------------------- | -------------------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| Cài                        | (không cần, dùng `./gradlew`)                                                          | `npm ci`                                                          |
| Build                      | `./gradlew build`                                                                      | `npm run build`                                                   |
| Format + test đầy đủ       | `./gradlew spotlessApply test`                                                         | `npm run format && npm run lint && npm run typecheck && npm test` |
| Chạy riêng một lớp test    | `./gradlew test --tests 'com.xilespa.module.staff.service.StaffServiceTest'`           | `npx vitest run src/shared/lib/money.test.ts`                     |
| Chạy riêng một method test | `./gradlew test --tests 'com.xilespa.module.staff.service.StaffServiceTest.tenMethod'` | `npx vitest run src/shared/lib/money.test.ts -t "tên test"`       |
| Chạy ứng dụng              | `./gradlew bootRun` (profile `dev`)                                                    | `npm run dev` (http://localhost:5173)                             |
| Test của hook agent        | `node --test tools/agent-hooks/hooks.test.mjs` (chạy ở gốc repo)                       |                                                                   |

Database dev: `docker compose up -d` ở gốc repo. Windows dùng `gradlew.bat` thay cho `./gradlew`.

## 4. Quy tắc bắt buộc

1. **Tiền là số nguyên đồng.** Không `float`, không `double` cho tiền (BR-01). Định dạng `1.234.000 đ`
   chỉ ở tầng hiển thị.
2. **Mọi thay đổi schema phải có migration Flyway mới** (`V{n}__mô_tả.sql`). Không sửa, không xóa migration
   đã có (`V1__`, `V2__`, ...). **Không dựa vào Hibernate để tạo hoặc cập nhật schema** (`ddl-auto` chỉ là
   `validate` hoặc `none`), không sửa schema bằng tay ngoài Flyway.
3. **Không xóa cứng** dữ liệu nghiệp vụ. Dịch vụ, combo đã dùng thì chỉ ngừng bán (BR-19); giao dịch thì
   xóa mềm trạng thái `VOID` (BR-09).
4. **Ngày đã chốt là bất biến.** Không viết code sửa dữ liệu ngày `CLOSED`, không làm chức năng "mở lại
   sổ". Sửa sai chỉ bằng bút toán điều chỉnh (BR-06 đến BR-08, ADR-0003).
5. **Không bỏ qua test cho tiện.** Không `@Disabled`, không `skip`, không nới lỏng assert để CI xanh. Test
   đỏ nghĩa là code sai hoặc kỳ vọng sai, phải nói rõ cái nào.
6. **Không ghi bí mật vào repo.** Mật khẩu, khóa API, service account của Google, dữ liệu khách thật đều
   đọc từ biến môi trường. Không commit `.env`. Không ghi mật khẩu, token ra log.
7. **Múi giờ nghiệp vụ là `Asia/Ho_Chi_Minh`.** "Ngày làm việc" là ngày người dùng chọn, lưu tách biệt
   với thời điểm tạo bản ghi (BR-02).
8. **Mọi thay đổi dữ liệu tài chính và danh mục phải ghi `audit_log`**, trong cùng transaction với thay
   đổi gốc (BR-18).
9. **Không tự ý quyết định nghiệp vụ.** Thiếu thông tin thì tra `docs/srs/07-open-questions.md`; nếu vẫn
   chưa có câu trả lời thì dừng và hỏi, ghi giả định vào PR thay vì đoán.
10. **Một card một PR, PR nhỏ.** Không sửa file nằm ngoài phạm vi card (nhất là SRS, ERD, migration cũ)
    mà không nói trước.
11. **Giữ các file AGENTS luôn đúng với code.** Khi đổi kiến trúc, cấu trúc thư mục, quy ước đặt tên, thư viện
    hay cách làm chung thì cập nhật `AGENTS.md`, `backend/AGENTS.md` hoặc `frontend/AGENTS.md` **trong cùng
    PR** (kèm ADR nếu là quyết định công nghệ), để cả nhóm và AI theo kịp.
12. **Không sửa tay file sinh tự động:** `frontend/package-lock.json` (chỉ đổi qua `npm install`), Gradle Wrapper
    (`backend/gradlew*`, `backend/gradle/wrapper/`), sơ đồ `docs/design/diagrams/` (sinh bằng
    `docs/design/tools/gen_erd_mermaid.py`), thư mục build (`build/`, `dist/`, `node_modules/`).

## 5. Quy trình làm một card

1. Đọc card trong backlog của sprint (`docs/backlog/`) và các mã FR / BR mà card trỏ tới. **Chuyển trạng thái
   card sang "Đang làm"** trong file backlog (và trên Trello).
2. **Nêu kế hoạch trước khi viết code**: sẽ sửa file nào, thêm bảng hay API nào, test gì. Chờ người duyệt.
   Antigravity dùng skill `/plan` (tính năng) hoặc `/fix-bug` (sửa lỗi, phải tái hiện bằng test đỏ trước).
3. Viết code theo quy tắc của `backend/AGENTS.md` hoặc `frontend/AGENTS.md`.
4. Viết test cho logic nghiệp vụ. Chạy build và test tại máy trước khi commit.
5. Tự đọc lại diff, nêu rõ phần nào còn chưa chắc.
6. Cập nhật tài liệu trong **cùng PR** nếu hành vi đổi (SRS, ERD, README), và các file AGENTS nếu đổi
   kiến trúc hay quy ước (quy tắc 11).
7. **Thêm hoặc sửa endpoint thì cập nhật Postman collection** `docs/postman/xile-spa.postman_collection.json`
   trong cùng PR: mỗi endpoint một request, có ví dụ body và kiểm tra mã trạng thái mong đợi.
8. **Card xong thì chuyển trạng thái sang "Xong"** trong file backlog ngay trong PR của card đó.
9. Nhánh, commit, PR và review theo `CONTRIBUTING.md` (nhánh `feature/S1-01-...`, Conventional Commits,
   squash merge). Cấu trúc thư mục theo `backend/AGENTS.md` / `frontend/AGENTS.md`, không tự đặt chỗ mới.

## 6. Khi AI lặp lại cùng một lỗi

Thêm **một dòng** vào mục 4 hoặc vào file quy tắc của phần liên quan, đừng viết thêm một bộ quy tắc mới.
Quy tắc sinh ra từ lỗi thật mới đáng giữ.

## 7. Điểm dễ gây lỗi trong repo

Đã gặp thật, kiểm tra kỹ khi viết và khi review:

- Spring Boot 4 dùng **Jackson 3**: import `tools.jackson.databind...`, không phải `com.fasterxml.jackson.databind...`.
- Gọi `Instant.now()`, `LocalDate.now()`, `ZoneId.systemDefault()` không qua `Clock`: sai múi giờ, test không ổn định.
- Test tích hợp `deleteAll()` bảng dùng chung hoặc đếm toàn bảng `audit_log`: làm đỏ test khác chạy chung database.
- Test tích hợp đỏ hàng loạt với lỗi kết nối: Docker chưa bật, không phải code sai.
- Mapper chép từ Request vào trường nhạy cảm (id, trạng thái, giá đã chốt): phải `ignore = true`, gán trong service.
- Gọi `AuditService.record` ngoài transaction (báo lỗi `MANDATORY`) hoặc truyền entity JPA làm `before`/`after`.
- Frontend tự tính tiền, gọi `fetch` trực tiếp trong component, import sâu vào file bên trong feature khác.
- Quên cập nhật Postman collection và trạng thái card trong `docs/backlog/` (mục 5, bước 7 và 8).
- Hai nhánh cùng lấy một số migration `V{n}`: báo số cho người kia trước (CONTRIBUTING.md mục 4).
- Dùng `@Profile("!prod")` để chọn hành vi theo môi trường: thay bằng thuộc tính và `@ConditionalOnProperty` (backend/AGENTS.md mục Cấu hình).

## 8. Vai trò từng agent

| Agent                             | Ai dùng   | Làm gì                                                       | Cấu hình riêng                           |
| --------------------------------- | --------- | ------------------------------------------------------------ | ---------------------------------------- |
| Antigravity                       | Đình, Huy | Viết code, test, tài liệu của card; git chỉ được đọc         | `.agents/` (rules, skills, `hooks.json`) |
| Claude Code (máy Đình)            | Đình      | Review PR của thành viên, tự soát code của Đình trước khi PR | `.claude/` chỉ ở máy Đình, không commit  |
| Claude app (dự án trên claude.ai) | Đình      | Lập kế hoạch sprint, chia card, release, tài liệu lớn        | bộ nhớ dự án trên claude.ai              |

- Hook dùng chung `tools/agent-hooks/` chặn: lệnh git làm đổi nhánh, commit, push; xóa hàng loạt; đọc, sửa file bí
  mật; sửa migration đã có trên `develop`; sửa file sinh tự động; tự sửa cấu hình hook. Bị chặn thì dừng và báo
  người dùng, không tìm cách lách.
- Cách dùng từng skill và luồng làm việc đầy đủ: `docs/guides/ai-agents.md`.
