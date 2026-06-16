/**
 * Lightweight Markdown → HTML renderer.
 * Also handles Feishu-flavored HTML (img tags, inline HTML).
 * Supports: h1–h5, **bold**, *italic*, `inline code`, [links](url),
 * unordered lists (- *), ordered lists (1. 2.), tables (|), fenced code blocks (```),
 * blockquotes (>), horizontal rules (---), images (![]() and <img> tags).
 */
export function renderHtml(input: string) {
  // If input looks like raw HTML (from Feishu), render it directly after sanitizing
  if (looksLikeHtml(input)) {
    return sanitizeHtml(input)
  }
  // Otherwise use markdown renderer
  return renderMarkdown(input)
}

function looksLikeHtml(text: string) {
  const t = text.trim()
  return t.startsWith('<') || t.includes('<img ') || t.includes('<p>') || t.includes('<h1>')
}

function sanitizeHtml(raw: string) {
  // Strip potentially dangerous tags/attributes but keep safe ones
  return raw
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/\son\w+\s*=\s*"[^"]*"/gi, '')        // strip event handlers
    .replace(/<img\s+name="([^"]*)"[^>]*href="([^"]*)"[^>]*\/>/g,
      '<img src="$2" alt="$1" loading="lazy" style="max-width:100%;border-radius:8px;margin:8px 0" />')
    // Also handle self-closing img with just src attribute
    .replace(/<img\s+[^>]*src="([^"]*)"[^>]*\/?>/g,
      (match: string, src: string) => {
        const alt = (match.match(/name="([^"]*)"/) || [])[1] || ''
        return `<img src="${src}" alt="${alt}" loading="lazy" style="max-width:100%;border-radius:8px;margin:8px 0" />`
      })
}

export function renderMarkdown(input: string) {
  const escaped = escapeHtml(input || '')
  const lines = escaped.split(/\r?\n/)
  const html: string[] = []

  let listType: 'ul' | 'ol' | '' = ''
  let inCodeBlock = false
  let codeBlockLines: string[] = []
  let codeBlockLang = ''
  let inTable = false
  let tableLines: string[] = []

  const closeList = () => {
    if (listType) {
      html.push(`</${listType}>`)
      listType = ''
    }
  }

  const closeTable = () => {
    if (inTable && tableLines.length >= 2) {
      const [headerRow, ...bodyRows] = tableLines
      const headers = parseTableRow(headerRow)
      let t = '<table><thead><tr>'
      for (const h of headers) t += `<th>${renderInline(h.trim())}</th>`
      t += '</tr></thead><tbody>'
      for (const row of bodyRows) {
        if (isSeparatorRow(row)) continue   // skip |----|----|
        const cells = parseTableRow(row)
        t += '<tr>'
        for (const c of cells) t += `<td>${renderInline(c.trim())}</td>`
        t += '</tr>'
      }
      t += '</tbody></table>'
      html.push(t)
    }
    tableLines = []
    inTable = false
  }

  for (const rawLine of lines) {
    const line = rawLine.trimEnd()

    // ---- Multi-line code block ----
    if (line.trimStart().startsWith('```')) {
      if (!inCodeBlock) {
        closeList()
        closeTable()
        inCodeBlock = true
        codeBlockLang = line.trimStart().slice(3).trim()
        codeBlockLines = []
        continue
      } else {
        // end of code block
        const code = codeBlockLines.join('\n')
        const langClass = codeBlockLang ? ` class="language-${escapeHtml(codeBlockLang)}"` : ''
        html.push(`<pre><code${langClass}>${code}</code></pre>`)
        inCodeBlock = false
        codeBlockLines = []
        codeBlockLang = ''
        continue
      }
    }
    if (inCodeBlock) {
      codeBlockLines.push(rawLine)
      continue
    }

    // ---- Table detection ----
    if (line.includes('|') && line.trim().startsWith('|')) {
      closeList()
      inTable = true
      tableLines.push(line)
      continue
    } else if (inTable) {
      closeTable()
    }

    // ---- Empty line: close open blocks, skip rendering ----
    // CSS margins on <p>/<h*>/<ul>/<ol> already handle block separation.
    if (!line.trim()) {
      closeList()
      closeTable()
      continue
    }

    // ---- Headings ----
    const heading = line.match(/^(#{1,5})\s+(.+)$/)
    if (heading) {
      closeList()
      closeTable()
      const level = heading[1].length
      html.push(`<h${level}>${renderInline(heading[2])}</h${level}>`)
      continue
    }

    // ---- Horizontal rule ----
    if (/^[-*_]{3,}\s*$/.test(line.trim())) {
      closeList()
      closeTable()
      html.push('<hr>')
      continue
    }

    // ---- Blockquote ----
    if (line.startsWith('> ')) {
      closeList()
      closeTable()
      html.push(`<blockquote><p>${renderInline(line.slice(2))}</p></blockquote>`)
      continue
    }

    // ---- Ordered list ----
    const ordered = line.match(/^\s*(\d+)\.\s+(.+)$/)
    if (ordered && !isNested(line)) {
      if (listType !== 'ol') { closeList(); html.push('<ol>'); listType = 'ol' }
      html.push(`<li>${renderInline(ordered[2])}</li>`)
      continue
    }

    // ---- Unordered list ----
    const unordered = line.match(/^\s*[-*]\s+(.+)$/)
    if (unordered && !isNested(line)) {
      if (listType !== 'ul') { closeList(); html.push('<ul>'); listType = 'ul' }
      html.push(`<li>${renderInline(unordered[1])}</li>`)
      continue
    }

    // ---- Default: paragraph ----
    closeList()
    closeTable()
    html.push(`<p>${renderInline(line)}</p>`)
  }

  closeList()
  closeTable()
  if (inCodeBlock) {
    html.push(`<pre><code>${codeBlockLines.join('\n')}</code></pre>`)
  }
  return html.join('')
}

// Inline formatting: bold, italic, code, links, strikethrough
function renderInline(input: string) {
  let s = input
  s = s.replace(/`([^`]+)`/g, '<code>$1</code>')
  s = s.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  s = s.replace(/\*([^*]+)\*/g, '<em>$1</em>')
  s = s.replace(/~~([^~]+)~~/g, '<del>$1</del>')
  s = s.replace(/\[([^\]]+)]\((https?:\/\/[^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noreferrer">$1</a>')
  return s
}

// Parse a markdown table row: split by |, trim, discard leading/trailing empties
function parseTableRow(line: string): string[] {
  let cells = line.split('|')
  // Remove leading/trailing empty cell from outer pipes
  if (cells.length > 1 && cells[0].trim() === '') cells = cells.slice(1)
  if (cells.length > 1 && cells[cells.length - 1].trim() === '') cells = cells.slice(0, -1)
  // Filter out separator rows like --- | ---
  const nonSep = cells.filter(c => !/^[-:]{3,}$/.test(c.trim()))
  return nonSep.length > 0 ? nonSep : cells
}

// Check if a table row is a separator row like |----|:---:|----|
function isSeparatorRow(line: string) {
  const cells = line.split('|').map(c => c.trim()).filter(c => c !== '')
  if (cells.length === 0) return false
  return cells.every(c => /^[-:]{3,}$/.test(c))
}

function isNested(line: string) {
  return line.startsWith('  ') || line.startsWith('\t')
}

function escapeHtml(input: string) {
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
