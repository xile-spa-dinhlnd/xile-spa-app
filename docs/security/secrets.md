# Bí mật

Cập nhật: 25/10/2026. Danh sách mọi bí mật, **không ghi giá trị thật**. Quy tắc gốc: `AGENTS.md` quy tắc 6, NFR-SEC-07.

## Nguyên tắc

- Bí mật chỉ đọc từ biến môi trường. `application-prod.yml` không có giá trị mặc định cho bí mật nào.
- Giá trị mặc định ở `application-dev.yml` và `.env.example` là **giá trị công khai cho máy dev**, không bao giờ dùng
  ở production.
- File `.env` không commit (`.gitignore`); hook của agent chặn đọc, sửa file bí mật. Agent cần biết tên biến thì đọc
  `.env.example`.
- Không ghi bí mật ra log, `audit_log`, thông báo lỗi, ảnh chụp màn hình, mô tả PR.

## Danh sách

| Biến | Dùng cho | Dev | Prod | Lộ thì làm gì |
| ---- | -------- | --- | ---- | ------------- |
| `JWT_SECRET` | Ký access token (HMAC-SHA256) | Có mặc định trong `application-dev.yml` | Bắt buộc, chuỗi ngẫu nhiên ≥ 32 byte; thiếu hoặc ngắn thì backend không khởi động | Đổi giá trị, khởi động lại: mọi access token cũ mất hiệu lực. Thu hồi refresh token nếu nghi lộ cả DB |
| `DB_USERNAME`, `DB_PASSWORD` | Kết nối PostgreSQL | `xile` / `xile_dev` | Bắt buộc | Đổi mật khẩu user DB, cập nhật biến, khởi động lại |
| `POSTGRES_PASSWORD` | Mật khẩu khởi tạo container PostgreSQL | `xile_dev` | Bắt buộc, khác giá trị dev | Như trên |
| `APP_OWNER_PASSWORD` | Mật khẩu tài khoản chủ tiệm **lần đầu** | Có mặc định dev | Đặt lúc triển khai lần đầu, rồi chủ tiệm đổi ngay trong ứng dụng | Đổi mật khẩu trong ứng dụng (thu hồi phiên khác) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP gửi email đặt lại mật khẩu (App Password của Gmail) | Không dùng (ghi link ra log) | Bắt buộc | Thu hồi App Password ở tài khoản Google, tạo cái mới |
| Service account Google | Đồng bộ Sheets (R2) | chưa có | chưa có | Thêm dòng khi làm R2: phạm vi quyền tối thiểu, chỉ chia sẻ đúng file |

## Thêm bí mật mới

1. Đọc qua biến môi trường, thêm dòng trong bảng trên và một dòng mẫu (không phải giá trị thật) trong `.env.example`.
2. Không đặt mặc định ở `application-prod.yml`; backend nên từ chối khởi động nếu thiếu (như `JWT_SECRET`).
3. Kiểm log và `audit_log` không in giá trị.
