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
import { filesMissingTests } from './stop.mjs'

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

describe('sửa file trên develop hoặc main', () => {
  const checkOn =
    (branch, ignored = []) =>
    (action) =>
      check(action, {
        existsOnDevelop: onDevelop,
        currentBranch: () => branch,
        isIgnoredByGit: (_root, repoPath) => ignored.includes(repoPath),
      })
  const claudeWrite = (file_path) =>
    normalize({ tool_name: 'Edit', tool_input: { file_path } }, { CLAUDE_PROJECT_DIR: root })

  for (const branch of ['develop', 'main']) {
    test(`chặn Antigravity sửa file của repo trên ${branch}`, () => {
      const reason = checkOn(branch)(agWrite('/repo/frontend/src/App.tsx'))

      assert.match(reason, new RegExp(`Đang ở nhánh ${branch}`))
      assert.match(reason, /git switch -c feature\//)
    })
  }
  test('chặn Claude Code sửa file của repo trên develop', () => {
    assert.match(checkOn('develop')(claudeWrite('/repo/docs/roadmap.md')), /Đang ở nhánh develop/)
  })
  for (const branch of [
    'feature/S2-10-css-modules',
    'chore/agents-branch-guard',
    'release/v0.3.0',
  ]) {
    test(`cho phép trên nhánh ${branch}`, () => {
      assert.equal(checkOn(branch)(agWrite('/repo/frontend/src/App.tsx')), null)
    })
  }
  test('cho phép khi không đọc được nhánh (detached HEAD, không phải repo git)', () => {
    assert.equal(checkOn(null)(agWrite('/repo/frontend/src/App.tsx')), null)
  })
  test('cho phép file nằm ngoài repo', () => {
    assert.equal(checkOn('develop')(claudeWrite('/tmp/scratchpad/notes.md')), null)
  })
  test('cho phép file git bỏ qua', () => {
    const file = '.claude/skills/self-review/SKILL.md'

    assert.equal(checkOn('develop', [file])(claudeWrite(`/repo/${file}`)), null)
  })
  test('quy tắc khác vẫn báo lý do riêng của nó', () => {
    assert.match(checkOn('develop')(agWrite('/repo/.env')), /bí mật/)
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

  test('sửa file trên develop bị chặn, file git bỏ qua và nhánh card thì cho qua', () => {
    const repo = fs.mkdtempSync(path.join(os.tmpdir(), 'xile-branch-'))
    const git = (...args) => execFileSync('git', args, { cwd: repo })
    git('init', '-q', '-b', 'develop')
    fs.writeFileSync(path.join(repo, '.gitignore'), 'local/\n')
    const write = (file) =>
      JSON.parse(
        run(
          'guard.mjs',
          {
            toolCall: { name: 'write_to_file', args: { TargetFile: path.join(repo, file) } },
            workspacePaths: [repo],
          },
          repo,
        ).stdout,
      )

    const onDevelopBranch = write('frontend/src/a.ts')
    const ignoredFile = write('local/notes.md')
    git('switch', '-q', '-c', 'feature/S9-01-thu')
    const onCardBranch = write('frontend/src/a.ts')

    assert.equal(onDevelopBranch.decision, 'deny')
    assert.match(onDevelopBranch.reason, /Đang ở nhánh develop/)
    assert.equal(ignoredFile.decision, 'allow')
    assert.equal(onCardBranch.decision, 'allow')
    fs.rmSync(repo, { recursive: true, force: true })
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

  test('Stop: nhắc /write-tests cho file code đổi mà chưa có test, có test rồi thì thôi', () => {
    const repo = fs.mkdtempSync(path.join(os.tmpdir(), 'xile-stop-tests-'))
    execFileSync('git', ['init', '-q'], { cwd: repo })
    const pageDir = path.join(repo, 'frontend/src/features/combo/pages')
    fs.mkdirSync(pageDir, { recursive: true })
    const input = {
      fullyIdle: true,
      terminationReason: 'model_stop',
      conversationId: `test-tests-${process.pid}-${Date.now()}`,
      workspacePaths: [repo],
    }

    fs.writeFileSync(path.join(pageDir, 'ComboListPage.tsx'), 'export const x = 1\n')
    const withoutTest = JSON.parse(run('stop.mjs', input, repo).stdout)
    fs.writeFileSync(path.join(pageDir, 'ComboListPage.test.tsx'), 'it("x", () => {})\n')
    const withTest = JSON.parse(run('stop.mjs', input, repo).stdout)

    assert.equal(withoutTest.decision, 'continue')
    assert.match(withoutTest.reason, /features\/combo\/pages\/ComboListPage\.tsx/)
    assert.match(withoutTest.reason, /\/write-tests/)
    assert.equal(withTest.decision, 'continue', 'trạng thái đổi nên nhắc lại một lần')
    assert.doesNotMatch(withTest.reason, /\/write-tests/)
    fs.rmSync(repo, { recursive: true, force: true })
  })
})

describe('Stop: file code đổi mà chưa có test', () => {
  test('liệt kê file code chính không có test đổi theo', () => {
    const files = [
      'backend/src/main/java/com/xilespa/module/combo/service/ComboService.java',
      'backend/src/main/java/com/xilespa/module/combo/controller/ComboController.java',
      'frontend/src/features/combo/schemas.ts',
      'frontend/src/features/combo/components/ComboFormDrawer.tsx',
    ]

    assert.deepEqual(filesMissingTests(files), files)
  })
  test('có test cùng tên gốc thì coi là đủ, kể cả test tách file', () => {
    const files = [
      'backend/src/main/java/com/xilespa/module/combo/service/ComboService.java',
      'backend/src/test/java/com/xilespa/module/combo/service/ComboServiceTest.java',
      'backend/src/main/java/com/xilespa/module/combo/controller/ComboController.java',
      'backend/src/test/java/com/xilespa/module/combo/controller/ComboControllerIT.java',
      'frontend/src/features/combo/pages/ComboListPage.tsx',
      'frontend/src/features/combo/pages/ComboListPage.filters.test.tsx',
      'frontend/src/features/combo/schemas.ts',
      'frontend/src/features/combo/schemas.test.ts',
    ]

    assert.deepEqual(filesMissingTests(files), [])
  })
  test('không đòi test cho DTO, mapper, repository, CSS, api.ts, types.ts', () => {
    const files = [
      'backend/src/main/java/com/xilespa/module/combo/dto/request/ComboRequest.java',
      'backend/src/main/java/com/xilespa/module/combo/mapper/ComboMapper.java',
      'backend/src/main/java/com/xilespa/module/combo/repository/ComboRepository.java',
      'frontend/src/features/combo/pages/ComboListPage.module.css',
      'frontend/src/features/combo/api.ts',
      'frontend/src/features/combo/types.ts',
    ]

    assert.deepEqual(filesMissingTests(files), [])
  })
})

describe('.agents/hooks.json', () => {
  // Antigravity chạy lệnh hook với thư mục làm việc là .agents/ (đã kiểm tra trên máy thật 22/10/2026).
  test('mọi lệnh node trỏ tới file có thật khi chạy từ .agents/', () => {
    const agentsDir = path.join(here, '..', '..', '.agents')
    const config = JSON.parse(fs.readFileSync(path.join(agentsDir, 'hooks.json'), 'utf8'))
    const commands = JSON.stringify(config).match(/node [^"]+?\.mjs/g) ?? []
    assert.equal(commands.length, 3)
    for (const command of commands) {
      const script = command.replace(/^node /, '')
      assert.ok(
        fs.existsSync(path.resolve(agentsDir, script)),
        `${script} không tồn tại khi tính từ .agents/`,
      )
    }
  })
})
