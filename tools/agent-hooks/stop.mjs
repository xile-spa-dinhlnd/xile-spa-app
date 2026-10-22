#!/usr/bin/env node
// Hook Stop của Antigravity, chạy khi agent sắp kết thúc một lượt:
// 1. Có file Java đổi thì chạy `./gradlew spotlessApply` một lần.
// 2. Có code đổi mà chưa nhắc với đúng trạng thái này thì giữ agent lại một lượt để tự soát
//    (test, Postman, trạng thái card, báo cáo). Mỗi trạng thái chỉ nhắc một lần nên không lặp vô hạn.
import { execFileSync } from 'node:child_process'
import crypto from 'node:crypto'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
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

export function changedFiles(porcelain) {
  return porcelain
    .split('\n')
    .filter(Boolean)
    .map((line) => line.slice(3).split(' -> ').pop().replace(/^"|"$/g, ''))
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

async function main() {
  const input = await readStdinJson()
  if (input.fullyIdle === false || input.terminationReason === 'error') return 'stop'
  const root = input.workspacePaths?.[0] ?? process.cwd()
  const porcelain = runGit(root, ['status', '--porcelain', '--untracked-files=all'])
  if (porcelain === null) return 'stop'
  const files = changedFiles(porcelain)
  if (!files.some((f) => CODE.test(f))) return 'stop'

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
  return remind ? 'continue' : 'stop'
}

let decision = 'stop'
try {
  decision = await main()
} catch (error) {
  process.stderr.write(`agent-hooks/stop: ${error.message}\n`)
}
process.stdout.write(
  JSON.stringify(decision === 'continue' ? { decision, reason: REMINDER } : { decision }),
)
