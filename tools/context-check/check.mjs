// Kiểm tra các file mà agent nạp vào context mỗi phiên (AGENTS.md mục 2, 6, 8).
// Chạy ở gốc repo: node tools/context-check/check.mjs
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

// AGENTS.md mục 6: đổi giới hạn thì sửa cả hai chỗ.
export const ROOT_LINE_LIMIT = 250
export const SUBDIR_LINE_LIMIT = 320

const FILE_EXTENSION = /\.(md|json|ya?ml|sql|ts|tsx|mjs|java|kts|toml)$/
const SKIPPED_DIRS = new Set(['.git', 'node_modules'])

export function sectionOf(markdown, number) {
  const lines = markdown.split('\n')
  const start = lines.findIndex((line) => line.startsWith(`## ${number}.`))
  if (start === -1) return null
  const end = lines.findIndex((line, i) => i > start && line.startsWith('## '))
  return lines.slice(start, end === -1 ? undefined : end).join('\n')
}

// Chỉ lấy đường dẫn tương đối trong repo, bỏ qua tên package, URL của API, câu lệnh.
export function pathsIn(text) {
  return [...text.matchAll(/`([^`]+)`/g)]
    .map((match) => match[1])
    .filter((token) => !/\s/.test(token) && !token.startsWith('/') && !token.includes('://'))
    .filter((token) => token.includes('/') || FILE_EXTENSION.test(token))
}

// Đường dẫn có chỗ giữ chỗ (`docs/design/flows/<module>/...`) thì kiểm thư mục đứng trước nó.
export function missingPaths(root, paths) {
  return paths.filter((relativePath) => {
    const placeholder = relativePath.indexOf('<')
    const concrete =
      placeholder === -1
        ? relativePath
        : relativePath.slice(0, relativePath.lastIndexOf('/', placeholder) + 1)
    return !fs.existsSync(path.join(root, concrete))
  })
}

export function lineCount(text) {
  return text.split('\n').length - (text.endsWith('\n') ? 1 : 0)
}

export function subdirsWithAgents(root) {
  return fs
    .readdirSync(root, { withFileTypes: true })
    .filter((entry) => entry.isDirectory() && !SKIPPED_DIRS.has(entry.name))
    .map((entry) => entry.name)
    .filter((dir) => fs.existsSync(path.join(root, dir, 'AGENTS.md')))
    .sort()
}

function readIfExists(file) {
  return fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : null
}

// AGENTS.md mục 8: Claude Code nạp qua <thư mục>/CLAUDE.md, Antigravity qua .agents/rules/<thư mục>.md.
export function loaderProblems(root, dir) {
  const problems = []
  if (!readIfExists(path.join(root, dir, 'CLAUDE.md'))?.includes('@AGENTS.md')) {
    problems.push(
      `${dir}/CLAUDE.md không có dòng @AGENTS.md, Claude Code sẽ không nạp ${dir}/AGENTS.md`,
    )
  }
  if (!readIfExists(path.join(root, '.agents/rules', `${dir}.md`))?.includes(`${dir}/AGENTS.md`)) {
    problems.push(
      `.agents/rules/${dir}.md không trỏ tới ${dir}/AGENTS.md, Antigravity sẽ không nạp file đó`,
    )
  }
  return problems
}

export function check(root) {
  const problems = []
  const sizes = []
  const subdirs = subdirsWithAgents(root)

  const routing = sectionOf(fs.readFileSync(path.join(root, 'AGENTS.md'), 'utf8'), 2)
  if (routing === null) {
    problems.push('AGENTS.md không có mục "## 2." (bảng đọc tài liệu nào cho loại việc nào)')
  } else {
    for (const missing of missingPaths(root, pathsIn(routing))) {
      problems.push(`AGENTS.md mục 2 trỏ tới \`${missing}\`, không có trong repo`)
    }
  }

  const limits = [
    ['AGENTS.md', ROOT_LINE_LIMIT],
    ...subdirs.map((dir) => [`${dir}/AGENTS.md`, SUBDIR_LINE_LIMIT]),
  ]
  for (const [file, limit] of limits) {
    const lines = lineCount(fs.readFileSync(path.join(root, file), 'utf8'))
    sizes.push(`${file} ${lines}/${limit}`)
    if (lines > limit) {
      problems.push(
        `${file} có ${lines} dòng, quá giới hạn ${limit} (AGENTS.md mục 6): gộp dòng cùng ý hoặc bỏ dòng mà lint, test, hook đã chặn`,
      )
    }
  }

  for (const dir of subdirs) problems.push(...loaderProblems(root, dir))
  return { problems, sizes }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
  const { problems, sizes } = check(root)
  console.log(`Số dòng: ${sizes.join(', ')}`)
  if (problems.length > 0) {
    console.error(problems.map((problem) => `- ${problem}`).join('\n'))
    process.exit(1)
  }
  console.log('Context của agent: không có lỗi.')
}
