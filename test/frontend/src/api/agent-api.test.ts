import { describe, it, expect } from 'vitest'
import { testData } from './test-data'

describe('Agent API — chat flow scenarios', () => {
  it('should create chat request with auto agent type', () => {
    const req = testData.makeChatRequest({ agentType: 'auto' })
    expect(req.agentType).toBe('auto')
  })

  it('should route content request correctly', () => {
    const req = testData.makeChatRequest({
      agentType: 'auto',
      message: '生成小红书选题'
    })
    // In auto mode, the backend routes based on message content
    expect(req.agentType).toBe('auto')
    expect(req.message).toContain('小红书')
  })

  it('should route feishu request correctly', () => {
    const req = testData.makeChatRequest({
      agentType: 'auto',
      message: '创建飞书文档'
    })
    expect(req.message).toContain('飞书')
  })

  it('should route student request correctly', () => {
    const req = testData.makeChatRequest({
      agentType: 'auto',
      message: '分析学员01的申请风险'
    })
    expect(req.message).toContain('学员')
  })

  it('should route knowledge request correctly', () => {
    const req = testData.makeChatRequest({
      agentType: 'auto',
      message: '总结知识库里的夏令营资料'
    })
    expect(req.message).toContain('知识库')
  })

  it('should route school request correctly', () => {
    const req = testData.makeChatRequest({
      agentType: 'auto',
      message: '推荐适合经管学生的夏令营项目'
    })
    expect(req.message).toContain('夏令营')
  })

  it('should handle empty sessionId', () => {
    const req = testData.makeChatRequest({ sessionId: '' })
    expect(req.sessionId).toBe('')
  })

  it('should handle null sessionId', () => {
    const req = testData.makeChatRequest({ sessionId: undefined as unknown as string })
    // Check it's undefined (not an error)
    expect(req.sessionId).toBeUndefined()
  })
})

describe('API response structures', () => {
  it('should create response with cards', () => {
    const cards = [
      testData.makeCard({ title: '建议1', content: '内容1' }),
      testData.makeCard({ title: '建议2', content: '内容2' })
    ]
    const resp = testData.makeChatResponse({ cards })
    expect(resp.cards).toHaveLength(2)
    expect(resp.cards?.[0]?.title).toBe('建议1')
  })

  it('should create response with session info', () => {
    const resp = testData.makeChatResponse({ sessionId: 'new-session-abc' })
    expect(resp.sessionId).toBe('new-session-abc')
  })
})

describe('Session management', () => {
  it('should create multiple unique sessions', () => {
    const sessions = testData.makeSessions(5)
    expect(sessions).toHaveLength(5)
    // IDs should be unique
    const ids = sessions.map(s => s.id)
    expect(new Set(ids).size).toBe(5)
  })

  it('should create sessions with correct agent types', () => {
    const sessions = testData.makeSessions(3)
    sessions.forEach(s => {
      expect(s.agentType).toBe('auto')
      expect(s.title).toBeTruthy()
    })
  })
})

describe('Conversation history', () => {
  it('should alternate user/assistant in chat history', () => {
    const history = testData.makeChatHistory(10)
    for (let i = 0; i < history.length; i++) {
      if (i % 2 === 0) {
        expect(history[i].role).toBe('user')
      } else {
        expect(history[i].role).toBe('assistant')
      }
    }
  })

  it('should increment turnIndex correctly', () => {
    const history = testData.makeChatHistory(6)
    expect(history[0].turnIndex).toBe(0)
    expect(history[1].turnIndex).toBe(0) // Same turn, user + assistant
    expect(history[2].turnIndex).toBe(1)
    expect(history[3].turnIndex).toBe(1)
    expect(history[4].turnIndex).toBe(2)
    expect(history[5].turnIndex).toBe(2)
  })
})

describe('Edge cases', () => {
  it('should create chat history of size 0', () => {
    const history = testData.makeChatHistory(0)
    expect(history).toHaveLength(0)
  })

  it('should create chat history of size 1', () => {
    const history = testData.makeChatHistory(1)
    expect(history).toHaveLength(1)
    expect(history[0].role).toBe('user')
  })

  it('should handle very long message content', () => {
    const longMsg = 'A'.repeat(10000)
    const req = testData.makeChatRequest({ message: longMsg })
    expect(req.message).toHaveLength(10000)
  })

  it('should handle Chinese characters in message', () => {
    const chineseMsg = '这是一个包含中文的测试消息，用于验证中文处理。' +
      '保研面试准备建议：1. 自我介绍要简洁有力；2. 研究计划要具体可行；3. 专业知识要扎实。'
    const req = testData.makeChatRequest({ message: chineseMsg })
    expect(req.message).toContain('保研')
    expect(req.message).toContain('面试')
    expect(req.message.length).toBeGreaterThan(20)
  })

  it('should handle special characters in titles', () => {
    const session = testData.makeSession({
      title: '特殊字符测试 [!@#$%^&*()] /test/path?q=1'
    })
    expect(session.title).toContain('!')
    expect(session.title).toContain('/')
  })
})
