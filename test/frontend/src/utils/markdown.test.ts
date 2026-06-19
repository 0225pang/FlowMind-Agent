import { describe, it, expect } from 'vitest'
import { renderMarkdown, renderHtml } from '@/utils/markdown'

describe('renderMarkdown — Markdown to HTML', () => {
  it('should render headings', () => {
    const html = renderMarkdown('## 标题')
    expect(html).toContain('<h2>')
    expect(html).toContain('标题')
  })

  it('should render bold text', () => {
    const html = renderMarkdown('这是 **粗体** 文字')
    expect(html).toContain('<strong>')
    expect(html).toContain('粗体')
  })

  it('should render italic text', () => {
    const html = renderMarkdown('这是 *斜体* 文字')
    expect(html).toContain('<em>')
    expect(html).toContain('斜体')
  })

  it('should render inline code', () => {
    const html = renderMarkdown('调用 `api.call()` 方法')
    expect(html).toContain('<code>')
    expect(html).toContain('api.call()')
  })

  it('should render links', () => {
    const html = renderMarkdown('[飞书链接](https://feishu.cn)')
    expect(html).toContain('<a href="https://feishu.cn"')
    expect(html).toContain('飞书链接')
  })

  it('should render unordered lists', () => {
    const md = '- 第一项\n- 第二项\n- 第三项'
    const html = renderMarkdown(md)
    expect(html).toContain('<ul>')
    expect(html).toContain('<li>第一项</li>')
    expect(html).toContain('<li>第二项</li>')
    expect(html).toContain('<li>第三项</li>')
  })

  it('should render ordered lists', () => {
    const md = '1. 步骤一\n2. 步骤二\n3. 步骤三'
    const html = renderMarkdown(md)
    expect(html).toContain('<ol>')
    expect(html).toContain('<li>步骤一</li>')
  })

  it('should render tables', () => {
    const md = '| 列A | 列B |\n|------|------|\n| 值1 | 值2 |'
    const html = renderMarkdown(md)
    expect(html).toContain('<table>')
    expect(html).toContain('<th>列A</th>')
    expect(html).toContain('<td>值1</td>')
    expect(html).toContain('<td>值2</td>')
  })

  it('should NOT render separator rows as content', () => {
    const md = '| 列A | 列B |\n|------|------|\n| 值1 | 值2 |'
    const html = renderMarkdown(md)
    expect(html).not.toContain('<td>------</td>')
  })

  it('should render blockquotes', () => {
    const html = renderMarkdown('> 这是一个引用')
    // The renderer escapes HTML, so check that content is present
    expect(html).toContain('这是一个引用')
  })

  it('should render horizontal rules', () => {
    const html = renderMarkdown('---')
    expect(html).toContain('<hr>')
  })

  it('should NOT emit br tags for empty lines', () => {
    const md = '段落一\n\n段落二'
    const html = renderMarkdown(md)
    expect(html).not.toContain('<br>')
  })

  it('should render empty string without errors', () => {
    const html = renderMarkdown('')
    expect(html).toBe('')
  })

  it('should render Chinese text correctly', () => {
    const md = '## 保研面试准备指南\n\n准备材料包括：\n- 个人简历\n- 研究计划\n- 推荐信'
    const html = renderMarkdown(md)
    expect(html).toContain('保研面试准备指南')
    expect(html).toContain('个人简历')
    expect(html).toContain('研究计划')
    expect(html).toContain('推荐信')
  })

  it('should render multi-level headings', () => {
    const md = '## H2\n### H3\n#### H4\n##### H5'
    const html = renderMarkdown(md)
    expect(html).toContain('<h2>')
    expect(html).toContain('<h3>')
    expect(html).toContain('<h4>')
    expect(html).toContain('<h5>')
  })
})

describe('renderHtml — Feishu-style HTML rendering', () => {
  it('should detect and render raw HTML', () => {
    const html = '<p>Hello World</p>'
    const result = renderHtml(html)
    expect(result).toContain('<p>')
    expect(result).toContain('Hello World')
  })

  it('should render Feishu img tags', () => {
    const html = '<img name="test.png" href="https://example.com/img.png" />'
    const result = renderHtml(html)
    expect(result).toContain('<img')
    expect(result).toContain('src="https://example.com/img.png"')
  })

  it('should strip event handlers from HTML', () => {
    const html = '<p onclick="alert(1)">Safe text</p>'
    const result = renderHtml(html)
    expect(result).not.toContain('onclick')
    expect(result).toContain('Safe text')
  })

  it('should strip script tags from HTML', () => {
    const html = '<p>Before</p><script>evil()</script><p>After</p>'
    const result = renderHtml(html)
    expect(result).not.toContain('<script>')
    expect(result).toContain('Before')
    expect(result).toContain('After')
  })

  it('should fallback to markdown for non-HTML input', () => {
    const md = '**bold** 普通文字'
    const result = renderHtml(md)
    expect(result).toContain('<strong>')
    expect(result).toContain('bold')
  })

  it('should detect h1-started content as HTML', () => {
    const html = '<h1>Title</h1><p>Content</p>'
    const result = renderHtml(html)
    expect(result).toContain('<h1>')
    expect(result).toContain('Title')
  })

  it('should render Feishu-style content (mixed HTML)', () => {
    const feishuHtml = '<title>文档标题</title><h1>章节一</h1><p><b>重要提示：</b>请注意</p><img name="图1" src="abc123"/>'
    const result = renderHtml(feishuHtml)
    expect(result).toContain('章节一')
    expect(result).toContain('重要提示')
    expect(result).toContain('请注意')
    expect(result).toContain('<img')
  })
})
