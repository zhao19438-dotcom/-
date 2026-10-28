/** 把后端时间格式化成可读短文本，例如 2026-09-21 10:30 */
export function formatTime (timeStr) {
  if (!timeStr) return ''
  return String(timeStr).replace('T', ' ').slice(0, 16)
}

/** 相对时间：刚刚 / n 分钟前 / n 小时前 / n 天前，超过一周回退到 formatTime */
export function relativeTime (timeStr) {
  if (!timeStr) return ''
  const normalized = String(timeStr).replace(' ', 'T')
  const t = new Date(normalized)
  if (Number.isNaN(t.getTime())) return formatTime(timeStr)
  const diff = Date.now() - t.getTime()
  if (diff < 0) return formatTime(timeStr)
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days} 天前`
  return formatTime(timeStr)
}

/** 从 Markdown 正文抽出列表摘要 */
export function getSnippet (content, maxLen = 110) {
  if (!content) return ''
  const plain = content
    .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/[#*`~>_-]/g, '')
    .replace(/<[^>]+>/g, '')
    .replace(/\n+/g, ' ')
    .trim()
  if (plain.length <= maxLen) return plain
  return plain.slice(0, maxLen) + '...'
}

export function displayName (user, fallback = '在校同学') {
  if (!user) return fallback
  return user.nickname || user.username || fallback
}

export function initials (user, fallback = '?') {
  const name = displayName(user, fallback)
  return String(name).slice(0, 1)
}
