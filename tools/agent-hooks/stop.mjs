#!/usr/bin/env node
// Hook Stop của Antigravity, chạy khi agent sắp kết thúc một lượt:
// 1. Có file Java đổi thì chạy `./gradlew spotlessApply` một lần.
// 2. Có code đổi mà chưa nhắc với đúng trạng thái này thì giữ agent lại một lượt để tự soát
//    (test, Postman, trạng thái card, báo cáo). Mỗi trạng thái chỉ nhắc một lần nên không lặp vô hạn.
// 3. Lời nhắc liệt kê file code chính đã đổi mà file test của nó chưa đổi, để agent chạy /write-tests.
import { execFileSync } from 'node:child_process'
import crypto from 'node:crypto'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { readStdinJson, runGit } from './lib.mjs'

export const REMINDER = [
  'Trước khi kết thúc lượt này, tự soát (AGENTS.md mục 5) và trả lời ngắn gọn từng ý:',
  '1. Đã chạy test liên quan chưa? Ghi lệnh và kết quả thật (số test qua/đỏ). Chưa chạy được thì nói lý do.',
  '2. Có thêm hoặc sửa endpoint không? Nếu có, đã cập nhật docs/postman/xile-spa.postman_collection.json chưa?',
  '3. Trạng thái card trong docs/backlog/ đã đúng chưa ("Đang làm" hoặc "Xong")?',
  '4. Báo cáo: file đã sửa, cách thử bằng Postman hoặc giao diện, gợi ý câu commit theo CONTRIBUTING.md mục 3.',
  'Nếu đang dừng để chờ người dùng duyệt kế hoạch thì chỉ cần nói vậy. Không sửa thêm code chỉ vì lời nhắc này.',
].join('\n')

const CODE =
  /^(backend\/src\/|frontend\/src\/|frontend\/e2e\/|backend\/build\.gradle\.kts|frontend\/package\.json)/

// File chứa hành vi cần test (implement bước test của từng tầng). DTO, mapper, repository, cấu hình, kiểu dữ liệu,
// api.ts, routes, index, CSS không tính: chúng được kiểm qua test của các file bên dưới.
const TESTABLE = [
  /^backend\/src\/main\/java\/.+\/(entity|service|controller|validator)\/[^/]+\.java$/,
  /^frontend\/src\/.+\/(components|pages|hooks)\/[^/]+\.tsx?$/,
  /^frontend\/src\/.+\/schemas\.ts$/,
  /^frontend\/src\/shared\/lib\/[^/]+\.ts$/,
]
const TEST_FILE = /^(backend\/src\/test\/|frontend\/(src|e2e)\/.+\.(test|spec)\.tsx?$)/

export function changedFiles(porcelain) {
  return porcelain
    .split('\n')
    .filter(Boolean)
    .map((line) => line.slice(3).split(' -> ').pop().replace(/^"|"$/g, ''))
}

// File code chính đã đổi mà không có file test nào cùng tên gốc đổi theo
// (StaffService.java ↔ StaffServiceTest.java, StaffListPage.tsx ↔ StaffListPage.filters.test.tsx).
export function filesMissingTests(files) {
  const baseName = (file) => path.posix.basename(file).replace(/\.(java|tsx?)$/, '')
  const testNames = files.filter((f) => TEST_FILE.test(f)).map((f) => path.posix.basename(f))
  return files
    .filter((f) => !TEST_FILE.test(f) && TESTABLE.some((pattern) => pattern.test(f)))
    .filter((f) => !testNames.some((name) => name.startsWith(baseName(f))))
}

export function reminderFor(missingTests) {
  if (missingTests.length === 0) return REMINDER
  return [
    REMINDER,
    '5. Các file code sau đã đổi mà file test của nó chưa đổi:',
    ...missingTests.map((file) => `   - ${file}`),
    '   Chạy /write-tests cho từng file, hoặc nói rõ vì sao không cần test mới (ví dụ chỉ đổi CSS, nhãn).',
  ].join('\n')
}

// Băm danh sách file đổi và nội dung của chúng (git diff không thấy file mới chưa add).
function stateHash(root) {
  const status = runGit(root, ['status', '--porcelain', '--untracked-files=all']) ?? ''
  const hash = crypto.createHash('sha1').update(status)
  for (const file of changedFiles(status)) {
    try {
      hash.update(fs.readFileSync(path.join(root, file)))
    } catch {
      // File đã xóa: trạng thái đã nằm trong status.
    }
  }
  return hash.digest('hex')
}

function loadState(file) {
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'))
  } catch {
    return {}
  }
}

function spotlessApply(root) {
  const backend = path.join(root, 'backend')
  const windows = process.platform === 'win32'
  try {
    execFileSync(windows ? 'gradlew.bat' : './gradlew', ['spotlessApply', '-q'], {
      cwd: backend,
      stdio: 'ignore',
      timeout: 150_000,
      shell: windows,
    })
  } catch {
    // Không có JDK hoặc code chưa biên dịch được: bỏ qua, lời nhắc chạy test sẽ lộ lỗi.
  }
}

// File đổi của cả nhánh so với develop (kể cả phần đã commit) cộng phần chưa commit: test của một bước có thể đã
// commit ở lượt trước.
function branchFiles(root, uncommitted) {
  const committed = runGit(root, ['diff', '--name-only', 'origin/develop...HEAD']) ?? ''
  return [...new Set([...committed.split('\n').filter(Boolean), ...uncommitted])]
}

async function main() {
  const input = await readStdinJson()
  if (input.fullyIdle === false || input.terminationReason === 'error') return { decision: 'stop' }
  const root = input.workspacePaths?.[0] ?? process.cwd()
  const porcelain = runGit(root, ['status', '--porcelain', '--untracked-files=all'])
  if (porcelain === null) return { decision: 'stop' }
  const files = changedFiles(porcelain)
  if (!files.some((f) => CODE.test(f))) return { decision: 'stop' }

  const stateFile = path.join(
    os.tmpdir(),
    'xile-agent-hooks',
    `${input.conversationId ?? 'default'}.json`,
  )
  const state = loadState(stateFile)
  const javaChanged = files.some((f) => f.startsWith('backend/') && f.endsWith('.java'))
  if (javaChanged && state.formattedHash !== stateHash(root)) {
    spotlessApply(root)
    state.formattedHash = stateHash(root)
  }
  const hash = stateHash(root)
  const remind = state.remindedHash !== hash
  state.remindedHash = hash
  fs.mkdirSync(path.dirname(stateFile), { recursive: true })
  fs.writeFileSync(stateFile, JSON.stringify(state))
  if (!remind) return { decision: 'stop' }
  return { decision: 'continue', reason: reminderFor(filesMissingTests(branchFiles(root, files))) }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  let output = { decision: 'stop' }
  try {
    output = await main()
  } catch (error) {
    process.stderr.write(`agent-hooks/stop: ${error.message}\n`)
  }
  process.stdout.write(JSON.stringify(output))
}
