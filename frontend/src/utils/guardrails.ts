import { ElMessage, ElMessageBox } from 'element-plus'

export type GuardLevel = 'ok' | 'warning' | 'blocked'

export type GuardResult<T = string> = {
  level: GuardLevel
  title: string
  message: string
  value?: T
  suggestions: string[]
}

export type RetryPolicy = {
  maxAttempts: number
  retryDelayMs: number
  retryOnStatuses: number[]
  retryOnNetworkError: boolean
}

export type SafeOperationOptions = {
  scene: string
  operation: string
  confirm?: boolean
  confirmTitle?: string
  confirmMessage?: string
  successMessage?: string
  failureMessage?: string
  retry?: Partial<RetryPolicy>
}

export const defaultRetryPolicy: RetryPolicy = {
  maxAttempts: 1,
  retryDelayMs: 500,
  retryOnStatuses: [408, 429, 500, 502, 503, 504],
  retryOnNetworkError: true
}

export const sceneRecoveryMap: Record<string, string[]> = {
  agent: ['检查后端服务是否启动', '确认 DeepSeek API Key 或 MockLLM 可用', '刷新页面后重新发送'],
  knowledge: ['检查 Weaviate 是否启动', '确认知识库已同步', '换一个更短的关键词'],
  content: ['刷新主题库和文案库', '确认当前账号有内容权限', '检查 MySQL 内容表数据'],
  student: ['确认当前账号有学员权限', '检查学员列表接口', '刷新后重新进入页面'],
  school: ['检查院校项目接口', '确认 Mock 数据存在', '缩短筛选条件后重试'],
  feishu: ['检查 lark-cli 授权', '确认共享文件夹权限', '检查 folderToken 是否正确'],
  settings: ['重新登录', '检查当前角色', '联系团队管理员调整权限'],
  default: ['稍后重试', '检查后端日志', '确认网络连接正常']
}

export function ok<T = string>(value?: T, message = 'ok'): GuardResult<T> {
  return { level: 'ok', title: '校验通过', message, value, suggestions: [] }
}

export function warning<T = string>(title: string, message: string, value?: T, suggestions: string[] = []): GuardResult<T> {
  return { level: 'warning', title, message, value, suggestions }
}

export function blocked<T = string>(title: string, message: string, suggestions: string[] = []): GuardResult<T> {
  return { level: 'blocked', title, message, suggestions }
}

export function normalizeBaseUrl(input: string) {
  let value = (input || '').trim()
  while (value.endsWith('/')) value = value.slice(0, -1)
  return value
}

export function validateBaseUrl(input: string): GuardResult {
  const value = (input || '').trim()
  if (!value) return blocked('后端地址不能为空', '请填写后端 Base URL，例如 https://xxx.ngrok-free.dev')
  if (!/^https?:\/\//.test(value)) return blocked('后端地址格式不正确', '必须以 http:// 或 https:// 开头')
  if (/\s/.test(value)) return blocked('后端地址包含空白字符', '请删除复制时带入的空格或换行')
  if (value.endsWith('/')) return warning('已自动修正地址', '系统会移除末尾的 /', normalizeBaseUrl(value))
  return ok(normalizeBaseUrl(value))
}

export function validateLogin(username: string, password: string): GuardResult {
  if (!username.trim()) return blocked('请输入账号', '可以使用 admin、content、teacher、ip、student')
  if (!password.trim()) return blocked('请输入密码', 'Demo 阶段默认密码是 123456')
  if (password.length < 3) return blocked('密码过短', '请确认密码是否输入完整')
  if (username.length > 64) return blocked('账号过长', '账号长度不能超过 64 个字符')
  return ok(username.trim())
}

export function validatePrompt(prompt: string): GuardResult {
  const value = prompt.trim()
  if (!value) return blocked('请输入任务内容', '可以点击快捷指令，或输入要生成/检索/同步的任务')
  if (value.length > 4000) return blocked('输入内容太长', '请拆成多轮对话，每轮建议不超过 4000 字')
  if (looksLikeSecret(value)) {
    return warning('疑似包含密钥', '请确认不要把 API Key、密码或 Token 发给模型', value, ['删除敏感字段', '改用环境变量或本地配置'])
  }
  return ok(value)
}

export function validateKeyword(keyword: string): GuardResult {
  const value = keyword.trim()
  if (!value) return blocked('请输入检索关键词', '建议使用 2-20 个字，例如：课程论文、保研简历')
  if (value.length < 2) return warning('关键词较短', '检索结果可能不稳定', value, ['尝试补充主题词'])
  if (value.length > 80) return warning('关键词较长', '建议缩短关键词以提升召回效果', value.slice(0, 80), ['删除无关描述'])
  return ok(value)
}

export function validateTitle(title: string): GuardResult {
  const value = title.trim()
  if (!value) return blocked('标题不能为空', '请填写主题或文案标题')
  if (value.length > 120) return blocked('标题太长', '标题建议控制在 120 字以内')
  return ok(value)
}

export function validateCopy(content: string): GuardResult {
  const value = content.trim()
  if (!value) return blocked('文案内容不能为空', '请填写正文，或先用 AI 工作台生成')
  if (value.length > 12000) return blocked('文案内容太长', '请拆分保存，当前限制 12000 字')
  if (looksLikeSecret(value)) return warning('文案中疑似包含敏感信息', '请确认是否误粘贴了密钥或 token', value)
  return ok(value)
}

export function validateRating(rating: number): GuardResult<number> {
  if (!Number.isFinite(rating)) return blocked('评分无效', '评分必须是 1-5 的数字')
  if (rating < 1 || rating > 5) return blocked('评分范围错误', '请点击 1-5 星进行评分')
  return ok(rating)
}

export function validateImageUrl(url: string): GuardResult {
  const value = url.trim()
  if (!value) return blocked('图片地址不能为空', '如果没有图片，可以先保留配图建议')
  if (!/^https?:\/\//.test(value)) return blocked('图片地址格式不正确', '请填写 http 或 https 开头的图片 URL')
  if (value.length > 1000) return blocked('图片地址过长', '请检查是否复制了错误内容')
  return ok(value)
}

export function validateStudentProfile(profile: string): GuardResult {
  const value = profile.trim()
  if (!value) return blocked('请输入学生画像', '至少包含 GPA、排名、英语、科研或目标方向')
  const missing: string[] = []
  if (!/(GPA|绩点|成绩)/i.test(value)) missing.push('GPA/成绩')
  if (!/(排名|前\s*\d|%|百分比)/.test(value)) missing.push('排名')
  if (!/(六级|雅思|托福|英语|CET|IELTS|TOEFL)/i.test(value)) missing.push('英语')
  if (missing.length) return warning('画像信息不完整', `建议补充：${missing.join('、')}`, value)
  return ok(value)
}

export function looksLikeSecret(value: string) {
  const lower = value.toLowerCase()
  return lower.includes('sk-')
    || lower.includes('api_key')
    || lower.includes('apikey')
    || lower.includes('secret')
    || lower.includes('password')
    || lower.includes('token=')
}

export function friendlyHttpError(error: any, fallback = '请求失败，请稍后重试') {
  const status = error?.response?.status
  const message = error?.response?.data?.message || error?.message || ''
  if (status === 401) return '登录状态失效，请重新登录'
  if (status === 403) return '当前账号没有权限访问该功能'
  if (status === 404) return '接口不存在，请检查后端版本或 Base URL'
  if (status === 408) return '请求超时，请检查网络或后端服务'
  if (status === 429) return '请求过于频繁，请稍后再试'
  if (status >= 500) return `后端处理异常：${message || '请查看控制台日志'}`
  if (/timeout/i.test(message)) return '请求超时，请检查网络、ngrok 或后端服务'
  if (/Network Error/i.test(message)) return '无法连接后端，请确认服务已启动'
  if (/ngrok/i.test(message)) return 'ngrok 连接异常，请确认穿透地址仍然有效'
  return message || fallback
}

export function recoveryActions(scene = 'default') {
  return sceneRecoveryMap[scene] || sceneRecoveryMap.default
}

export async function confirmDanger(message: string, title = '确认操作') {
  await ElMessageBox.confirm(message, title, {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
    distinguishCancelAndClose: true
  })
}

export async function runSafeOperation<T>(options: SafeOperationOptions, action: () => Promise<T>): Promise<T | undefined> {
  const policy = { ...defaultRetryPolicy, ...(options.retry || {}) }
  if (options.confirm) {
    await confirmDanger(options.confirmMessage || '该操作会修改数据，是否继续？', options.confirmTitle || '确认操作')
  }
  let lastError: any
  for (let attempt = 1; attempt <= policy.maxAttempts; attempt++) {
    try {
      const result = await action()
      if (options.successMessage) ElMessage.success(options.successMessage)
      return result
    } catch (error: any) {
      lastError = error
      const status = error?.response?.status
      const retryableStatus = status && policy.retryOnStatuses.includes(status)
      const retryableNetwork = !status && policy.retryOnNetworkError
      if (attempt < policy.maxAttempts && (retryableStatus || retryableNetwork)) {
        await sleep(policy.retryDelayMs * attempt)
        continue
      }
      ElMessage.error(options.failureMessage || friendlyHttpError(error))
      return undefined
    }
  }
  ElMessage.error(options.failureMessage || friendlyHttpError(lastError))
  return undefined
}

export function sleep(ms: number) {
  return new Promise(resolve => window.setTimeout(resolve, ms))
}

export function safeArray<T = any>(value: any): T[] {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.records)) return value.records
  if (Array.isArray(value?.items)) return value.items
  if (Array.isArray(value?.list)) return value.list
  if (Array.isArray(value?.data)) return value.data
  return []
}

export function ensureNotEmpty<T>(items: T[], scene: string, label = '数据') {
  if (items.length) return ok(items, `${label}加载成功`)
  return warning(`${label}为空`, '当前接口返回空数组，请按提示检查数据源', items, recoveryActions(scene))
}

export function formatGuard(result: GuardResult) {
  const prefix = result.level === 'blocked' ? '阻止' : result.level === 'warning' ? '提醒' : '通过'
  return `${prefix}：${result.title}${result.message ? ` - ${result.message}` : ''}`
}

export function showGuard(result: GuardResult) {
  if (result.level === 'blocked') ElMessage.error(formatGuard(result))
  else if (result.level === 'warning') ElMessage.warning(formatGuard(result))
}

export function routeScene(path: string) {
  if (path.includes('agent')) return 'agent'
  if (path.includes('knowledge')) return 'knowledge'
  if (path.includes('content')) return 'content'
  if (path.includes('student')) return 'student'
  if (path.includes('school')) return 'school'
  if (path.includes('feishu')) return 'feishu'
  if (path.includes('settings')) return 'settings'
  return 'default'
}

export function buildOperationLog(scene: string, operation: string, status: string, detail = '') {
  return {
    scene,
    operation,
    status,
    detail,
    timestamp: Date.now(),
    timeText: new Date().toLocaleTimeString()
  }
}

export function createDebounced<T extends (...args: any[]) => void>(fn: T, delay = 300) {
  let timer: number | undefined
  return (...args: Parameters<T>) => {
    if (timer) window.clearTimeout(timer)
    timer = window.setTimeout(() => fn(...args), delay)
  }
}

export function createThrottled<T extends (...args: any[]) => void>(fn: T, delay = 500) {
  let last = 0
  return (...args: Parameters<T>) => {
    const now = Date.now()
    if (now - last < delay) return
    last = now
    fn(...args)
  }
}

export function clampText(value: string, max = 160) {
  if (!value) return ''
  const clean = value.replace(/\s+/g, ' ').trim()
  return clean.length > max ? `${clean.slice(0, Math.max(0, max - 3))}...` : clean
}

export function missingFieldHints(record: Record<string, any>, required: string[]) {
  return required.filter(key => {
    const value = record[key]
    return value === undefined || value === null || String(value).trim() === ''
  })
}

export function assertFields(record: Record<string, any>, required: string[], label = '表单'): GuardResult {
  const missing = missingFieldHints(record, required)
  if (!missing.length) return ok(label)
  return blocked(`${label}信息不完整`, `缺少字段：${missing.join('、')}`)
}

export function permissionHint(roleText: string, pageName: string) {
  return `当前角色「${roleText}」暂未开放「${pageName}」，请联系团队管理员在系统设置中调整角色权限。`
}

export function buildFallbackRows<T>(rows: T[], fallback: T[]) {
  return rows.length ? rows : fallback
}

export function parseApiData(response: any) {
  if (response?.data?.code && response.data.code !== 200) {
    throw new Error(response.data.message || '接口返回失败')
  }
  return response?.data?.data ?? response?.data ?? response
}

export function detectNgrokHtml(payload: any) {
  if (typeof payload !== 'string') return false
  return payload.includes('ngrok') && payload.includes('<html')
}

export function readablePermission(pattern: string) {
  const map: Record<string, string> = {
    '/api/agents/**': 'AI 工作台',
    '/api/knowledge/**': '知识库',
    '/api/content/**': '内容运营',
    '/api/students/**': '学员管理',
    '/api/schools/**': '院校情报',
    '/api/school-projects/**': '院校项目',
    '/api/analytics/**': '数据分析',
    '/api/feishu/**': '飞书同步',
    '/api/users/**': '系统设置'
  }
  return map[pattern] || pattern
}
