import type { AxiosRequestConfig } from 'axios'
import http from '@/api/client'
import {
  ensureNotEmpty,
  friendlyHttpError,
  parseApiData,
  recoveryActions,
  runSafeOperation,
  safeArray,
  type SafeOperationOptions
} from '@/utils/guardrails'
import { endpointByPath } from '@/utils/feature-parity'

export type SafeRequestState<T = any> = {
  loading: boolean
  failed: boolean
  empty: boolean
  data: T | null
  error: string
  updatedAt: number
  attempts: number
}

export type SafeListResult<T = any> = {
  rows: T[]
  empty: boolean
  warning: string
  recovery: string[]
}

export type RequestLog = {
  scene: string
  method: string
  path: string
  status: 'start' | 'success' | 'failed' | 'empty'
  message: string
  timestamp: number
  durationMs?: number
}

const logs: RequestLog[] = []
const memoryCache = new Map<string, { value: any; timestamp: number }>()

export function createState<T = any>(): SafeRequestState<T> {
  return {
    loading: false,
    failed: false,
    empty: false,
    data: null,
    error: '',
    updatedAt: 0,
    attempts: 0
  }
}

export async function safeGet<T = any>(path: string, config?: AxiosRequestConfig, options?: Partial<SafeOperationOptions>) {
  return safeRequest<T>('GET', path, undefined, config, options)
}

export async function safePost<T = any>(path: string, body?: any, config?: AxiosRequestConfig, options?: Partial<SafeOperationOptions>) {
  return safeRequest<T>('POST', path, body, config, options)
}

export async function safePut<T = any>(path: string, body?: any, config?: AxiosRequestConfig, options?: Partial<SafeOperationOptions>) {
  return safeRequest<T>('PUT', path, body, config, options)
}

export async function safeDelete<T = any>(path: string, config?: AxiosRequestConfig, options?: Partial<SafeOperationOptions>) {
  return safeRequest<T>('DELETE', path, undefined, config, options)
}

export async function safeRequest<T = any>(
  method: 'GET' | 'POST' | 'PUT' | 'DELETE',
  path: string,
  body?: any,
  config?: AxiosRequestConfig,
  options?: Partial<SafeOperationOptions>
) {
  const endpoint = endpointByPath(`/api${path}`) || endpointByPath(path)
  const scene = options?.scene || routeSceneFromPath(path)
  const operation = options?.operation || endpoint?.name || `${method} ${path}`
  return runSafeOperation<T>({
    scene,
    operation,
    confirm: endpoint?.mutating && method !== 'GET' ? options?.confirm : options?.confirm,
    successMessage: options?.successMessage,
    failureMessage: options?.failureMessage,
    retry: options?.retry
  }, async () => {
    const started = Date.now()
    pushLog({ scene, method, path, status: 'start', message: operation, timestamp: started })
    try {
      const response = method === 'GET'
        ? await http.get(path, config)
        : method === 'POST'
          ? await http.post(path, body, config)
          : method === 'PUT'
            ? await http.put(path, body, config)
            : await http.delete(path, config)
      const data = parseApiData(response)
      pushLog({ scene, method, path, status: 'success', message: '请求成功', timestamp: Date.now(), durationMs: Date.now() - started })
      cacheResponse(method, path, data)
      return data as T
    } catch (error) {
      pushLog({ scene, method, path, status: 'failed', message: friendlyHttpError(error), timestamp: Date.now(), durationMs: Date.now() - started })
      throw error
    }
  })
}

export async function safeList<T = any>(path: string, scene: string, label: string, config?: AxiosRequestConfig): Promise<SafeListResult<T>> {
  try {
    const data = await safeGet(path, config, { scene, operation: `加载${label}` })
    const rows = safeArray<T>(data)
    const guard = ensureNotEmpty(rows, scene, label)
    if (guard.level === 'warning') {
      pushLog({ scene, method: 'GET', path, status: 'empty', message: guard.message, timestamp: Date.now() })
      return { rows, empty: true, warning: guard.message, recovery: guard.suggestions }
    }
    return { rows, empty: false, warning: '', recovery: [] }
  } catch (error) {
    return { rows: [], empty: true, warning: friendlyHttpError(error), recovery: recoveryActions(scene) }
  }
}

export function bindState<T>(state: SafeRequestState<T>) {
  return {
    async run(path: string, loader: () => Promise<T | undefined>) {
      state.loading = true
      state.failed = false
      state.error = ''
      state.attempts += 1
      try {
        const data = await loader()
        state.data = data ?? null
        state.empty = Array.isArray(data) ? data.length === 0 : !data
        state.updatedAt = Date.now()
        return data
      } catch (error) {
        state.failed = true
        state.error = friendlyHttpError(error)
        state.updatedAt = Date.now()
        throw error
      } finally {
        state.loading = false
      }
    },
    reset() {
      state.loading = false
      state.failed = false
      state.empty = false
      state.data = null
      state.error = ''
      state.updatedAt = 0
      state.attempts = 0
    }
  }
}

export function cacheResponse(method: string, path: string, data: any) {
  if (method !== 'GET') return
  memoryCache.set(path, { value: data, timestamp: Date.now() })
  trimCache()
}

export function getCached<T = any>(path: string, maxAgeMs = 5 * 60 * 1000): T | undefined {
  const item = memoryCache.get(path)
  if (!item) return undefined
  if (Date.now() - item.timestamp > maxAgeMs) {
    memoryCache.delete(path)
    return undefined
  }
  return item.value as T
}

export function clearSafeCache() {
  memoryCache.clear()
}

export function pushLog(log: RequestLog) {
  logs.unshift(log)
  while (logs.length > 120) logs.pop()
}

export function latestLogs(count = 20) {
  return logs.slice(0, count)
}

export function clearLogs() {
  logs.splice(0, logs.length)
}

export function requestSummary() {
  const byStatus: Record<string, number> = {}
  const byScene: Record<string, number> = {}
  for (const log of logs) {
    byStatus[log.status] = (byStatus[log.status] || 0) + 1
    byScene[log.scene] = (byScene[log.scene] || 0) + 1
  }
  return {
    total: logs.length,
    byStatus,
    byScene,
    cacheSize: memoryCache.size
  }
}

export function routeSceneFromPath(path: string) {
  if (path.includes('/agents') || path.includes('/prompts')) return 'agent'
  if (path.includes('/knowledge')) return 'knowledge'
  if (path.includes('/content')) return 'content'
  if (path.includes('/students')) return 'student'
  if (path.includes('/schools') || path.includes('/school-projects')) return 'school'
  if (path.includes('/feishu')) return 'feishu'
  if (path.includes('/users') || path.includes('/roles') || path.includes('/permissions')) return 'settings'
  return 'default'
}

function trimCache() {
  while (memoryCache.size > 80) {
    const first = memoryCache.keys().next().value
    if (!first) break
    memoryCache.delete(first)
  }
}
