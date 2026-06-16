<template>
  <div :class="['msg', role]">
    <div class="avatar">{{ role === 'user' ? 'U' : 'AI' }}</div>
    <div class="bubble">
      <div class="content" v-html="renderedContent" />
      <span v-if="streaming" class="cursor" />
      <div v-if="cards?.length" class="cards">
        <div v-for="(card, index) in cards" :key="index" class="mini">
          <strong>{{ card.title || card.agent || card.name || '建议' }}</strong>
          <span>{{ card.content || card.sop || card.summary || card.description || JSON.stringify(card) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps<{
  role: 'user' | 'assistant'
  content: string
  cards?: Record<string, unknown>[]
  streaming?: boolean
}>()

const renderedContent = computed(() => renderMarkdown(props.content || (props.streaming ? '正在思考...' : '')))
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

.bubble {
  max-width: 76%;
  border: 1px solid #e7ebf3;
  border-radius: 8px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10px 30px rgba(21, 32, 51, 0.06);
}

.user .bubble {
  border: 0;
  background: linear-gradient(135deg, #5b6cff, #8b5cf6);
  color: #fff;
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
  0%, 45% {
    opacity: 1;
  }
  46%, 100% {
    opacity: 0;
  }
}

@media (max-width: 760px) {
  .bubble {
    max-width: 92%;
  }

  .cards {
    grid-template-columns: 1fr;
  }
}
</style>
