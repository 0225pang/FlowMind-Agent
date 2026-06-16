/**
 * Knowledge API — synced Feishu documents with local MySQL cache.
 */
import http from './client'

export interface KnowledgeDoc {
  id: number
  feishuToken: string
  title: string
  content: string
  summary: string
  tags: string[]
  feishuUrl: string
  feishuType: string
  createdAt: string
  updatedAt: string
}

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const API_BASE = '/knowledge'

async function get<T>(path: string): Promise<T> {
  const response = await http.get<ApiResponse<T>>(path)
  return response.data.data
}

async function put<T>(path: string, body?: unknown): Promise<T> {
  const response = await http.put<ApiResponse<T>>(path, body)
  return response.data.data
}

async function post<T>(path: string, body?: unknown): Promise<T> {
  const response = await http.post<ApiResponse<T>>(path, body)
  return response.data.data
}

export const knowledgeApi = {
  async searchDocs(keyword?: string): Promise<KnowledgeDoc[]> {
    const params = keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''
    return get<KnowledgeDoc[]>(`${API_BASE}/docs${params}`)
  },

  async getDoc(id: number): Promise<KnowledgeDoc> {
    return get<KnowledgeDoc>(`${API_BASE}/docs/${id}`)
  },

  async updateTags(id: number, tags: string[]): Promise<KnowledgeDoc> {
    return put<KnowledgeDoc>(`${API_BASE}/docs/${id}/tags`, { tags })
  },

  async syncFromFeishu(): Promise<{ status: string; message: string; added: number; updated: number; errors: number }> {
    return post(`${API_BASE}/sync`)
  },

  async getStats(): Promise<{ docCount: number }> {
    return get<{ docCount: number }>(`${API_BASE}/stats`)
  }
}
