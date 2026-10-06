# AGENTS.md — quy tắc chung cho AI làm việc trong repo này

File này là **nguồn quy tắc duy nhất** cho mọi công cụ AI (Claude Code, Antigravity, hoặc công cụ khác).
`CLAUDE.md` chỉ trỏ về đây. Nếu quy tắc ở nơi khác lệch với file này thì file này đúng.

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
| Nhánh, commit, Pull Request, nhịp sprint                | `CONTRIBUTING.md`                                                                                                             |

Quy tắc riêng của từng phần: `backend/AGENTS.md`, `frontend/AGENTS.md`.

## 3. Công nghệ

Java 21 · Spring Boot 4 · Gradle (Kotlin DSL) · Spring Data JPA · Spring Security (JWT trong cookie
`httpOnly`) · Flyway · PostgreSQL · React + TypeScript (Vite) · Ant Design · TanStack Query ·
JUnit 5 + Testcontainers · Vitest · Docker Compose · GitHub Actions.

Chi tiết và lý do: `docs/adr/0001-tech-stack.md`. **Muốn thêm thư viện hoặc đổi cách làm thì đối chiếu
ADR-0001 trước và nói rõ trong PR**, đừng tự thêm dependency.

## 4. Mười quy tắc bắt buộc

1. **Tiền là số nguyên đồng.** Không `float`, không `double` cho tiền (BR-01). Định dạng `1.234.000 đ`
   chỉ ở tầng hiển thị.
2. **Không sửa, không xóa migration đã có** (`V1__`, `V2__`, ...). Mọi thay đổi cấu trúc là một file
   `V{n}__mô_tả.sql` mới. Không bao giờ sửa schema bằng tay ngoài Flyway.
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

## 5. Quy trình làm một card

1. Đọc card trong `docs/backlog/sprint-0-1.md` và các mã FR / BR mà card trỏ tới.
2. **Nêu kế hoạch trước khi viết code**: sẽ sửa file nào, thêm bảng hay API nào, test gì. Chờ người duyệt.
3. Viết code theo quy tắc của `backend/AGENTS.md` hoặc `frontend/AGENTS.md`.
4. Viết test cho logic nghiệp vụ. Chạy build và test tại máy trước khi commit.
5. Tự đọc lại diff, nêu rõ phần nào còn chưa chắc.
6. Cập nhật tài liệu trong **cùng PR** nếu hành vi đổi (SRS, ERD, README).
7. Nhánh, commit, PR và review theo `CONTRIBUTING.md` (nhánh `feature/S1-01-...`, Conventional Commits,
   squash merge). Cấu trúc thư mục theo `backend/AGENTS.md` / `frontend/AGENTS.md`, không tự đặt chỗ mới.

## 6. Khi AI lặp lại cùng một lỗi

Thêm **một dòng** vào mục 4 hoặc vào file quy tắc của phần liên quan, đừng viết thêm một bộ quy tắc mới.
Quy tắc sinh ra từ lỗi thật mới đáng giữ.
