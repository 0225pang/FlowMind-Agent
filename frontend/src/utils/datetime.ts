/**
 * Format a datetime string as Beijing-time display.
 * Backend now returns Beijing-time strings ("yyyy-MM-dd HH:mm:ss").
 * If the string has no timezone info (short format), treat as-is.
 */
export function fmtBeijing(ts: string | null | undefined): string {
  if (!ts) return ''
  // Backend returns "2026-06-15 21:05:30" format (Beijing time)
  if (ts.length <= 19) {
    return ts.length > 10 ? ts.slice(5, 16).replace('T', ' ') : ts
  }
  // Fallback: parse from ISO string
  const d = new Date(ts)
  if (isNaN(d.getTime())) return ts.slice(0, 16)
  // Use locale display — accurate for display purposes
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) + ' '
    + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

/**
 * Relative time display (e.g. "5分钟前", "3天前")
 */
export function relativeTime(ts: string | null | undefined): string {
  if (!ts) return ''
  const d = new Date(ts)
  if (isNaN(d.getTime())) return ts.slice(0, 16)
  const now = Date.now()
  const diff = now - d.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return Math.floor(diff / 60_000) + '分钟前'
  if (diff < 86_400_000) return Math.floor(diff / 3_600_000) + '小时前'
  if (diff < 604_800_000) return Math.floor(diff / 86_400_000) + '天前'
  return fmtBeijing(ts)
}
