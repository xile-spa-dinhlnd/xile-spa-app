// Chặn comment tắt lint trong src/ (chạy sau oxlint trong `npm run lint`). oxlint không có quy tắc cấm
// `eslint-disable`, `oxlint-disable`, nên AI có thể tắt lint thay vì sửa code; ở đây coi đó là lỗi.
// `@ts-ignore`, `@ts-nocheck` đã do quy tắc typescript/ban-ts-comment trong .oxlintrc.json chặn.
import fs from 'node:fs'
import path from 'node:path'

const SOURCE_DIR = path.resolve(import.meta.dirname, '..', 'src')
const SOURCE_FILE = /\.(ts|tsx|js|jsx)$/
const SUPPRESSION = /\b(eslint|oxlint)-disable/

function sourceFiles(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) return sourceFiles(full)
    return SOURCE_FILE.test(entry.name) ? [full] : []
  })
}

const violations = sourceFiles(SOURCE_DIR).flatMap((file) =>
  fs
    .readFileSync(file, 'utf8')
    .split('\n')
    .map((line, index) => ({ file, line: index + 1, text: line.trim() }))
    .filter(({ text }) => SUPPRESSION.test(text)),
)

if (violations.length > 0) {
  for (const { file, line, text } of violations) {
    console.error(`${path.relative(process.cwd(), file)}:${line}: ${text}`)
  }
  console.error(
    `\n${violations.length} chỗ tắt lint bằng comment. Sửa code cho đúng quy tắc thay vì tắt (frontend/AGENTS.md mục React và TypeScript).`,
  )
  process.exit(1)
}
