<template>
  <div :class="['safe-state', `safe-state-${type}`]">
    <div class="safe-icon">
      <el-icon v-if="type === 'loading'"><Loading /></el-icon>
      <el-icon v-else-if="type === 'error'"><WarningFilled /></el-icon>
      <el-icon v-else-if="type === 'forbidden'"><Lock /></el-icon>
      <el-icon v-else-if="type === 'success'"><CircleCheckFilled /></el-icon>
      <el-icon v-else><InfoFilled /></el-icon>
    </div>
    <div class="safe-body">
      <div class="safe-title">{{ titleText }}</div>
      <p v-if="messageText">{{ messageText }}</p>
      <div v-if="actions.length" class="safe-actions">
        <el-tag v-for="item in actions" :key="item" effect="plain">{{ item }}</el-tag>
      </div>
      <div v-if="$slots.default" class="safe-extra">
        <slot />
      </div>
      <div v-if="showButtons" class="safe-buttons">
        <el-button v-if="retryable" type="primary" @click="$emit('retry')">重试</el-button>
        <el-button v-if="backable" @click="$emit('back')">返回</el-button>
        <el-button v-if="clearable" @click="$emit('clear')">清空</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { recoveryActions } from '@/utils/guardrails'

const props = withDefaults(defineProps<{
  type?: 'empty' | 'loading' | 'error' | 'forbidden' | 'success' | 'info'
  title?: string
  message?: string
  scene?: string
  suggestions?: string[]
  retryable?: boolean
  backable?: boolean
  clearable?: boolean
}>(), {
  type: 'empty',
  title: '',
  message: '',
  scene: 'default',
  suggestions: () => [],
  retryable: false,
  backable: false,
  clearable: false
})

defineEmits<{
  retry: []
  back: []
  clear: []
}>()

const titleText = computed(() => props.title || defaultTitle(props.type))
const messageText = computed(() => props.message || defaultMessage(props.type))
const actions = computed(() => props.suggestions.length ? props.suggestions : recoveryActions(props.scene))
const showButtons = computed(() => props.retryable || props.backable || props.clearable)

function defaultTitle(type: string) {
  const map: Record<string, string> = {
    loading: '正在加载',
    error: '请求失败',
    forbidden: '暂无权限',
    success: '操作成功',
    info: '提示',
    empty: '暂无数据'
  }
  return map[type] || '提示'
}

function defaultMessage(type: string) {
  const map: Record<string, string> = {
    loading: '正在请求后端服务，请稍候。',
    error: '当前操作没有成功，可以按下面的建议排查。',
    forbidden: '当前角色暂未开放这个功能，请联系团队管理员。',
    success: '操作已完成。',
    info: '请根据提示继续操作。',
    empty: '当前没有可展示的数据。'
  }
  return map[type] || ''
}
</script>

<style scoped>
.safe-state {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 18px;
  border: 1px solid #e7ebf3;
  border-radius: 8px;
  background: linear-gradient(135deg, #fff, #f8fbff);
  box-shadow: 0 12px 30px rgba(21, 32, 51, .06);
}

.safe-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  flex: none;
  border-radius: 8px;
  background: #eef3ff;
  color: #5b6cff;
  font-size: 20px;
}

.safe-state-loading .safe-icon {
  color: #5b6cff;
}

.safe-state-error .safe-icon {
  background: #fff1f1;
  color: #ef4444;
}

.safe-state-forbidden .safe-icon {
  background: #fff7ed;
  color: #f59e0b;
}

.safe-state-success .safe-icon {
  background: #edfdf5;
  color: #19b37b;
}

.safe-body {
  min-width: 0;
  flex: 1;
}

.safe-title {
  font-weight: 760;
  color: #152033;
  margin-bottom: 4px;
}

.safe-body p {
  margin: 0;
  color: #667085;
  line-height: 1.7;
}

.safe-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.safe-extra {
  margin-top: 12px;
}

.safe-buttons {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}
</style>
