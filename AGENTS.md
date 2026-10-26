# AGENTS.md — quy tắc chung cho AI làm việc trong repo này

File này là **nguồn quy tắc duy nhất** cho mọi công cụ AI (Claude Code, Antigravity, hoặc công cụ khác).
Antigravity tự đọc file này; `CLAUDE.md` chỉ trỏ về đây. Quy tắc riêng của `backend/`, `frontend/` nằm ở
`AGENTS.md` trong thư mục đó và được nạp khi agent đụng file ở đó (mục 8). Phần riêng của từng agent nằm ở mục 8. Nếu quy tắc ở nơi khác lệch với file này thì file này đúng.

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
| Bố cục, câu chữ, trạng thái của màn hình                | `docs/design/ux-guidelines.md`                                                                                                |
| Bảng, cột, index, migration                             | `docs/design/erd.md`, rồi `backend/src/main/resources/db/migration/`                                                          |
| Thiết kế, sửa lỗi, đổi luồng một tính năng              | `docs/design/flows/<module>/<tính-năng>.md` (thiết kế viết trước khi code, khớp code khi mở PR; khung ở `docs/design/flows/README.md`) |
| Lựa chọn công nghệ, thư viện                            | `docs/adr/0001-tech-stack.md`                                                                                                 |
| Google Sheets, nguồn dữ liệu gốc                        | `docs/adr/0002-source-of-truth-and-sheets-mirror.md`                                                                          |
| Thuật ngữ, tên trường                                   | `docs/srs/06-data-dictionary.md`                                                                                              |
| Chưa rõ nghiệp vụ, thấy mâu thuẫn                       | `docs/srs/07-open-questions.md` (mã OQ) — **hỏi lại, đừng tự quyết**                                                          |
| Card đang làm, phạm vi sprint                           | `docs/backlog/sprint-0-1.md`                                                                                                  |
| Phiên bản nào chứa gì, mốc tới v1.0.0                   | `docs/roadmap.md`                                                                                                             |
| Nhánh, commit, Pull Request, nhịp sprint                | `CONTRIBUTING.md`                                                                                                             |
| Thử API bằng Postman                                    | `docs/postman/` (collection và environment dev)                                                                               |
| Bảo mật, dữ liệu nhạy cảm, bí mật                       | `docs/security/` (`security-checklist.md` cho mọi card)                                                                       |
| Dùng skill, hook của agent; luồng làm việc với AI       | `docs/guides/ai-agents.md`                                                                                                    |

Quy tắc riêng của từng phần: `backend/AGENTS.md`, `frontend/AGENTS.md`.

## 3. Công nghệ

Java 21 · Spring Boot 4 · Gradle (Kotlin DSL) · Spring Data JPA · Spring Security (JWT trong cookie
`httpOnly`) · Flyway · PostgreSQL · React + TypeScript (Vite) · Ant Design · TanStack Query ·
JUnit 6 + Testcontainers · Vitest · Docker Compose · GitHub Actions.

Chi tiết và lý do: `docs/adr/0001-tech-stack.md`. Thư viện đang dùng, phiên bản chính và lưu ý API: mục "Thư viện"
của `backend/AGENTS.md`, `frontend/AGENTS.md`; nguồn sự thật là `backend/gradle/libs.versions.toml` và
`frontend/package.json` (không có ở đó là chưa có, không được import).

**Thêm thư viện: hỏi người làm card trước khi cài, không tự thêm** (AI cũng vậy, kể cả thư viện chỉ dùng cho test).
Câu hỏi nêu đủ bốn điểm:

1. **Mục đích:** dùng làm gì, vì sao thư viện đang có (bảng "Thư viện") không làm được.
2. **Ưu, nhược:** so với cách không thêm thư viện và với một lựa chọn khác.
3. **Uy tín:** ai duy trì, giấy phép, lần phát hành gần nhất, mức độ được dùng (lượt tải, sao GitHub).
4. **Hiệu năng:** frontend thì tăng bao nhiêu kích thước bundle; backend thì thời gian khởi động, bộ nhớ; nặng nhẹ khi cài.

Được đồng ý mới cài; trong cùng PR ghi một dòng "Cập nhật" ở ADR-0001 và thêm dòng vào bảng "Thư viện".

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
10. **Một card một nhánh một PR.** Không sửa file nằm ngoài phạm vi card (nhất là SRS, ERD, migration cũ)
    mà không nói trước. Code chính (không tính test, Postman, tài liệu) dự kiến quá khoảng 400 dòng thì nêu trong
    thiết kế để tách card khi lập sprint (CONTRIBUTING.md mục 2); việc không thuộc card đi PR riêng.
11. **Giữ các file AGENTS luôn đúng với code.** Khi đổi kiến trúc, cấu trúc thư mục, quy ước đặt tên, thư viện
    (thêm, bỏ, nâng phiên bản chính) hay cách làm chung thì cập nhật `AGENTS.md`, `backend/AGENTS.md` hoặc `frontend/AGENTS.md` **trong cùng
    PR** (kèm ADR nếu là quyết định công nghệ), để cả nhóm và AI theo kịp.
12. **Không sửa tay file sinh tự động:** `frontend/package-lock.json` (chỉ đổi qua `npm install`), Gradle Wrapper
    (`backend/gradlew*`, `backend/gradle/wrapper/`), sơ đồ `docs/design/diagrams/` (sinh bằng
    `docs/design/tools/gen_erd_mermaid.py`), thư mục build (`build/`, `dist/`, `node_modules/`).

## 5. Quy trình làm một card

1. **Thiết kế trước khi code.** Đọc card trong backlog của sprint (`docs/backlog/`) và các mã FR / BR / OQ mà card
   trỏ tới, rồi viết **file luồng** `docs/design/flows/<module>/<tính-năng>.md` theo khung ở
   `docs/design/flows/README.md`: điểm vào, sơ đồ, quy tắc, lỗi, chỗ bị ảnh hưởng, **bảo mật** (theo
   `docs/security/security-checklist.md` phần A), bảng test. Huy dùng `/plan-task`
   của Antigravity; Đình dùng `/plan-feature` của Claude Code. Sửa lỗi dùng `/fix-bug` (tái hiện bằng test đỏ trước).
   Chờ người làm card duyệt. **Chuyển trạng thái card sang "Đang làm"** trong file backlog (và trên Trello).
2. **Nhánh:** skill đầu tiên ghi file của card tự tạo nhánh `<loại>/<mã-card>-<mô-tả>` từ `origin/develop` khi đang
   ở `develop` hoặc `main` (`.agents/rules/writer-role.md` mục "Nhánh"; mẫu tên ở CONTRIBUTING.md mục 2).
3. Viết code theo đúng file luồng và quy tắc của `backend/AGENTS.md` hoặc `frontend/AGENTS.md` (`/implement <file
   luồng>`). Card lớn thì file luồng chia phần, code và soát từng phần (`/implement <file luồng> phần <N>`, mục "Chia
   phần" của `docs/design/flows/README.md`). Thiết kế sai hoặc không làm được thì dừng, hỏi, sửa thiết kế trước.
4. Viết test cho logic nghiệp vụ. Chạy build và test tại máy.
5. Tự đọc lại diff, nêu rõ phần nào còn chưa chắc. Đình chạy `/self-review` (Claude Code) trước khi mở PR.
6. **File luồng khớp code** trước khi mở PR: sơ đồ, lỗi, tên test thật, `Trạng thái: khớp code`, dòng lịch sử.
   Cập nhật tài liệu khác trong **cùng PR** nếu hành vi đổi (SRS, ERD, README), và các file AGENTS nếu đổi kiến
   trúc hay quy ước (quy tắc 11). Bảo mật: làm phần D của `docs/security/security-checklist.md` (cập nhật bề mặt tấn
   công, bí mật, mô hình mối đe dọa nếu đổi).
7. **Thêm hoặc sửa endpoint thì cập nhật Postman collection** `docs/postman/xile-spa.postman_collection.json`
   trong cùng PR: mỗi endpoint một request, có ví dụ body và kiểm tra mã trạng thái mong đợi.
8. **Card xong thì chuyển trạng thái sang "Xong"** trong file backlog ngay trong PR của card đó.
9. **Git chỉ hướng dẫn ở một chỗ:** người làm card gõ `/pre-pr-check` (Antigravity) khi sẵn sàng; skill đó chạy kiểm
   tra rồi đưa câu commit, lệnh push, bản nháp PR và cách squash merge theo `CONTRIBUTING.md`. Cấu trúc thư mục theo
   `backend/AGENTS.md` / `frontend/AGENTS.md`, không tự đặt chỗ mới.

## 6. Khi AI lặp lại cùng một lỗi

Thêm **một dòng** vào mục 4, mục 7 hoặc file quy tắc của phần liên quan, đừng viết thêm một bộ quy tắc mới.
Quy tắc sinh ra từ lỗi thật mới đáng giữ.

- **Đúng chỗ:** lỗi chỉ dính backend hoặc chỉ dính frontend ghi vào `backend/AGENTS.md`, `frontend/AGENTS.md`, cạnh
  quy tắc cùng chủ đề. Mục 7 chỉ giữ lỗi dính nhiều phần (backend và frontend, Postman, tài liệu, quy trình).
- **Giới hạn độ dài:** mọi agent nạp các file này ở mỗi phiên, nên `AGENTS.md` không quá khoảng 250 dòng,
  `backend/AGENTS.md` và `frontend/AGENTS.md` không quá khoảng 320 dòng mỗi file. Thêm mà vượt thì gộp các dòng cùng
  ý, hoặc bỏ dòng mà lint, test hay hook đã tự chặn; không nới giới hạn.

## 7. Điểm dễ gây lỗi trong repo

Đã gặp thật, kiểm tra kỹ khi viết và khi review. Lỗi riêng của backend, frontend nằm ở file của phần đó (mục 6).

- Quên cập nhật Postman collection và trạng thái card trong `docs/backlog/` (mục 5, bước 7 và 8).
- Request Postman dùng `{{biến}}` do thư mục khác tạo (S2-07, S3-08): mỗi thư mục tự tạo dữ liệu nó cần, chạy riêng
  vẫn xanh; body bị từ chối trước khi đọc id (sai kiểu, sai Content-Type) thì ghi giá trị cố định.
- Hai nhánh cùng lấy một số migration `V{n}`: báo số cho người kia trước (CONTRIBUTING.md mục 4).
- Đổi hành vi một endpoint (tham số mặc định, trường trả về, mã lỗi) mà không tìm nơi gọi: grep URL trong
  `frontend/src`, `backend/src/test`, `docs/postman` và sửa theo trong cùng PR (S2-06: ô chọn nhóm hiện nhóm đã ngừng dùng).
- Chuyển method sang lớp khác mà để lại bản cũ không ai gọi, hoặc hai method repository làm cùng một việc: grep tên cũ, xóa.
- Thêm ràng buộc trạng thái (ngừng dùng, đã chốt...) mà chỉ chặn ở một chỗ: liệt kê mọi method đưa bản ghi vào hoặc ra
  khỏi trạng thái đó, xét cả bản ghi tham chiếu tới (OQ-21: ngừng dùng nhóm, bán lại dịch vụ trong nhóm đã ngừng).
- Test chỉ kiểm một phần kết quả: không kiểm từng trường của danh sách con, không đọc lại từ DB sau khi sửa, không
  kiểm thứ tự khi API hứa sắp xếp (S2-07: bỏ `orphanRemoval` hay đảo chiều sắp xếp mà test vẫn xanh).
- Câu commit tiếng Anh hoặc dùng `fix` cho việc mới của card: theo CONTRIBUTING.md mục 3, dùng câu gợi ý ở cuối `/implement`, `/self-review` (mục 8).

## 8. Vai trò từng agent

| Agent                             | Ai dùng   | Làm gì                                                       | Cấu hình riêng                           |
| --------------------------------- | --------- | ------------------------------------------------------------ | ---------------------------------------- |
| Antigravity                       | Đình, Huy | Thiết kế (Huy), code, test, tài liệu; tạo nhánh card; hướng dẫn git ở `/pre-pr-check` | `.agents/` (rules, skills, `hooks.json`) |
| Claude Code (máy Đình)            | Đình      | Thiết kế tính năng cho Đình, tự soát code của Đình, review PR của Huy (chấm điểm)     | `.claude/` chỉ ở máy Đình, không commit  |
| Claude app (dự án trên claude.ai) | Đình      | Lập kế hoạch sprint, chia card, release, tài liệu lớn        | bộ nhớ dự án trên claude.ai              |

- Hook dùng chung `tools/agent-hooks/` chặn: lệnh git làm đổi nhánh, commit, push (ngoại lệ duy nhất: Antigravity
  `git fetch origin` và `git switch -c <loại>/<tên> origin/develop` khi đang ở `develop` hoặc `main`); xóa hàng loạt;
  đọc, sửa file bí mật; sửa migration đã có trên `develop`; sửa file sinh tự động; tự sửa cấu hình hook. Bị chặn thì
  dừng và báo người dùng, không tìm cách lách.
- **Câu commit gợi ý:** cuối mỗi lần `/implement` (kể cả một phần, một khối sửa), `/fix-bug`, và khi `/self-review`
  thấy một phần sạch, báo cáo có một câu commit theo CONTRIBUTING.md mục 3 cho thay đổi của lần đó. Chỉ câu, không
  lệnh. Việc trong card dùng `feat`, `refactor`, `test`, `docs`, không dùng `fix` (`fix` chỉ cho lỗi đã có trên
  `develop`). Người làm card tự commit.
- **Lệnh git chỉ `/pre-pr-check` hướng dẫn** (push, mở PR, squash, dọn nhánh), và chỉ chạy khi người dùng gõ. Skill
  khác, hook và Claude không đưa lệnh git.
- **Nạp quy tắc của thư mục con:** nội dung chỉ viết ở `backend/AGENTS.md`, `frontend/AGENTS.md`. Antigravity nạp
  qua rule glob `.agents/rules/backend.md`, `frontend.md`; Claude Code nạp qua `backend/CLAUDE.md`,
  `frontend/CLAUDE.md` (một dòng `@AGENTS.md`). Thêm thư mục có `AGENTS.md` riêng thì thêm cả hai lớp trỏ này.
- **Review PR** (người hay AI) theo `CONTRIBUTING.md` mục 4 "Review": mức bình luận, thứ tự đọc, danh sách soát.
  Nhận xét do AI soạn chỉ là bản nháp, người review đọc và quyết định trước khi đăng.
- Cách dùng từng skill và luồng làm việc đầy đủ: `docs/guides/ai-agents.md`.

## 9. Code sạch (áp dụng cho cả backend và frontend)

Chỉ ghi điều cụ thể cho repo này. Kiến trúc từng phần: `backend/AGENTS.md`, `frontend/AGENTS.md`.

- **Một hàm làm một việc**, đọc được từ trên xuống: kiểm tra đầu vào và trả về sớm (early return), không lồng
  `if` quá 2 cấp. Hàm dài quá khoảng 40 dòng hoặc component quá khoảng 200 dòng thì tách.
- **Đặt tên theo nghiệp vụ** trong `docs/srs/06-data-dictionary.md` (`businessDate`, `tourFee`, không `date2`,
  `data`, `tmp`). Boolean bắt đầu bằng `is`, `has`, `can`. Code và tên bằng tiếng Anh; thông báo, nhãn giao diện
  bằng tiếng Việt có dấu.
- **Không số ma thuật**: hằng số nghiệp vụ (số lần đăng nhập sai, thời hạn token, kích thước trang) đặt tên rõ,
  để ở cấu hình hoặc hằng số của module.
- **Không nuốt lỗi**: không `catch` rồi bỏ qua. Bắt thì dịch sang lỗi nghiệp vụ hoặc ghi log rồi ném tiếp.
- **Không trừu tượng sớm**: lặp lại lần thứ ba mới tách hàm hay lớp dùng chung. Không thêm interface chỉ có một
  cài đặt, không thêm tham số "để sau này dùng".
- **Comment giải thích vì sao**, không kể lại code làm gì; ghi mã BR/FR/OQ khi code thực thi một quy tắc nghiệp vụ.
- Không để lại code bị comment bỏ, `console.log`, `System.out.println`, `TODO` không có mã card.

## 10. Viết test: điều kiện đạt

Mỗi hành vi nghiệp vụ mới hoặc sửa (service, endpoint, schema form, hàm tiện ích có logic) phải có đủ các nhóm ca
sau. Thiếu nhóm nào thì ghi lý do trong PR; người review coi thiếu ca là lỗi `bắt buộc:` với tiền, chốt sổ, bút
toán, đăng nhập.

| Nhóm ca               | Kiểm tra gì                                                                                                                                               |
| --------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Ca chuẩn (happy)      | Đầu vào hợp lệ điển hình cho kết quả đúng, kể cả dữ liệu được lưu và `audit_log` được ghi                                                                 |
| Dữ liệu sai           | Từng ràng buộc validation: trống, quá dài, sai định dạng, sai enum → 400 với `fieldErrors` đúng ô; sai kiểu: số thập phân hoặc chuỗi cho trường tiền, số lượng; `null` trong danh sách |
| Ca biên               | Tiền 0 và số rất lớn; chuỗi chỉ có khoảng trắng, kể cả NBSP và ký tự vô hình (zero-width); trùng tên khác hoa thường hoặc khác loại khoảng trắng; danh sách rỗng, trang cuối, trang cực lớn; ngày đầu/cuối tháng, ngày đã chốt, ngày tương lai |
| Lỗi nghiệp vụ         | Mỗi `ErrorCode` mà code có thể ném (409 trùng, 409 `DAY_CLOSED`, 404 không tồn tại...)                                                                    |
| Quyền, bảo mật        | Chưa đăng nhập → 401 (mỗi controller ít nhất một test); không gán được trường nhạy cảm qua body (id, trạng thái); lỗi không lộ chi tiết kỹ thuật, dữ liệu nhạy cảm không ra log |
| Không có tác dụng phụ | Khi lỗi: không lưu, không ghi audit (`verify(..., never())` hoặc kiểm tra DB)                                                                             |

Ca đầu vào bất thường dùng chung cho mọi API (JSON hỏng, sai Content-Type → 415, trang cực lớn, chuỗi rất dài) test
một lần ở `common`, không lặp ở từng controller. Ca theo nghiệp vụ (tiền thập phân, tên vô hình, `null` trong danh
sách) test ở từng endpoint có trường đó.

Quy tắc chung:

- **Một test kiểm tra một hành vi**, tên test nói hành vi đó: backend `camelCase` tiếng Anh
  (`createWithActiveDuplicateNameIsRejected`), frontend `it('...')` tiếng Việt. Thân test chia ba đoạn
  chuẩn bị, thực hiện, kiểm tra (Arrange, Act, Assert), cách nhau một dòng trống.
- **Assert giá trị cụ thể** (`isEqualTo(150_000L)`, `toHaveTextContent('1.500.000 đ')`), không chỉ `isNotNull()`
  hay "không ném lỗi". Kiểm tra lỗi bằng mã (`ErrorCode`, `code` trong JSON), không bằng câu thông báo.
- **Test phải đỏ được**: viết xong thì thử làm sai code một chút xem test có đỏ không. Test không bao giờ đỏ là vô ích.
- **Độc lập và lặp lại được**: thời gian qua `Clock.fixed(...)`, không phụ thuộc thứ tự chạy, không phụ thuộc dữ liệu
  test khác để lại.
- Cách viết riêng: `backend/AGENTS.md` mục Test, `frontend/AGENTS.md` mục Test và lệnh. Skill Antigravity `/write-tests`.
