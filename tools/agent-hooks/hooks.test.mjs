// Chạy: node --test tools/agent-hooks/hooks.test.mjs
import assert from 'node:assert/strict'
import { execFileSync, spawnSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { describe, test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { check, splitSegments } from './guard.mjs'
import { normalize } from './lib.mjs'

const here = path.dirname(fileURLToPath(import.meta.url))
const root = '/repo'
const onDevelop = (_root, repoPath) => repoPath.endsWith('V1__core_schema.sql')

const agCommand = (CommandLine) =>
  normalize({
    toolCall: { name: 'run_command', args: { CommandLine, Cwd: root } },
    workspacePaths: [root],
  })
const claudeCommand = (command) =>
  normalize({ tool_name: 'Bash', tool_input: { command } }, { CLAUDE_PROJECT_DIR: root })
const agWrite = (TargetFile) =>
  normalize({
    toolCall: { name: 'replace_file_content', args: { TargetFile } },
    workspacePaths: [root],
  })
const denied = (action) => check(action, { existsOnDevelop: onDevelop }) !== null

describe('splitSegments', () => {
  test('tách theo toán tử và bỏ dấu nháy', () => {
    assert.deepEqual(splitSegments(`cd backend && git log --grep "a b" | head; echo 'x'`), [
      ['cd', 'backend'],
      ['git', 'log', '--grep', 'a b'],
      ['head'],
      ['echo', 'x'],
    ])
  })
})

describe('git của Antigravity', () => {
  for (const cmd of [
    'git status',
    'git log --oneline -20',
    'git diff origin/develop...HEAD -- backend',
    'git -C backend show HEAD~1:build.gradle.kts',
    'git blame -L 10,20 AGENTS.md',
    'git branch',
    'git branch -a --show-current',
    'git branch --contains abc123',
    'git stash list',
    'git remote -v',
    'cd frontend && git log -p src/app | head -50',
  ]) {
    test(`cho phép: ${cmd}`, () => assert.equal(denied(agCommand(cmd)), false))
  }
  for (const cmd of [
    'git push',
    'git commit -m "x"',
    'git checkout -- src/App.tsx',
    'git switch develop',
    'git restore .',
    'git reset --hard',
    'git stash',
    'git stash pop',
    'git clean -fd',
    'git add .',
    'git rebase develop',
    'git branch feature/x',
    'git branch -D feature/x',
    'git tag v1.0.0',
    'git fetch origin',
    'git -C backend commit -am x',
    'npm test && git push origin HEAD',
    'bash -c "git push --force"',
    'git diff --output=patch.txt',
    'gh pr merge 31',
    'gh pr create --fill',
  ]) {
    test(`chặn: ${cmd}`, () => assert.equal(denied(agCommand(cmd)), true))
  }
})

describe('git và gh của Claude (review)', () => {
  for (const cmd of [
    'git fetch origin pull/31/head:pr-31',
    'git worktree add ../xile-review-31 pr-31',
    'git worktree remove ../xile-review-31',
    'git branch -D pr-31',
    'gh pr view 31 --json title,body',
    'gh pr diff 31',
    'gh pr review 31 --request-changes --body-file review.md',
    'gh api repos/o/r/pulls/31/reviews --method POST --input review.json',
  ]) {
    test(`cho phép: ${cmd}`, () => assert.equal(denied(claudeCommand(cmd)), false))
  }
  for (const cmd of [
    'git push',
    'git checkout pr-31',
    'git branch -D develop',
    'gh pr merge 31 --squash',
    'gh pr review 31 --approve',
    'gh api -X PUT repos/o/r/pulls/31/merge',
    'gh api repos/o/r/pulls/31/reviews -f event=APPROVE',
  ]) {
    test(`chặn: ${cmd}`, () => assert.equal(denied(claudeCommand(cmd)), true))
  }
})

describe('lệnh nguy hiểm khác', () => {
  for (const cmd of [
    'cat .env',
    'cat frontend/.env.local',
    'grep PASSWORD .env',
    'docker compose --env-file=.env up',
    'rm -rf frontend/node_modules',
    'Remove-Item -Recurse -Force dist',
    'docker compose down -v',
    'docker volume rm xile_pg',
    "sed -i 's/a/b/' backend/src/main/resources/db/migration/V1__core_schema.sql",
    'cat secrets/server.pem',
  ]) {
    test(`chặn: ${cmd}`, () => assert.equal(denied(agCommand(cmd)), true))
  }
  for (const cmd of [
    'cat .env.example',
    'cd backend && ./gradlew spotlessApply test',
    "./gradlew test --tests 'com.xilespa.module.staff.service.StaffServiceTest'",
    'npx vitest run src/shared/lib/money.test.ts',
    'docker compose up -d',
    'cat backend/src/main/resources/db/migration/V1__core_schema.sql',
    'rm frontend/src/old.ts',
    'node -e "console.log(process.env.HOME)"',
  ]) {
    test(`cho phép: ${cmd}`, () => assert.equal(denied(agCommand(cmd)), false))
  }
})

describe('sửa file', () => {
  for (const file of [
    '/repo/backend/src/main/resources/db/migration/V1__core_schema.sql',
    '/repo/.env',
    'frontend/.env.local',
    '/repo/frontend/package-lock.json',
    '/repo/backend/gradle/wrapper/gradle-wrapper.properties',
    '/repo/docs/design/diagrams/sales.png',
    '/repo/docs/design/diagrams/sales.mmd',
    '/repo/.agents/hooks.json',
    '/repo/tools/agent-hooks/guard.mjs',
  ]) {
    test(`chặn: ${file}`, () => assert.equal(denied(agWrite(file)), true))
  }
  for (const file of [
    '/repo/backend/src/main/resources/db/migration/V3__service_group.sql',
    '/repo/.env.example',
    '/repo/backend/src/main/java/com/xilespa/module/staff/service/StaffService.java',
    '/repo/docs/design/erd.md',
  ]) {
    test(`cho phép: ${file}`, () => assert.equal(denied(agWrite(file)), false))
  }
  test('Claude không đọc .env', () => {
    const action = normalize(
      { tool_name: 'Read', tool_input: { file_path: '/repo/.env' } },
      { CLAUDE_PROJECT_DIR: root },
    )
    assert.equal(denied(action), true)
  })
})

describe('chạy như hook thật', () => {
  const run = (script, input, cwd = here) =>
    spawnSync(process.execPath, [path.join(here, script)], {
      input: JSON.stringify(input),
      cwd,
      encoding: 'utf8',
    })

  test('Antigravity nhận decision deny kèm lý do', () => {
    const out = JSON.parse(
      run('guard.mjs', {
        toolCall: { name: 'run_command', args: { CommandLine: 'git push' } },
        workspacePaths: [root],
      }).stdout,
    )
    assert.equal(out.decision, 'deny')
    assert.match(out.reason, /chỉ đọc/)
  })
  test('Antigravity nhận allow cho lệnh hợp lệ', () => {
    const out = JSON.parse(
      run('guard.mjs', {
        toolCall: { name: 'run_command', args: { CommandLine: 'git status' } },
        workspacePaths: [root],
      }).stdout,
    )
    assert.deepEqual(out, { decision: 'allow' })
  })
  test('Claude Code nhận permissionDecision deny', () => {
    const out = JSON.parse(
      run('guard.mjs', { tool_name: 'Bash', tool_input: { command: 'git push' }, cwd: root })
        .stdout,
    )
    assert.equal(out.hookSpecificOutput.permissionDecision, 'deny')
  })
  test('Claude Code: lệnh hợp lệ không in gì', () => {
    assert.equal(
      run('guard.mjs', { tool_name: 'Bash', tool_input: { command: 'git status' }, cwd: root })
        .stdout,
      '',
    )
  })
  test('input hỏng thì cho qua', () => {
    const result = spawnSync(process.execPath, [path.join(here, 'guard.mjs')], {
      input: '{oops',
      encoding: 'utf8',
    })
    assert.equal(result.stdout, '')
  })

  test('Stop: nhắc một lần khi có code đổi, lần sau cho dừng', () => {
    const repo = fs.mkdtempSync(path.join(os.tmpdir(), 'xile-stop-'))
    const git = (...args) => execFileSync('git', args, { cwd: repo })
    git('init', '-q')
    fs.mkdirSync(path.join(repo, 'frontend/src'), { recursive: true })
    fs.writeFileSync(path.join(repo, 'README.md'), 'x\n')
    const input = {
      fullyIdle: true,
      terminationReason: 'model_stop',
      conversationId: `test-${process.pid}-${Date.now()}`,
      workspacePaths: [repo],
    }
    assert.equal(
      JSON.parse(run('stop.mjs', input, repo).stdout).decision,
      'stop',
      'chỉ đổi tài liệu thì không nhắc',
    )
    fs.writeFileSync(path.join(repo, 'frontend/src/a.ts'), 'export const a = 1\n')
    const first = JSON.parse(run('stop.mjs', input, repo).stdout)
    assert.equal(first.decision, 'continue')
    assert.match(first.reason, /Postman/)
    assert.equal(JSON.parse(run('stop.mjs', input, repo).stdout).decision, 'stop')
    fs.writeFileSync(path.join(repo, 'frontend/src/a.ts'), 'export const a = 2\n')
    assert.equal(
      JSON.parse(run('stop.mjs', input, repo).stdout).decision,
      'continue',
      'code đổi tiếp thì nhắc lại',
    )
    fs.rmSync(repo, { recursive: true, force: true })
  })
})
