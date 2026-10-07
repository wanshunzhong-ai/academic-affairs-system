#!/usr/bin/env node
/**
 * 前端开发服务器「端口预检」—— 由 package.json 的 predev 钩子在 `npm run dev` 之前自动执行。
 *
 * 目的：把 Vite 那句英文报错
 *     Error: Port 5173 is already in use
 * 换成人能看懂的中文提示，并直接告诉你是谁占着、怎么清理。
 *
 * 行为约定：
 *   - 端口从 vite.config.js 里读，保证与真正启动的端口一致（单一真源）
 *   - 端口空闲 → 静默通过（退出码 0），你察觉不到它存在
 *   - 端口被占 → 打印提示（退出码 1），阻止 Vite 再抛一遍英文堆栈
 *   - 本脚本自身出错 → 一律放行（退出码 0），绝不因为它导致项目起不来
 *
 * 手动排查：node scripts/check-port.mjs          检查 vite.config.js 里配的端口
 *           node scripts/check-port.mjs --port 5174   检查指定端口
 */
import net from 'node:net'
import fs from 'node:fs'
import path from 'node:path'
import { execFileSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const HERE = path.dirname(fileURLToPath(import.meta.url))
const FRONTEND_DIR = path.resolve(HERE, '..')
const CONFIG_FILE = path.join(FRONTEND_DIR, 'vite.config.js')
const DEFAULT_PORT = 5173

/** 从 vite.config.js 里读端口 */
function readConfiguredPort() {
  try {
    const src = fs.readFileSync(CONFIG_FILE, 'utf8')
    const m = src.match(/port\s*:\s*(\d+)/)
    if (m) {
      const n = Number(m[1])
      if (Number.isInteger(n) && n > 0 && n < 65536) return n
    }
  } catch {
    /* 读不到就用默认值 */
  }
  return DEFAULT_PORT
}

/** 命令行 --port N 覆盖（只用于人工排查：node scripts/check-port.mjs --port 5199）
 *
 * 注意：`npm run dev -- --port 5199` 这种写法这里拿不到 5199 ——
 * npm 不会把它写进环境变量，predev 钩子收不到任何参数。
 * 本项目端口是固定值（与后端启动横幅打印的入口地址一致），要换端口请改 vite.config.js。 */
function readArgPort() {
  const i = process.argv.indexOf('--port')
  const raw = i >= 0 ? process.argv[i + 1] : ''
  const n = Number(raw)
  if (Number.isInteger(n) && n > 0 && n < 65536) return n
  return 0
}

const PORT = readArgPort() || readConfiguredPort()

/** 尝试占用端口：能占上说明空闲 */
function portIsBusy(port) {
  return new Promise((resolve) => {
    const srv = net.createServer()
    srv.once('error', (err) => {
      resolve(err && (err.code === 'EADDRINUSE' || err.code === 'EACCES'))
    })
    srv.once('listening', () => srv.close(() => resolve(false)))
    try {
      srv.listen(port, '0.0.0.0')
    } catch {
      resolve(false)
    }
  })
}

/** 中文 Windows 的控制台输出多为 GBK，尽量正确解码 */
function decode(buf) {
  try {
    return new TextDecoder('gbk').decode(buf)
  } catch {
    return buf.toString('utf8')
  }
}

function run(cmd, args) {
  try {
    return decode(
      execFileSync(cmd, args, {
        windowsHide: true,
        stdio: ['ignore', 'pipe', 'ignore'],
        timeout: 8000
      })
    )
  } catch {
    return ''
  }
}

/** Windows：从 netstat 拿 PID，再从 tasklist / PowerShell 补进程名与命令行 */
function findHoldersWindows(port) {
  const out = run('netstat', ['-ano'])
  const re = new RegExp(':' + port + '\\s+\\S+\\s+LISTENING\\s+(\\d+)')
  const pids = new Set()
  for (const line of out.split(/\r?\n/)) {
    const m = line.match(re)
    if (m) pids.add(m[1])
  }
  return [...pids].map((pid) => {
    let name = ''
    const t = run('tasklist', ['/FI', `PID eq ${pid}`, '/FO', 'CSV', '/NH'])
    const nm = t.match(/^"([^"]+)"/)
    if (nm) name = nm[1]
    const cmdline = run('powershell', [
      '-NoProfile',
      '-NonInteractive',
      '-Command',
      `(Get-CimInstance Win32_Process -Filter 'ProcessId=${pid}').CommandLine`
    ]).trim()
    return { pid, name, cmdline }
  })
}

/** 非 Windows：退化为 lsof（拿不到就只报告端口被占） */
function findHoldersUnix(port) {
  const out = run('lsof', ['-nP', `-i:${port}`, '-sTCP:LISTEN'])
  return out
    .split('\n')
    .slice(1)
    .filter((l) => l.trim())
    .map((l) => {
      const cols = l.split(/\s+/)
      return { pid: cols[1] || '?', name: cols[0] || '', cmdline: '' }
    })
}

function buildMessage(holders) {
  const L = []
  const bar = '='.repeat(60)
  L.push('')
  L.push(bar)
  L.push(`  端口 ${PORT} 已被占用，前端开发服务器无法启动`)
  L.push(bar)
  L.push('')
  if (holders.length) {
    L.push('占用者：')
    for (const h of holders) {
      L.push(`  PID ${h.pid}${h.name ? '   ' + h.name : ''}`)
      if (h.cmdline) L.push(`        ${h.cmdline}`)
    }
    L.push('')
  }
  L.push('这通常是「上一次启动的前端没退干净」—— IDEA 点「停止」不会回收 Vite 的')
  L.push('node 子进程，旧实例一直占着端口。处理方式二选一：')
  L.push('')
  L.push('  ① 直接用旧的：它可能还在正常服务，浏览器打开就行')
  L.push(`       http://localhost:${PORT}/login`)
  L.push('')
  L.push('  ② 清理后重启：先在 IDEA 里对正在运行的前端点「停止」；若端口仍被占用，执行')
  if (process.platform === 'win32') {
    L.push('       CMD:')
    L.push(`         for /f "tokens=5" %a in ('netstat -ano ^| findstr :${PORT} ^| findstr LISTENING') do taskkill /F /PID %a`)
    L.push('       PowerShell:')
    L.push(`         Get-NetTCPConnection -LocalPort ${PORT} -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }`)
  } else {
    L.push(`         lsof -ti tcp:${PORT} | xargs kill -9`)
  }
  L.push('')
  L.push('  ⚠ 只结束上面列出的 PID，不要批量结束 node 进程（可能误伤 IDE 的后台服务）。')
  L.push(`  ⓘ 本项目端口固定 ${PORT}（与后端启动横幅打印的入口地址一致）。`)
  L.push('    如需换端口，请改 vite.config.js 里的 port —— 用 --port 参数启动会被这里拦住。')
  L.push('  详见《运行指南.md》7.1「清理遗留的前端进程」')
  L.push(bar)
  L.push('')
  return L.join('\n')
}

async function main() {
  if (!(await portIsBusy(PORT))) return 0 // 空闲 → 静默放行
  const holders =
    process.platform === 'win32' ? findHoldersWindows(PORT) : findHoldersUnix(PORT)
  console.log(buildMessage(holders))
  return 1
}

main()
  .then((code) => process.exit(code))
  .catch(() => process.exit(0)) // 预检自身异常一律放行
