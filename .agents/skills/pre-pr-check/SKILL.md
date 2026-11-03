---
name: pre-pr-check
description: Only when the user types /pre-pr-check; never after /implement, /fix-bug or when code looks done. Runs every check before a Xile Spa pull request (format, lint, typecheck, tests, context check), walks the PR checklist, drafts the English PR title and description, and is the only place that gives git commands (commit, push, open PR, squash merge, cleanup) for the user to run.
---

# /pre-pr-check — ready for a PR, and the git guide

- Runs **only when the user types it**. Never assume the work is correct and finished.
- The **only** skill that gives git commands (`AGENTS.md` section 8). It never commits, pushes or opens the PR itself;
  it prepares them for the user. Exception: creating the branch in step 1.

## 1. Branch and scope

```bash
git branch --show-current
git status --short
git log --format='%h %s' origin/develop..HEAD
git diff --stat origin/develop...HEAD
```

- On `develop`/`main`: create the branch per `.agents/rules/builder.md` ("Branch"). On another card's branch: stop and ask.
- Group changed files (backend, frontend, DB, docs); call out files outside the card.
- **Size** (main code only):
  `git diff --numstat $(git merge-base origin/develop HEAD) -- backend/src/main frontend/src ':(exclude)frontend/src/**/*.test.ts' ':(exclude)frontend/src/**/*.test.tsx'`
  plus untracked files. Over ~400 lines, more than one card, or work outside the card → put it at the top of the report
  with a proposal; the user decides.
- **Tests travel with code:** every changed main file (`entity`, `service`, `controller`, validator; `schemas.ts`,
  component, page, hook, `shared/lib`) has its test file changed or added. Missing → not ready; propose `/write-tests`
  for that file. Pure styling or copy changes: write "no new test needed" with the reason.

## 2. Checks (only the parts that changed)

```bash
cd backend && ./gradlew spotlessApply test
cd frontend && npm run format && npm run lint && npm run typecheck && npm test
node tools/context-check/check.mjs                 # always, from the repo root
node --test "tools/**/*.test.mjs"                  # when tools/ changed
```

Report real numbers (passed, failed, skipped). Backend tests need Docker; if it cannot run, say so. A red test → stop,
analyze it the `/fix-bug` way, do not report ready.

## 3. Checklist

Walk the "Checklist" of `.github/pull_request_template.md`, plus:

- [ ] Flow file has `Trạng thái: khớp code`, this card's history line, and real test names in "Hành vi và test" (open
      each test). Do not tick just because tests are green.
- [ ] Every line of the flow file's "Ảnh hưởng" is done; changed URLs and method names grepped again.
- [ ] Card is `Xong` in `docs/backlog/` (or still `Đang làm` for a draft PR); endpoints are in Postman.
- [ ] No `console.log`, `System.out.println`, commented-out code, TODO without a card code; no secrets, no `.env`, no
      old migration or generated file touched; no `style={{}}` left in edited frontend files.
- [ ] Every commit message on the branch follows `CONTRIBUTING.md` section 3 (English). Wrong ones are not rewritten;
      fix them in the squash message (step 5).
- [ ] `.handoff/<card>.md` has no open fix request (Đình's cards: last section is a clean `/self-review`).

Unticked items are listed with a proposed fix; after fixing, rerun step 2.

## 4. Draft the PR (English)

- **Title:** `<type>(<scope>): <short description> [<card>]`, type `feat` for card features, `fix` only for bugs already
  on `develop`. Example: `feat(catalog): manage service groups [S2-06]`.
- **Body:** every section of `.github/pull_request_template.md`; "Testing" with real numbers; "Notes for reviewers"
  with the flow file path, assumptions, and the `/self-review` line if the user provides it.
- **Squash message:** the PR title; body only `Refs: <card>, <FR/BR/OQ>`.

Return it in one markdown block for the user to copy.

## 5. Git commands for the user

Print the exact commands for the current state, with branch, files and message filled in:

1. **Commit** (skip if nothing is uncommitted): `git status`, `git add <card files>` (listed; never `git add .` when
   other files exist), `git commit -m "<type>(<scope>): <english description>" -m "Refs: <card>, <FR/BR/OQ>"`.
2. **Push:** `git push -u origin <branch>` the first time, then `git push`.
3. **Open the PR** into `develop` with the title and body from step 4. Huy's PR: request Đình's review. Đình's PR:
   merge with bypass once `/self-review` is clean (`CONTRIBUTING.md` section 4).
4. **After review:** apply the fix request, commit as in 1, `git push`, click **Re-request review**.
5. **Merge:** **Squash and merge**; title = PR title; body = only the `Refs:` line.
6. **After merge:** `git switch develop && git pull && git branch -D <branch>` (`-D` because squash hides the merge).
