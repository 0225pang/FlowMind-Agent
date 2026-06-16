/**
 * Knowledge Sync API — sync logs, status
 */
import http from './client'

export interface SyncLog {
  id: number
  syncType: string
  status: string
  message: string
  added: number
  updated: number
  skipped: number
  errors: number
  createdAt: string
}

export interface SyncTypeStatus {
  status: string
  lastSync: string | null
  added: number
  updated: number
  skipped: number
  errors: number
  count: number
}

export interface SyncStatus {
  docs: SyncTypeStatus
  bitable: SyncTypeStatus
  tasks: SyncTypeStatus
  bot: SyncTypeStatus
}

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

async function get<T>(path: string): Promise<T> {
  const response = await http.get<ApiResponse<T>>(path)
  return response.data.data
}

export const knowledgeSyncApi = {
  async getLogs(): Promise<SyncLog[]> {
    return get<SyncLog[]>('/knowledge/sync-logs')
  },

  async getStatus(): Promise<SyncStatus> {
    return get<SyncStatus>('/knowledge/sync-status')
  }
}
