// Chạy: node --test tools/context-check/check.test.mjs
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
  SUBDIR_LINE_LIMIT,
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
  `# Quy tắc\n\n## 2. Đọc gì\n\n| Việc | Đọc |\n| --- | --- |\n| A | ${cell} |\n\n## 3. Khác\n`
const loaders = (dir) => ({
  [`${dir}/AGENTS.md`]: lines(10),
  [`${dir}/CLAUDE.md`]: '@AGENTS.md\n',
  [`.agents/rules/${dir}.md`]: `Đọc \`${dir}/AGENTS.md\`.\n`,
})

describe('sectionOf', () => {
  test('lấy đúng mục, dừng ở tiêu đề mục kế tiếp', () => {
    const section = sectionOf(routing('`README.md`'), 2)

    assert.ok(section.startsWith('## 2. Đọc gì'))
    assert.ok(section.includes('`README.md`'))
    assert.ok(!section.includes('## 3.'))
  })

  test('không có mục thì trả null', () => {
    assert.equal(sectionOf('# Tiêu đề\n\n## 3. Khác\n', 2), null)
  })
})

describe('pathsIn', () => {
  test('lấy đường dẫn và tên file, bỏ tên package, URL của API, câu lệnh', () => {
    const text =
      '`docs/srs/04-business-rules.md`, `README.md`, `docs/postman/`, `tools.jackson.databind`, ' +
      '`/api/visits`, `https://x.y/z`, `npm run dev`, `@AGENTS.md`, `mã OQ`'

    assert.deepEqual(pathsIn(text), [
      'docs/srs/04-business-rules.md',
      'README.md',
      'docs/postman/',
      '@AGENTS.md',
    ])
  })
})

describe('missingPaths', () => {
  test('báo đường dẫn không có, giữ đường dẫn có thật', () => {
    const root = makeRepo({ 'docs/a.md': '', 'README.md': '' })

    assert.deepEqual(missingPaths(root, ['docs/a.md', 'README.md', 'docs/', 'docs/b.md']), [
      'docs/b.md',
    ])
  })

  test('đường dẫn có chỗ giữ chỗ thì kiểm thư mục đứng trước', () => {
    const root = makeRepo({ 'docs/design/flows/README.md': '' })

    assert.deepEqual(
      missingPaths(root, [
        'docs/design/flows/<module>/<tính-năng>.md',
        'docs/backlog/sprint-<n>.md',
      ]),
      ['docs/backlog/sprint-<n>.md'],
    )
  })
})

describe('lineCount', () => {
  test('đếm như wc -l, có hay không dòng trống cuối file', () => {
    assert.equal(lineCount('a\nb\n'), 2)
    assert.equal(lineCount('a\nb'), 2)
  })
})

describe('loaderProblems', () => {
  test('đủ hai lớp nạp thì không báo gì', () => {
    assert.deepEqual(loaderProblems(makeRepo(loaders('backend')), 'backend'), [])
  })

  test('file nạp có nhưng không trỏ tới AGENTS.md thì báo', () => {
    const root = makeRepo({
      ...loaders('frontend'),
      'frontend/CLAUDE.md': '# Ghi chú riêng\n',
      '.agents/rules/frontend.md': 'Đọc `frontend/README.md`.\n',
    })

    assert.deepEqual(loaderProblems(root, 'frontend'), [
      'frontend/CLAUDE.md không có dòng @AGENTS.md, Claude Code sẽ không nạp frontend/AGENTS.md',
      '.agents/rules/frontend.md không trỏ tới frontend/AGENTS.md, Antigravity sẽ không nạp file đó',
    ])
  })

  test('thiếu CLAUDE.md và rule của Antigravity thì báo cả hai', () => {
    const root = makeRepo({ 'backend/AGENTS.md': lines(1) })

    assert.deepEqual(loaderProblems(root, 'backend'), [
      'backend/CLAUDE.md không có dòng @AGENTS.md, Claude Code sẽ không nạp backend/AGENTS.md',
      '.agents/rules/backend.md không trỏ tới backend/AGENTS.md, Antigravity sẽ không nạp file đó',
    ])
  })
})

describe('check', () => {
  test('repo hợp lệ: không lỗi, in số dòng của từng file', () => {
    const root = makeRepo({
      'AGENTS.md': routing('`README.md`'),
      'README.md': '',
      ...loaders('backend'),
    })

    const { problems, sizes } = check(root)

    assert.deepEqual(problems, [])
    assert.deepEqual(sizes, [
      `AGENTS.md 9/${ROOT_LINE_LIMIT}`,
      `backend/AGENTS.md 10/${SUBDIR_LINE_LIMIT}`,
    ])
  })

  test('mục 2 trỏ tới file đã xóa thì báo đúng đường dẫn', () => {
    const root = makeRepo({ 'AGENTS.md': routing('`docs/backlog/sprint-0-1.md`') })

    assert.deepEqual(check(root).problems, [
      'AGENTS.md mục 2 trỏ tới `docs/backlog/sprint-0-1.md`, không có trong repo',
    ])
  })

  test('file gốc vừa đủ giới hạn thì qua, thêm một dòng thì báo', () => {
    const base = routing('`README.md`')
    const atLimit = base + lines(ROOT_LINE_LIMIT - lineCount(base))
    const overLimit = atLimit + lines(1)

    assert.deepEqual(check(makeRepo({ 'AGENTS.md': atLimit, 'README.md': '' })).problems, [])
    assert.match(
      check(makeRepo({ 'AGENTS.md': overLimit, 'README.md': '' })).problems[0],
      /^AGENTS\.md có 251 dòng/,
    )
  })

  test('file con quá giới hạn thì báo theo giới hạn của file con', () => {
    const root = makeRepo({
      'AGENTS.md': routing('`README.md`'),
      'README.md': '',
      ...loaders('frontend'),
      'frontend/AGENTS.md': lines(SUBDIR_LINE_LIMIT + 1),
    })

    assert.match(check(root).problems[0], /^frontend\/AGENTS\.md có 321 dòng, quá giới hạn 320/)
  })

  test('không có mục 2 thì báo', () => {
    const root = makeRepo({ 'AGENTS.md': '# Quy tắc\n' })

    assert.deepEqual(check(root).problems, [
      'AGENTS.md không có mục "## 2." (bảng đọc tài liệu nào cho loại việc nào)',
    ])
  })

  test('bỏ qua node_modules có AGENTS.md của thư viện', () => {
    const root = makeRepo({
      'AGENTS.md': routing('`README.md`'),
      'README.md': '',
      'node_modules/AGENTS.md': lines(1),
    })

    assert.deepEqual(check(root).problems, [])
  })

  test('repo thật đang sạch', () => {
    assert.deepEqual(check(repoRoot).problems, [])
  })
})
