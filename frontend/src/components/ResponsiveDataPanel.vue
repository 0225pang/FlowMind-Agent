<template>
  <section class="data-panel surface">
    <header class="data-head">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ description }}</p>
      </div>
      <div class="data-actions">
        <el-tag v-if="rows.length" effect="plain">{{ rows.length }} 条</el-tag>
        <el-button v-if="refreshable" :loading="loading" @click="$emit('refresh')">刷新</el-button>
      </div>
    </header>

    <SafeStateView v-if="loading" type="loading" :scene="scene" title="正在加载" :message="loadingText" />
    <SafeStateView
      v-else-if="error"
      type="error"
      :scene="scene"
      :message="error"
      retryable
      @retry="$emit('refresh')"
    />
    <SafeStateView
      v-else-if="!rows.length"
      type="empty"
      :scene="scene"
      :title="emptyTitle"
      :message="emptyMessage"
      retryable
      @retry="$emit('refresh')"
    />

    <div v-else class="data-grid" :style="{ gridTemplateColumns }">
      <article
        v-for="(row, index) in rows"
        :key="rowKey(row, index)"
        class="data-card hoverable"
        @click="$emit('open', row)"
      >
        <div class="card-top">
          <strong>{{ pick(row, titleKeys) || `记录 ${index + 1}` }}</strong>
          <el-tag size="small" effect="plain">{{ pick(row, statusKeys) || label }}</el-tag>
        </div>
        <p>{{ clamp(pick(row, bodyKeys), 180) || '暂无摘要' }}</p>
        <div class="meta">
          <span v-for="item in meta(row)" :key="item">{{ item }}</span>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import SafeStateView from '@/components/SafeStateView.vue'
import { clampText } from '@/utils/guardrails'

const props = withDefaults(defineProps<{
  title: string
  description?: string
  rows: any[]
  loading?: boolean
  error?: string
  scene?: string
  label?: string
  refreshable?: boolean
  minCardWidth?: number
  titleKeys?: string[]
  bodyKeys?: string[]
  statusKeys?: string[]
  metaKeys?: string[]
  emptyTitle?: string
  emptyMessage?: string
  loadingText?: string
}>(), {
  description: '',
  rows: () => [],
  loading: false,
  error: '',
  scene: 'default',
  label: '详情',
  refreshable: true,
  minCardWidth: 260,
  titleKeys: () => ['title', 'name', 'projectName', 'schoolName'],
  bodyKeys: () => ['summary', 'content', 'description', 'requirements', 'chunkText'],
  statusKeys: () => ['status', 'usageStatus', 'channel', 'projectType', 'source'],
  metaKeys: () => ['publishDate', 'deadline', 'updatedAt', 'source', 'rating'],
  emptyTitle: '暂无数据',
  emptyMessage: '当前没有可展示的数据。',
  loadingText: '正在请求后端服务，请稍候。'
})

defineEmits<{
  refresh: []
  open: [row: any]
}>()

const gridTemplateColumns = computed(() => `repeat(auto-fit, minmax(${props.minCardWidth}px, 1fr))`)

function rowKey(row: any, index: number) {
  return row?.id || row?.uuid || row?.token || index
}

function pick(row: any, keys: string[]) {
  if (!row) return ''
  for (const key of keys) {
    const value = row[key]
    if (value !== undefined && value !== null && String(value).trim()) return String(value)
  }
  return ''
}

function meta(row: any) {
  return props.metaKeys
    .map(key => {
      const value = row?.[key]
      if (value === undefined || value === null || String(value).trim() === '') return ''
      return `${labelOf(key)} ${value}`
    })
    .filter(Boolean)
    .slice(0, 4)
}

function labelOf(key: string) {
  const map: Record<string, string> = {
    publishDate: '发布',
    deadline: '截止',
    updatedAt: '更新',
    source: '来源',
    rating: '评分'
  }
  return map[key] || key
}

function clamp(value: string, max: number) {
  return clampText(value, max)
}
</script>

<style scoped>
.data-panel {
  padding: 16px;
}

.data-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.data-head h3 {
  margin: 0 0 6px;
}

.data-head p {
  margin: 0;
  color: #667085;
}

.data-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.data-grid {
  display: grid;
  gap: 14px;
}

.data-card {
  padding: 14px;
  border: 1px solid #e7ebf3;
  border-radius: 8px;
  background: linear-gradient(135deg, #fff, #fbfcff);
  cursor: pointer;
}

.card-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.card-top strong {
  color: #152033;
  line-height: 1.5;
}

.data-card p {
  color: #667085;
  line-height: 1.7;
  margin: 10px 0;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: #8b95a7;
  font-size: 12px;
}
</style>
