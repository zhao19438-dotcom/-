import { marked } from 'marked'
import DOMPurify from 'dompurify'

// 把后端返回的 Markdown 正文转成安全的 HTML。
// marked 负责解析，DOMPurify 负责过滤 XSS（帖子内容可能含用户输入的 HTML）。
marked.setOptions({ breaks: true, gfm: true })

export function renderMarkdown (text) {
  if (!text) return ''
  const rawHtml = marked.parse(text)
  return DOMPurify.sanitize(rawHtml)
}
