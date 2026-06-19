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

export interface MessageMetadata {
  version?: number
  agentType?: string
  traceItems?: AgentTraceItem[]
  thinking?: string
  thinkingHistory?: string[]
  modelThinking?: string
  createdAt?: string
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
  metadata?: string | MessageMetadata | null
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
  onThinking?: (text: string) => void,
  onReasoning?: (text: string) => void
) {
  await streamAgentChatWithXhr(payload, onDelta, onSession, onTrace, onThinking, onReasoning)
}

function streamAgentChatWithXhr(
  payload: AgentChatRequest,
  onDelta: (text: string) => void,
  onSession?: (sessionId: string) => void,
  onTrace?: (items: AgentTraceItem[]) => void,
  onThinking?: (text: string) => void,
  onReasoning?: (text: string) => void
) {
  return new Promise<void>((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    let cursor = 0
    let buffer = ''
    let settled = false

    const consume = (flush = false) => {
      const chunk = xhr.responseText.slice(cursor)
      cursor = xhr.responseText.length
      if (chunk) buffer += chunk
      const extracted = extractSseFrames(buffer, flush)
      buffer = extracted.rest
      for (const frame of extracted.frames) {
        handleSseEvent(frame, onDelta, onSession, onTrace, onThinking, onReasoning)
      }
    }

    xhr.open('POST', `${API_BASE}/chat/stream`, true)
    xhr.setRequestHeader('Authorization', authHeaders().Authorization)
    xhr.setRequestHeader('Content-Type', 'application/json')
    xhr.setRequestHeader('Accept', 'text/event-stream')
    xhr.setRequestHeader('Cache-Control', 'no-cache')
    xhr.overrideMimeType('text/event-stream; charset=utf-8')

    xhr.onprogress = () => {
      try {
        consume(false)
      } catch (err) {
        if (!settled) {
          settled = true
          xhr.abort()
          reject(err)
        }
      }
    }

    xhr.onload = () => {
      if (settled) return
      try {
        consume(true)
        settled = true
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve()
        } else {
          reject(new Error(`HTTP ${xhr.status}: ${xhr.responseText || xhr.statusText}`))
        }
      } catch (err) {
        reject(err)
      }
    }

    xhr.onerror = () => {
      if (settled) return
      settled = true
      reject(new Error('SSE network error'))
    }

    xhr.ontimeout = () => {
      if (settled) return
      settled = true
      reject(new Error('SSE request timeout'))
    }

    xhr.timeout = 300_000
    xhr.send(JSON.stringify(payload))
  })
}

function handleSseEvent(
  frame: string,
  onDelta: (text: string) => void,
  onSession?: (sessionId: string) => void,
  onTrace?: (items: AgentTraceItem[]) => void,
  onThinking?: (text: string) => void,
  onReasoning?: (text: string) => void
) {
  const event = parseSseFrame(frame)
  if (!event) return
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
  if (event.event === 'thinking' && onThinking) {
    const content = event.data?.content
    if (typeof content === 'string') onThinking(content)
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

function extractSseFrames(buffer: string, flush = false) {
  const normalized = buffer.replace(/\r\n/g, '\n')
  const frames: string[] = []
  let rest = normalized
  let index = rest.indexOf('\n\n')
  while (index >= 0) {
    const frame = rest.slice(0, index).trim()
    if (frame) frames.push(frame)
    rest = rest.slice(index + 2)
    index = rest.indexOf('\n\n')
  }
  if (flush && rest.trim()) {
    frames.push(rest.trim())
    rest = ''
  }
  return { frames, rest }
}

function parseSseFrame(frame: string) {
  const lines = frame.split(/\r?\n/)
  const event = lines.find(line => line.startsWith('event:'))?.slice(6).trim() || 'message'
  const dataLines = lines
    .filter(line => line.startsWith('data:'))
    .map(line => line.slice(5).trimStart())
  if (!dataLines.length) return undefined
  const dataText = dataLines.join('\n').trim()
  try {
    return { event, data: JSON.parse(dataText) }
  } catch {
    return { event, data: { content: dataText } }
  }
}
