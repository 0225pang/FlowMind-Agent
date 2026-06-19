<template>
  <div class="parity surface">
    <div class="parity-head">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ description }}</p>
      </div>
      <div class="summary">
        <el-statistic title="场景" :value="summary.scenes" />
        <el-statistic title="接口" :value="summary.endpoints" />
        <el-statistic title="角色" :value="summary.roles" />
      </div>
    </div>

    <el-collapse v-model="active">
      <el-collapse-item v-for="scene in scenes" :key="scene.route" :name="scene.route">
        <template #title>
          <div class="scene-title">
            <strong>{{ scene.title }}</strong>
            <span>{{ scene.subtitle }}</span>
          </div>
        </template>
        <div class="scene-body">
          <SafeStateView
            type="info"
            :title="scene.emptyTitle"
            :message="scene.emptyMessage"
            :suggestions="scene.recoveryActions"
          />
          <div class="endpoint-grid">
            <div v-for="endpoint in scene.endpoints" :key="endpoint.path + endpoint.name" class="endpoint">
              <div class="endpoint-top">
                <el-tag :type="endpoint.mutating ? 'warning' : endpoint.streaming ? 'success' : 'info'">
                  {{ endpoint.method }}
                </el-tag>
                <strong>{{ endpoint.name }}</strong>
              </div>
              <code>{{ endpoint.path }}</code>
              <p>{{ endpoint.description }}</p>
              <small>权限：{{ endpoint.permission }}</small>
            </div>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import SafeStateView from '@/components/SafeStateView.vue'
import { paritySummary, scenes } from '@/utils/feature-parity'

withDefaults(defineProps<{
  title?: string
  description?: string
}>(), {
  title: 'Web / 手机端能力一致性',
  description: '用于检查移动端和 Web 前端在场景、接口、权限、防呆提示上的覆盖情况。'
})

const summary = paritySummary()
const active = ref(['/agent'])
</script>

<style scoped>
.parity {
  padding: 16px;
}

.parity-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.parity h3 {
  margin: 0 0 6px;
}

.parity p {
  margin: 0;
  color: #667085;
}

.summary {
  display: flex;
  gap: 20px;
}

.scene-title {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.scene-title span {
  color: #667085;
  font-size: 12px;
}

.scene-body {
  display: grid;
  gap: 14px;
}

.endpoint-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}

.endpoint {
  border: 1px solid #e7ebf3;
  border-radius: 8px;
  padding: 12px;
  background: #fbfcff;
}

.endpoint-top {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.endpoint code {
  display: block;
  padding: 6px 8px;
  border-radius: 6px;
  background: #f2f4f8;
  color: #4c5cff;
  word-break: break-all;
}

.endpoint p {
  margin: 8px 0;
  line-height: 1.6;
}

.endpoint small {
  color: #98a2b3;
}
</style>
