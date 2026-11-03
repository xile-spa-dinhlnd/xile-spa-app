---
name: implement
description: Build an approved Xile Spa design. Input is a flow file docs/design/flows/<module>/<feature>.md (optionally "part N") or "--fix <card>" to apply the latest fix request in .handoff/<card>.md. Creates the card branch if needed, writes backend and frontend code with tests per layer, Postman, syncs the flow file, appends a build report to .handoff/<card>.md, then stops. Use when the user types /implement.
---

# /implement — build an approved design

## Input (exactly one)

- **Flow file** with `Trạng thái: thiết kế (đã duyệt)`. Still has "Câu hỏi mở" or "chờ duyệt" → stop and ask.
  If it has a "Các phần" section the user must give `part <N>`; missing → ask. Part N needs part N-1 marked `đã soát`
  (Huy: `đã code`).
- **`--fix <card>`**: read the last "Fix request" section in `.handoff/<card>.md`. Do exactly its numbered items,
  nothing more; respect its "Keep as is" list.

Read only: the flow file (or fix request), the files it names, and the closest existing module to copy from
(`module/staff` for simple catalogs, `module/catalog` for priced ones). Do not read other docs unless the flow file
points to them.

## Rules

- Build exactly what the design says. Design wrong, incomplete or impossible (new table, API change, touching the other
  person's module) → stop and ask; once approved, update the flow file first, then code.
- Need a library not in `package.json` / `libs.versions.toml` → stop and ask (`AGENTS.md` section 3).
- An unexpected red test (existing test, another part's test) → stop and report: which test, the error, your suspicion.
  Never edit test infrastructure (`src/test/resources`, `support/`) or other parts' tests to make things green.
- Design states a result but not how → pick the simplest way that fits existing code; note it under "Differs from
  design".

## Steps

0. **Branch** per `.agents/rules/builder.md` ("Branch") before writing any file.
1. **Backlog** (part 1 or whole card only): card to `Đang làm` in `docs/backlog/sprint-*.md`.
2. **DB** if needed: `/new-migration`.
3. **Backend** (`backend/AGENTS.md` module template), test right after each layer with `/write-tests`:
   1. `entity/` (+ `enums/`): business methods instead of setters, timestamps passed in from `Clock`. Entity test if it
      has business methods.
   2. `repository/`.
   3. `service/`: `@Transactional`, business checks, `AuditService.record(...)` in the same transaction,
      `BusinessException` + `ErrorCode`. Service test (Mockito).
   4. `dto/request`, `dto/response` (records, Vietnamese validation messages), `mapper/` (MapStruct, copy only).
   5. `controller/`: URL convention, no logic. `...ControllerIT` (`@IntegrationTest`). Walk `endpoint-checklist.md`
      (next to this file) for every endpoint.
4. **Frontend** (`frontend/AGENTS.md`): `types.ts` → `api.ts` → `schemas.ts` → components, pages → `routes.tsx` →
   `index.ts`. Use the `antd` skill before writing Ant Design code. Screens follow the flow file's "Màn hình" section and
   `docs/design/ux-guidelines.md` exactly (layout, four states, exact copy). CSS Modules only, no `style={{}}`. Test
   schemas and components with `/write-tests`. Check phone, iPad and desktop widths.
5. **Impact**: do every line of the flow file's "Ảnh hưởng" section; grep changed URLs and method names in
   `frontend/src`, `backend/src/test`, `docs/postman` until nothing is left (`AGENTS.md` section 7).
6. **Postman**: add or update requests in `docs/postman/xile-spa.postman_collection.json` with body and status check;
   main error cases get their own request.
7. **Docs and security**: SRS, ERD, README if behavior changed; AGENTS files if a convention changed (rule 11). Follow
   the flow file's "Bảo mật" section and parts B and D of `docs/security/security-checklist.md`.
8. **Flow file matches code** (gate): update diagrams, rules, errors to the code you wrote; the "Hành vi và test" table
   lists real test names that exist now; groups not applicable say `không áp dụng: <reason>`. Set
   `Trạng thái: khớp code` and add history line `<dd/MM/yyyy> | <user name> (Antigravity) | <card> | Khớp code`. Empty
   cells → back to `/write-tests`.
9. **Backlog**: card to `Xong` when the Definition of Done (`AGENTS.md` section 5) is met.
10. **Stop** and report.

Part N: do steps 0–4 only for the classes, tests and Postman listed for part N; write the real test names for part N in
"Hành vi và test" and set part N to `đã code`. Steps 5–9 only in the last part. Never write code of a later part.

Fix mode: only the fix request's items, then step 8 if behavior changed vs. the flow file.

## Before reporting

- Scope check per `.agents/rules/builder.md` ("Scope discipline").
- Every changed main file (entity, service, controller, schema, component, page, hook) has its test file changed or
  added.
- Commands for the parts you touched are green: `cd backend && ./gradlew spotlessApply test`;
  `cd frontend && npm run format && npm run lint && npm run typecheck && npm test`.

## Report

Use the template in `.agents/rules/builder.md` ("Report"): append it to `.handoff/<card>.md` and show it. Round number:
fix mode uses the fix request's round; a normal run starts at round 0 (or the next part's first entry).

Suggested commit: inside a card use `feat`, `refactor` or `test`, never `fix`; part runs end the description with
`(part N)`. Next step line:

- Đình: "Run `/self-review` (or `/self-review part N`) in Claude Code."
- Huy: "Read the diff; mark the part `đã soát` and run `/implement <flow file> part <N+1>`, or type `/pre-pr-check`
  after the last part."
