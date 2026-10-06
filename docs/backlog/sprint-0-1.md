# Backlog Sprint 0 và Sprint 1

Nguồn: `docs/srs/03-functional-requirements.md` (mã FR), `docs/srs/07-open-questions.md` (mã OQ), `docs/design/erd.md`.

**Cách dùng file này:** Đình tạo và giao card trên Trello. File này chỉ là **sổ theo dõi**: card nào, ai làm, đang ở cột nào. Mô tả chi tiết của card dán vào Trello (cột "Nội dung dán vào card"), không lặp lại checklist ở đây. Khi trạng thái card đổi trên Trello, cập nhật cột "Cột Trello" bên dưới.

## 1. Thiết lập Trello

**Cột:** `Product Backlog` → `Sprint Backlog` → `Đang làm` → `Review (PR)` → `Xong` (+ `Bị chặn`).

**Nhãn:** `BE`, `FE`, `DevOps`, `Docs`, `Spike`, `Bug`; độ ưu tiên `Must` / `Should`.

**Tên card:** `[S1-07] Đăng nhập (FR-AUTH-01, 02) (5)`; số điểm ở ngoặc cuối.

**Ước lượng:** 1 điểm ≈ nửa ngày (2 đến 3 giờ). Thang 1, 2, 3, 5, 8; trên 8 thì tách. Năng lực ≈ 28 điểm/sprint, sprint đầu nhận khoảng 24 điểm.

**Definition of Done (dán vào mô tả board):**

1. Đã qua Pull Request. PR của Huy: Đình approve. PR của Đình: tự review theo danh sách soát trong `CONTRIBUTING.md` rồi merge.
2. Build và test chạy qua ở máy người viết, ghi rõ trong PR. (Khi có CI ở giai đoạn DevOps: CI xanh.)
3. Có test cho logic nghiệp vụ; đổi DB bằng migration Flyway mới.
4. Làm đủ yêu cầu của card.
5. Cập nhật tài liệu nếu đổi hành vi (SRS, ERD, README).
6. Chạy được trên máy dev theo hướng dẫn trong README.
7. Sau khi có bản deploy đầu tiên (giai đoạn DevOps, mục 5): đã lên VPS và thử trên điện thoại.

---

## 2. Sprint 0: Nền móng dự án (15 điểm)

Mục tiêu: khung backend và frontend chạy được trên máy dev, DB tạo bằng migration, có test chạy được ở máy.
**Không làm DevOps** (CI, Dockerfile, deploy): dồn sang giai đoạn riêng ở mục 5, làm khi đã có sản phẩm chạy
được và đã học xong phần cơ bản.

| Mã    | Card                             | Điểm | Nhãn | Người làm | Cột Trello     | Nội dung dán vào card                                                                                                                                                      |
| ----- | -------------------------------- | ---- | ---- | --------- | -------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| S0-01 | Dựng board Trello và quy ước     | 1    | Docs | Đình      | Xong           | Tạo cột, nhãn theo mục 1, mời Huy, dán DoD, tạo card từ file này, chốt lịch planning/review/retro                                                                          |
| S0-02 | Quy tắc repo, nhánh, Pull Request | 1    | Docs | Đình      | Xong           | Repo GitHub theo Git Flow: nhánh `develop` (mặc định) và `main`, ruleset cho cả hai (bắt buộc PR, Đình là code owner duyệt, Đình bypass được với PR của mình; chưa bật điều kiện CI), quy ước trong `CONTRIBUTING.md`, PR template, `.gitignore`, `.editorconfig` |
| S0-03 | Tài liệu dev và quy tắc cho AI   | 1    | Docs | Đình      | Xong           | README chạy từ máy trống; `AGENTS.md`, `backend/AGENTS.md`, `frontend/AGENTS.md`, `CONTRIBUTING.md`. Huy cấu hình Antigravity đọc `AGENTS.md` khi bắt đầu Sprint 1 |
| S0-04 | Khung backend Spring Boot        | 3    | BE   | Đình      | Xong           | Gradle Kotlin DSL, Java 21, Spring Boot 4 (ADR-0001), MapStruct; cấu trúc module theo `backend/AGENTS.md`; profile dev/prod, bí mật từ biến môi trường; định dạng lỗi chung; endpoint health; package gốc `com.xilespa` |
| S0-05 | PostgreSQL trên máy dev và chạy migration | 2 | BE | Đình  | Xong           | Docker Desktop chỉ dùng như một phần mềm để chạy Postgres (`docker-compose.yml` chỉ có service `postgres`, cổng 5433 để không đụng Postgres cài sẵn); Flyway tự chạy V1, V2 khi khởi động; cách chạy trong README |
| S0-06 | Kiểm thử schema bằng Testcontainers | 2 | BE  | Đình      | Xong           | `schema_smoke_test.sql` chạy trong `./gradlew test` trên Postgres thật (`SchemaSmokeIT`, Testcontainers); lớp nền test tích hợp dùng chung (`@IntegrationTest`). Đưa vào CI để ở D-01 |
| S0-07 | Ánh xạ lỗi DB sang lỗi nghiệp vụ | 2    | BE   | Đình      | Xong           | `XL001` → `DAY_CLOSED`, `XL002` → `RECORD_IMMUTABLE`, `XL004` → `FUTURE_DAY_CLOSING`, lỗi tiếng Việt theo định dạng chung; test với trigger thật |
| S0-08 | Khung frontend React             | 3    | FE   | Đình      | Xong           | Vite + TypeScript + Ant Design + TanStack Query; cấu trúc theo `frontend/AGENTS.md`; layout dùng được trên điện thoại; lớp gọi API chung xử lý lỗi theo định dạng S0-04; oxlint, Prettier, Vitest |

**Tổng điểm:** Đình 15. Ngày 06/10/2026 Đình quyết định tự làm toàn bộ Sprint 0 cho gọn; Huy bắt đầu nhận card
từ Sprint 1. Trạng thái "Xong" ở trên tính khi các PR của Sprint 0 đã merge vào `develop`.

**Việc ngoài card (đừng bỏ sót):**

- OQ-10: chọn dịch vụ gửi email và tên miền (cần cho quên mật khẩu ở Sprint 2). Trạng thái: chưa làm.
- Hỏi chủ tiệm OQ-12 (nhập bao nhiêu tháng dữ liệu cũ) và OQ-14 (dashboard có hiện ngày chưa chốt không). Trạng thái: chưa hỏi.

---

## 3. Sprint 1: Đăng nhập và bảng giá (25 điểm)

Mục tiêu: đăng nhập được, quản lý bảng giá dịch vụ, mọi thay đổi có nhật ký. Demo trên máy dev: đăng nhập, tạo dịch vụ, đổi giá, ngừng bán, xem nhật ký.

| Mã    | Card                              | Điểm | Nhãn | Người làm | Cột Trello      | Nội dung dán vào card                                                                                                                                                                                              |
| ----- | --------------------------------- | ---- | ---- | --------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| S1-01 | Backend đăng nhập và phiên        | 5    | BE   | Huy       | Đang làm        | FR-AUTH-01, 02. Login email + mật khẩu (Argon2/BCrypt), access token cookie `httpOnly`, refresh token chỉ lưu băm và xoay vòng, logout thu hồi; API khác trả 401 khi chưa đăng nhập; lỗi không lộ email có tồn tại |
| S1-02 | Khởi tạo tài khoản chủ tiệm       | 2    | BE   | Huy       | Đang làm        | FR-AUTH-07. Tạo từ biến môi trường ở lần chạy đầu, không có trang đăng ký, không log mật khẩu, chạy lại không tạo trùng                                                                                            |
| S1-03 | Giao diện đăng nhập, bảo vệ trang | 5    | FE   | Huy       | Đang làm        | FR-AUTH-01, 02. Trang đăng nhập dùng tốt trên điện thoại, chuyển hướng khi chưa đăng nhập, tự làm mới phiên, nút đăng xuất, lỗi tiếng Việt                                                                         |
| S1-04 | Nhật ký thay đổi (audit log)      | 3    | BE   | Đình      | Đang làm        | FR-SYS-01, BR-18. Cơ chế dùng chung ghi `audit_log` trong cùng transaction; không ghi mật khẩu/token; API xem có phân trang                                                                                        |
| S1-05 | Backend quản lý dịch vụ           | 5    | BE   | Đình      | Đang làm        | FR-SVC-01 đến 04. Danh sách/tìm/lọc, tạo, sửa (đổi giá ghi `service_price_history`), ngừng bán/bán lại, không xóa cứng (BR-19), tiền là số nguyên không âm (BR-01), ghi audit; test tích hợp DB thật               |
| S1-06 | Giao diện quản lý dịch vụ         | 5    | FE   | Đình      | Đang làm        | FR-SVC-01 đến 04. Bảng có tìm/lọc, form tạo/sửa báo lỗi từng ô, ô tiền định dạng `1.234.000 đ`, ngừng bán có xác nhận, ghi rõ đổi giá niêm yết không ảnh hưởng giao dịch cũ                                        |

**Tổng điểm:** Huy 12, Đình 13.

**Phụ thuộc:** S1-05 và S1-06 cần S0-04, S0-08. S1-04 làm trước hoặc cùng lúc S1-05. S1-03 cần S1-01 chạy được.

**Lưu ý đăng nhập:** phần nhạy cảm về bảo mật. Huy chia thành PR nhỏ (đăng nhập, làm mới phiên, đăng xuất); Đình review kỹ, đối chiếu mục Bảo mật trong `backend/AGENTS.md`.

---

## 4. Ứng viên cho Sprint 2 (chưa chia nhỏ)

Theo thứ tự ưu tiên để sớm "dùng thật được":

1. Đổi/quên mật khẩu, chống dò mật khẩu (FR-AUTH-03, 04, 05). Cần chốt OQ-10 trước.
2. Nhân viên (FR-STF-01) và nhóm dịch vụ (FR-SVC-05).
3. Combo (FR-CMB-01 đến 05).
4. Nhập bảng giá từ Excel (FR-IMP-01).
5. Khách hàng cơ bản (FR-CUS-01, 02); lưu ý OQ-08.
6. Giao dịch (FR-VIS-01 đến 11), tách hai sprint; chốt trước OQ-06, OQ-07.
7. Chi phí (FR-EXP), chốt sổ và bút toán (FR-CLS), dashboard (FR-DSH-01, 03, 06, 07, 09).

Gói liệu trình, đồng bộ Google Sheets, CRM nâng cao và lương thuộc R2, R3.

**Epic để dành (R3 trở đi, chưa chia card):** Hỗ trợ nội dung bằng AI (FR-AI-01 đến 05): AI soạn nháp tin chăm sóc khách và bài đăng, người duyệt, gửi Zalo thủ công, đăng Fanpage một nút. Cần CRM (FR-CUS-04, 06, 07) và giai đoạn DevOps xong trước; chốt OQ-18, OQ-19, OQ-20.

## 5. Giai đoạn DevOps (để sau)

Gom toàn bộ việc DevOps vào đây để Sprint 0 đến các sprint đầu chỉ tập trung xây sản phẩm. Bắt đầu khi
**có bản chạy được đủ để chủ tiệm thử** (dự kiến sau khi xong giao dịch và chốt sổ), và **trước khi nhập dữ
liệu thật**. Trong lúc chờ, ai muốn học thì đọc và thử riêng, chưa cần thành card.

Thứ tự gợi ý, mỗi bước nhỏ, làm cặp để cả hai cùng học:

| Mã   | Card                                      | Điểm | Nội dung                                                                                                 |
| ---- | ----------------------------------------- | ---- | -------------------------------------------------------------------------------------------------------- |
| D-01 | CI bằng GitHub Actions                    | 3    | Build + test backend (kể cả `schema_smoke_test.sql`) và frontend trên mỗi PR; bật điều kiện "CI xanh" cho `main`; huy hiệu CI trong README (NFR-MNT-04) |
| D-02 | Docker hóa cả hệ thống trên máy           | 3    | Dockerfile backend/frontend; `docker compose up` dựng Postgres + backend + frontend; README giải thích từng file bằng lời của mình |
| D-03 | VPS: SSH key và bảo mật cơ bản            | 2    | Tạo user, tắt đăng nhập bằng mật khẩu, tường lửa                                                         |
| D-04 | VPS: chạy hệ thống bằng Docker Compose    | 2    | Chạy lại `docker compose` của D-02 trên VPS, giới hạn RAM (NFR-PERF-03)                                  |
| D-05 | Caddy và tên miền, HTTPS                  | 2    | Cần OQ-10 (tên miền)                                                                                     |
| D-06 | Sao lưu PostgreSQL hằng đêm, thử khôi phục | 3   | FR-SYS-02, NFR-REL-01: mã hóa, lưu ngoài VPS, giữ 14 bản. **Xong trước khi nhập dữ liệu thật**           |
| D-07 | Tự động deploy (CD)                       | 3    | NFR-OPS-01. Làm sau cùng, khi đã deploy tay vài lần và hiểu từng bước                                    |

## 6. Rủi ro cần theo dõi

| Rủi ro                                       | Cách giảm                                                                                                             |
| -------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| Chưa biết DevOps nên deploy lần đầu dễ vướng | Dồn DevOps sang giai đoạn riêng (mục 5), chia bước nhỏ, làm cặp; bắt đầu trước khi chủ tiệm cần dùng thật ít nhất một sprint |
| Chưa có CI nên `main` có thể bị hỏng         | Người viết chạy build + test ở máy và ghi vào PR; người review kéo nhánh về chạy thử với PR lớn; làm D-01 sớm nhất trong giai đoạn DevOps |
| Hai người chạm cùng file                     | Mỗi card một nhánh, PR nhỏ, merge thường xuyên                                                                        |
| Mất dữ liệu ở production                     | Sao lưu hằng đêm (FR-SYS-02) xong trước khi nhập dữ liệu thật                                                         |
| Chủ tiệm chưa trả lời câu hỏi mở             | Hỏi theo lô vào buổi review, ghi kết quả vào `07-open-questions.md`                                                   |
