# Luồng xử lý: auth (phần chung)

Module `backend/.../module/auth`, `security/`; frontend `features/auth`, `shared/api/client.ts`. Yêu cầu:
FR-AUTH-01 đến FR-AUTH-05, NFR-SEC-02, NFR-SEC-03. Cấu hình ở `application.yml`, nhóm `xile.security.jwt` và
`xile.security.auth` (thời hạn token, số lần sai, thời hạn link đặt lại); số trong sơ đồ là giá trị mặc định.

Chung cho mọi tính năng của module:

- Access token (JWT) và refresh token nằm trong cookie `HttpOnly`, `SameSite=Strict`; frontend không đọc được.
- Refresh token và token đặt lại mật khẩu chỉ lưu **bản băm** SHA-256 (`TokenHasher`); chuỗi gốc chỉ nằm trong
  cookie hoặc trong link email.
- Endpoint công khai: `login`, `refresh`, `logout`, `forgot-password`, `reset-password` (`SecurityConfig`). Mọi
  endpoint khác cần access token hợp lệ, thiếu thì `401 UNAUTHORIZED`.

| Tính năng                      | File                 |
| ------------------------------ | -------------------- |
| Đăng nhập, khóa tạm khi sai    | `login.md`           |
| Duy trì phiên, làm mới token   | `session-refresh.md` |
| Đăng xuất                      | `logout.md`          |
| Đổi mật khẩu                   | `change-password.md` |
| Quên và đặt lại mật khẩu       | `forgot-password.md` |
