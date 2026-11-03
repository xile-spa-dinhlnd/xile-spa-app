// Chặn màu viết thẳng và `!important` trong *.module.css (chạy sau oxlint trong `npm run lint`). Quy tắc có sẵn ở
// frontend/AGENTS.md mục CSS nhưng AI vẫn viết `#ff4d4f`, `!important` (S2-08 phần 5); ở đây coi đó là lỗi. Màu lấy
// từ biến trong src/styles/tokens.css hoặc token của Ant Design (`Typography.Text type="danger"`...).
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const SOURCE_DIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', 'src')
const MODULE_CSS = /\.module\.css$/

// Khai báo `thuộc-tính: giá trị` kết thúc bằng `;` hoặc `}`; nhờ vậy bỏ qua `a:hover {` và `@media (min-width: ...) {`.
const DECLARATION = /([-\w]+)\s*:\s*([^;{}]+)(?=[;}])/g
const HEX_COLOR = /#[0-9a-fA-F]{3,8}\b/
const COLOR_FUNCTION = /\b(rgba?|hsla?|hwb|lab|lch|oklab|oklch)\(/i
const NAMED_COLOR =
  /(^|[\s,(])(white|black|red|green|blue|gray|grey|yellow|orange|purple|pink|brown|silver|gold|navy|teal)(?=$|[\s,)])/i
const COLOR_PROPERTY =
  /^(color|background(-color)?|border(-[a-z]+)*|outline(-color)?|fill|stroke|box-shadow|text-shadow|text-decoration(-color)?|caret-color|accent-color)$/i

/** Thay comment bằng khoảng trắng cùng số dòng, để số dòng báo lỗi vẫn đúng. */
function stripComments(css) {
  return css.replace(/\/\*[\s\S]*?\*\//g, (comment) => comment.replace(/[^\n]/g, ' '))
}

function problemOf(property, value) {
  if (/!important/i.test(value)) {
    return 'không dùng !important; lồng selector dưới lớp cha (.header .title)'
  }
  if (HEX_COLOR.test(value)) {
    return 'không viết mã màu; dùng biến trong tokens.css hoặc token Ant Design'
  }
  if (COLOR_FUNCTION.test(value)) {
    return 'không viết màu bằng rgb()/hsl(); dùng biến trong tokens.css'
  }
  if (COLOR_PROPERTY.test(property) && NAMED_COLOR.test(value)) {
    return 'không viết tên màu; dùng biến trong tokens.css hoặc token Ant Design'
  }
  return null
}

/** Các khai báo vi phạm trong một file CSS: `{ line, text, problem }`, `line` tính từ 1. */
export function findViolations(css) {
  const text = stripComments(css)
  const violations = []
  for (const match of text.matchAll(DECLARATION)) {
    const [declaration, property, value] = match
    const problem = problemOf(property, value)
    if (problem) {
      const line = text.slice(0, match.index).split('\n').length
      violations.push({ line, text: declaration.replace(/\s+/g, ' ').trim(), problem })
    }
  }
  return violations
}

function moduleCssFiles(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) return moduleCssFiles(full)
    return MODULE_CSS.test(entry.name) ? [full] : []
  })
}

function main() {
  const violations = moduleCssFiles(SOURCE_DIR).flatMap((file) =>
    findViolations(fs.readFileSync(file, 'utf8')).map((violation) => ({ file, ...violation })),
  )
  if (violations.length === 0) return

  for (const { file, line, text, problem } of violations) {
    console.error(`${path.relative(process.cwd(), file)}:${line}: ${text} (${problem})`)
  }
  console.error(
    `\n${violations.length} chỗ viết màu hoặc !important trong CSS Modules. Sửa theo frontend/AGENTS.md mục CSS.`,
  )
  process.exit(1)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) main()
