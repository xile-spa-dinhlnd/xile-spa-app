// Run: node --test tools/context-check/check.test.mjs
import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { describe, test } from 'node:test'
import { fileURLToPath } from 'node:url'
import {
  check,
  lineCount,
  loaderProblems,
  missingPaths,
  pathsIn,
  ROOT_LINE_LIMIT,
  sectionOf,
  SKILL_LINE_LIMIT,
  SUBDIR_LINE_LIMIT,
  vietnameseProseLines,
} from './check.mjs'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')

function makeRepo(files) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'context-check-'))
  for (const [file, content] of Object.entries(files)) {
    fs.mkdirSync(path.dirname(path.join(root, file)), { recursive: true })
    fs.writeFileSync(path.join(root, file), content)
  }
  return root
}

const lines = (count) => 'x\n'.repeat(count)
const routing = (cell) =>
  `# Rules\n\n## 2. What to read\n\n| Job | Read |\n| --- | --- |\n| A | ${cell} |\n\n## 3. Other\n`
const validRoot = { 'AGENTS.md': routing('`README.md`'), 'README.md': '' }
const loaders = (dir) => ({
  [`${dir}/AGENTS.md`]: lines(10),
  [`${dir}/CLAUDE.md`]: '@AGENTS.md\n',
  [`.agents/rules/${dir}.md`]: `Read \`${dir}/AGENTS.md\`.\n`,
})

describe('sectionOf', () => {
  test('returns the section and stops at the next heading', () => {
    const section = sectionOf(routing('`README.md`'), 2)

    assert.ok(section.startsWith('## 2. What to read'))
    assert.ok(section.includes('`README.md`'))
    assert.ok(!section.includes('## 3.'))
  })

  test('returns null when the section is missing', () => {
    assert.equal(sectionOf('# Title\n\n## 3. Other\n', 2), null)
  })
})

describe('pathsIn', () => {
  test('keeps paths and file names, skips package names, API URLs and commands', () => {
    const text =
      '`docs/srs/04-business-rules.md`, `README.md`, `docs/postman/`, `tools.jackson.databind`, ' +
      '`/api/visits`, `https://x.y/z`, `npm run dev`, `@AGENTS.md`, `OQ code`'

    assert.deepEqual(pathsIn(text), [
      'docs/srs/04-business-rules.md',
      'README.md',
      'docs/postman/',
      '@AGENTS.md',
    ])
  })
})

describe('missingPaths', () => {
  test('reports missing paths and keeps existing ones', () => {
    const root = makeRepo({ 'docs/a.md': '', 'README.md': '' })

    assert.deepEqual(missingPaths(root, ['docs/a.md', 'README.md', 'docs/', 'docs/b.md']), [
      'docs/b.md',
    ])
  })

  test('a path with a placeholder checks the folder before it', () => {
    const root = makeRepo({ 'docs/design/flows/README.md': '' })

    assert.deepEqual(
      missingPaths(root, ['docs/design/flows/<module>/<feature>.md', 'docs/backlog/sprint-<n>.md']),
      ['docs/backlog/sprint-<n>.md'],
    )
  })
})

describe('lineCount', () => {
  test('counts like wc -l with or without a trailing newline', () => {
    assert.equal(lineCount('a\nb\n'), 2)
    assert.equal(lineCount('a\nb'), 2)
  })
})

describe('loaderProblems', () => {
  test('both loaders present: no problems', () => {
    assert.deepEqual(loaderProblems(makeRepo(loaders('backend')), 'backend'), [])
  })

  test('loaders that do not point to AGENTS.md are reported', () => {
    const root = makeRepo({
      ...loaders('frontend'),
      'frontend/CLAUDE.md': '# Private notes\n',
      '.agents/rules/frontend.md': 'Read `frontend/README.md`.\n',
    })

    assert.deepEqual(loaderProblems(root, 'frontend'), [
      'frontend/CLAUDE.md lacks the @AGENTS.md line, so Claude Code will not load frontend/AGENTS.md',
      '.agents/rules/frontend.md does not point to frontend/AGENTS.md, so Antigravity will not load it',
    ])
  })

  test('missing CLAUDE.md and Antigravity rule are both reported', () => {
    const root = makeRepo({ 'backend/AGENTS.md': lines(1) })

    assert.equal(loaderProblems(root, 'backend').length, 2)
  })
})

describe('vietnameseProseLines', () => {
  test('flags Vietnamese prose with its line number', () => {
    assert.deepEqual(vietnameseProseLines('English line\nĐọc file này trước\n'), [2])
  })

  test('ignores code blocks, inline code, double quotes and allowed terms', () => {
    const markdown = [
      'Set `Trạng thái: khớp code` and show "Thử lại".',
      'Ask Đình; money is whole đồng.',
      '```mermaid',
      'A[Người dùng] --> B',
      '```',
    ].join('\n')

    assert.deepEqual(vietnameseProseLines(markdown), [])
  })

  test('ignores quotes and inline code that wrap across lines', () => {
    const markdown = 'Ask "Bạn duyệt thiết kế\nnày chứ?" and set `đã\nsoát`.\n'

    assert.deepEqual(vietnameseProseLines(markdown), [])
  })

  test('ASCII-only Vietnamese words cannot be detected and pass', () => {
    assert.deepEqual(vietnameseProseLines('Move the card to Xong.\n'), [])
  })
})

describe('check', () => {
  test('valid repo: no problems, prints line counts', () => {
    const root = makeRepo({ ...validRoot, ...loaders('backend') })

    const { problems, sizes } = check(root)

    assert.deepEqual(problems, [])
    assert.deepEqual(sizes, [`AGENTS.md 9/${ROOT_LINE_LIMIT}`, `backend/AGENTS.md 10/${SUBDIR_LINE_LIMIT}`])
  })

  test('section 2 pointing to a deleted file reports that path', () => {
    const root = makeRepo({ 'AGENTS.md': routing('`docs/backlog/sprint-0-1.md`') })

    assert.deepEqual(check(root).problems, [
      'AGENTS.md section 2 points to `docs/backlog/sprint-0-1.md`, which is not in the repo',
    ])
  })

  test('root file at the limit passes, one more line fails', () => {
    const base = routing('`README.md`')
    const atLimit = base + lines(ROOT_LINE_LIMIT - lineCount(base))

    assert.deepEqual(check(makeRepo({ ...validRoot, 'AGENTS.md': atLimit })).problems, [])
    assert.match(
      check(makeRepo({ ...validRoot, 'AGENTS.md': atLimit + lines(1) })).problems[0],
      new RegExp(`^AGENTS\\.md has ${ROOT_LINE_LIMIT + 1} lines, over the limit of ${ROOT_LINE_LIMIT}`),
    )
  })

  test('subfolder file over its own limit is reported', () => {
    const root = makeRepo({
      ...validRoot,
      ...loaders('frontend'),
      'frontend/AGENTS.md': lines(SUBDIR_LINE_LIMIT + 1),
    })

    assert.match(
      check(root).problems[0],
      new RegExp(`^frontend/AGENTS\\.md has ${SUBDIR_LINE_LIMIT + 1} lines, over the limit of ${SUBDIR_LINE_LIMIT}`),
    )
  })

  test('a skill over the skill limit is reported, other skill files are not counted', () => {
    const root = makeRepo({
      ...validRoot,
      '.agents/skills/implement/SKILL.md': lines(SKILL_LINE_LIMIT + 1),
      '.agents/skills/implement/endpoint-checklist.md': lines(SKILL_LINE_LIMIT + 50),
      '.claude/skills/self-review/SKILL.md': lines(SKILL_LINE_LIMIT),
    })

    const { problems, sizes } = check(root)

    assert.deepEqual(problems, [
      `.agents/skills/implement/SKILL.md has ${SKILL_LINE_LIMIT + 1} lines, over the limit of ${SKILL_LINE_LIMIT} (AGENTS.md section 6): merge lines or drop ones a check already enforces`,
    ])
    assert.ok(!sizes.some((size) => size.includes('endpoint-checklist')))
  })

  test('Vietnamese prose in agent files is reported per file with line numbers', () => {
    const root = makeRepo({
      ...validRoot,
      'CLAUDE.md': '@AGENTS.md\n\nTrả lời bằng tiếng Việt.\n',
      '.agents/rules/builder.md': 'English only.\n',
      '.claude/skills/x/SKILL.md': 'Use it.\nĐừng dùng.\n',
      'docs/guide.md': 'Tài liệu tiếng Việt là đúng.\n',
    })

    assert.deepEqual(
      check(root).problems.map((problem) => problem.split(' (')[0]),
      ['CLAUDE.md has Vietnamese prose on line(s) 3', '.claude/skills/x/SKILL.md has Vietnamese prose on line(s) 2'],
    )
  })

  test('missing section 2 is reported', () => {
    assert.deepEqual(check(makeRepo({ 'AGENTS.md': '# Rules\n' })).problems, [
      'AGENTS.md has no "## 2." section (which doc to read for which job)',
    ])
  })

  test('ignores node_modules containing a library AGENTS.md', () => {
    const root = makeRepo({ ...validRoot, 'node_modules/AGENTS.md': lines(1) })

    assert.deepEqual(check(root).problems, [])
  })

  test('the real repo is clean', () => {
    assert.deepEqual(check(repoRoot).problems, [])
  })
})
