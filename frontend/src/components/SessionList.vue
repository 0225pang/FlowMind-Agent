<template>
  <div class="session-panel surface">
    <div class="session-head">
      <strong>对话</strong>
      <el-button :icon="Plus" size="small" text @click="$emit('new')">新建</el-button>
    </div>
    <div class="session-list" v-if="sessions.length">
      <div
        v-for="s in sessions"
        :key="s.id"
        :class="['session-item', { active: activeId === s.id }]"
        @click="$emit('select', s.id)"
      >
        <span class="session-title">{{ s.title || '无标题' }}</span>
        <span class="session-time">{{ formatTime(s.time) }}</span>
        <el-button class="session-del" :icon="Close" size="small" text @click.stop="$emit('delete', s.id)" />
      </div>
    </div>
    <div v-else class="session-empty">
      暂无对话<br>点击「新建」开始
    </div>
  </div>
</template>

<script setup lang="ts">
import { Plus, Close } from '@element-plus/icons-vue'
import { relativeTime } from '@/utils/datetime'

defineProps<{
  sessions: Array<{ id: string; title: string; time: string }>
  activeId: string
}>()

defineEmits<{
  (e: 'select', id: string): void
  (e: 'new'): void
  (e: 'delete', id: string): void
}>()

function formatTime(ts: string) {
  if (!ts) return ''
  return relativeTime(ts)
}
</script>

<style scoped>
.session-panel {
  border: 1px solid #e7ebf3;
  border-radius: 8px;
  background: #fff;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.session-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #eef2f7;
  flex-shrink: 0;
}

.session-head strong {
  font-size: 14px;
  color: #101828;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
}

.session-item {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 4px;
  align-items: center;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background .15s;
}

.session-item:hover { background: #f4f6ff; }
.session-item.active { background: #eef2ff; }

.session-title {
  font-size: 13px;
  color: #101828;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 10px;
  color: #9ca3af;
  white-space: nowrap;
}

.session-del {
  opacity: 0;
  transition: opacity .15s;
}

.session-item:hover .session-del { opacity: 1; }

.session-empty {
  flex: 1;
  display: grid;
  place-items: center;
  color: #9ca3af;
  font-size: 12px;
  text-align: center;
  line-height: 1.6;
}
</style>
