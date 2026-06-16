<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">飞书同步</h2>
        <p class="subtle">文档同步状态、多维表格、任务和群机器人的接入情况。</p>
      </div>
      <el-button type="primary" :loading="refreshing" @click="refresh">
        {{ refreshing ? '刷新中...' : '刷新状态' }}
      </el-button>
    </div>

    <!-- Status cards -->
    <div class="grid four">
      <div class="sync-card surface">
        <div class="sync-card-icon" style="background:#eef2ff;color:#5b6cff">
          <el-icon :size="20"><Document /></el-icon>
        </div>
        <div class="sync-card-body">
          <strong>文档同步</strong>
          <span class="sync-card-status">
            <el-tag :type="tagType(status.docs.status)" size="small" effect="plain">
              {{ statusLabel(status.docs.status) }}
            </el-tag>
          </span>
          <span class="sync-card-detail">
            共 {{ status.docs.count }} 篇 ·
            新增 {{ status.docs.added }} ·
            更新 {{ status.docs.updated }} ·
            跳过 {{ status.docs.skipped }}
          </span>
          <span class="sync-card-time" v-if="status.docs.lastSync">
            最近同步：{{ fmt(status.docs.lastSync) }}
          </span>
        </div>
      </div>

      <div class="sync-card surface">
        <div class="sync-card-icon" style="background:#e6f7ec;color:#19b37b">
          <el-icon :size="20"><Grid /></el-icon>
        </div>
        <div class="sync-card-body">
          <strong>多维表格</strong>
          <span class="sync-card-status">
            <el-tag :type="tagType(status.bitable.status)" size="small" effect="plain">
              {{ statusLabel(status.bitable.status) }}
            </el-tag>
          </span>
          <span class="sync-card-detail">-- 待开发</span>
        </div>
      </div>

      <div class="sync-card surface">
        <div class="sync-card-icon" style="background:#fef4e6;color:#f59e0b">
          <el-icon :size="20"><Tickets /></el-icon>
        </div>
        <div class="sync-card-body">
          <strong>任务同步</strong>
          <span class="sync-card-status">
            <el-tag :type="tagType(status.tasks.status)" size="small" effect="plain">
              {{ statusLabel(status.tasks.status) }}
            </el-tag>
          </span>
          <span class="sync-card-detail">-- 待开发</span>
        </div>
      </div>

      <div class="sync-card surface">
        <div class="sync-card-icon" style="background:#f3eeff;color:#8b5cf6">
          <el-icon :size="20"><Promotion /></el-icon>
        </div>
        <div class="sync-card-body">
          <strong>群机器人</strong>
          <span class="sync-card-status">
            <el-tag :type="tagType(status.bot.status)" size="small" effect="plain">
              {{ statusLabel(status.bot.status) }}
            </el-tag>
          </span>
          <span class="sync-card-detail">-- 待开发</span>
        </div>
      </div>
    </div>

    <!-- Sync logs table -->
    <div class="block surface" v-if="logs.length">
      <div class="block-head">
        <strong>同步日志</strong>
        <span class="block-sub">最近 50 条记录</span>
      </div>
      <el-table :data="logs" stripe size="small" class="sync-table">
        <el-table-column label="时间" width="170">
          <template #default="s">{{ fmt(s.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="s">
            <el-tag :type="syncTypeTag(s.row.syncType)" size="small" effect="plain">
              {{ syncTypeLabel(s.row.syncType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="s">
            <el-tag :type="tagType(s.row.status)" size="small">
              {{ statusLabel(s.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="详情" min-width="280">
          <template #default="s">
            <span class="log-msg">{{ s.row.message }}</span>
          </template>
        </el-table-column>
        <el-table-column label="新增" width="60" align="center">
          <template #default="s">{{ s.row.added }}</template>
        </el-table-column>
        <el-table-column label="更新" width="60" align="center">
          <template #default="s">{{ s.row.updated }}</template>
        </el-table-column>
        <el-table-column label="跳过" width="60" align="center">
          <template #default="s">{{ s.row.skipped }}</template>
        </el-table-column>
        <el-table-column label="错误" width="60" align="center">
          <template #default="s">
            <span :style="{ color: s.row.errors > 0 ? '#ef4444' : '#9ca3af' }">{{ s.row.errors }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-else class="empty-state">
      <el-empty description="暂无同步记录，请先执行同步" :image-size="80" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Document, Grid, Tickets, Promotion } from '@element-plus/icons-vue'
import { knowledgeSyncApi, type SyncLog, type SyncStatus, type SyncTypeStatus } from '@/api/knowledge-sync'
import { fmtBeijing } from '@/utils/datetime'

const logs = ref<SyncLog[]>([])
const refreshing = ref(false)

const status = ref<SyncStatus>({
  docs: emptyStatus(),
  bitable: emptyStatus(),
  tasks: emptyStatus(),
  bot: emptyStatus()
})

onMounted(() => refresh())

async function refresh() {
  refreshing.value = true
  try {
    const [s, l] = await Promise.all([
      knowledgeSyncApi.getStatus(),
      knowledgeSyncApi.getLogs()
    ])
    status.value = s
    logs.value = l
  } catch {
    // keep defaults
  } finally {
    refreshing.value = false
  }
}

function emptyStatus(): SyncTypeStatus {
  return { status: '--', lastSync: null, added: 0, updated: 0, skipped: 0, errors: 0, count: 0 }
}

function fmt(ts: string | null) {
  if (!ts) return '--'
  return fmtBeijing(ts)
}

function tagType(s: string): 'success' | 'warning' | 'danger' | 'info' {
  if (s === 'SUCCESS') return 'success'
  if (s === 'PARTIAL') return 'warning'
  if (s === 'FAILED') return 'danger'
  return 'info'
}

function statusLabel(s: string) {
  const map: Record<string, string> = { SUCCESS: '正常', PARTIAL: '部分成功', FAILED: '失败', '--': '待开发' }
  return map[s] || s
}

function syncTypeTag(t: string): 'primary' | 'success' | 'warning' | 'info' {
  const map: Record<string, string> = { docs: 'primary', bitable: 'success', tasks: 'warning', bot: 'info' }
  return (map[t] || 'info') as 'primary' | 'success' | 'warning' | 'info'
}

function syncTypeLabel(t: string) {
  const map: Record<string, string> = { docs: '文档', bitable: '多维表格', tasks: '任务', bot: '机器人' }
  return map[t] || t
}
</script>

<style scoped>
/* Sync status cards */
.sync-card {
  padding: 16px;
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.sync-card-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  flex: none;
}

.sync-card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.sync-card-body strong {
  font-size: 14px;
  color: #101828;
}

.sync-card-status {
  display: flex;
}

.sync-card-detail {
  font-size: 12px;
  color: #667085;
  line-height: 1.4;
}

.sync-card-time {
  font-size: 11px;
  color: #9ca3af;
}

/* Table */
.block {
  border: 1px solid #e7ebf3;
  border-radius: 8px;
  padding: 16px;
  margin-top: 16px;
}

.block-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 12px;
}

.block-head strong {
  font-size: 14px;
  color: #101828;
}

.block-sub {
  font-size: 12px;
  color: #9ca3af;
}

.log-msg {
  color: #475467;
  font-size: 13px;
}

.sync-table {
  font-size: 13px;
}

.empty-state {
  padding: 60px 0;
  text-align: center;
}
</style>
