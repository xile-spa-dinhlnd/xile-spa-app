# Bề mặt tấn công

Cập nhật: 25/10/2026. Mọi điểm mà dữ liệu từ ngoài đi vào hệ thống hoặc dữ liệu đi ra ngoài. Endpoint cụ thể từng
module xem ở Postman (`docs/postman/`) và file luồng; ở đây ghi theo nhóm, riêng endpoint **không cần đăng nhập** ghi
từng cái vì đó là chỗ người ngoài chạm được.

## Không cần đăng nhập

Danh sách khớp `SecurityConfig` (`permitAll`). Thêm endpoint vào đây phải có lý do và kiểm soát, và người review soát kỹ.

| Endpoint | Vì sao công khai | Kiểm soát | Mối đe dọa |
| -------- | ---------------- | --------- | ---------- |
| `POST /api/auth/login` | Chưa có phiên | Khóa tạm theo tài khoản, thông báo và thời gian như nhau | T-01, T-02, T-13 |
| `POST /api/auth/refresh` | Access token đã hết hạn | Refresh token băm, xoay vòng, phát hiện dùng lại | T-05 |
| `POST /api/auth/logout` | Đăng xuất cả khi access token đã hết hạn | Chỉ thu hồi token trong cookie của chính request | T-05 |
| `POST /api/auth/forgot-password` | Người quên mật khẩu | Luôn cùng câu trả lời; tối đa 3 yêu cầu mỗi người | T-02, T-06, T-13 |
| `POST /api/auth/reset-password` | Người đang đặt lại mật khẩu | Token băm, 30 phút, một lần; chính sách mật khẩu | T-06 |
| `GET /actuator/health` | Kiểm tra sống khi triển khai | Không lộ chi tiết (`show-details: never`) | T-14 |

## Cần đăng nhập

Mọi URL khác (`anyRequest().authenticated()`), chưa đăng nhập trả 401. **Chưa phân quyền theo vai trò** (T-07).

| Nhóm | Đường dẫn | Dữ liệu vào | Ghi chú |
| ---- | --------- | ----------- | ------- |
| Tài khoản | `/api/auth/me`, `/api/auth/change-password` | mật khẩu cũ, mới | Đổi mật khẩu thu hồi phiên khác |
| Nhật ký | `/api/audit-logs` | lọc, phân trang | Chỉ đọc; không có API sửa, xóa |
| Danh mục | `/api/catalog/service-groups`, `/services`, `/combos` (kể cả `/combos/quote`) | tên, tiền, số lượng, danh sách thành phần | Đầu vào bất thường: T-09 |
| Nhân viên | `/api/staff` | tên, vai trò | |

## Cookie và trình duyệt

| Thứ | Thuộc tính |
| --- | ---------- |
| `access_token`, `refresh_token` | `HttpOnly`, `SameSite=Strict`, `Secure` khi `JWT_COOKIE_SECURE=true` (prod) |
| Frontend | SPA tĩnh; dev chạy Vite ở `localhost:5173` và proxy `/api`; không lưu token ở `localStorage` |

## Đầu vào lúc khởi động và kết nối ra ngoài

| Thứ | Hướng | Ghi chú |
| --- | ----- | ------- |
| Biến môi trường | vào | Tài khoản chủ tiệm đầu tiên, bí mật (`secrets.md`) |
| PostgreSQL | hai chiều | Dev chỉ mở `127.0.0.1`; prod sau tường lửa |
| SMTP (prod) | ra | Gửi link đặt lại mật khẩu; không gửi dữ liệu khách |
| Dữ liệu mẫu dev | vào | Chỉ khi `xile.dev-data.enabled=true`, dữ liệu giả |

## Sắp có (cập nhật khi làm)

| Thứ | Card | Cần chú ý |
| --- | ---- | --------- |
| Tải lên file Excel | S2-09 | T-15: kích thước, file nén, công thức trong ô |
| Đồng bộ Google Sheets | R2 | T-11, NFR-PRV-04: service account, phạm vi quyền tối thiểu |
| Gọi mô hình AI | R3 | T-11, NFR-PRV-05: chỉ gửi dữ liệu tối thiểu |
| Reverse proxy Caddy | D-05 | Rate limit theo IP, giới hạn body, HTTPS, CSP (T-01, T-03, T-13) |
