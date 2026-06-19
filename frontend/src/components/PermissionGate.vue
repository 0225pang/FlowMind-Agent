<template>
  <slot v-if="allowed" />
  <SafeStateView
    v-else-if="mode === 'block'"
    type="forbidden"
    scene="settings"
    :title="title"
    :message="messageText"
    :suggestions="suggestions"
    backable
    @back="$router.push(fallback)"
  />
  <el-tooltip v-else :content="messageText" placement="top">
    <span class="permission-disabled">
      <slot />
    </span>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { deniedSceneMessage } from '@/utils/feature-parity'
import SafeStateView from '@/components/SafeStateView.vue'

const props = withDefaults(defineProps<{
  route?: string
  permission?: string
  mode?: 'block' | 'disable'
  title?: string
  message?: string
  fallback?: string
  suggestions?: string[]
}>(), {
  route: '',
  permission: '',
  mode: 'block',
  title: '暂无权限',
  message: '',
  fallback: '/agent',
  suggestions: () => ['联系团队管理员', '切换到有权限的账号', '返回可访问页面']
})

const auth = useAuthStore()
const allowed = computed(() => {
  if (auth.isAdmin) return true
  if (props.permission) return auth.user.permissions.includes(props.permission)
  if (props.route) return auth.canVisit(props.route)
  return false
})

const messageText = computed(() => {
  if (props.message) return props.message
  if (props.route) return deniedSceneMessage(props.route, auth.roleText)
  return `当前角色「${auth.roleText}」暂未开放该功能。`
})
</script>

<style scoped>
.permission-disabled {
  display: inline-flex;
  opacity: .48;
  cursor: not-allowed;
  filter: grayscale(.45);
  pointer-events: none;
}
</style>
