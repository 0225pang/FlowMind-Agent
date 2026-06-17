export interface AgentChatRequest {
  agentType: string
  message: string
  sessionId?: string
  context?: Record<string, unknown>
}

export interface AgentCard {
  title?: string
  content?: string
  [key: string]: unknown
}

export interface AgentTraceItem {
  name: string
  type: string
  status: 'used' | 'skipped' | 'failed' | string
  durationMs?: number
  summary?: string
  detail?: string
}

export interface AgentChatResponse {
  agentType: string
  reply: string
  sessionId?: string
  cards?: AgentCard[]
}

export interface ConversationItem {
  id: number
  agentType: string
  sessionId: string
  turnIndex: number
  role: string
  content: string
  createdAt: string
}

export interface SessionInfo {
  id: string
  title: string
  agentType: string
  createdAt: string
  updatedAt: string
}

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

function authHeaders() {
  const token = localStorage.getItem('flowmind-token') || 'mock-jwt.demo'
  return {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
}

const API_BASE = '/api/agents'

// ---- Session list (from MySQL sessions table) ----

export async function listSessions(): Promise<SessionInfo[]> {
  const response = await fetch(`${API_BASE}/sessions`, {
    headers: authHeaders()
  })
  if (!response.ok) return []
  const body = await response.json() as ApiResponse<SessionInfo[]>
  return body.data || []
}

export async function deleteSession(agentType: string, sessionId: string): Promise<void> {
  await fetch(`${API_BASE}/sessions/${sessionId}`, {
    method: 'DELETE',
    headers: authHeaders()
  })
}

// ---- Conversation history ----

export async function loadConversationHistory(agentType: string, sessionId: string): Promise<ConversationItem[]> {
  const response = await fetch(`${API_BASE}/conversations/${agentType}/${sessionId}`, {
    headers: authHeaders()
  })
  if (!response.ok) return []
  const body = await response.json() as ApiResponse<ConversationItem[]>
  return body.data || []
}

export async function clearConversationHistory(agentType: string, sessionId: string): Promise<void> {
  await fetch(`${API_BASE}/conversations/${agentType}/${sessionId}`, {
    method: 'DELETE',
    headers: authHeaders()
  })
}

export async function createNewSession(): Promise<string> {
  const response = await fetch(`${API_BASE}/conversations/new`, {
    method: 'POST',
    headers: authHeaders()
  })
  const body = await response.json() as ApiResponse<{ sessionId: string }>
  return body.data?.sessionId || ''
}

// ---- Chat API ----

export async function chatWithAgent(payload: AgentChatRequest) {
  const response = await fetch(`${API_BASE}/chat`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(payload)
  })
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
  const body = await response.json() as ApiResponse<AgentChatResponse>
  return body.data
}

export async function streamAgentChat(
  payload: AgentChatRequest,
  onDelta: (text: string) => void,
  onSession?: (sessionId: string) => void,
  onTrace?: (items: AgentTraceItem[]) => void,
  onReasoning?: (text: string) => void
) {
  const response = await fetch(`${API_BASE}/chat/stream`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(payload)
  })
  if (!response.ok || !response.body) throw new Error(`HTTP ${response.status}`)

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const frames = buffer.split('\n\n')
    buffer = frames.pop() || ''
    for (const frame of frames) {
      const event = parseSseFrame(frame)
      if (!event) continue
      if (event.event === 'session' && onSession && event.data?.sessionId) {
        onSession(event.data.sessionId)
      }
      if (event.event === 'delta') {
        const content = event.data.content
        if (typeof content === 'string' && content.length > 0 && content !== 'null') {
          onDelta(content)
        }
      }
      if (event.event === 'trace' && onTrace) {
        const items = Array.isArray(event.data?.items) ? event.data.items : []
        onTrace(items)
      }
      if (event.event === 'reasoning' && onReasoning) {
        const content = event.data?.content
        if (typeof content === 'string') onReasoning(content)
      }
      if (event.event === 'done' && event.data?.sessionId && onSession) {
        onSession(event.data.sessionId)
      }
      if (event.event === 'error') throw new Error(String(event.data.message || 'LLM stream failed'))
    }
  }
}

function parseSseFrame(frame: string) {
  const lines = frame.split(/\r?\n/)
  const event = lines.find(line => line.startsWith('event:'))?.slice(6).trim() || 'message'
  const dataLine = lines.find(line => line.startsWith('data:'))
  if (!dataLine) return undefined
  try {
    return { event, data: JSON.parse(dataLine.slice(5).trim()) }
  } catch {
    return { event, data: { content: dataLine.slice(5).trim() } }
  }
}
