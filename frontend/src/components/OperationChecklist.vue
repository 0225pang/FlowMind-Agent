<template>
  <div class="checklist surface">
    <div class="checklist-head">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ description }}</p>
      </div>
      <el-progress
        type="circle"
        :width="54"
        :percentage="percent"
        :status="percent === 100 ? 'success' : undefined"
      />
    </div>

    <div class="checklist-items">
      <div
        v-for="(item, index) in normalizedItems"
        :key="item.key"
        :class="['checklist-item', { done: item.done, warning: item.warning }]"
      >
        <div class="index">{{ index + 1 }}</div>
        <div class="content">
          <strong>{{ item.title }}</strong>
          <span>{{ item.description }}</span>
        </div>
        <el-tag :type="item.done ? 'success' : item.warning ? 'warning' : 'info'" effect="plain">
          {{ item.done ? '完成' : item.warning ? '需确认' : '待处理' }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type ChecklistItem = {
  key?: string
  title: string
  description?: string
  done?: boolean
  warning?: boolean
}

const props = withDefaults(defineProps<{
  title?: string
  description?: string
  items: ChecklistItem[]
}>(), {
  title: '操作检查',
  description: '为避免误操作，请确认以下检查项。',
  items: () => []
})

const normalizedItems = computed(() => props.items.map((item, index) => ({
  key: item.key || `${index}-${item.title}`,
  title: item.title,
  description: item.description || '无补充说明',
  done: Boolean(item.done),
  warning: Boolean(item.warning)
})))

const percent = computed(() => {
  if (!normalizedItems.value.length) return 0
  const done = normalizedItems.value.filter(item => item.done).length
  return Math.round(done * 100 / normalizedItems.value.length)
})
</script>

<style scoped>
.checklist {
  padding: 16px;
}

.checklist-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.checklist h3 {
  margin: 0 0 6px;
}

.checklist p {
  margin: 0;
  color: #667085;
  font-size: 13px;
}

.checklist-items {
  display: grid;
  gap: 10px;
}

.checklist-item {
  display: grid;
  grid-template-columns: 34px 1fr auto;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid #e7ebf3;
  border-radius: 8px;
  background: #fff;
}

.checklist-item.done {
  background: #f2fbf7;
  border-color: rgba(25, 179, 123, .28);
}

.checklist-item.warning {
  background: #fffaf0;
  border-color: rgba(245, 158, 11, .34);
}

.index {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: #eef3ff;
  color: #5b6cff;
  font-weight: 760;
}

.content {
  min-width: 0;
}

.content strong {
  display: block;
  color: #152033;
}

.content span {
  display: block;
  color: #667085;
  font-size: 12px;
  margin-top: 2px;
}
</style>
