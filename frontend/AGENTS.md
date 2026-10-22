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
   ├─ main.tsx                   # điểm vào, gắn <App /> và nạp styles/global.css
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
   ├─ styles/                    # tokens.css, global.css, theme.ts (mục CSS)
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
- Form dùng **React Hook Form + Zod** (schema trong `schemas.ts` của tính năng, khớp kiểm tra của backend),
  bọc component Ant Design bằng `Controller`. Báo lỗi tại từng ô bằng `Form.Item` (`validateStatus`, `help`),
  thông báo tiếng Việt. Dùng `useWatch` thay cho `watch` (để React Compiler không cảnh báo).
- Lỗi từ backend gắn vào ô tương ứng bằng `setError`: mã lỗi nghiệp vụ (ví dụ `SERVICE_NAME_DUPLICATE` → ô
  tên) và `fieldErrors` của lỗi `VALIDATION_FAILED`; lỗi khác hiện bằng `message.error`. Mẫu:
  `features/catalog/components/ServiceFormDrawer.tsx`.
- Danh sách phân trang dùng kiểu `PageResponse<T>` (`shared/api/page.ts`), trang bắt đầu từ 0; ô tìm kiếm dùng
  `useDebounce`. Máy tính hiển thị bảng (`Table`), điện thoại hiển thị danh sách thẻ (`Card`), chọn theo
  `Grid.useBreakpoint()`.
- Trang danh sách có lọc: khi đang lọc thì hiện nút **"Xóa bộ lọc"**, danh sách trống thì nói rõ "không có kết
  quả khớp bộ lọc" (không để trang trắng); lỗi tải dữ liệu hiện `Alert` có nút "Thử lại", không im lặng.

## Tiền và ngày

- **Không tự tính tiền ở giao diện** (tiền giảm, tổng doanh thu, thực nhận...). Backend là nơi tính; giao diện chỉ hiển thị.
- Giao diện chỉ **định dạng** số tiền nguyên đồng thành `1.234.000 đ` (`formatMoney`); ô nhập tiền luôn dùng
  `shared/components/MoneyInput` (chỉ nhận chữ số, trả số nguyên ≥ 0 hoặc null).
- Ngày theo giờ `Asia/Ho_Chi_Minh`, hiển thị dd/MM/yyyy.

## Bảo mật

- **Không lưu token vào `localStorage`/`sessionStorage`.** Phiên nằm trong cookie `httpOnly` do backend đặt.
- Không đưa bí mật vào mã frontend hay biến `VITE_*` (mọi thứ trong đó đều lộ ra trình duyệt).

## React và TypeScript

- Component là function, **export có tên** (`export function StaffListPage`), không `export default`.
  Kiểu props đặt tên `<Tên>Props`, khai báo ngay trên component.
- **Không `any`**, không `as` để ép kiểu cho qua lỗi, không `// @ts-ignore`. Dữ liệu chưa rõ kiểu dùng `unknown`
  rồi thu hẹp (Zod `parse`, type guard). Không tắt chế độ strict của TypeScript. Import kiểu bằng `import type`.
- Hằng số và kiểu dữ liệu nghiệp vụ dùng union của chuỗi khớp enum backend (`type StaffRole = 'OWNER' |
'THERAPIST'`), nhãn tiếng Việt trong một `Record<StaffRole, string>` (mẫu: `features/staff/types.ts`).
- **State tối thiểu**: giá trị tính được từ props hoặc dữ liệu query thì tính trong lúc render, không `useState` +
  `useEffect` để đồng bộ. `useEffect` chỉ để nối với thứ bên ngoài React (DOM, timer, subscription).
- Dữ liệu máy chủ chỉ qua TanStack Query (mục Dữ liệu và trạng thái). Không `fetch` trong `useEffect`.
- Logic của trang tách ra hook riêng (`useStaffFilters`) hoặc component con khi file quá khoảng 200 dòng.
- `key` của danh sách là id ổn định, không dùng chỉ số mảng. `useMemo`, `useCallback`, `memo` chỉ dùng khi đo thấy
  chậm hoặc cần giữ tham chiếu ổn định cho hook khác.
- Xử lý đủ bốn trạng thái của mỗi màn hình có dữ liệu: đang tải, lỗi (có "Thử lại"), trống, có dữ liệu.

## Giao diện

- Dùng tốt trên điện thoại, iPad và máy tính (chủ tiệm dùng cả ba). Kiểm tra cả ba cỡ màn hình.
- Ưu tiên component có sẵn của Ant Design; bật tree-shaking; không thêm thư viện UI khác (ADR-0001).
- Bố cục ưu tiên component của Ant Design (`Flex`, `Space`, `Row`/`Col`, `Grid.useBreakpoint()`) trước khi viết CSS.

## CSS

Quyết định 22/10/2026: **CSS Modules đặt cạnh component**, phần dùng chung ở `src/styles/`. Vite hỗ trợ sẵn, không
thêm thư viện.

```
src/
├─ styles/
│  ├─ tokens.css          # biến CSS: màu thương hiệu, chữ, nền, khoảng cách, bo góc (--xl-color-brand...)
│  ├─ global.css          # reset, html/body, focus-visible; import tokens.css; nạp một lần ở main.tsx
│  └─ theme.ts            # ThemeConfig của Ant Design (cùng giá trị màu với tokens.css), providers.tsx dùng
├─ app/layout/
│  ├─ AppLayout.tsx
│  └─ AppLayout.module.css
└─ features/staff/pages/
   ├─ StaffListPage.tsx
   └─ StaffListPage.module.css
```

- **Không `style={{...}}`**, trừ giá trị chỉ biết lúc chạy (chiều rộng tính từ dữ liệu, màu lấy từ dữ liệu). Không
  dùng thuộc tính `styles`/`style` của Ant Design để thay cho CSS Modules.
- File CSS đặt cùng tên với component: `LoginForm.tsx` → `LoginForm.module.css`, import
  `import styles from './LoginForm.module.css'`, dùng `className={styles.header}`.
- Tên class `camelCase` theo vai trò (`.header`, `.filterBar`, `.emptyState`), không theo hình thức (`.red`,
  `.mt16`). Không selector lồng quá 2 cấp, không `!important`, không `:global` trừ khi phải ghi đè class của
  Ant Design (ghi chú lý do).
- **Màu, khoảng cách, bo góc lấy từ biến** trong `tokens.css` (`var(--xl-space-md)`) hoặc token Ant Design; không
  viết mã màu trực tiếp. Màu thương hiệu đổi ở một chỗ.
- Responsive: mobile trước, `@media (min-width: ...)` với breakpoint của Ant Design: 576, 768, 992, 1200 px (CSS
  không cho dùng `var()` trong `@media` nên ghi số, xem chú thích đầu `tokens.css`).
- Component dùng chung trong `shared/components/` cũng có `.module.css` riêng; không import CSS của feature khác.
- Code cũ còn `style={{}}` (card chuyển đổi riêng): sửa file nào thì chuyển file đó, không để lẫn hai kiểu trong
  một file.

## Test và lệnh

- Logic và hook: Vitest. Luồng chính (đăng nhập, tạo dịch vụ): Playwright.
- Đủ các nhóm ca theo `AGENTS.md` mục 10. Phân tầng:
  - **Hàm thuần** (`shared/lib`, `schemas.ts`): mỗi nhánh một `it`, dùng `it.each` cho nhiều đầu vào cùng kiểu.
    Schema Zod: ca hợp lệ, từng ràng buộc sai, ca biên (chuỗi toàn khoảng trắng, số 0). Mẫu: `money.test.ts`,
    `features/staff/schemas.test.ts`.
  - **Component, trang:** Testing Library, bọc `Providers` và `MemoryRouter`, giả API bằng `vi.fn()` gán cho
    `fetch` (không gọi backend thật). Kiểm tra: dữ liệu hiển thị, trạng thái tải/lỗi/trống, lỗi backend gắn đúng ô
    (`setError`), gọi đúng API với body đúng. Mẫu: `StaffListPage.test.tsx`.
- Tìm phần tử theo vai trò và chữ người dùng thấy (`getByRole('button', { name: 'Lưu' })`, `getByLabelText`), không
  theo class CSS hay `data-testid` (trừ khi không còn cách). Chờ kết quả bất đồng bộ bằng `findBy...`/`waitFor`.
- Mỗi test tự dọn: `afterEach(() => vi.restoreAllMocks())`; không phụ thuộc thứ tự chạy.
- Chạy trước khi commit (trong `frontend/`):

  ```bash
  npm run format && npm run lint && npm run typecheck && npm test
  ```

- Chạy riêng một file test: `npx vitest run src/shared/lib/money.test.ts` (thêm `-t "tên test"` cho một test);
  Playwright (khi đã thêm): `npx playwright test e2e/login.spec.ts`.
- Chạy dev: `npm run dev` (http://localhost:5173). `/api` và `/actuator` được proxy tới backend
  `http://localhost:8080` (đổi bằng biến `BACKEND_URL`).
