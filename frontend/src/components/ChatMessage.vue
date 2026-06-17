<template>
  <div :class="['msg', role]">
    <div class="avatar">{{ role === 'user' ? 'U' : 'AI' }}</div>
    <div class="msg-main">
      <div v-if="role === 'assistant' && thinking" class="thinking-line">
        <span class="pulse" :class="{ done: !streaming }" />
        <span>{{ thinking }}</span>
      </div>
      <div v-if="role === 'assistant' && modelThinkingText && streaming" class="model-thinking-line">
        <span>Thinking:</span>
        <em>{{ modelThinkingPreview }}</em>
      </div>

      <div class="bubble">
        <details v-if="role === 'assistant' && modelThinkingText && !streaming" class="model-thinking-panel">
          <summary>模型 Thinking</summary>
          <pre>{{ modelThinkingText }}</pre>
        </details>

        <details v-if="role === 'assistant' && hasThinkingHistory && !streaming" class="process-panel">
          <summary>处理过程</summary>
          <ol>
            <li v-for="(line, index) in normalizedThinkingHistory" :key="`${index}-${line}`">{{ line }}</li>
          </ol>
        </details>

        <details v-if="role === 'assistant' && hasTrace" class="trace-panel">
          <summary>工具调用过程</summary>
          <div class="trace-list">
            <div v-for="(item, index) in usedTraceItems" :key="index" class="trace-item">
              <div class="trace-head">
                <span class="dot" :class="item.status" />
                <strong>{{ item.name }}</strong>
                <em>{{ item.type }}</em>
                <small>{{ item.durationMs ?? 0 }}ms</small>
              </div>
              <p>{{ item.summary || statusText(item.status) }}</p>
              <details v-if="item.detail" class="trace-detail">
                <summary>查看查到的结果</summary>
                <pre>{{ item.detail }}</pre>
              </details>
            </div>
          </div>
        </details>

        <div class="content" v-html="renderedContent" />
        <span v-if="streaming" class="cursor" />
        <div v-if="cards?.length" class="cards">
          <div v-for="(card, index) in visibleCards" :key="index" class="mini">
            <strong>{{ card.title || card.agent || card.name || '建议' }}</strong>
            <span>{{ card.content || card.sop || card.summary || card.description || JSON.stringify(card) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'
import type { AgentTraceItem } from '@/api/agent'

const props = defineProps<{
  role: 'user' | 'assistant'
  content: string
  cards?: Record<string, unknown>[]
  streaming?: boolean
  traceItems?: AgentTraceItem[]
  thinking?: string
  thinkingHistory?: string[]
  modelThinking?: string
}>()

const renderedContent = computed(() => renderMarkdown(props.content || (props.streaming ? '正在生成...' : '')))
const usedTraceItems = computed(() => (props.traceItems || []).filter(item => item.status !== 'skipped'))
const hasTrace = computed(() => usedTraceItems.value.length > 0)
const visibleCards = computed(() => (props.cards || []).filter(card => card.type !== 'trace' && card.type !== 'thinking'))
const normalizedThinkingHistory = computed(() => {
  const seen = new Set<string>()
  return (props.thinkingHistory || [])
    .map(line => String(line || '').trim())
    .filter(line => {
      if (!line || seen.has(line)) return false
      seen.add(line)
      return true
    })
})
const hasThinkingHistory = computed(() => normalizedThinkingHistory.value.length > 0)
const modelThinkingText = computed(() => String(props.modelThinking || '').trim())
const modelThinkingPreview = computed(() => {
  const text = modelThinkingText.value.replace(/\s+/g, ' ')
  return text.length <= 180 ? text : `${text.slice(Math.max(0, text.length - 180))}`
})

function statusText(status: string) {
  if (status === 'used') return '已调用并返回上下文。'
  if (status === 'failed') return '调用失败。'
  return '未触发。'
}
</script>

<style scoped>
.msg {
  display: flex;
  gap: 10px;
  margin: 16px 0;
  align-items: flex-start;
}

.msg.user {
  flex-direction: row-reverse;
}

.msg-main {
  max-width: min(76%, 920px);
  min-width: 0;
}

.user .msg-main {
  display: flex;
  justify-content: flex-end;
}

.avatar {
  width: 34px;
  height: 34px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #eef2ff;
  color: #5b6cff;
  font-size: 12px;
  font-weight: 800;
}

.user .avatar {
  background: #111827;
  color: #fff;
}

.thinking-line {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0 0 7px 2px;
  color: #98a2b3;
  font-size: 12px;
  line-height: 1.4;
}

.model-thinking-line {
  display: flex;
  align-items: center;
  gap: 7px;
  max-width: min(100%, 920px);
  margin: -2px 0 7px 2px;
  color: #a3acbd;
  font-size: 12px;
  line-height: 1.45;
}

.model-thinking-line span {
  flex: none;
  color: #8b94a7;
  font-weight: 700;
}

.model-thinking-line em {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-style: normal;
}

.pulse {
  width: 7px;
  height: 7px;
  flex: none;
  border-radius: 50%;
  background: #8b5cf6;
  box-shadow: 0 0 0 0 rgba(139, 92, 246, .35);
  animation: pulse 1.2s infinite;
}

.pulse.done {
  background: #12b76a;
  animation: none;
  box-shadow: none;
}

.bubble {
  width: 100%;
  border: 1px solid #e7ebf3;
  border-radius: 8px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10px 30px rgba(21, 32, 51, 0.06);
  overflow-wrap: anywhere;
}

.user .bubble {
  border: 0;
  background: linear-gradient(135deg, #5b6cff, #8b5cf6);
  color: #fff;
}

.model-thinking-panel,
.process-panel,
.trace-panel {
  max-height: 320px;
  overflow: auto;
  margin-bottom: 12px;
  border: 1px solid #dfe5f2;
  border-radius: 8px;
  background: #f8faff;
}

.model-thinking-panel summary,
.process-panel summary,
.trace-panel summary {
  cursor: pointer;
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 9px 11px;
  background: #f8faff;
  color: #344054;
  font-size: 12px;
  font-weight: 700;
  user-select: none;
}

.model-thinking-panel pre {
  margin: 0 10px 10px;
  max-height: 220px;
  overflow: auto;
  white-space: pre-wrap;
  border-radius: 8px;
  padding: 10px;
  background: #ffffff;
  color: #667085;
  font-size: 12px;
  line-height: 1.65;
}

.process-panel ol {
  margin: 0;
  padding: 0 12px 11px 30px;
  color: #667085;
  font-size: 12px;
  line-height: 1.65;
}

.process-panel li {
  margin: 4px 0;
}

.trace-list {
  display: grid;
  gap: 8px;
  padding: 0 10px 10px;
}

.trace-item {
  border: 1px solid #e7ebf3;
  border-radius: 8px;
  padding: 9px;
  background: #fff;
}

.trace-head {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #101828;
  font-size: 12px;
}

.trace-head em {
  color: #667085;
  font-style: normal;
}

.trace-head small {
  margin-left: auto;
  color: #98a2b3;
}

.dot {
  width: 8px;
  height: 8px;
  flex: none;
  border-radius: 50%;
  background: #cbd5e1;
}

.dot.used {
  background: #12b76a;
}

.dot.failed {
  background: #f04438;
}

.trace-item p {
  margin: 6px 0 0;
  color: #475467;
  font-size: 12px;
  line-height: 1.55;
}

.trace-detail {
  margin-top: 7px;
}

.trace-detail summary {
  position: static;
  padding: 0;
  color: #5b6cff;
  font-weight: 600;
}

.trace-detail pre {
  margin: 8px 0 0;
  max-height: 180px;
  overflow: auto;
  white-space: pre-wrap;
  border-radius: 8px;
  padding: 10px;
  background: #101828;
  color: #e2e8f0;
  font-size: 12px;
  line-height: 1.55;
}

.content {
  line-height: 1.75;
}

.content :deep(p) {
  margin: 0 0 8px;
}

.content :deep(p:last-child),
.content :deep(ul:last-child),
.content :deep(ol:last-child) {
  margin-bottom: 0;
}

.content :deep(h2),
.content :deep(h3),
.content :deep(h4),
.content :deep(h5) {
  margin: 10px 0 8px;
  color: #101828;
  font-size: 15px;
  line-height: 1.5;
}

.content :deep(ul),
.content :deep(ol) {
  margin: 6px 0 10px;
  padding-left: 22px;
}

.content :deep(li) {
  margin: 4px 0;
}

.content :deep(strong) {
  color: #101828;
  font-weight: 700;
}

.content :deep(code) {
  border-radius: 5px;
  padding: 2px 5px;
  background: #eef2ff;
  color: #4f46e5;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
}

.content :deep(a) {
  color: #5b6cff;
  text-decoration: none;
}

.content :deep(a:hover) {
  text-decoration: underline;
}

.content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 10px 0;
  font-size: 13px;
}

.content :deep(th),
.content :deep(td) {
  border: 1px solid #dfe5f2;
  padding: 8px 12px;
  text-align: left;
}

.content :deep(th) {
  background: #f4f6ff;
  color: #101828;
  font-weight: 700;
}

.content :deep(td) {
  color: #344054;
}

.content :deep(pre) {
  margin: 10px 0;
  border-radius: 8px;
  padding: 14px 16px;
  background: #1e293b;
  color: #e2e8f0;
  overflow-x: auto;
  font-size: 12px;
  line-height: 1.65;
}

.content :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
  font-size: inherit;
}

.content :deep(blockquote) {
  margin: 10px 0;
  border-left: 3px solid #5b6cff;
  padding: 6px 14px;
  background: #f4f6ff;
  color: #475467;
}

.content :deep(blockquote p) {
  margin: 0;
}

.content :deep(hr) {
  margin: 16px 0;
  border: 0;
  border-top: 1px solid #e7ebf3;
}

.cursor {
  display: inline-block;
  width: 7px;
  height: 16px;
  margin-left: 2px;
  border-radius: 99px;
  background: #5b6cff;
  vertical-align: -2px;
  animation: blink 0.9s infinite;
}

.cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.mini {
  border: 1px solid #e7ebf3;
  border-radius: 8px;
  padding: 10px;
  background: #f6f8fc;
}

.mini strong,
.mini span {
  display: block;
}

.mini strong {
  color: #101828;
  font-size: 13px;
}

.mini span {
  margin-top: 5px;
  color: #667085;
  font-size: 12px;
  line-height: 1.55;
}

@keyframes blink {
  0%, 45% { opacity: 1; }
  46%, 100% { opacity: 0; }
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(139, 92, 246, .35); }
  70% { box-shadow: 0 0 0 7px rgba(139, 92, 246, 0); }
  100% { box-shadow: 0 0 0 0 rgba(139, 92, 246, 0); }
}

@media (max-width: 760px) {
  .msg-main {
    max-width: 92%;
  }

  .cards {
    grid-template-columns: 1fr;
  }
}
</style>
