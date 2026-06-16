<template>
  <div class="history-panel" :class="{ empty: !items.length }">
    <div class="history-head">
      <strong>对话历史</strong>
      <el-tag v-if="items.length" size="small" type="info">{{ items.filter(i => i.role === 'user').length }} 轮</el-tag>
    </div>

    <div class="history-list" v-if="items.length">
      <div
        v-for="(item, index) in items"
        :key="index"
        class="history-item"
        :class="item.role"
      >
        <span class="role-tag">{{ item.role === 'user' ? 'U' : 'AI' }}</span>
        <span class="history-preview">{{ truncate(item.content, 60) }}</span>
      </div>
    </div>

    <div v-else class="history-empty">
      暂无对话记录
    </div>

    <div v-if="items.length" class="history-foot">
      <el-button size="small" text @click="$emit('clear')">清空历史</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{ role: string; content: string }>
  active: boolean
}>()

defineEmits<{
  (e: 'clear'): void
}>()

function truncate(s: string, n: number) {
  if (!s) return ''
  const t = s.replace(/\n/g, ' ').trim()
  return t.length > n ? t.slice(0, n) + '…' : t
}
</script>

<style scoped>
.history-panel {
  border: 1px solid #e7ebf3;
  border-radius: 8px;
  background: #fff;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.history-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-bottom: 1px solid #eef2f7;
  flex-shrink: 0;
}

.history-head strong {
  font-size: 14px;
  color: #101828;
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.history-item {
  display: flex;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 6px;
  margin-bottom: 4px;
  align-items: flex-start;
  font-size: 12px;
  line-height: 1.4;
  cursor: default;
}

.history-item:hover {
  background: #f4f6ff;
}

.role-tag {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  display: grid;
  place-items: center;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
  color: #fff;
}

.history-item.user .role-tag {
  background: #111827;
}

.history-item.assistant .role-tag {
  background: #5b6cff;
}

.history-preview {
  color: #475467;
  word-break: break-all;
}

.history-empty {
  flex: 1;
  display: grid;
  place-items: center;
  color: #9ca3af;
  font-size: 12px;
}

.history-foot {
  padding: 8px 14px;
  border-top: 1px solid #eef2f7;
  flex-shrink: 0;
  text-align: right;
}
</style>
