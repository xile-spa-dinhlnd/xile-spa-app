# frontend/AGENTS.md — quy tắc riêng cho frontend

Đọc `../AGENTS.md` trước. File này chỉ bổ sung phần riêng của frontend.
Stack: React 19 + TypeScript (Vite), Ant Design 6, TanStack Query, React Router, React Hook Form + Zod, Recharts,
Vitest, Playwright. Lint bằng oxlint (mặc định của Vite), format bằng Prettier.

## Cấu trúc

Chia theo tính năng, **cùng tên với module backend** (`auth`, `catalog`, `customer`, `visit`, `closing`, ...) để
người nhận một tính năng làm cả hai đầu mà ít đụng file của người kia.

```
frontend/
├─ index.html, package.json, vite.config.ts (alias @/, proxy /api tới backend), tsconfig*.json
├─ .oxlintrc.json, .prettierrc.json
├─ .env.example                  # chỉ VITE_API_BASE_URL..., không có bí mật
├─ playwright.config.ts          # thêm khi viết test luồng đầu tiên (S1-03)
├─ Dockerfile                    # thêm ở giai đoạn DevOps (D-02)
├─ public/
├─ e2e/                          # Playwright: login.spec.ts, services.spec.ts
└─ src/
   ├─ main.tsx                   # điểm vào, chỉ gắn <App />
   ├─ app/
   │  ├─ App.tsx
   │  ├─ providers.tsx           # QueryClient, ConfigProvider (locale vi_VN, theme)
   │  ├─ queryClient.ts          # mặc định: không thử lại lỗi 4xx
   │  ├─ router.tsx              # khai báo route, bảo vệ trang khi chưa đăng nhập
   │  └─ layout/                 # AppLayout, menu, header; co giãn điện thoại / iPad / máy tính
   ├─ features/
   │  ├─ auth/
   │  ├─ catalog/
   │  ├─ customer/
   │  ├─ visit/
   │  ├─ expense/
   │  ├─ closing/
   │  └─ dashboard/
   ├─ shared/                    # dùng chung, KHÔNG import từ features/
   │  ├─ api/
   │  │  ├─ client.ts            # wrapper fetch duy nhất: credentials 'include', parse lỗi chung (S0-08)
   │  │  └─ errors.ts            # kiểu ApiError, ánh xạ lỗi từng ô cho form
   │  ├─ components/             # MoneyInput, MoneyText, DateText, ConfirmButton, PageHeader...
   │  ├─ hooks/                  # useDebounce, useBreakpoint...
   │  └─ lib/
   │     ├─ money.ts             # formatMoney(1234000) → "1.234.000 đ"; KHÔNG có hàm tính tiền
   │     └─ date.ts              # định dạng dd/MM/yyyy, giờ Asia/Ho_Chi_Minh
   └─ test/setup.ts              # cấu hình Vitest
```

Một tính năng bên trong (ví dụ `catalog`):

```
features/catalog/
├─ api.ts                # hàm gọi API + query key + hook: useServices, useCreateService...
├─ types.ts              # kiểu dữ liệu khớp DTO backend (ServiceResponse...)
├─ schemas.ts            # schema Zod cho form
├─ pages/
│  └─ ServiceListPage.tsx
├─ components/
│  ├─ ServiceTable.tsx
│  └─ ServiceForm.tsx
├─ routes.tsx            # route của tính năng, app/router.tsx gom lại
└─ index.ts              # chỉ export những gì tính năng khác được dùng
```

Quy tắc:

- Tính năng này chỉ import từ `index.ts` của tính năng khác, không import sâu vào file bên trong.
- Mọi lời gọi API đi qua `shared/api/client.ts`. Không gọi `fetch`/`axios` trong component.
- Query key đặt trong `api.ts` của tính năng: `['catalog', 'services', filters]`. Sau khi sửa dữ liệu thì
  `invalidateQueries` theo key, không tự sửa cache bằng tay trừ khi có lý do.
- Đặt tên: component và page `PascalCase.tsx`, hook `useXxx.ts`, còn lại `camelCase.ts`. Page kết thúc
  bằng `Page`.
- Test đặt cạnh file: `money.test.ts`, `ServiceForm.test.tsx`. Luồng nhiều trang để ở `e2e/`.
- Dùng alias `@/` cho `src/` (`@/shared/lib/money`), không viết `../../../`.

## Dữ liệu và trạng thái

- Dữ liệu từ máy chủ dùng **TanStack Query**; không chép vào state cục bộ rồi tự đồng bộ.
- Form dùng React Hook Form + Zod. Báo lỗi tại từng ô, thông báo tiếng Việt dễ hiểu.

## Tiền và ngày

- **Không tự tính tiền ở giao diện** (tiền giảm, tổng doanh thu, thực nhận...). Backend là nơi tính; giao diện chỉ hiển thị.
- Giao diện chỉ **định dạng** số tiền nguyên đồng thành `1.234.000 đ`; ô nhập chỉ nhận chữ số.
- Ngày theo giờ `Asia/Ho_Chi_Minh`, hiển thị dd/MM/yyyy.

## Bảo mật

- **Không lưu token vào `localStorage`/`sessionStorage`.** Phiên nằm trong cookie `httpOnly` do backend đặt.
- Không đưa bí mật vào mã frontend hay biến `VITE_*` (mọi thứ trong đó đều lộ ra trình duyệt).

## Giao diện

- Dùng tốt trên điện thoại, iPad và máy tính (chủ tiệm dùng cả ba). Kiểm tra cả ba cỡ màn hình.
- Ưu tiên component có sẵn của Ant Design; bật tree-shaking; không thêm thư viện UI khác (ADR-0001).

## Test và lệnh

- Logic và hook: Vitest. Luồng chính (đăng nhập, tạo dịch vụ): Playwright.
- Chạy trước khi commit (trong `frontend/`):

  ```bash
  npm run format && npm run lint && npm run typecheck && npm test
  ```

- Chạy dev: `npm run dev` (http://localhost:5173). `/api` và `/actuator` được proxy tới backend
  `http://localhost:8080` (đổi bằng biến `BACKEND_URL`).
