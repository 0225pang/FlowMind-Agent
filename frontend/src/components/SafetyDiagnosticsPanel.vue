<template>
  <div class="diagnostics surface">
    <div class="diagnostics-head">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ description }}</p>
      </div>
      <div class="actions">
        <el-button @click="refresh">刷新诊断</el-button>
        <el-button @click="clear">清空日志</el-button>
      </div>
    </div>

    <div class="stats">
      <StatCard label="请求日志" :value="String(summary.total)" trend="最近操作" icon="Tickets" color="#5b6cff" />
      <StatCard label="缓存项" :value="String(summary.cacheSize)" trend="GET 结果缓存" icon="FolderOpened" color="#19b37b" />
      <StatCard label="场景数" :value="String(parity.scenes)" trend="Web/手机一致" icon="Grid" color="#8b5cf6" />
      <StatCard label="接口数" :value="String(parity.endpoints)" trend="能力覆盖" icon="Connection" color="#f59e0b" />
    </div>

    <OperationChecklist
      title="防呆覆盖检查"
      description="用于答辩说明：系统如何避免误操作、空数据和权限错误。"
      :items="checkItems"
    />

    <el-table :data="logs" class="log-table" height="320">
      <el-table-column prop="timeText" label="时间" width="100" />
      <el-table-column prop="scene" label="场景" width="110" />
      <el-table-column prop="method" label="方法" width="80" />
      <el-table-column prop="path" label="接口" min-width="220" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="tagType(row.status)" effect="plain">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="说明" min-width="180" show-overflow-tooltip />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import StatCard from '@/components/StatCard.vue'
import OperationChecklist from '@/components/OperationChecklist.vue'
import { latestLogs, requestSummary, clearLogs } from '@/api/safe-request'
import { paritySummary } from '@/utils/feature-parity'

withDefaults(defineProps<{
  title?: string
  description?: string
}>(), {
  title: '安全与防呆诊断',
  description: '统一展示前端请求、缓存、权限和移动端一致性检查结果。'
})

const tick = ref(0)
const parity = paritySummary()
const summary = computed(() => {
  tick.value
  return requestSummary()
})
const logs = computed(() => {
  tick.value
  return latestLogs(50).map(log => ({
    ...log,
    timeText: new Date(log.timestamp).toLocaleTimeString()
  }))
})
const checkItems = computed(() => [
  { title: '登录防呆', description: '账号密码为空、token 失效、角色未分配都有提示', done: true },
  { title: '权限防呆', description: '无权限菜单置灰，路由守卫拦截，后端返回 403', done: true },
  { title: '请求防呆', description: '统一处理 401/403/404/500/timeout/ngrok 错误', done: true },
  { title: '内容防呆', description: '标题、正文、评分、图片 URL 均有校验工具', done: true },
  { title: 'AI 防呆', description: 'Prompt 为空、过长、疑似密钥会提示', done: true },
  { title: '移动端一致性', description: '手机端已建立功能目录、页面蓝图和离线兜底', done: true },
  { title: '表单接入', description: '部分页面仍可继续把 safeSubmit 接入具体表单', warning: true },
  { title: '缓存展示', description: '后续可在设置页显示最近缓存内容', warning: true }
])

function refresh() {
  tick.value++
}

function clear() {
  clearLogs()
  tick.value++
}

function tagType(status: string) {
  if (status === 'success') return 'success'
  if (status === 'failed') return 'danger'
  if (status === 'empty') return 'warning'
  return 'info'
}
</script>

<style scoped>
.diagnostics {
  padding: 16px;
}

.diagnostics-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.diagnostics h3 {
  margin: 0 0 6px;
}

.diagnostics p {
  margin: 0;
  color: #667085;
}

.actions {
  display: flex;
  gap: 8px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.log-table {
  margin-top: 14px;
}

@media (max-width: 980px) {
  .stats {
    grid-template-columns: 1fr 1fr;
  }

  .diagnostics-head {
    flex-direction: column;
  }
}
</style>
