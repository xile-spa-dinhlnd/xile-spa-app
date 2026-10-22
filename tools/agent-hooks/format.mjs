#!/usr/bin/env node
// Hook PostToolUse của Antigravity: chạy Prettier trên đúng file frontend vừa sửa (dưới 1 giây).
// File Java không format ở đây (Gradle chậm); stop.mjs chạy spotlessApply một lần khi agent làm xong.
import fs from 'node:fs'
import { createRequire } from 'node:module'
import path from 'node:path'
import { pathToFileURL } from 'node:url'
import { normalize, readStdinJson, toRepoPath } from './lib.mjs'

const EXTENSIONS = new Set([
  '.ts',
  '.tsx',
  '.js',
  '.jsx',
  '.mjs',
  '.cjs',
  '.json',
  '.css',
  '.html',
  '.md',
  '.yml',
  '.yaml',
])

async function main() {
  const action = normalize(await readStdinJson())
  if (action.kind !== 'write') return
  const frontend = path.join(action.root, 'frontend')
  let prettier
  try {
    const require = createRequire(path.join(frontend, 'package.json'))
    const loaded = await import(pathToFileURL(require.resolve('prettier')).href)
    prettier = loaded.default ?? loaded
  } catch {
    return // chưa chạy npm install thì bỏ qua
  }
  for (const file of action.paths) {
    const repoPath = toRepoPath(action.root, file)
    if (!repoPath.startsWith('frontend/') || !EXTENSIONS.has(path.extname(repoPath))) continue
    const absolute = path.join(action.root, repoPath)
    if (!fs.existsSync(absolute)) continue
    const info = await prettier.getFileInfo(absolute, {
      ignorePath: path.join(frontend, '.prettierignore'),
    })
    if (info.ignored || !info.inferredParser) continue
    const options = (await prettier.resolveConfig(absolute)) ?? {}
    const source = fs.readFileSync(absolute, 'utf8')
    const formatted = await prettier.format(source, { ...options, filepath: absolute })
    if (formatted !== source) fs.writeFileSync(absolute, formatted)
  }
}

try {
  await main()
} catch (error) {
  // File đang viết dở có thể chưa parse được; lần sửa sau sẽ format lại.
  process.stderr.write(`agent-hooks/format: ${error.message}\n`)
}
process.stdout.write('{}')
