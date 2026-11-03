# Checklist for every new or changed endpoint

Walk the whole list before reporting done.

## Backend

- [ ] URL `/api/{module}/{resource}`, plural, kebab-case; business actions are `POST .../{id}/<action>`.
- [ ] No `GET` changes data.
- [ ] Controller takes and returns DTOs only, has `@Valid`, never calls a repository, never builds an error
      `ResponseEntity`.
- [ ] Request DTO is a `record` whose validation annotations carry Vietnamese `message`s; response DTO is a `record`.
- [ ] Mapper uses `@Mapper(config = MapStructConfig.class)`; sensitive fields (id, status, locked price, `created_by`)
      are `ignore = true` and set in the service; no calculations in mappers.
- [ ] Service is `@Transactional`; changes to financial or catalog data call `AuditService.record` with DTOs, never
      entities.
- [ ] Money is `long`; timestamps come from `Clock`; business date is the user-chosen `LocalDate`.
- [ ] Writes for a business date check the closed day first and use `lock_business_day(d)` when needed (ERD 5.5).
- [ ] New business errors are added to `ErrorCode` (code, HTTP status, Vietnamese message).
- [ ] Other modules are used only through their `service/` and `dto/`; references by id.
- [ ] Paged lists return `PageResponse<T>`, pages start at 0.
- [ ] Endpoint requires login (default); passwords and tokens never logged.

## Tests

- [ ] All six groups of `AGENTS.md` section 10 (via `/write-tests`).
- [ ] A unit test per service branch (success and each error).
- [ ] `...ControllerIT` with `@IntegrationTest`: 2xx, 400 (validation with `fieldErrors`), 401, 404, 409 when relevant.
- [ ] IT data uses its own prefix, no `deleteAll()` on shared tables, never counts the whole `audit_log`.
- [ ] No `@Disabled`, no loosened asserts.

## Frontend

- [ ] API calls go through `shared/api/client.ts`; query keys live in `api.ts`; `invalidateQueries` after writes.
- [ ] `types.ts` matches the DTOs; the Zod schema matches backend validation.
- [ ] Backend errors attach to fields with `setError` (business codes and `fieldErrors`); others use `message.error`.
- [ ] Money only via `formatMoney` and `MoneyInput`; no money math in the UI.
- [ ] Desktop uses `Table`, phone uses `Card`; empty state; load error has a "Thử lại" button.
- [ ] Styles in `<Component>.module.css` with variables from `src/styles/tokens.css`; no `style={{}}`, no raw colors.

## Docs

- [ ] Postman requests (success and main errors) with status checks.
- [ ] SRS, ERD updated if behavior or schema changed.
