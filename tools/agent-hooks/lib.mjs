// Phần dùng chung cho các hook của Antigravity (.agents/hooks.json) và Claude Code (.claude/settings.json).
// Chỉ dùng thư viện có sẵn của Node (>= 22), chạy được trên macOS, Linux và Windows.
import { execFileSync } from 'node:child_process'
import path from 'node:path'

export const MIGRATION_DIR = 'backend/src/main/resources/db/migration/'

const ANTIGRAVITY_WRITE_TOOLS = new Set([
  'write_to_file',
  'replace_file_content',
  'multi_replace_file_content',
])
const CLAUDE_WRITE_TOOLS = new Set(['Edit', 'Write', 'MultiEdit', 'NotebookEdit'])

export async function readStdinJson() {
  const chunks = []
  for await (const chunk of process.stdin) chunks.push(chunk)
  const text = Buffer.concat(chunks).toString('utf8').trim()
  return text ? JSON.parse(text) : {}
}

// Đưa input của hai agent về một dạng chung.
export function normalize(input, env = process.env) {
  if (input.toolCall) {
    const { name = '', args = {} } = input.toolCall
    const root = input.workspacePaths?.[0] ?? process.cwd()
    const base = { agent: 'antigravity', tool: name, root, cwd: args.Cwd ?? root }
    if (name === 'run_command') return { ...base, kind: 'command', command: args.CommandLine ?? '' }
    if (ANTIGRAVITY_WRITE_TOOLS.has(name)) return { ...base, kind: 'write', paths: pathArgs(args) }
    if (name === 'view_file') return { ...base, kind: 'read', paths: pathArgs(args) }
    return { ...base, kind: 'other' }
  }
  const name = input.tool_name ?? ''
  const args = input.tool_input ?? {}
  const root = env.CLAUDE_PROJECT_DIR ?? input.cwd ?? process.cwd()
  const base = { agent: 'claude', tool: name, root, cwd: input.cwd ?? root }
  if (name === 'Bash') return { ...base, kind: 'command', command: args.command ?? '' }
  if (CLAUDE_WRITE_TOOLS.has(name)) return { ...base, kind: 'write', paths: pathArgs(args) }
  if (name === 'Read') return { ...base, kind: 'read', paths: pathArgs(args) }
  return { ...base, kind: 'other' }
}

// Antigravity đặt tên tham số kiểu TargetFile, AbsolutePath; Claude Code dùng file_path, notebook_path.
function pathArgs(args) {
  return Object.entries(args)
    .filter(([key, value]) => typeof value === 'string' && /file|path/i.test(key))
    .map(([, value]) => value)
}

// Đường dẫn tương đối so với gốc repo, dùng dấu "/" (kể cả trên Windows).
export function toRepoPath(root, file) {
  const absolute = path.resolve(root, file)
  return path.relative(root, absolute).split(path.sep).join('/').replaceAll('\\', '/')
}

export function isSecretFile(repoPath) {
  const name = repoPath.split('/').pop().toLowerCase()
  if (name === '.env.example') return false
  if (name === '.env' || name.startsWith('.env.')) return true
  if (name.endsWith('.pem') || name.endsWith('.key')) return true
  if (
    name.endsWith('.json') &&
    (name.includes('service-account') || name.startsWith('credentials'))
  )
    return true
  return false
}

const GENERATED = [
  /^frontend\/package-lock\.json$/,
  /^backend\/gradlew(\.bat)?$/,
  /^backend\/gradle\/wrapper\//,
  /^docs\/design\/diagrams\//, // sinh bằng docs/design/tools/gen_erd_mermaid.py
  /(^|\/)node_modules\//,
  /(^|\/)\.gradle\//,
  /^backend\/build\//,
  /^frontend\/dist\//,
]

export function isGeneratedFile(repoPath) {
  return GENERATED.some((pattern) => pattern.test(repoPath))
}

// File cấu hình của chính các hook: agent không được tự tắt rào chắn của mình.
export function isGuardFile(repoPath) {
  return (
    repoPath === '.agents/hooks.json' ||
    repoPath.startsWith('tools/agent-hooks/') ||
    repoPath === '.claude/settings.json' ||
    repoPath === '.claude/settings.local.json'
  )
}

export function isMigrationFile(repoPath) {
  return repoPath.startsWith(MIGRATION_DIR) && /\/V\d+__[^/]+\.sql$/.test(repoPath)
}

// Migration đã có trên develop thì không được sửa (AGENTS.md quy tắc 2). Migration mới của nhánh thì được.
export function existsOnDevelop(root, repoPath, git = runGit) {
  for (const ref of ['origin/develop', 'develop']) {
    if (git(root, ['cat-file', '-e', `${ref}:${repoPath}`]) !== null) return true
  }
  return false
}

export function runGit(root, args) {
  try {
    return execFileSync('git', args, {
      cwd: root,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore'],
    })
  } catch {
    return null
  }
}

// Kết quả trả về theo đúng định dạng của từng agent.
export function emitDecision(agent, decision, reason) {
  if (agent === 'antigravity') {
    process.stdout.write(JSON.stringify(reason ? { decision, reason } : { decision }))
    return
  }
  if (decision === 'deny') {
    process.stdout.write(
      JSON.stringify({
        hookSpecificOutput: {
          hookEventName: 'PreToolUse',
          permissionDecision: 'deny',
          permissionDecisionReason: reason,
        },
      }),
    )
  }
}
