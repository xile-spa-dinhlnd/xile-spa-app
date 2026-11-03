---
name: write-tests
description: Write Xile Spa tests for backend (JUnit, Mockito, Testcontainers) or frontend (Vitest, Testing Library) - list the cases across the six groups of the test bar, write them, prove each can fail by breaking the guarded line, run them and report real numbers. Use when tests are needed, when the user types /write-tests, or in the test step of /implement.
---

# /write-tests — complete cases, able to fail, really run

Bar to meet: `AGENTS.md` section 10. Layer specifics: `backend/AGENTS.md` ("Test"), `frontend/AGENTS.md` ("Tests and
commands"). Models in the repo: `StaffServiceTest`, `StaffControllerIT`, `money.test.ts`, `StaffListPage.test.tsx`.

## 1. What to test

Read the code under test and the flow file's "Hành vi và test" table. List **every branch**: each `if`, each
`throw new BusinessException(ErrorCode.X)`, each validation annotation, each screen state.

Called from `/implement`: the test list is the table (or the fix request), with the exact test names written there,
only for the current part. Extra edge cases you find are allowed if they belong to the current part; report them under
"Differs from design". Never rename or drop a test listed in the table.

## 2. Case table first

| #   | Group          | Layer   | Input / situation          | Expected                                  |
| --- | -------------- | ------- | -------------------------- | ----------------------------------------- |
| 1   | Happy path     | Service | name `"  Lan   A "`        | saves "Lan A", audit CREATE               |
| 2   | Invalid input  | IT      | empty name                 | 400, `fieldErrors[0].field = fullName`    |
| 3   | Edge case      | IT      | duplicate differing in case | 409 `STAFF_NAME_DUPLICATE`               |
| 4   | Business error | Service | ...                        | `ErrorCode...`, no `save`, no audit       |
| 5   | Auth           | IT      | not logged in              | 401                                       |

Cover all six groups; a group that does not apply says "not applicable: <reason>". Think in Xile Spa terms: money 0 and
huge, closed day, future day, first/last day of month, whitespace-only text, empty list, last page. Pick the cheapest
layer that proves it: pure logic in unit tests; DB, triggers, transactions, security, JSON in integration tests.

## 3. Write

- One test, one behavior; the name says it (backend `camelCase`, frontend `it('...')` in English).
- Arrange / act / assert separated by blank lines. Assert concrete values; check errors by code.
- Time via `Clock.fixed(...)`; IT data with a random prefix; never `deleteAll()` shared tables.
- Frontend: query by role and visible text; fake the API with `vi.fn()` on `fetch`. Data screens test at least the load
  error (with "Thử lại") and the empty state.

Mistakes reviewers keep returning (all happened):

- **Text that appears in several places**: the asserted text also shows in a drawer, select or form, so the test passes
  even when the target did not change. Narrow with `within(...)`.
- **Test name ≠ what it checks** ("calls deactivate" but only clicks Cancel). Rename, then add the missing test.
- **Mocks unlike the backend**: data the business forbids, `fieldErrors` the backend never sends, unused mock branches.
- **Asserting a mock instead of the screen**: spying on `App.useApp` or `message.error` instead of finding the text.
- **`any`, `as`, `!`, CSS-class selectors** (`.ant-popover`): Ant Design popovers have `role="tooltip"`.

## 4. Prove each test can fail

For each new behavior (at least the happy path, each business error, each data refresh after a write): temporarily break
exactly the line the test guards (flip a condition, drop a check, remove `invalidateQueries`), run that test alone, see
it red, **restore the code**, run it green. A test that cannot go red gets a better assert.

| Code broken (file:line, change) | Test that went red | Error (one line) |
| ------------------------------- | ------------------ | ---------------- |

"Tried breaking it" without this table counts as not tried.

## 5. Run and report

```bash
cd backend && ./gradlew test --tests '<test class>'      # then ./gradlew spotlessApply test
cd frontend && npx vitest run <test file>                # then npm test
```

Report: the case table (marked written), real pass/fail counts, the mutation table, cases not written and why. Never
write "passed" when it did not run (for example Docker is off); say why.

## Never

- `@Disabled`, `it.skip`, deleting a red test, loosening an assert (`AGENTS.md` rule 5).
- A test that only checks a mock was called without checking the result.
- Calling the real backend or external services from frontend tests.
