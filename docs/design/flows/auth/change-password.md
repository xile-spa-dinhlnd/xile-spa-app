# Đổi mật khẩu

Trạng thái: khớp code · Card: S2-01 · Yêu cầu: FR-AUTH-04, NFR-SEC-03

| Ngày       | Người         | Card / PR            | Thay đổi                                                     |
| ---------- | ------------- | -------------------- | ------------------------------------------------------------ |
| 23/10/2026 | Đình (Claude) | chore/agent-workflow | Tách từ `auth.md` (vẽ theo code, commit 56aca20), nội dung không đổi |

Phần chung của module (cookie, bản băm token, endpoint công khai, cấu hình): `README.md` cùng thư mục.

Điểm vào: `POST /api/auth/change-password` (cần đăng nhập) → `AuthService.changePassword`. Frontend:
`ChangePasswordModal`.

```mermaid
flowchart TD
    A[Đã đăng nhập] --> B{Mật khẩu hiện tại đúng?}
    B -- sai --> E1[400 PASSWORD_INCORRECT]
    B -- đúng --> C{PasswordPolicyValidator}
    C -- vi phạm --> E2[400 PASSWORD_POLICY_VIOLATION]
    C -- đạt --> D[Lưu hash mới]
    D --> F[Thu hồi mọi refresh token khác, GIỮ phiên hiện tại]
    F --> G[Ghi audit_log CHANGE_PASSWORD, không có mật khẩu hay hash] --> OK[200]
```

Chính sách mật khẩu (`PasswordPolicyValidator`): tối thiểu 10 ký tự, tối đa 72 byte (giới hạn của BCrypt), không
nằm trong danh sách mật khẩu phổ biến, không trùng mật khẩu cũ.
