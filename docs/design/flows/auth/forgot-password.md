# Quên và đặt lại mật khẩu

Trạng thái: khớp code · Card: S2-02 · Yêu cầu: FR-AUTH-03

| Ngày       | Người         | Card / PR            | Thay đổi                                                     |
| ---------- | ------------- | -------------------- | ------------------------------------------------------------ |
| 23/10/2026 | Đình (Claude) | chore/agent-workflow | Tách từ `auth.md` (vẽ theo code, commit 56aca20), nội dung không đổi |

Phần chung của module (cookie, bản băm token, endpoint công khai, cấu hình): `README.md` cùng thư mục.

Điểm vào: `POST /api/auth/forgot-password` → `AuthService.forgotPassword`; `POST /api/auth/reset-password` →
`AuthService.resetPassword`. Frontend: `ForgotPasswordPage`, `ResetPasswordPage` (đọc `?token=` trên URL).

```mermaid
sequenceDiagram
    participant U as Người dùng
    participant UI as Frontend
    participant S as AuthService
    participant M as PasswordResetEmailSender
    U->>UI: Nhập email
    UI->>S: POST /forgot-password
    alt Email có, tài khoản bật, dưới 3 lần trong 15 phút
        S->>S: Vô hiệu các link cũ còn hạn, tạo token mới (lưu bản băm, hạn password-reset-duration 30m)
        S->>M: Gửi link reset-password-base-url?token=...
    else Không có email, tài khoản tắt, hoặc quá tần suất
        S->>S: Không làm gì (chỉ ghi log)
    end
    S-->>UI: Luôn 200, cùng một thông báo
    U->>UI: Mở link, nhập mật khẩu mới
    UI->>S: POST /reset-password (token, newPassword)
    alt Token có, chưa dùng, còn hạn, mật khẩu đạt chính sách
        S->>S: Đánh dấu token đã dùng, lưu hash mới, mở khóa tài khoản (đếm sai về 0)
        S->>S: Xóa mọi token đặt lại của tài khoản, thu hồi MỌI phiên, ghi audit_log RESET_PASSWORD
        S-->>UI: 200
    else Token không hợp lệ hoặc hết hạn
        S-->>UI: 400 RESET_TOKEN_INVALID
    else Mật khẩu vi phạm chính sách
        S-->>UI: 400 PASSWORD_POLICY_VIOLATION
    end
```

Quy tắc giữ trong luồng:

- `forgot-password` luôn trả `200` với cùng thông báo, kể cả khi email không tồn tại hay bị chặn tần suất, để không
  lộ email nào có tài khoản.
- Link chỉ dùng một lần và chỉ link mới nhất còn hiệu lực.
- Đặt lại thành công thì mở khóa tài khoản và đăng xuất mọi thiết bị (khác với đổi mật khẩu, giữ phiên hiện tại).
