#!/usr/bin/env node
// Hook PreToolUse dùng chung: chặn lệnh và thao tác file mà agent không được làm.
// Antigravity: .agents/hooks.json. Claude Code: .claude/settings.json (chỉ có ở máy Đình).
// Quy tắc gốc: AGENTS.md mục 4 và mục 8; cách dùng: docs/guides/ai-agents.md.
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  currentBranch,
  emitDecision,
  existsOnDevelop,
  isGeneratedFile,
  isGuardFile,
  isMigrationFile,
  isSecretFile,
  normalize,
  readStdinJson,
  toRepoPath,
} from './lib.mjs'

const GIT_READ_ONLY = new Set([
  'status',
  'log',
  'diff',
  'show',
  'blame',
  'shortlog',
  'rev-parse',
  'rev-list',
  'ls-files',
  'ls-tree',
  'grep',
  'describe',
  'cat-file',
  'merge-base',
  'name-rev',
  'for-each-ref',
  'show-ref',
  'check-ignore',
  'range-diff',
  'diff-tree',
  'whatchanged',
  'version',
  'help',
])

const GIT_DENY_REASON =
  'Agent chỉ được chạy lệnh git chỉ đọc (status, log, diff, show, blame, branch để xem...). ' +
  'Đổi nhánh, commit, push, stash, restore... là việc của người làm card (CONTRIBUTING.md mục 8). ' +
  'Ngoại lệ duy nhất của Antigravity: `git fetch origin` rồi ' +
  '`git switch -c <loại>/<mã-card>-<mô-tả> origin/develop` khi đang ở develop hoặc main. ' +
  'Hãy dừng lại và ghi lệnh cần chạy vào báo cáo cho người dùng.'

// Nhánh card mà Antigravity được tự tạo (AGENTS.md mục 5 bước 1, mẫu tên ở CONTRIBUTING.md mục 2).
const CARD_BRANCH = /^(feature|fix|docs|chore)\/[A-Za-z0-9][A-Za-z0-9._-]*$/
const BASE_BRANCHES = new Set(['develop', 'main'])

const SHELLS = new Set(['sh', 'bash', 'zsh', 'pwsh', 'powershell', 'cmd', 'eval'])
const WRAPPERS = new Set(['sudo', 'command', 'env', 'time', 'nohup', 'exec'])

// Trả về lý do chặn (chuỗi) hoặc null nếu cho phép.
export function check(action, deps = {}) {
  const onDevelop = deps.existsOnDevelop ?? existsOnDevelop
  if (action.kind === 'command') {
    const withBranch = { ...action, branchOf: deps.currentBranch ?? currentBranch }
    return checkCommand(withBranch, action.command, onDevelop, 0)
  }
  if (action.kind === 'read') {
    for (const file of action.paths) {
      const repoPath = toRepoPath(action.root, file)
      if (isSecretFile(repoPath))
        return `Không đọc file bí mật (${repoPath}). Cấu hình mẫu nằm ở .env.example (AGENTS.md quy tắc 6).`
    }
    return null
  }
  if (action.kind === 'write') {
    for (const file of action.paths) {
      const reason = checkWritePath(action, toRepoPath(action.root, file), onDevelop)
      if (reason) return reason
    }
  }
  return null
}

function checkWritePath(action, repoPath, onDevelop) {
  if (isSecretFile(repoPath))
    return `Không sửa file bí mật (${repoPath}). Chỉ .env.example được có trong repo (AGENTS.md quy tắc 6).`
  if (isGuardFile(repoPath))
    return `Không tự sửa cấu hình hook (${repoPath}). Cần đổi thì nêu đề xuất để người dùng sửa.`
  if (isGeneratedFile(repoPath))
    return `${repoPath} là file sinh tự động, không sửa tay (AGENTS.md mục 4, quy tắc 12). Sinh lại bằng công cụ của nó.`
  if (isMigrationFile(repoPath) && onDevelop(action.root, repoPath))
    return `${repoPath} đã có trên develop nên không được sửa (AGENTS.md quy tắc 2). Thêm migration V{n} mới bằng skill /new-migration.`
  return null
}

function checkCommand(action, command, onDevelop, depth) {
  const reason = checkRawCommand(action, command, onDevelop)
  if (reason) return reason
  for (const segment of splitSegments(command)) {
    const reasonForSegment = checkSegment(action, segment, onDevelop, depth)
    if (reasonForSegment) return reasonForSegment
  }
  return null
}

function checkRawCommand(action, command, onDevelop) {
  if (/(^|[\s'"=/\\<>])\.env(?!\.example\b)(\.[\w.-]+)?(?=$|[\s'"/\\;|&)<>])/.test(command))
    return 'Lệnh có đụng tới file .env (bí mật). Không đọc hay sửa .env; dùng .env.example (AGENTS.md quy tắc 6).'
  if (
    /[\w-]+\.(pem|key)(?=$|[\s'";|&)])/.test(command) ||
    /service-account[^\s'"]*\.json/i.test(command)
  )
    return 'Lệnh có đụng tới khóa hoặc service account (bí mật). Không làm (AGENTS.md quy tắc 6).'
  if (
    /\brm\s+(-[a-zA-Z]*[rR][a-zA-Z]*|--recursive)\b/.test(command) ||
    /\bRemove-Item\b[^;|&]*-Recurse/i.test(command) ||
    /\b(rmdir|rd|del)\s+\/s\b/i.test(command) ||
    /\bfind\b[^;|&]*\s-delete\b/.test(command)
  )
    return 'Không xóa thư mục hàng loạt. Cần xóa thì liệt kê đường dẫn để người dùng tự xóa.'
  if (
    /\bdocker(-compose|\s+compose)\s+down\b[^;|&]*\s(-v|--volumes)\b/.test(command) ||
    /\bdocker\s+(volume\s+(rm|prune)|system\s+prune)\b/.test(command)
  )
    return 'Lệnh này xóa volume Docker (mất dữ liệu PostgreSQL ở máy dev). Để người dùng tự chạy nếu thật sự cần.'
  if (/\bnpm\s+publish\b/.test(command)) return 'Không publish package.'
  const migrations = command.match(/V\d+__[\w.-]+\.sql/g) ?? []
  const writes =
    /(\bsed\s+-i|>|\btee\b|\bmv\b|\brm\b|\bcp\b|Set-Content|Out-File|Add-Content|Move-Item|Remove-Item)/
  if (migrations.length && writes.test(command)) {
    for (const name of migrations) {
      const repoPath = `backend/src/main/resources/db/migration/${name}`
      if (onDevelop(action.root, repoPath))
        return `${name} đã có trên develop nên không được sửa, đổi tên hay xóa (AGENTS.md quy tắc 2).`
    }
  }
  return null
}

function checkSegment(action, tokens, onDevelop, depth) {
  let i = 0
  while (i < tokens.length && (/^[A-Za-z_]\w*=/.test(tokens[i]) || WRAPPERS.has(tokens[i]))) i++
  if (i >= tokens.length) return null
  const program = path
    .basename(tokens[i])
    .toLowerCase()
    .replace(/\.exe$/, '')
  const args = tokens.slice(i + 1)
  if (SHELLS.has(program) && depth < 3) {
    for (const arg of args) {
      if (/\s/.test(arg)) {
        const reason = checkCommand(action, arg, onDevelop, depth + 1)
        if (reason) return reason
      }
    }
    return null
  }
  if (program === 'git') return checkGit(action, args)
  if (program === 'gh') return checkGh(action.agent, args)
  return null
}

function checkGit(action, args) {
  const { agent } = action
  let i = 0
  while (i < args.length && args[i].startsWith('-')) {
    if (['-C', '-c', '--git-dir', '--work-tree', '--namespace'].includes(args[i])) i++
    i++
  }
  const sub = args[i]
  const rest = args.slice(i + 1)
  if (!sub) return null
  if (rest.some((a) => a === '--output' || a.startsWith('--output=')))
    return 'Không ghi kết quả git ra file bằng --output.'
  if (GIT_READ_ONLY.has(sub)) return null
  if (sub === 'branch') return checkGitBranch(agent, rest)
  if (sub === 'tag') return onlyListing(rest, ['-l', '--list', '--contains', '--points-at', '-n'])
  if (sub === 'stash') return ['list', 'show'].includes(rest[0]) ? null : GIT_DENY_REASON
  if (sub === 'remote')
    return rest.length === 0 || ['-v', 'show', 'get-url'].includes(rest[0]) ? null : GIT_DENY_REASON
  if (sub === 'reflog')
    return rest.length === 0 || rest[0] === 'show' || rest[0].startsWith('-')
      ? null
      : GIT_DENY_REASON
  if (sub === 'config')
    return rest.some((a) => ['--get', '--get-all', '--get-regexp', '--list', '-l'].includes(a))
      ? null
      : GIT_DENY_REASON
  if (agent === 'antigravity') {
    if (sub === 'fetch') return isPlainFetch(rest) ? null : GIT_DENY_REASON
    if (sub === 'switch') return checkCreateCardBranch(action, rest)
  }
  if (agent === 'claude') {
    // Review PR: lấy code PR về một worktree riêng, không đổi nhánh của thư mục đang làm.
    if (sub === 'fetch') return null
    if (sub === 'worktree' && ['add', 'list', 'remove', 'prune'].includes(rest[0])) return null
  }
  return GIT_DENY_REASON
}

// Chỉ cập nhật nhánh theo dõi từ origin, không lấy ref lạ về (refspec có dấu ":").
function isPlainFetch(rest) {
  return rest.every((a) => ['origin', 'develop', 'main', '-q', '--quiet', '--prune'].includes(a))
}

// Đúng một mẫu: git switch -c <loại>/<tên> origin/develop, và chỉ khi đang ở develop hoặc main. Tạo nhánh mới ở
// máy không mất code: thay đổi chưa commit đi theo sang nhánh mới; git tự từ chối nếu xung đột.
function checkCreateCardBranch(action, rest) {
  const [flag, name, start, ...extra] = rest.filter((a) => a !== '-q' && a !== '--quiet')
  const isCardBranch =
    ['-c', '--create'].includes(flag) &&
    CARD_BRANCH.test(name ?? '') &&
    start === 'origin/develop' &&
    extra.length === 0
  if (!isCardBranch) return GIT_DENY_REASON
  const branch = action.branchOf(action.root)
  if (!BASE_BRANCHES.has(branch))
    return `Đang ở nhánh ${branch ?? '(không rõ)'}, không phải develop hoặc main. Không tự tạo nhánh mới; dừng lại và hỏi người dùng.`
  return null
}

function checkGitBranch(agent, rest) {
  const destructive = [
    '-d',
    '-D',
    '--delete',
    '-m',
    '-M',
    '--move',
    '-c',
    '-C',
    '--copy',
    '-f',
    '--force',
  ]
  if (rest.some((a) => destructive.includes(a))) {
    // Claude được dọn nhánh tạm pr-<số> do chính skill review-pr tạo.
    const names = rest.filter((a) => !a.startsWith('-'))
    const onlyDelete = rest.every((a) => ['-d', '-D', '--delete'].includes(a) || !a.startsWith('-'))
    if (
      agent === 'claude' &&
      onlyDelete &&
      names.length > 0 &&
      names.every((n) => /^pr-\d+$/.test(n))
    )
      return null
    return GIT_DENY_REASON
  }
  if (rest.some((a) => /^(-u|--set-upstream-to|--unset-upstream|--edit-description)/.test(a)))
    return GIT_DENY_REASON
  return onlyListing(rest, [
    '-l',
    '--list',
    '--contains',
    '--no-contains',
    '--merged',
    '--no-merged',
    '--points-at',
  ])
}

// Cho phép khi không có tham số vị trí, hoặc tham số vị trí đi sau một cờ dạng liệt kê.
function onlyListing(rest, listingFlags) {
  const positional = rest.filter((a) => !a.startsWith('-'))
  if (positional.length === 0) return null
  return rest.some((a) => listingFlags.includes(a)) ? null : GIT_DENY_REASON
}

function checkGh(agent, args) {
  const [group, action] = args
  const readOnly =
    args.includes('--version') ||
    (group === 'auth' && action === 'status') ||
    (group === 'pr' && ['view', 'diff', 'list', 'checks', 'status'].includes(action)) ||
    (group === 'issue' && ['view', 'list'].includes(action)) ||
    (group === 'run' && ['view', 'list'].includes(action)) ||
    (group === 'repo' && action === 'view')
  if (readOnly) return null
  const ghDeny = 'Lệnh gh này làm thay đổi GitHub. Agent không merge, approve, đóng hay tạo PR.'
  if (agent !== 'claude') return ghDeny
  if (group === 'pr' && action === 'review')
    return args.some((a) => a === '-a' || a === '--approve')
      ? 'Claude không approve PR; Đình tự approve.'
      : null
  if (group === 'pr' && action === 'comment') return null
  if (group === 'api') return checkGhApi(args.slice(1))
  return ghDeny
}

function checkGhApi(args) {
  const method = args.find((a, i) => args[i - 1] === '-X' || args[i - 1] === '--method') ?? ''
  if (['PUT', 'DELETE', 'PATCH'].includes(method.toUpperCase()))
    return 'Claude chỉ được đọc và đăng review qua gh api.'
  if (args.some((a) => /\/merge\b/.test(a))) return 'Claude không merge PR.'
  if (args[0] === 'graphql' && args.some((a) => /\bmutation\b/.test(a)))
    return 'Claude không chạy mutation GraphQL.'
  if (args.some((a) => /APPROVE/.test(a))) return 'Claude không approve PR; Đình tự approve.'
  const inputIndex = args.indexOf('--input')
  if (inputIndex >= 0 && args[inputIndex + 1] && args[inputIndex + 1] !== '-') {
    try {
      const body = fs.readFileSync(args[inputIndex + 1], 'utf8')
      if (/"event"\s*:\s*"APPROVE"/.test(body)) return 'Claude không approve PR; Đình tự approve.'
    } catch {
      // Không đọc được file thì để gh tự báo lỗi.
    }
  }
  return null
}

// Tách lệnh shell thành các đoạn (theo && || ; | & xuống dòng, ngoặc), mỗi đoạn là mảng token đã bỏ dấu nháy.
export function splitSegments(command) {
  const segments = []
  let tokens = []
  let current = ''
  let hasToken = false
  let quote = null
  const pushToken = () => {
    if (hasToken) tokens.push(current)
    current = ''
    hasToken = false
  }
  const pushSegment = () => {
    pushToken()
    if (tokens.length) segments.push(tokens)
    tokens = []
  }
  for (let i = 0; i < command.length; i++) {
    const ch = command[i]
    if (quote) {
      if (ch === quote) quote = null
      else if (ch === '\\' && quote === '"' && command[i + 1] === '"') current += command[++i]
      else current += ch
      continue
    }
    if (ch === '"' || ch === "'") {
      quote = ch
      hasToken = true
    } else if (/\s/.test(ch) && ch !== '\n') {
      pushToken()
    } else if ('\n;&|()`'.includes(ch)) {
      pushSegment()
    } else if (ch === '$' && command[i + 1] === '(') {
      pushSegment()
      i++
    } else {
      current += ch
      hasToken = true
    }
  }
  pushSegment()
  return segments
}

async function main() {
  let agent = 'claude'
  try {
    const action = normalize(await readStdinJson())
    agent = action.agent
    const reason = check(action)
    emitDecision(agent, reason ? 'deny' : 'allow', reason ?? undefined)
  } catch (error) {
    // Lỗi của chính hook thì cho qua (để không khóa cứng agent) và báo ra stderr.
    process.stderr.write(`agent-hooks/guard: ${error.message}\n`)
    emitDecision(agent, 'allow')
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1]))
  await main()
