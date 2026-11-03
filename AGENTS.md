# AGENTS.md — project facts and hard rules for every AI agent

Shared by every agent (Antigravity, Claude Code, others). It holds **facts and hard rules only**. How each agent works
lives in its own config: Antigravity in `.agents/` (role: `.agents/rules/builder.md`), Claude Code in `CLAUDE.md`
and `.claude/`. Part-specific coding conventions: `backend/AGENTS.md`, `frontend/AGENTS.md`. If another file
disagrees with this one, this file wins. Point to docs; do not copy them here.

## 1. Project

Admin system for Xile Spa & Stretching (one small shop; main user is the owner, on desktop, phone and iPad). Replaces a
monthly Excel workflow: services and combos, daily revenue book with day closing, customer CRM, tour fees and payroll,
one-way sync to Google Sheets. Team of two, **Đình** and **Huy**, split by feature; 2-week sprints; cards on Trello.

## 2. Which doc to read for which job

Do not read all of `docs/` (written in Vietnamese). Read only the row that matches the job.

| Job                                                  | Read first                                                                                                                    |
| ---------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| Anything                                             | this file, `README.md`                                                                                                        |
| Money, discounts, revenue, day closing, adjustments  | `docs/srs/04-business-rules.md`, `docs/design/erd.md` (sections 5, 6), `docs/adr/0003-closed-day-corrections-via-adjustments.md` |
| A new API or screen                                  | the matching FR section in `docs/srs/03-functional-requirements.md`                                                           |
| Screen layout, copy, states                          | `docs/design/ux-guidelines.md`                                                                                                |
| Tables, columns, indexes, migrations                 | `docs/design/erd.md`, then `backend/src/main/resources/db/migration/`                                                         |
| Designing, fixing or changing a feature              | `docs/design/flows/<module>/<feature>.md` (template: `docs/design/flows/README.md`)                                           |
| Tech and library choices                             | `docs/adr/0001-tech-stack.md`                                                                                                 |
| Google Sheets, source of truth                       | `docs/adr/0002-source-of-truth-and-sheets-mirror.md`                                                                          |
| Terms, field names                                   | `docs/srs/06-data-dictionary.md`                                                                                              |
| Unclear or conflicting business rule                 | `docs/srs/07-open-questions.md` (OQ codes) — **ask, do not decide**                                                           |
| Current card, sprint scope                           | latest sprint file in `docs/backlog/` (Trello runs in parallel)                                                               |
| Releases, road to v1.0.0                             | `docs/roadmap.md`                                                                                                             |
| Branches, commits, pull requests                     | `CONTRIBUTING.md`                                                                                                             |
| Trying the API with Postman                          | `docs/postman/`                                                                                                               |
| Security, sensitive data, secrets                    | `docs/security/` (`docs/security/security-checklist.md` for every card)                                                       |
| Human guide to the AI workflow                       | `docs/guides/ai-agents.md`                                                                                                    |

## 3. Stack and commands

Java 21 · Spring Boot 4 · Gradle (Kotlin DSL) · Spring Data JPA · Spring Security (JWT in `httpOnly` cookie) · Flyway ·
PostgreSQL · React + TypeScript (Vite) · Ant Design · TanStack Query · JUnit 6 + Testcontainers · Vitest. Versions and
API notes: the "Libraries" table in `backend/AGENTS.md`, `frontend/AGENTS.md`; source of truth is
`backend/gradle/libs.versions.toml` and `frontend/package.json` (not listed there = not available, do not import).

**Adding a library: ask the card owner first, never install on your own** (test-only libraries too). The question
covers: purpose (why existing libraries cannot do it), pros/cons vs. no library and vs. one alternative, reputation
(maintainer, license, last release, usage), performance (bundle size or startup/memory). Once approved, the same PR adds
an "Update" line to ADR-0001 and a row to the "Libraries" table.

Needs JDK 21, Node 22+, Docker Desktop **running** (dev database and Testcontainers). Dev DB: `docker compose up -d`.

| Task                 | Backend (in `backend/`)                                                      | Frontend (in `frontend/`)                                         |
| -------------------- | ---------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| Format + full test   | `./gradlew spotlessApply test`                                               | `npm run format && npm run lint && npm run typecheck && npm test` |
| One test class       | `./gradlew test --tests 'com.xilespa.module.staff.service.StaffServiceTest'` | `npx vitest run src/shared/lib/money.test.ts`                     |
| One test method      | append `.methodName` to the class above                                      | append `-t "test name"`                                           |
| Run the app          | `./gradlew bootRun` (profile `dev`)                                          | `npm run dev` (http://localhost:5173)                             |
| Agent tooling (root) | `node --test "tools/**/*.test.mjs"` and `node tools/context-check/check.mjs` |                                                                   |

## 4. Hard rules

1. **Money is whole đồng.** `long`/`bigint`; never `float`, `double`, `BigDecimal` (BR-01). Format `1.234.000 đ` only in
   the UI.
2. **Every schema change is a new Flyway migration** `V{n}__description.sql`. Never edit or delete an existing one.
   `ddl-auto` is only `validate` or `none`; no manual schema edits.
3. **No hard deletes** of business data. Used services and combos are deactivated (BR-19); visits become `VOID` (BR-09).
4. **A closed day is immutable.** No code that edits `CLOSED` days, no "reopen" feature. Corrections only through
   adjustment entries (BR-06 to BR-08, ADR-0003).
5. **Never weaken tests to go green.** No `@Disabled`, no `skip`, no loosened asserts. A red test means the code or the
   expectation is wrong; say which.
6. **No secrets in the repo.** Passwords, API keys, Google service accounts, real customer data come from environment
   variables. Never commit `.env`; never log passwords or tokens.
7. **Business timezone is `Asia/Ho_Chi_Minh`.** The business date is the date the user picks, stored separately from
   the creation timestamp (BR-02).
8. **Every change to financial or catalog data writes `audit_log`** in the same transaction (BR-18).
9. **Do not invent business rules.** Check `docs/srs/07-open-questions.md`; still unclear → stop and ask; write the
   assumption in the PR instead of guessing.
10. **One card, one branch, one PR.** Do not touch files outside the card (especially SRS, ERD, old migrations) without
    saying so. Main code (excluding tests, Postman, docs) expected over ~400 lines → say so in the design.
11. **Keep the AGENTS files true to the code.** Changing architecture, folder layout, naming, libraries or shared
    conventions updates `AGENTS.md`, `backend/AGENTS.md` or `frontend/AGENTS.md` in the same PR.
12. **Never hand-edit generated files:** `frontend/package-lock.json`, Gradle Wrapper (`backend/gradlew*`,
    `backend/gradle/wrapper/`), `docs/design/diagrams/` (generated by `docs/design/tools/gen_erd_mermaid.py`), build
    output (`build/`, `dist/`, `node_modules/`).

**Language.** Agent config and rule files (`AGENTS.md`, `CLAUDE.md`, `.agents/`, `.claude/`), code, code comments, test
names, commit messages, PR text, review comments and handoff files: **English**. `docs/`, `README.md`,
`CONTRIBUTING.md`, UI labels and user-facing messages, Excel/Sheets columns: **Vietnamese** (with diacritics). Agents talk
to Đình and Huy in Vietnamese. `tools/context-check` fails on Vietnamese prose in agent files.

## 5. Card lifecycle and Definition of Done

`SPEC → BUILD → REVIEW → SHIP`. Who does what: section 8; step details live in each agent's own config.

A card is done when: the flow file `docs/design/flows/<module>/<feature>.md` matches the code (`Trạng thái: khớp code`,
history line, real test names); business logic has tests meeting section 10; format, lint, typecheck and tests pass
locally; every new or changed endpoint is in `docs/postman/xile-spa.postman_collection.json` with a status check;
SRS/ERD/README updated if behavior changed; part D of `docs/security/security-checklist.md` done; the card is `Xong` in
`docs/backlog/`; the PR follows `CONTRIBUTING.md`.

## 6. Adding a rule after a repeated mistake

Prefer a check over prose: a lint rule, a test or a hook beats a sentence. Otherwise add **one line** next to the rule
on the same topic: backend-only or frontend-only mistakes go to that part's AGENTS file; section 7 keeps only
cross-cutting ones. Size limits (every agent loads these files every session): `AGENTS.md` ≤ 180 lines, part AGENTS
files ≤ 260, each skill ≤ 150. Over the limit → merge lines or drop lines a check already enforces; never raise the
limit. `tools/context-check` enforces limits, section 2 paths, loader files and the language rule.

## 7. Known pitfalls (seen for real; check when writing and reviewing)

- Forgetting the Postman collection and the card status in `docs/backlog/`.
- A Postman request using `{{variables}}` created by another folder (S2-07, S3-08): each folder creates its own data and
  passes when run alone; a body rejected before the id is read gets a fixed value.
- Two branches taking the same migration number `V{n}`: tell the other person first (`CONTRIBUTING.md` section 4).
- Changing an endpoint's behavior without finding its callers: grep the URL in `frontend/src`, `backend/src/test`,
  `docs/postman` and fix them in the same PR (S2-06).
- Moving a method and leaving the old copy, or two repository methods doing the same thing: grep the old name, delete.
- A new state constraint (inactive, closed...) enforced in one place only: list every method that moves a record into or
  out of that state, plus records that reference it (OQ-21).
- Tests that check only part of the result: unchecked child-list fields, no re-read from DB after a write, no order check
  when the API promises sorting (S2-07).
- Commit messages that break `CONTRIBUTING.md` section 3, or `fix` used for new work inside a card.

## 8. Roles

| Agent                             | Used by   | Role                    | Owns                                                                  | Config                                   |
| --------------------------------- | --------- | ----------------------- | --------------------------------------------------------------------- | ---------------------------------------- |
| Antigravity                       | Đình, Huy | Builder                 | SPEC for Huy's cards (`/plan-task`), BUILD, SHIP (`/pre-pr-check`)    | `.agents/` (rules, skills, `hooks.json`) |
| Claude Code (Đình's machine)      | Đình      | Architect and Reviewer  | SPEC for Đình's cards (`/plan-feature`), REVIEW (`/self-review`, `/review-pr`), small review fixes | `CLAUDE.md`, `.claude/` (local only)     |
| Claude app (project on claude.ai) | Đình      | Planner                 | sprint planning, card splitting, releases, large docs                 | claude.ai project memory                 |

- Builder and reviewer hand work over through `.handoff/<card>.md` (git-ignored): the reviewer appends a fix request,
  the builder appends a build report. Nobody pastes long blocks between chats.
- Shared hooks in `tools/agent-hooks/` block: git commands that switch branches, commit or push (only exception:
  Antigravity `git fetch origin` and `git switch -c <type>/<name> origin/develop` while on `develop` or `main`); mass
  deletes; reading or editing secret files; editing migrations already on `develop`; editing generated files; editing
  hook config. Blocked → stop and tell the user; never work around it.
- Only `/pre-pr-check` gives git commands (push, PR, squash, cleanup), and only when the user types it.
- Subfolder rules live only in `backend/AGENTS.md`, `frontend/AGENTS.md`. Antigravity loads them via
  `.agents/rules/backend.md`, `frontend.md`; Claude Code via `backend/CLAUDE.md`, `frontend/CLAUDE.md` (`@AGENTS.md`).

## 9. Clean code (backend and frontend)

- One function, one job; validate input and return early; no `if` nesting deeper than 2. Functions over ~40 lines or
  components over ~200 lines get split.
- Business names from `docs/srs/06-data-dictionary.md` (`businessDate`, `tourFee`; never `date2`, `data`, `tmp`).
  Booleans start with `is`, `has`, `can`.
- No magic numbers: named constants in config or the module (login attempts, token lifetime, page size).
- Never swallow errors: translate to a business error, or log and rethrow.
- No premature abstraction: extract on the third repetition. No interface with one implementation, no "for later"
  parameters.
- Comments explain why, not what; cite BR/FR/OQ codes when code enforces a business rule.
- No commented-out code, `console.log`, `System.out.println`, or `TODO` without a card code.

## 10. Test bar

Every new or changed business behavior (service, endpoint, form schema, utility with logic) covers all groups below. A
missing group is explained in the PR; for money, closing, adjustments and login a missing case blocks the merge.

| Group           | Checks                                                                                                                                                       |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Happy path      | typical valid input gives the right result, including stored data and `audit_log`                                                                            |
| Invalid input   | each validation rule: empty, too long, bad format, bad enum → 400 with the right `fieldErrors`; decimals or strings for money and quantities; `null` in lists |
| Edge cases      | money 0 and very large; whitespace-only strings incl. NBSP and zero-width; duplicates differing in case or whitespace; empty list, last page, huge page; first/last day of month, closed day, future day |
| Business errors | every `ErrorCode` the code can throw (409 duplicate, 409 `DAY_CLOSED`, 404 ...)                                                                              |
| Auth, security  | not logged in → 401 (at least one test per controller); sensitive fields not settable via body; errors leak no internals; sensitive data never logged        |
| No side effects | on error: nothing saved, no audit (`verify(..., never())` or a DB check)                                                                                     |

Generic malformed input (broken JSON, wrong Content-Type → 415, huge page, very long string) is tested once in
`common`. Business-specific cases (decimal money, invisible names, `null` in lists) are tested per endpoint.

- One test, one behavior; the name states it: backend `camelCase` (`createWithActiveDuplicateNameIsRejected`), frontend
  `it('...')` in English. Arrange, act, assert separated by blank lines.
- Assert concrete values (`isEqualTo(150_000L)`, `toHaveTextContent('1.500.000 đ')`); check errors by code, not text.
- **A test must be able to fail**: break the guarded line once and see it go red.
- Independent and repeatable: time via `Clock.fixed(...)`, no dependence on order or leftover data.
