import { ElMessage } from 'element-plus'
import {
  assertFields,
  blocked,
  ok,
  validateBaseUrl,
  validateCopy,
  validateImageUrl,
  validateKeyword,
  validateLogin,
  validatePrompt,
  validateRating,
  validateStudentProfile,
  validateTitle,
  type GuardResult
} from '@/utils/guardrails'

export type FieldRule = {
  key: string
  label: string
  required?: boolean
  maxLength?: number
  minLength?: number
  type?: 'text' | 'url' | 'rating' | 'prompt' | 'copy' | 'keyword' | 'studentProfile' | 'baseUrl'
}

export type FormSafetyReport = {
  ok: boolean
  errors: GuardResult[]
  warnings: GuardResult[]
  normalized: Record<string, any>
}

export function validateForm(record: Record<string, any>, rules: FieldRule[]): FormSafetyReport {
  const errors: GuardResult[] = []
  const warnings: GuardResult[] = []
  const normalized: Record<string, any> = { ...record }

  for (const rule of rules) {
    const value = record[rule.key]
    const text = value === undefined || value === null ? '' : String(value)
    if (rule.required && !text.trim()) {
      errors.push(blocked(`${rule.label}不能为空`, `请填写${rule.label}`))
      continue
    }
    if (rule.maxLength && text.length > rule.maxLength) {
      errors.push(blocked(`${rule.label}过长`, `${rule.label}不能超过 ${rule.maxLength} 字`))
      continue
    }
    if (rule.minLength && text.trim().length < rule.minLength) {
      warnings.push(blocked(`${rule.label}偏短`, `${rule.label}建议至少 ${rule.minLength} 字`))
    }
    const result = validateByType(rule.type, text, value)
    if (result.level === 'blocked') errors.push(result)
    if (result.level === 'warning') warnings.push(result)
    if (result.value !== undefined) normalized[rule.key] = result.value
  }

  return {
    ok: errors.length === 0,
    errors,
    warnings,
    normalized
  }
}

export function validateByType(type: FieldRule['type'], text: string, raw: any): GuardResult<any> {
  if (!type || type === 'text') return ok(text)
  if (type === 'url') return validateImageUrl(text)
  if (type === 'rating') return validateRating(Number(raw))
  if (type === 'prompt') return validatePrompt(text)
  if (type === 'copy') return validateCopy(text)
  if (type === 'keyword') return validateKeyword(text)
  if (type === 'studentProfile') return validateStudentProfile(text)
  if (type === 'baseUrl') return validateBaseUrl(text)
  return ok(text)
}

export function showFormReport(report: FormSafetyReport) {
  if (report.errors.length) {
    ElMessage.error(report.errors[0].message || report.errors[0].title)
    return false
  }
  if (report.warnings.length) {
    ElMessage.warning(report.warnings[0].message || report.warnings[0].title)
  }
  return true
}

export function contentThemeRules(): FieldRule[] {
  return [
    { key: 'title', label: '主题标题', required: true, maxLength: 120, type: 'text' },
    { key: 'summary', label: '主题摘要', maxLength: 1000, type: 'text' },
    { key: 'channel', label: '渠道', required: true, maxLength: 32, type: 'text' },
    { key: 'status', label: '状态', required: true, maxLength: 32, type: 'text' },
    { key: 'rating', label: '评分', type: 'rating' }
  ]
}

export function draftRules(): FieldRule[] {
  return [
    { key: 'title', label: '文案标题', required: true, maxLength: 120, type: 'text' },
    { key: 'content', label: '文案正文', required: true, maxLength: 12000, type: 'copy' },
    { key: 'channel', label: '发布渠道', required: true, maxLength: 32, type: 'text' },
    { key: 'usageStatus', label: '使用状态', required: true, maxLength: 32, type: 'text' },
    { key: 'rating', label: '评分', type: 'rating' }
  ]
}

export function imageRules(): FieldRule[] {
  return [
    { key: 'imageUrl', label: '图片 URL', required: true, maxLength: 1000, type: 'url' }
  ]
}

export function schoolProfileRules(): FieldRule[] {
  return [
    { key: 'profile', label: '学生画像', required: true, minLength: 20, maxLength: 3000, type: 'studentProfile' }
  ]
}

export function loginRules(): FieldRule[] {
  return [
    { key: 'username', label: '账号', required: true, maxLength: 64, type: 'text' },
    { key: 'password', label: '密码', required: true, maxLength: 128, type: 'text' }
  ]
}

export function validateLoginForm(username: string, password: string) {
  const quick = validateLogin(username, password)
  if (quick.level === 'blocked') return { ok: false, errors: [quick], warnings: [], normalized: { username, password } }
  return validateForm({ username, password }, loginRules())
}

export function validateRequiredPayload(payload: Record<string, any>, fields: string[], label: string) {
  const result = assertFields(payload, fields, label)
  if (result.level === 'blocked') {
    return { ok: false, errors: [result], warnings: [], normalized: payload }
  }
  return { ok: true, errors: [], warnings: [], normalized: payload }
}

export function normalizeDraftPayload(payload: Record<string, any>) {
  const result = validateForm(payload, draftRules())
  return {
    ...result,
    normalized: {
      ...result.normalized,
      title: String(result.normalized.title || '').trim(),
      content: String(result.normalized.content || '').trim(),
      channel: result.normalized.channel || '朋友圈',
      usageStatus: result.normalized.usageStatus || '未使用'
    }
  }
}

export function normalizeThemePayload(payload: Record<string, any>) {
  const result = validateForm(payload, contentThemeRules())
  return {
    ...result,
    normalized: {
      ...result.normalized,
      title: String(result.normalized.title || '').trim(),
      summary: String(result.normalized.summary || '').trim(),
      channel: result.normalized.channel || '小红书',
      status: result.normalized.status || '待创作'
    }
  }
}

export function safeSubmit<T extends Record<string, any>>(
  payload: T,
  rules: FieldRule[],
  submitter: (normalized: T) => Promise<void>
) {
  return async () => {
    const report = validateForm(payload, rules)
    if (!showFormReport(report)) return
    await submitter(report.normalized as T)
  }
}
