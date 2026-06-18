<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">系统设置</h2>
        <p class="subtle">模型、Prompt、飞书应用、角色权限与日志配置。</p>
      </div>
      <el-tag>{{ auth.roleText }}</el-tag>
    </div>

    <el-tabs class="surface tabs">
      <el-tab-pane label="AI 模型">
        <el-form label-width="120px">
          <el-form-item label="Provider">
            <el-select model-value="deepseek">
              <el-option value="mock" label="MockLLMClient" />
              <el-option value="deepseek" label="DeepSeek OpenAI Compatible API" />
              <el-option value="openai" label="OpenAI Compatible API" />
            </el-select>
          </el-form-item>
          <el-form-item label="API Key">
            <el-input placeholder="请放在 application-local.yml 或环境变量中" disabled />
          </el-form-item>
          <el-alert
            title="防呆提示：不要把真实 API Key 写入 Git。后端会优先读取本地配置或环境变量。"
            type="warning"
            :closable="false"
          />
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="Prompt 模板">
        <el-table :data="prompts">
          <el-table-column prop="agent" label="Agent" />
          <el-table-column prop="name" label="模板" />
          <el-table-column prop="text" label="内容" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="飞书应用">
        <el-form label-width="120px">
          <el-form-item label="App ID"><el-input placeholder="cli_xxx" /></el-form-item>
          <el-form-item label="App Secret"><el-input type="password" /></el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="角色权限">
        <div class="rbac-head">
          <div>
            <h3>角色权限配置</h3>
            <p>权限存储在 MySQL 的 sys_role / sys_permission / sys_role_permission 表中，修改后下一次请求立即生效。</p>
          </div>
          <el-button :disabled="!auth.isAdmin" :loading="loadingRbac" @click="loadRbac">刷新权限</el-button>
        </div>

        <el-alert
          v-if="!auth.isAdmin"
          title="当前账号不是团队管理员，只能查看基础设置，不能修改角色权限。"
          type="info"
          :closable="false"
          show-icon
        />

        <el-skeleton v-if="loadingRbac" :rows="6" animated />
        <div v-else class="role-grid">
          <div v-for="role in roles" :key="role.roleCode" class="role-card">
            <div class="role-title">
              <div>
                <strong>{{ role.roleName }}</strong>
                <span>{{ role.roleCode }}</span>
              </div>
              <el-tag :type="role.roleCode === 'STUDENT_USER' ? 'warning' : 'success'">
                {{ selectedCodes(role).length }} 项权限
              </el-tag>
            </div>
            <el-checkbox-group v-model="role.selected" :disabled="!auth.isAdmin || role.roleCode === 'TEAM_ADMIN'">
              <el-checkbox
                v-for="permission in permissions"
                :key="permission.permissionCode"
                :label="permission.permissionCode"
              >
                <span class="perm-name">{{ permission.permissionName }}</span>
                <small>{{ permission.pathPattern }}</small>
              </el-checkbox>
            </el-checkbox-group>
            <div class="role-actions">
              <el-button
                type="primary"
                :disabled="!auth.isAdmin || role.roleCode === 'TEAM_ADMIN'"
                @click="saveRole(role)"
              >
                保存该角色
              </el-button>
              <el-tooltip content="团队管理员拥有全部权限，为避免锁死后台，Demo 阶段不允许清空。">
                <el-button v-if="role.roleCode === 'TEAM_ADMIN'" disabled>受保护</el-button>
              </el-tooltip>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="系统日志">
        <el-timeline>
          <el-timeline-item timestamp="2026-06-14">ContentAgent mock response generated</el-timeline-item>
          <el-timeline-item timestamp="2026-06-14">Feishu mock sync completed</el-timeline-item>
          <el-timeline-item timestamp="2026-06-19">RBAC roles initialized in MySQL</el-timeline-item>
        </el-timeline>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '@/api/client'
import { useAuthStore } from '@/stores/auth'

type Permission = {
  permissionCode: string
  permissionName: string
  pathPattern: string
  frontendRoute?: string
}

type RoleRow = {
  roleCode: string
  roleName: string
  permissions: Permission[]
  selected: string[]
}

const auth = useAuthStore()
const loadingRbac = ref(false)
const roles = ref<RoleRow[]>([])
const permissions = ref<Permission[]>([])
const prompts = [
  { agent: 'ContentAgent', name: '小红书选题', text: '围绕主题生成教育服务选题' },
  { agent: 'StudentAgent', name: '学员画像', text: '根据 GPA、排名、英语成绩判断风险' }
]

onMounted(() => {
  if (auth.isAdmin) loadRbac()
})

function selectedCodes(role: RoleRow) {
  return role.selected || []
}

async function loadRbac() {
  if (!auth.isAdmin) return
  loadingRbac.value = true
  try {
    const [roleRes, permRes] = await Promise.all([http.get('/roles'), http.get('/permissions')])
    permissions.value = permRes.data.data || []
    roles.value = (roleRes.data.data || []).map((role: any) => ({
      ...role,
      selected: (role.permissions || []).map((p: Permission) => p.permissionCode)
    }))
  } finally {
    loadingRbac.value = false
  }
}

async function saveRole(role: RoleRow) {
  if (!auth.isAdmin) return ElMessage.warning('只有团队管理员可以修改权限')
  if (role.roleCode === 'TEAM_ADMIN') return ElMessage.warning('团队管理员权限受保护，不能在前端清空')
  if (!role.selected.length) return ElMessage.warning('至少保留一个权限，避免该角色完全无法使用系统')
  await http.put(`/roles/${role.roleCode}/permissions`, { permissionCodes: role.selected })
  ElMessage.success(`${role.roleName} 权限已保存`)
  await loadRbac()
}
</script>

<style scoped>
.tabs {
  padding: 16px;
}

.rbac-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.rbac-head h3 {
  margin: 0 0 6px;
}

.rbac-head p {
  margin: 0;
  color: #667085;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.role-card {
  border: 1px solid #e7ebf3;
  border-radius: 8px;
  padding: 14px;
  background: #fbfcff;
}

.role-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.role-title span {
  display: block;
  color: #98a2b3;
  font-size: 12px;
  margin-top: 3px;
}

.role-card :deep(.el-checkbox) {
  display: flex;
  height: auto;
  margin-right: 0;
  margin-bottom: 8px;
  white-space: normal;
}

.perm-name {
  font-weight: 600;
}

.role-card small {
  display: block;
  color: #98a2b3;
  margin-top: 2px;
}

.role-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

@media (max-width: 980px) {
  .role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
