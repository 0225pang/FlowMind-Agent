import { describe, it, expect } from 'vitest'
import { faker } from './test-data'
import type {
  AgentChatRequest, AgentChatResponse, AgentCard,
  SessionInfo, ConversationItem,
} from '@/api/agent'

/**
 * Mock data generators for tests.
 * Creates realistic test data without backend dependencies.
 */

export const testData = {
  /** Create a mock agent chat request */
  makeChatRequest(overrides: Partial<AgentChatRequest> = {}): AgentChatRequest {
    return {
      agentType: 'auto',
      message: '测试消息',
      sessionId: '',
      ...overrides
    }
  },

  /** Create a mock agent chat response */
  makeChatResponse(overrides: Partial<AgentChatResponse> = {}): AgentChatResponse {
    return {
      agentType: 'content',
      reply: '这是模拟回复内容，根据你的问题生成的答案。',
      sessionId: 'mock-session-id',
      cards: [],
      ...overrides
    }
  },

  /** Create a mock agent card */
  makeCard(overrides: Partial<AgentCard> = {}): AgentCard {
    return {
      title: '核心建议',
      content: '这是一个模拟的建议卡片内容。',
      ...overrides
    }
  },

  /** Create a mock session */
  makeSession(overrides: Partial<SessionInfo> = {}): SessionInfo {
    return {
      id: 'session-' + Date.now(),
      title: '测试会话',
      agentType: 'auto',
      createdAt: '2026-06-15T10:00:00',
      updatedAt: '2026-06-15T10:00:00',
      ...overrides
    }
  },

  /** Create a mock conversation item */
  makeConversation(overrides: Partial<ConversationItem> = {}): ConversationItem {
    return {
      id: Date.now(),
      agentType: 'auto',
      sessionId: 'session-1',
      turnIndex: 0,
      role: 'user',
      content: '测试问题',
      createdAt: '2026-06-15T10:00:00',
      ...overrides
    }
  },

  /** Create multiple conversation items simulating a chat history */
  makeChatHistory(count: number): ConversationItem[] {
    const items: ConversationItem[] = []
    for (let i = 0; i < count; i++) {
      items.push(this.makeConversation({
        id: i + 1,
        turnIndex: Math.floor(i / 2),
        role: i % 2 === 0 ? 'user' : 'assistant',
        content: i % 2 === 0 ? `用户问题 ${Math.floor(i / 2) + 1}` : `AI 回复 ${Math.floor(i / 2) + 1}`
      }))
    }
    return items
  },

  /** Create multiple sessions */
  makeSessions(count: number): SessionInfo[] {
    const sessions: SessionInfo[] = []
    for (let i = 0; i < count; i++) {
      sessions.push(this.makeSession({
        id: 'session-' + i,
        title: `会话 ${i + 1}`
      }))
    }
    return sessions
  }
}

describe('Test Data Helpers', () => {
  it('should create valid chat request', () => {
    const req = testData.makeChatRequest()
    expect(req.agentType).toBe('auto')
    expect(req.message).toBeTruthy()
    expect(req.sessionId).toBeDefined()
  })

  it('should create valid chat response', () => {
    const resp = testData.makeChatResponse()
    expect(resp.agentType).toBeTruthy()
    expect(resp.reply).toBeTruthy()
    expect(resp.sessionId).toBeTruthy()
  })

  it('should create valid session', () => {
    const session = testData.makeSession()
    expect(session.id).toBeTruthy()
    expect(session.title).toBeTruthy()
    expect(session.agentType).toBeTruthy()
  })

  it('should create chat history of correct length', () => {
    const history = testData.makeChatHistory(6)
    expect(history).toHaveLength(6)
    expect(history[0].role).toBe('user')
    expect(history[1].role).toBe('assistant')
    expect(history[2].role).toBe('user')
    expect(history[3].role).toBe('assistant')
  })

  it('should allow overrides', () => {
    const req = testData.makeChatRequest({
      agentType: 'feishu',
      message: '创建飞书文档'
    })
    expect(req.agentType).toBe('feishu')
    expect(req.message).toBe('创建飞书文档')
  })

  it('should create card with overrides', () => {
    const card = testData.makeCard({ title: '自定义标题' })
    expect(card.title).toBe('自定义标题')
  })
})

// Re-export for convenience
export const faker = testData
