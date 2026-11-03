// Checks the files every agent loads into context each session (AGENTS.md sections 2, 4 "Language", 6, 8).
// Run from the repo root: node tools/context-check/check.mjs
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

// AGENTS.md section 6: change a limit in both places.
export const ROOT_LINE_LIMIT = 180
export const SUBDIR_LINE_LIMIT = 260
export const SKILL_LINE_LIMIT = 150

const FILE_EXTENSION = /\.(md|json|ya?ml|sql|ts|tsx|mjs|java|kts|toml)$/
const SKIPPED_DIRS = new Set(['.git', 'node_modules'])
const SKILL_ROOTS = ['.agents/skills', '.claude/skills']
const AGENT_DIRS = ['.agents', '.claude']

// Vietnamese-only letters (with diacritics). Plain ASCII words like "Xong" cannot be told apart and are allowed.
const VIETNAMESE = /[ăâđêôơưáàảãạấầẩẫậắằẳẵặéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ]/i
// A team member's name and the currency unit are not prose.
const ALLOWED_TERMS = /Đình|đồng/g

export function sectionOf(markdown, number) {
  const lines = markdown.split('\n')
  const start = lines.findIndex((line) => line.startsWith(`## ${number}.`))
  if (start === -1) return null
  const end = lines.findIndex((line, i) => i > start && line.startsWith('## '))
  return lines.slice(start, end === -1 ? undefined : end).join('\n')
}

// Only repo-relative paths; skip package names, API URLs, commands.
export function pathsIn(text) {
  return [...text.matchAll(/`([^`]+)`/g)]
    .map((match) => match[1])
    .filter((token) => !/\s/.test(token) && !token.startsWith('/') && !token.includes('://'))
    .filter((token) => token.includes('/') || FILE_EXTENSION.test(token))
}

// A path with a placeholder (`docs/design/flows/<module>/...`) checks the folder before it.
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

function markdownFilesUnder(root, dir) {
  const absolute = path.join(root, dir)
  if (!fs.existsSync(absolute)) return []
  return fs
    .readdirSync(absolute, { withFileTypes: true, recursive: true })
    .filter((entry) => entry.isFile() && entry.name.endsWith('.md'))
    .map((entry) => path.relative(root, path.join(entry.parentPath, entry.name)))
    .sort()
}

// AGENTS.md section 8: Claude Code loads <dir>/CLAUDE.md, Antigravity loads .agents/rules/<dir>.md.
export function loaderProblems(root, dir) {
  const problems = []
  if (!readIfExists(path.join(root, dir, 'CLAUDE.md'))?.includes('@AGENTS.md')) {
    problems.push(`${dir}/CLAUDE.md lacks the @AGENTS.md line, so Claude Code will not load ${dir}/AGENTS.md`)
  }
  if (!readIfExists(path.join(root, '.agents/rules', `${dir}.md`))?.includes(`${dir}/AGENTS.md`)) {
    problems.push(`.agents/rules/${dir}.md does not point to ${dir}/AGENTS.md, so Antigravity will not load it`)
  }
  return problems
}

// Prose only: code blocks, inline code, double-quoted strings (doc headings, UI copy) and names are not prose.
// Spans may wrap across lines, so blank them on the whole text and keep line breaks for line numbers.
const blank = (span) => span.replace(/[^\n]/g, ' ')

export function vietnameseProseLines(markdown) {
  const prose = markdown
    .replace(/^(`{3,}|~{3,})[\s\S]*?^\1/gm, blank)
    .replace(/`[^`]*`/g, blank)
    .replace(/"[^"]*"/g, blank)
    .replace(ALLOWED_TERMS, blank)
  return prose
    .split('\n')
    .map((line, index) => ({ line, number: index + 1 }))
    .filter(({ line }) => VIETNAMESE.test(line))
    .map(({ number }) => number)
}

export function languageFiles(root) {
  const fixed = ['AGENTS.md', 'CLAUDE.md', ...subdirsWithAgents(root).map((dir) => `${dir}/AGENTS.md`)]
  return [
    ...fixed.filter((file) => fs.existsSync(path.join(root, file))),
    ...AGENT_DIRS.flatMap((dir) => markdownFilesUnder(root, dir)),
  ]
}

export function skillFiles(root) {
  return SKILL_ROOTS.flatMap((dir) => markdownFilesUnder(root, dir)).filter((file) =>
    file.endsWith('/SKILL.md'),
  )
}

export function check(root) {
  const problems = []
  const sizes = []
  const subdirs = subdirsWithAgents(root)

  const routing = sectionOf(fs.readFileSync(path.join(root, 'AGENTS.md'), 'utf8'), 2)
  if (routing === null) {
    problems.push('AGENTS.md has no "## 2." section (which doc to read for which job)')
  } else {
    for (const missing of missingPaths(root, pathsIn(routing))) {
      problems.push(`AGENTS.md section 2 points to \`${missing}\`, which is not in the repo`)
    }
  }

  const limits = [
    ['AGENTS.md', ROOT_LINE_LIMIT],
    ...subdirs.map((dir) => [`${dir}/AGENTS.md`, SUBDIR_LINE_LIMIT]),
    ...skillFiles(root).map((file) => [file, SKILL_LINE_LIMIT]),
  ]
  for (const [file, limit] of limits) {
    const lines = lineCount(fs.readFileSync(path.join(root, file), 'utf8'))
    sizes.push(`${file} ${lines}/${limit}`)
    if (lines > limit) {
      problems.push(
        `${file} has ${lines} lines, over the limit of ${limit} (AGENTS.md section 6): merge lines or drop ones a check already enforces`,
      )
    }
  }

  for (const dir of subdirs) problems.push(...loaderProblems(root, dir))

  for (const file of languageFiles(root)) {
    const lines = vietnameseProseLines(fs.readFileSync(path.join(root, file), 'utf8'))
    if (lines.length > 0) {
      problems.push(
        `${file} has Vietnamese prose on line(s) ${lines.join(', ')} (AGENTS.md section 4, "Language"): write English; quote Vietnamese doc headings or UI copy with backticks or double quotes`,
      )
    }
  }
  return { problems, sizes }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
  const { problems, sizes } = check(root)
  console.log(`Lines: ${sizes.join(', ')}`)
  if (problems.length > 0) {
    console.error(problems.map((problem) => `- ${problem}`).join('\n'))
    process.exit(1)
  }
  console.log('Agent context: no problems.')
}
