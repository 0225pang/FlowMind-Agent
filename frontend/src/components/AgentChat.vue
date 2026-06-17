<template>
  <div class="chat surface">
    <div ref="box" class="messages">
      <ChatMessage
        v-for="msg in messages"
        :key="msg.id"
        :role="msg.role"
        :content="msg.content"
        :cards="msg.cards"
        :streaming="msg.streaming"
        :trace-items="msg.traceItems"
        :thinking="msg.thinking"
        :thinking-history="msg.thinkingHistory"
        :model-thinking="msg.modelThinking"
      />
    </div>

    <div v-if="messages.length <= 1" class="prompts">
      <button v-for="p in prompts" :key="p" type="button" @click="send(p)">{{ p }}</button>
    </div>

    <div class="composer">
      <el-input
        v-model="input"
        class="composer-input"
        size="large"
        placeholder="直接输入任务，例如：根据保研知识库总结资料、创建飞书文档、生成小红书选题"
        :disabled="loading"
        @keyup.enter="send(input)"
      />
      <el-button type="primary" :loading="loading" @click="send(input)">发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import ChatMessage from './ChatMessage.vue'
import {
  chatWithAgent,
  loadConversationHistory,
  streamAgentChat,
  type AgentCard,
  type AgentTraceItem,
  type MessageMetadata
} from '@/api/agent'

const props = defineProps<{ agentType: string; sessionId: string }>()
const emit = defineEmits<{
  (e: 'session-created', id: string): void
  (e: 'message-sent'): void
}>()

interface ChatItem {
  id: number
  role: 'user' | 'assistant'
  content: string
  cards?: AgentCard[]
  streaming?: boolean
  traceItems?: AgentTraceItem[]
  thinking?: string
  thinkingHistory?: string[]
  modelThinking?: string
}

const input = ref('')
const loading = ref(false)
const box = ref<HTMLElement>()
const messages = ref<ChatItem[]>([])

const prompts = [
  '根据保研知识库，总结期末如何速成课程论文',
  '帮我生成 10 个保研小红书选题，并给出爆款结构',
  '联网搜索今天最新的保研通知，并说明来源可信度',
  '在保研知识库中创建一篇飞书文档',
  '分析学员01的申请风险并给出下一步动作'
]

async function loadHistory(sid: string, at: string) {
  if (!sid) {
    messages.value = [welcomeMessage('我是 FlowMind 总智能体。你可以直接输入任务，我会自动选择合适的 Agent，并展示工具调用过程。')]
    return
  }

  try {
    const items = await loadConversationHistory(at, sid)
    if (items.length > 0) {
      messages.value = items.map(item => {
        const metadata = parseMessageMetadata(item.metadata)
        return {
          id: item.id || Date.now() + Math.random() * 10000,
          role: item.role as 'user' | 'assistant',
          content: item.content,
          cards: [],
          traceItems: metadata.traceItems || [],
          thinking: metadata.thinking || last(metadata.thinkingHistory),
          thinkingHistory: metadata.thinkingHistory || [],
          modelThinking: metadata.modelThinking || ''
        }
      })
    } else {
      messages.value = [welcomeMessage('新会话已创建。直接输入你的任务，我会自动选择合适的 Agent。')]
    }
  } catch {
    messages.value = [welcomeMessage('新会话已创建。直接输入你的任务，我会自动选择合适的 Agent。')]
  }
}

onMounted(() => {
  loadHistory(props.sessionId, props.agentType)
})

watch([() => props.sessionId, () => props.agentType], ([sid, at]) => {
  loadHistory(sid, at)
})

async function send(text: string) {
  const prompt = text.trim()
  if (!prompt || loading.value) return

  messages.value.push({ id: Date.now(), role: 'user', content: prompt })
  input.value = ''
  loading.value = true

  const am: ChatItem = {
    id: Date.now() + 1,
    role: 'assistant',
    content: '',
    cards: [],
    streaming: true,
    traceItems: [],
    thinking: '正在准备...',
    thinkingHistory: ['正在准备...'],
    modelThinking: ''
  }
  messages.value.push(am)
  await scrollToBottom()

  const sid = props.sessionId
  let pendingSessionId: string | null = null

  try {
    await streamAgentChat({
      agentType: props.agentType || 'auto',
      message: prompt,
      sessionId: sid
    }, async delta => {
      am.content += delta
      await scrollToBottom()
    }, newSid => {
      if (newSid && newSid !== props.sessionId) {
        pendingSessionId = newSid
      }
    }, async traceItems => {
      am.traceItems = traceItems
      await scrollToBottom()
    }, async thinking => {
      am.thinking = thinking
      if (thinking && am.thinkingHistory?.[am.thinkingHistory.length - 1] !== thinking) {
        am.thinkingHistory = [...(am.thinkingHistory || []), thinking]
      }
      await scrollToBottom()
    }, async reasoning => {
      am.modelThinking = `${am.modelThinking || ''}${reasoning}`
      await scrollToBottom()
    })

    if (pendingSessionId && pendingSessionId !== props.sessionId) {
      emit('session-created', pendingSessionId)
    }

    if (!am.content.trim()) {
      const resp = await chatWithAgent({ agentType: props.agentType || 'auto', message: prompt, sessionId: sid })
      am.content = resp.reply
      am.cards = resp.cards || []
    }
  } catch (err: any) {
    am.content = `Error: ${err.message || 'unknown'}`
  } finally {
    am.streaming = false
    loading.value = false
    emit('message-sent')
    await scrollToBottom()
  }
}

async function scrollToBottom() {
  await nextTick()
  box.value?.scrollTo({ top: box.value.scrollHeight, behavior: 'smooth' })
}

function parseMessageMetadata(raw: unknown): MessageMetadata {
  if (!raw) return {}
  if (typeof raw === 'object') return raw as MessageMetadata
  if (typeof raw !== 'string' || !raw.trim()) return {}
  try {
    const parsed = JSON.parse(raw) as MessageMetadata
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function last(values?: string[]) {
  return values && values.length > 0 ? values[values.length - 1] : undefined
}

function welcomeMessage(content: string): ChatItem {
  return {
    id: Date.now(),
    role: 'assistant',
    content
  }
}

defineExpose({ send })
</script>

<style scoped>
.chat {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(91,108,255,.12);
  border-radius: 8px;
  background: radial-gradient(circle at 12% 0%,rgba(91,108,255,.10),transparent 28%),linear-gradient(180deg,#ffffff 0%,#fbfcff 100%);
}

.messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px;
  scroll-padding-bottom: 24px;
}

.prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 16px 10px;
  flex-shrink: 0;
}

.prompts button {
  min-height: 32px;
  border: 1px solid #dfe5f2;
  border-radius: 999px;
  padding: 0 12px;
  background: #fff;
  color: #475467;
  cursor: pointer;
  font-size: 12px;
  transition: border-color .16s, color .16s, transform .16s;
}

.prompts button:hover {
  border-color: #5b6cff;
  color: #5b6cff;
  transform: translateY(-1px);
}

.composer {
  display: flex;
  gap: 10px;
  border-top: 1px solid #e7ebf3;
  padding: 12px 16px;
  background: #fff;
  flex-shrink: 0;
}

.composer-input {
  flex: 1;
}

.composer .el-button {
  height: 40px;
  min-width: 88px;
}
</style>
