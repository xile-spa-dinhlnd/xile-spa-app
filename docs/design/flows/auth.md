# Luồng xử lý: auth

Module `backend/.../module/auth`, `security/`; frontend `features/auth`, `shared/api/client.ts`. Yêu cầu:
FR-AUTH-01 đến FR-AUTH-05, NFR-SEC-02, NFR-SEC-03. Cấu hình ở `application.yml`, nhóm `xile.security.jwt` và
`xile.security.auth` (thời hạn token, số lần sai, thời hạn link đặt lại); số trong sơ đồ là giá trị mặc định.

Chung cho mọi luồng:

- Access token (JWT) và refresh token nằm trong cookie `HttpOnly`, `SameSite=Strict`; frontend không đọc được.
- Refresh token và token đặt lại mật khẩu chỉ lưu **bản băm** SHA-256 (`TokenHasher`); chuỗi gốc chỉ nằm trong
  cookie hoặc trong link email.
- Endpoint công khai: `login`, `refresh`, `logout`, `forgot-password`, `reset-password` (`SecurityConfig`). Mọi
  endpoint khác cần access token hợp lệ, thiếu thì `401 UNAUTHORIZED`.

## Đăng nhập (FR-AUTH-01, FR-AUTH-05)

Điểm vào: `POST /api/auth/login` → `AuthController.login` → `AuthService.login`. Frontend: `LoginPage`,
`useLogin`.

```mermaid
flowchart TD
    A[POST /api/auth/login] --> B{Tìm email, không phân biệt hoa thường}
    B -- không có --> X1[Chạy BCrypt với hash giả] --> E401[401 UNAUTHORIZED, thông báo chung]
    B -- có --> C{"Đang tạm khóa? (locked_until sau thời điểm hiện tại)"}
    C -- có --> X2[Chạy BCrypt với hash giả] --> E401
    C -- không --> D{Mật khẩu đúng?}
    D -- sai --> F[AuthSecurityAuditService.recordFailedLogin, REQUIRES_NEW] --> E401
    D -- đúng --> G{Tài khoản đang bật?}
    G -- không --> E401b[401 UNAUTHORIZED, Tài khoản đã bị vô hiệu hóa]
    G -- có --> H[recordSuccessfulLogin: đếm sai về 0, lưu last_login_at]
    H --> I[Tạo access token JWT, tạo refresh token, lưu bản băm]
    I --> J[200 + UserResponse, Set-Cookie access và refresh]
```

Khóa tạm thời (`AppUser.recordFailedLogin`):

```mermaid
flowchart LR
    A[Sai mật khẩu] --> B[failed_login_count + 1]
    B --> C{"Đếm đạt max-failed-attempts (5)?"}
    C -- chưa --> D[Không khóa]
    C -- rồi --> E["Khóa 2^(count - max) phút, tối thiểu 1, tối đa lock-duration (15m)"]
    E --> F[Ghi audit_log ACCOUNT_LOCKED]
```

Quy tắc giữ trong luồng:

- Ba nhánh "không có email", "đang khóa", "sai mật khẩu" trả **cùng một thông báo** và đều chạy BCrypt, để không
  lộ email có tồn tại hay không qua nội dung hoặc thời gian phản hồi (NFR-SEC-02).
- Kiểm tra khóa **trước** mật khẩu: đang khóa thì không đếm thêm lần sai.
- Bộ đếm sai được ghi trong transaction riêng (`REQUIRES_NEW`), nên không bị rollback khi ném 401.
- Bộ đếm chỉ về 0 khi đăng nhập đúng hoặc khi đặt lại mật khẩu; hết hạn khóa thì bỏ `locked_until` nhưng giữ bộ
  đếm, nên lần sai tiếp theo khóa lâu hơn.

Lỗi trả về: `401 UNAUTHORIZED` (sai thông tin, đang khóa, tài khoản tắt), `400 VALIDATION_FAILED` (thiếu email,
mật khẩu).

## Duy trì phiên và làm mới token (FR-AUTH-02)

Điểm vào: mọi request qua `JwtAuthenticationFilter`; `POST /api/auth/refresh` → `AuthService.refresh`. Frontend:
`shared/api/client.ts` (`apiRequest`, `tryRefreshSession`).

```mermaid
sequenceDiagram
    participant UI as Frontend (apiRequest)
    participant F as JwtAuthenticationFilter
    participant API as API nghiệp vụ
    participant R as AuthService.refresh
    UI->>F: Request kèm cookie access token
    F->>API: Token hợp lệ: đặt UserPrincipal
    API-->>UI: 401 khi access token hết hạn (mặc định 15m)
    UI->>R: POST /api/auth/refresh (gom mọi 401 cùng lúc thành một lần gọi)
    alt Refresh token hợp lệ
        R->>R: Thu hồi token cũ, tạo token mới (xoay vòng)
        R-->>UI: 200, Set-Cookie access và refresh mới
        UI->>API: Gửi lại request ban đầu một lần (_isRetry)
    else Không hợp lệ, hết hạn, tài khoản tắt
        R-->>UI: 401
        UI->>UI: notifySessionExpired, rồi ném ApiError 401 cho request ban đầu
    end
```

Khi refresh thất bại, `notifySessionExpired` hiện **chưa có component nào đăng ký** (`onSessionExpired`), nên trang
đang mở chỉ hiện lỗi 401 của request đó. Người dùng được đưa về `/login` khi `ProtectedRoute` thấy query
`authKeys.me` lỗi, tức là khi query này chạy lại (hết `staleTime` 5 phút, hoặc tải lại trang).

Kiểm tra trong `AuthService.refresh`, theo thứ tự:

```mermaid
flowchart TD
    A[Cookie refresh token] --> B{Có giá trị?}
    B -- không --> E1[401]
    B -- có --> C{Tìm theo bản băm}
    C -- không thấy --> E1
    C -- thấy --> D{Đã bị thu hồi?}
    D -- rồi --> R[Nghi bị đánh cắp: thu hồi MỌI phiên của tài khoản, REQUIRES_NEW] --> E2[401, Phiên không an toàn]
    D -- chưa --> G{Hết hạn? mặc định 7d}
    G -- rồi --> E1
    G -- chưa --> H{Tài khoản đang bật?}
    H -- không --> E1
    H -- có --> I[Thu hồi token cũ, cấp refresh và access mới] --> OK[200]
```

Quy tắc giữ trong luồng:

- Mỗi refresh token chỉ dùng được một lần. Dùng lại token đã thu hồi là dấu hiệu bị đánh cắp, nên thu hồi toàn bộ
  phiên, và việc thu hồi phải commit ngay dù request trả lỗi (`AuthSecurityAuditService`, `REQUIRES_NEW`).
- Frontend gom các 401 đồng thời vào một promise refresh; nếu không, hai request cùng lúc sẽ dùng cùng một refresh
  token và lần thứ hai bị coi là dùng lại, làm mất phiên.
- Frontend không tự refresh cho chính `login`, `refresh`, `logout` (`isAuthBypassPath`).

## Đăng xuất (FR-AUTH-02)

Điểm vào: `POST /api/auth/logout` → `AuthService.logout`.

- Có refresh token trong cookie thì thu hồi đúng token đó; không có hoặc không tìm thấy thì bỏ qua.
- Luôn trả `204` và xóa cả hai cookie, kể cả khi token không hợp lệ.

## Đổi mật khẩu (FR-AUTH-04, NFR-SEC-03)

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

## Quên và đặt lại mật khẩu (FR-AUTH-03)

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
