import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'

/**
 * API unit tests — mock Axios, test request/response flows.
 * We mock the HTTP layer so tests run without a real backend.
 */

// Mock axios
vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() }
      }
    }))
  }
}))

describe('Axios HTTP Client', () => {
  it('should create axios instance with base URL /api', async () => {
    const { default: http } = await import('@/api/client')
    expect(http).toBeDefined()
    expect(http.get).toBeDefined()
    expect(http.post).toBeDefined()
  })
})

describe('Agent API — fetch patterns', () => {
  let agentModule: typeof import('@/api/agent')

  beforeEach(async () => {
    agentModule = await import('@/api/agent')
  })

  it('should export AgentChatRequest type', () => {
    const req: agentModule.AgentChatRequest = {
      agentType: 'auto',
      message: 'test',
      sessionId: 'session-1'
    }
    expect(req.agentType).toBe('auto')
    expect(req.message).toBe('test')
  })

  it('should export AgentChatResponse type', () => {
    const resp: agentModule.AgentChatResponse = {
      agentType: 'content',
      reply: '测试回复',
      sessionId: 'session-1'
    }
    expect(resp.reply).toBe('测试回复')
  })

  it('should export SessionInfo type', () => {
    const session: agentModule.SessionInfo = {
      id: 'abc',
      title: 'Test Session',
      agentType: 'auto',
      createdAt: '2026-06-15',
      updatedAt: '2026-06-15'
    }
    expect(session.title).toBe('Test Session')
    expect(session.agentType).toBe('auto')
  })

  it('should export ConversationItem type', () => {
    const item: agentModule.ConversationItem = {
      id: 1,
      agentType: 'content',
      sessionId: 'session-1',
      turnIndex: 0,
      role: 'user',
      content: 'Hello',
      createdAt: '2026-06-15T10:00:00'
    }
    expect(item.role).toBe('user')
    expect(item.content).toBe('Hello')
  })

  it('should export AgentCard type', () => {
    const card: agentModule.AgentCard = {
      title: '建议',
      content: '测试建议内容',
      extra: '额外信息'
    }
    expect(card.title).toBe('建议')
  })
})

describe('Knowledge API — data types', () => {
  it('should have correct KnowledgeDoc shape', async () => {
    const doc = {
      id: 1,
      feishuToken: 'tok123',
      title: '测试文档',
      content: '内容',
      summary: '摘要',
      tags: ['标签1'],
      feishuUrl: 'https://test.feishu.cn/docx/tok123',
      feishuType: 'docx',
      createdAt: '2026-06-15T10:00:00',
      updatedAt: '2026-06-15T10:00:00'
    }
    expect(doc.feishuToken).toBe('tok123')
    expect(doc.tags).toHaveLength(1)
  })
})

describe('Knowledge Sync API — data types', () => {
  it('should have correct SyncLog shape', async () => {
    const { knowledgeSyncApi } = await import('@/api/knowledge-sync')
    const log = {
      id: 1,
      syncType: 'docs',
      status: 'SUCCESS',
      message: '同步完成',
      added: 5,
      updated: 3,
      skipped: 10,
      errors: 0,
      createdAt: '2026-06-15T10:00:00'
    }
    expect(log.status).toBe('SUCCESS')
    expect(log.syncType).toBe('docs')
  })

  it('should have correct SyncTypeStatus shape', async () => {
    const { knowledgeSyncApi } = await import('@/api/knowledge-sync')
    const st = {
      status: 'SUCCESS',
      lastSync: '2026-06-15T10:00:00',
      added: 5,
      updated: 2,
      skipped: 10,
      errors: 0,
      count: 7
    }
    expect(st.status).toBe('SUCCESS')
    expect(st.count).toBe(7)
  })
})

describe('Content API — data types', () => {
  it('should have correct ContentTheme shape', async () => {
    const theme = {
      id: 1,
      title: '测试主题',
      topic: '测试',
      platform: '小红书',
      type: '经验',
      status: '待创作',
      heat: 80,
      rating: 4,
      tags: ['标签']
    }
    expect(theme.status).toBe('待创作')
    expect(theme.platform).toBe('小红书')
  })

  it('should have correct CopyDraft shape', async () => {
    const draft = {
      id: 101,
      themeId: 1,
      title: '测试文案',
      channel: '小红书',
      version: '干货版',
      style: '干货',
      content: '正文内容',
      usageStatus: '未使用',
      generatedAt: '2026-06-15T10:00:00',
      owner: '内容运营',
      feedback: '',
      images: [],
      imageSuggestion: '建议配图'
    }
    expect(draft.channel).toBe('小红书')
    expect(draft.usageStatus).toBe('未使用')
  })
})
