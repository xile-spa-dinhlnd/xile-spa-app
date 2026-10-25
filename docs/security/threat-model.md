# Mô hình mối đe dọa

Cập nhật: 25/10/2026 (v0.3.0, sau S2-07). Mỗi mối đe dọa có mã `T-xx`; file luồng và PR trỏ tới mã này.

## Tài sản cần bảo vệ

| Tài sản | Vì sao quan trọng | Nằm ở |
| ------- | ----------------- | ----- |
| Số liệu tiền (giao dịch, chi phí, chốt sổ) | Sai là sai lương, sai doanh thu; ngày đã chốt phải bất biến (BR-06) | `visit`, `expense`, `day_closing`, `adjustment` |
| Dữ liệu khách: tên, SĐT, ngày sinh, **ghi chú sức khỏe** | Dữ liệu cá nhân, ghi chú sức khỏe là nhạy cảm (NFR-PRV-02) | `customer` (từ S3-01) |
| Tài khoản chủ tiệm | Người giữ tài khoản làm được mọi việc | `app_user`, cookie phiên |
| Nhật ký thay đổi | Bằng chứng ai sửa gì (BR-18) | `audit_log` (chỉ thêm, trigger XL002) |
| Bí mật hệ thống | Lộ là mất quyền kiểm soát | biến môi trường (`secrets.md`) |
| Bản sao Google Sheets (R2) | Chứa số liệu, có thể chứa thông tin khách | tài khoản Google của chủ tiệm |

## Ai có thể gây hại

| Tác nhân | Khả năng |
| -------- | -------- |
| Người ngoài trên internet | Gọi mọi URL công khai (`attack-surface.md`), đoán mật khẩu, gửi đầu vào lạ |
| Người cầm thiết bị đang đăng nhập | iPad, điện thoại ở quầy; nhân viên dùng chung máy với chủ tiệm |
| Người dùng hợp lệ lỡ tay | Nhập nhầm, bấm hai lần, sửa ngày đã chốt |
| Thành viên nhóm, AI agent | Lỡ commit bí mật, dữ liệu khách thật, thêm thư viện độc hại, ghi bí mật ra log |

## Mối đe dọa và kiểm soát

Trạng thái: **có** (đã có code và test), **một phần**, **chưa** (kèm card hoặc đợt dự kiến).

| Mã | Mối đe dọa | Kiểm soát hiện có | Trạng thái, còn thiếu |
| -- | ---------- | ----------------- | --------------------- |
| T-01 | Đoán mật khẩu chủ tiệm | BCrypt; khóa tạm sau `xile.security.auth.max-failed-attempts` lần sai (5, khóa 15 phút); thông báo chung | **một phần**: chưa giới hạn tốc độ theo IP (NFR-SEC-05, Caddy ở D-05) |
| T-02 | Dò email có tồn tại không | Cùng thông báo và cùng thời gian phản hồi (băm giả khi không có email); quên mật khẩu luôn trả cùng câu | **có** |
| T-03 | Đánh cắp phiên qua XSS | Token trong cookie `httpOnly`; React tự escape; không dùng `dangerouslySetInnerHTML` | **một phần**: chưa có Content-Security-Policy (đặt ở Caddy, D-05) |
| T-04 | CSRF | Cookie `SameSite=Strict`, frontend và API cùng site; API đổi dữ liệu không dùng `GET` | **có**; đổi tên miền thì xem lại (`backend/AGENTS.md` mục Bảo mật) |
| T-05 | Refresh token bị lộ, dùng lại | Chỉ lưu bản băm; xoay vòng mỗi lần làm mới; token cũ dùng lại thì thu hồi mọi phiên | **có** |
| T-06 | Link đặt lại mật khẩu bị lộ | Lưu băm, hết hạn 30 phút, dùng một lần, tối đa 3 yêu cầu mỗi người trong một khoảng; đặt lại xong thu hồi mọi phiên | **có**; máy dev ghi link ra log là cố ý (OQ-10) |
| T-07 | Người cầm thiết bị đang đăng nhập làm việc của chủ tiệm | Access token 15 phút, refresh 7 ngày; đăng xuất thu hồi phiên | **một phần**: **chưa phân quyền** (đăng nhập là được làm mọi thứ dù `UserRole` có `STAFF`). Hiện chỉ có tài khoản chủ tiệm; trước khi cho nhân viên tài khoản riêng phải có card phân quyền |
| T-08 | Sửa số liệu tài chính rồi chối, sửa ngày đã chốt | `audit_log` cùng transaction, chỉ thêm (XL002); ngày `CLOSED` bất biến (XL001), sửa bằng bút toán (ADR-0003) | **có** (chốt sổ từ Sprint 4) |
| T-09 | Đầu vào bất thường làm sai tiền hoặc gây 500 | Bean Validation, `GlobalExceptionHandler` | **một phần**: 7 lỗi đã tìm ra, sửa ở S3-08 (`docs/design/flows/common/input-hardening.md`) |
| T-10 | SQL injection | JPA và truy vấn có tham số; `likePattern` thoát `%`, `_`, `\` | **có**; cấm nối chuỗi vào câu SQL |
| T-11 | Lộ dữ liệu khách nhạy cảm (log, audit, email, Sheets, AI) | NFR-PRV-02, 04, 05; `AuditService` che trường mật khẩu, token | **chưa** cho khách hàng: **phải chốt trước S3-01** ghi chú sức khỏe có vào `audit_log` không (BR-18 bắt ghi thay đổi, NFR-PRV-02 cấm đưa ra ngoài hồ sơ) |
| T-12 | Lộ bí mật qua repo, log, ảnh chụp | `secrets.md`; hook chặn agent đọc file bí mật; `application-prod.yml` không có giá trị mặc định; `JWT_SECRET` thiếu hoặc ngắn thì không khởi động | **có** |
| T-13 | Làm cạn tài nguyên (BCrypt, body lớn, gọi dồn dập) | Khóa theo tài khoản; giới hạn quên mật khẩu theo người; `size` trang tối đa 100 | **một phần**: chưa giới hạn theo IP và kích thước body (Caddy, D-05) |
| T-14 | Lộ chi tiết kỹ thuật qua lỗi | Lỗi 500 chỉ trả câu chung, chi tiết vào log; Actuator chỉ `health`, `show-details: never` | **một phần**: vài lỗi định dạng còn ra 500 (S3-08) |
| T-15 | File nhập độc hại (Excel ở S2-09) | Chưa có | **chưa**: khi làm S2-09 phải giới hạn kích thước file, chặn file nén bất thường, coi ô bắt đầu bằng `=` là chữ |
| T-16 | Thư viện độc hại hoặc có lỗ hổng | Thêm thư viện phải hỏi trước, nêu uy tín (`AGENTS.md` mục 3); `package-lock.json` cố định phiên bản | **một phần**: chưa quét lỗ hổng tự động (đưa vào CI, S3-09) |
| T-17 | Database bị truy cập trực tiếp | Dev: chỉ mở `127.0.0.1:5433`; prod: tường lửa, SSH bằng khóa (NFR-SEC-08) | **một phần**: phần prod làm ở đợt DevOps |

## Cập nhật file này khi

- Tính năng tạo tài sản mới (khách hàng, gói, đồng bộ Sheets, AI) hoặc tác nhân mới (tài khoản nhân viên).
- Một mối đe dọa đổi trạng thái (đã sửa, hoặc phát hiện hở mới). Ghi ngày ở đầu file.
