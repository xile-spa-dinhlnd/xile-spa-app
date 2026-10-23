# Duy trì phiên và làm mới token

Trạng thái: khớp code · Card: S1-01, S2-04 · Yêu cầu: FR-AUTH-02

| Ngày       | Người         | Card / PR            | Thay đổi                                                     |
| ---------- | ------------- | -------------------- | ------------------------------------------------------------ |
| 23/10/2026 | Đình (Claude) | chore/agent-workflow | Tách từ `auth.md` (vẽ theo code, commit 56aca20), nội dung không đổi |

Phần chung của module (cookie, bản băm token, endpoint công khai, cấu hình): `README.md` cùng thư mục.

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
`authKeys.me` lỗi, tức là khi query này chạy lại (hết `staleTime` 5 phút, hoặc tải lại trang). Sửa ở thẻ S3-07
(`docs/backlog/sprint-2-3.md`); sửa xong thì cập nhật đoạn này theo luồng mới.

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
