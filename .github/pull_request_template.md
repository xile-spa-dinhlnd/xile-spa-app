<!--
PR title: <type>(<scope>): <short english description> [<card>]
Example:  feat(catalog): manage services [S1-05]
Base: develop (only release/* and hotfix/* go to main). Full rules: CONTRIBUTING.md sections 3, 4.
These comment lines do not show in the rendered PR. Write "None" for sections that do not apply.
-->

## Summary

<!-- 1–3 sentences: what problem this PR solves and why. -->

## Links

- Card: <!-- one card, e.g. S2-01 (Trello link). Process work without a card: "None". -->
- Design: <!-- flow file, e.g. docs/design/flows/catalog/service-group.md -->
- Requirements: <!-- FR-SVC-01, BR-19, OQ-06, ADR-0003 ... -->

## Type of change

- [ ] `feat`: new feature
- [ ] `fix`: bug fix
- [ ] `refactor` / `perf`: structure or performance, no behavior change
- [ ] `test` / `docs` / `build` / `ci` / `chore`
- [ ] **Breaking change**: changes an API the frontend calls, the schema, configuration or environment variables

## Main changes

<!-- Bullets grouped by backend / frontend / DB / docs. -->

-

## Testing

<!-- Real results, e.g. "Backend 142/142 (incl. integration), Frontend 59/59". Say why if something could not run. -->

- Automated:
- Manual (Postman or UI, with the expected result):
  1.

## Screenshots

<!-- UI changes: desktop and phone, ideally before / after. -->

| Before | After |
| ------ | ----- |
|        |       |

## Risks and rollback

<!-- What can break, which data is affected, migrations, environment variables needed at deploy. -->

## Notes for reviewers

<!-- Assumptions made because the SRS is unclear, uncertain spots, out-of-scope changes, files worth a close look,
the /self-review line for Đình's PRs. -->

## Checklist

- [ ] I read the whole diff (including AI-written parts); one card, main code under ~400 lines (excluding tests, Postman, docs) or explained
- [ ] The flow file matches the code: `Trạng thái: khớp code`, a history line for this card, real test names in the test table
- [ ] Build and tests pass locally: `./gradlew spotlessApply test` (Docker on) and `npm run format:check && npm run lint && npm run typecheck && npm test`; CI green if present
- [ ] Business logic has tests; no `@Disabled`, no `skip`, no loosened asserts
- [ ] Hard rules (AGENTS.md section 4): whole-đồng money, no edits to closed days, `audit_log` in the same transaction, no hard deletes, time via `Clock`
- [ ] Schema changes use a new `V{n}__...` migration; old migrations untouched
- [ ] No secrets, `.env` or real customer data; no passwords or tokens logged; login does not reveal whether an email exists
- [ ] Structure and conventions follow `backend/AGENTS.md`, `frontend/AGENTS.md`; new libraries recorded in ADR-0001
- [ ] Updated in this PR: SRS / ERD / README if behavior changed, Postman if endpoints changed, card status in the backlog, AGENTS files if conventions changed
- [ ] Code, comments, test names and commit messages in English; UI text in Vietnamese
