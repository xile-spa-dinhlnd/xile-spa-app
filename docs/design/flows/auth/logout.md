# Đăng xuất

Trạng thái: khớp code · Card: S1-01 · Yêu cầu: FR-AUTH-02

| Ngày       | Người         | Card / PR            | Thay đổi                                                     |
| ---------- | ------------- | -------------------- | ------------------------------------------------------------ |
| 23/10/2026 | Đình (Claude) | chore/agent-workflow | Tách từ `auth.md` (vẽ theo code, commit 56aca20), nội dung không đổi |

Phần chung của module (cookie, bản băm token, endpoint công khai, cấu hình): `README.md` cùng thư mục.

Điểm vào: `POST /api/auth/logout` → `AuthService.logout`.

- Có refresh token trong cookie thì thu hồi đúng token đó; không có hoặc không tìm thấy thì bỏ qua.
- Luôn trả `204` và xóa cả hai cookie, kể cả khi token không hợp lệ.
