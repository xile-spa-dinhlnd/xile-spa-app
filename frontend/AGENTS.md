# frontend/AGENTS.md — frontend coding conventions

Read `../AGENTS.md` first; this file adds only frontend specifics. Stack: React 19 + TypeScript (Vite), Ant Design 6,
TanStack Query, React Router, React Hook Form + Zod, Vitest; lint with oxlint, format with Prettier. Recharts and
Playwright are chosen in ADR-0001 but **not installed**. Code, comments and test names in English; every label and
message the user sees in Vietnamese with diacritics. Writing Ant Design code: look up the v6 API with the `antd` skill.

## Libraries

Source of truth: `package.json` (changed only via `npm install`; never hand-edit `package-lock.json`). Not there = not
available. Adding one: ask first per `../AGENTS.md` section 3. Major-version changes update this table (rule 11).

| Library                                             | Version      | Used for                       | API notes (docs and old examples often target the previous version)                                                                     |
| --------------------------------------------------- | ------------ | ------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------- |
| React, React DOM                                    | 19           | UI                             | Function components and hooks; see "React and TypeScript"                                                                               |
| TypeScript                                          | 6 (`strict`) | types                          | `npm run typecheck` is the truth, not editor squiggles                                                                                  |
| Vite (+ `@vitejs/plugin-react`)                     | 8            | dev server, build              | Alias `@/`, `/api` proxy in `vite.config.ts`                                                                                            |
| Ant Design, `@ant-design/icons`                     | 6            | components                     | `message`, `modal` from `App.useApp()`, never static `message`; renamed props vs. v5 (`destroyOnHidden` replaces `destroyOnClose`)      |
| TanStack Query                                      | 5            | API calls, cache               | Object form `useQuery({ queryKey, queryFn })`; first load is `isPending` (v4: `isLoading`); keep the old page with `placeholderData`    |
| React Router                                        | 8            | routing                        | Import from `react-router`; there is no `react-router-dom`                                                                              |
| React Hook Form, `@hookform/resolvers`              | 7, 5         | forms                          | Zod schema via `zodResolver`                                                                                                            |
| Zod                                                 | 4            | form validation                | Error text as `{ error: '...' }` like existing code (v3: `{ message }`)                                                                 |
| dayjs                                               | 1.11         | dates                          | `vi` locale loaded in `app/providers.tsx`; no `date-fns`, `moment`                                                                      |
| Vitest, jsdom                                       | 5, 29        | tests                          | `npm test`; see "Tests and commands"                                                                                                    |
| Testing Library (`react`, `user-event`, `jest-dom`) | 16, 14, 7    | component tests                | Query by role and label as the user sees them                                                                                           |
| oxlint                                              | 1            | lint                           | Not ESLint; `eslint-disable`/`oxlint-disable` comments are banned (`scripts/check-lint-suppressions.mjs`)                                |
| Prettier                                            | 3            | format                         | `npm run format`                                                                                                                        |
| Recharts, Playwright                                | not installed | dashboard (Sprint 5), E2E     | Install when a card needs them, still asking first                                                                                      |

## Structure

Features share names with backend modules (`auth`, `catalog`, `customer`, `visit`, `closing`, ...), so one person can own
both ends of a feature.

```
frontend/
├─ index.html, package.json, vite.config.ts (alias @/, /api proxy), tsconfig*.json, .oxlintrc.json, .prettierrc.json
├─ scripts/check-lint-suppressions.mjs  # in `npm run lint`: blocks eslint-disable, oxlint-disable comments
├─ scripts/check-css-tokens.mjs         # in `npm run lint`: blocks raw colors and !important in *.module.css
├─ .env.example                          # VITE_API_BASE_URL... only, no secrets
├─ e2e/                                  # Playwright, once added
└─ src/
   ├─ main.tsx                   # entry: mounts <App />, loads styles/global.css
   ├─ app/                       # App.tsx; providers.tsx (QueryClient, ConfigProvider vi_VN + theme); queryClient.ts
   │                             #   (no retry on 4xx); router.tsx (routes, auth guard); layout/ (AppLayout, menu, header)
   ├─ features/                  # auth/ catalog/ customer/ visit/ expense/ closing/ dashboard/ staff/
   ├─ styles/                    # tokens.css, global.css, theme.ts (see "CSS")
   ├─ shared/                    # shared code, NEVER imports from features/
   │  ├─ api/                    # client.ts (the only fetch wrapper: credentials 'include', shared error parsing), errors.ts
   │  ├─ components/             # MoneyInput, MoneyText, DateText, ConfirmButton, PageHeader...
   │  ├─ hooks/                  # useDebounce, useBreakpoint...
   │  └─ lib/                    # money.ts (formatMoney(1234000) → "1.234.000 đ", NO money math), date.ts
   └─ test/setup.ts              # Vitest setup
```

Inside a feature (example `catalog`): `api.ts` (API functions, query keys, hooks like `useServices`), `types.ts` (types
matching backend DTOs), `schemas.ts` (Zod form schemas), `pages/` (`ServiceListPage.tsx`), `components/`, `routes.tsx`
(collected by `app/router.tsx`), `index.ts` (only what other features may use).

- Import other features only through their `index.ts`; use the `@/` alias, never `../../../`.
- Every API call goes through `shared/api/client.ts`; no `fetch`/`axios` in components.
- Query keys in the feature's `api.ts` (`['catalog', 'services', filters]`); after writes `invalidateQueries` by key.
- Naming: components and pages `PascalCase.tsx` (pages end with `Page`), hooks `useXxx.ts`, others `camelCase.ts`.
  Tests sit next to the file (`money.test.ts`); multi-page flows go in `e2e/`.

## Data and state

- Server data only via **TanStack Query**; never copy it into local state and sync by hand; no `fetch` in `useEffect`.
- Forms: **React Hook Form + Zod** (schema in `schemas.ts`, matching backend validation), Ant Design inputs wrapped in
  `Controller`, field errors via `Form.Item` (`validateStatus`, `help`) in Vietnamese; `useWatch`, not `watch`.
- Backend errors attach to fields with `setError`: business codes (`SERVICE_NAME_DUPLICATE` → name field) and
  `fieldErrors` of `VALIDATION_FAILED`; anything else via `message.error`. Model: `features/catalog/components/ServiceFormDrawer.tsx`.
- Paged lists use `PageResponse<T>` (`shared/api/page.ts`), pages from 0; search boxes use `useDebounce`. Desktop shows a
  `Table`, phone a list of `Card`s, chosen with `Grid.useBreakpoint()`.
- Filtered lists show a "Xóa bộ lọc" button while filtering and say explicitly when nothing matches; load errors show an
  `Alert` with a "Thử lại" button. Every data screen handles four states: loading, error (retry), empty, data.

## Money and dates

- **No money math in the UI** (discounts, revenue totals, net income...). The backend computes; the UI displays.
- Format whole đồng as `1.234.000 đ` with `formatMoney`; money inputs always use `shared/components/MoneyInput` (digits
  only, integer ≥ 0 or null). Dates in `Asia/Ho_Chi_Minh`, shown dd/MM/yyyy.

## Security

- **Never store tokens in `localStorage`/`sessionStorage`**; the session is an `httpOnly` cookie set by the backend.
- No secrets in frontend code or `VITE_*` variables (everything there reaches the browser).

## React and TypeScript

- Function components with **named exports** (`export function StaffListPage`), no `export default`; props type named
  `<Name>Props`, declared right above the component.
- **No `any`**, no `as` to silence errors, no `// @ts-ignore`; unknown data is `unknown` narrowed by Zod `parse` or a type
  guard; strict mode stays on; `import type` for types. `npm run lint` blocks `any`, `@ts-ignore`, `@ts-nocheck` and
  disable comments: fix the code, never the rule.
- Editors bundle TypeScript 5 without our `strict` settings, so their errors differ from `npm run typecheck` (seen):
  keep `"strict": true` in `tsconfig.app.json`, pick "Use Workspace Version", trust `npm run typecheck`.
- Business constants are string unions matching backend enums (`type StaffRole = 'OWNER' | 'THERAPIST'`) with
  Vietnamese labels in a `Record<StaffRole, string>` (model `features/staff/types.ts`).
- **Minimal state**: values derivable from props or query data are computed during render, never `useState` +
  `useEffect`; `useEffect` only connects to things outside React (DOM, timers, subscriptions).
- Page logic moves into a hook (`useStaffFilters`) or child components past ~200 lines.
- List `key`s are stable ids, never indexes. `useMemo`, `useCallback`, `memo` only when measured slow or a stable
  reference is needed.

## UI

- Layout per screen size, four states, copy (success, error, confirm), numbers and dates: `docs/design/ux-guidelines.md`.
  This file only covers the technical side. Check phone, iPad and desktop (the owner uses all three).
- Prefer Ant Design components and layout (`Flex`, `Space`, `Row`/`Col`, `Grid.useBreakpoint()`) before writing CSS; no
  other UI library (ADR-0001).

## CSS

**CSS Modules next to the component**, shared parts in `src/styles/` (decided 2026-10-22): `tokens.css` (CSS variables
for brand color, text, backgrounds, spacing, radius: `--xl-color-brand`...), `global.css` (reset, html/body,
focus-visible; imports tokens; loaded once in `main.tsx`), `theme.ts` (Ant Design `ThemeConfig` with the same colors as
`tokens.css`, used by `providers.tsx`). Example: `app/layout/AppLayout.tsx` + `AppLayout.module.css`.

- **No `style={{...}}`** except values known only at runtime (width from data, color from data); no Ant Design
  `styles`/`style` props as a CSS Modules substitute.
- The CSS file shares the component name (`LoginForm.tsx` → `LoginForm.module.css`,
  `import styles from './LoginForm.module.css'`, `className={styles.header}`).
- Class names `camelCase` by role (`.header`, `.filterBar`, `.emptyState`), never by look (`.red`, `.mt16`). Nesting
  ≤ 2, no `!important`, `:global` only to override an Ant Design class (with a comment why).
- **Colors, spacing, radius from variables** in `tokens.css` (`var(--xl-space-md)`) or Ant Design tokens; never raw
  colors. `npm run lint` blocks hex, `rgb()`/`hsl()`, named colors and `!important` in `*.module.css`
  (`scripts/check-css-tokens.mjs`): when lint fails, switch to a variable; never work around it.
- A class on an Ant Design component can lose to stronger antd selectors (`h4.ant-typography`, `div.ant-typography`,
  `.ant-drawer .ant-drawer-body`, `.ant-btn.ant-btn-icon-only`, `.ant-card:not(...)`): nest under a parent class in the
  same module (`.header .title`) or use a token in `theme.ts`, with a comment. jsdom tests cannot catch this; check
  visually or with screenshots.
- Off-scale spacing rounds to the nearest step (20px → `--xl-space-xl`, 10px → `--xl-space-sm` or `--xl-space-md`);
  negatives use `calc(-1 * var(--xl-space-sm))`. No variables for odd values.
- Responsive: mobile first, `@media (min-width: ...)` at Ant Design breakpoints 576, 768, 992, 1200 px (numbers, since
  CSS cannot use `var()` in `@media`; see the comment atop `tokens.css`).
- Shared components in `shared/components/` have their own `.module.css`; never import another feature's CSS.
- Old code still using `style={{}}`: convert a file when you edit it; never mix both styles in one file.

## Tests and commands

- Logic and hooks: Vitest. Main flows (login, create service): Playwright, once installed.
- Cover the groups of `AGENTS.md` section 10, by layer:
  - **Pure functions** (`shared/lib`, `schemas.ts`): one `it` per branch, `it.each` for many inputs of one shape. Zod
    schemas: valid case, each broken rule, edge cases (whitespace-only, zero). Models: `money.test.ts`,
    `features/staff/schemas.test.ts`.
  - **Components, pages:** Testing Library wrapped in `Providers` and `MemoryRouter`, API faked with `vi.fn()` on `fetch`
    (never the real backend). Check displayed data, loading/error/empty states, backend errors on the right field
    (`setError`), the right API call and body. Model: `StaffListPage.test.tsx`.
- Query by role and visible text (`getByRole('button', { name: 'Lưu' })`, `getByLabelText`), not CSS classes or
  `data-testid` (unless nothing else works); wait with `findBy...`/`waitFor`.
- Each test cleans up (`afterEach(() => vi.restoreAllMocks())`) and does not depend on order. New `it('...')` names are
  English; existing Vietnamese names are translated when the file is touched.
- Before committing (in `frontend/`): `npm run format && npm run lint && npm run typecheck && npm test`. One file:
  `npx vitest run src/shared/lib/money.test.ts` (add `-t "test name"`). Dev: `npm run dev` (http://localhost:5173);
  `/api` and `/actuator` proxy to `http://localhost:8080` (override with `BACKEND_URL`).
