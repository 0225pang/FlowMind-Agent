<template>
  <div class="login">
    <div class="panel">
      <div class="brand">
        <div class="mark">F</div>
        <h1>FlowMind Agent</h1>
        <p>基于角色权限的 AI 内容运营与知识管理智能体平台</p>
        <div class="role-hints">
          <button
            v-for="account in accounts"
            :key="account.username"
            type="button"
            :class="{ active: username === account.username }"
            @click="fill(account.username)"
          >
            <strong>{{ account.label }}</strong>
            <span>{{ account.username }} / 123456</span>
          </button>
        </div>
      </div>
      <el-form class="form" @submit.prevent>
        <div>
          <h2>登录工作空间</h2>
          <p>账号密码存储在 MySQL 中，当前 Demo 阶段按明文校验。</p>
        </div>
        <el-form-item label="账号">
          <el-input v-model="username" size="large" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="password"
            size="large"
            type="password"
            show-password
            autocomplete="current-password"
            @keyup.enter="login"
          />
        </el-form-item>
        <el-alert
          title="团队管理员：admin / 123456；学员用户：student / 123456"
          type="success"
          :closable="false"
        />
        <el-button type="primary" size="large" :loading="loading" @click="login">进入工作台</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const accounts = [
  { username: 'admin', label: '团队管理员' },
  { username: 'content', label: '内容运营人员' },
  { username: 'teacher', label: '教育咨询老师' },
  { username: 'ip', label: '个人IP运营者' },
  { username: 'student', label: '学员用户' }
]

const username = ref('admin')
const password = ref('123456')
const loading = ref(false)
const router = useRouter()
const auth = useAuthStore()

function fill(value: string) {
  username.value = value
  password.value = '123456'
}

async function login() {
  if (!username.value.trim()) return ElMessage.warning('请输入账号')
  if (!password.value.trim()) return ElMessage.warning('请输入密码')
  loading.value = true
  try {
    await auth.login(username.value.trim(), password.value)
    ElMessage.success(`欢迎回来，${auth.user.nickname}`)
    router.push(auth.firstAllowedRoute())
  } catch (error: any) {
    ElMessage.error(error?.message || '登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 12% 18%, rgba(25,179,123,.22), transparent 28%),
    radial-gradient(circle at 80% 8%, rgba(91,108,255,.26), transparent 32%),
    linear-gradient(135deg, #eef3ff, #f7fbff 48%, #f2fbf7);
}

.panel {
  width: min(980px, 92vw);
  display: grid;
  grid-template-columns: 1.08fr .92fr;
  background: rgba(255,255,255,.86);
  backdrop-filter: blur(22px);
  border: 1px solid #fff;
  border-radius: 8px;
  box-shadow: 0 30px 80px rgba(21,32,51,.14);
  overflow: hidden;
}

.brand {
  padding: 52px;
  background: linear-gradient(135deg, rgba(91,108,255,.96), rgba(139,92,246,.9));
  color: #fff;
}

.mark {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  background: rgba(255,255,255,.22);
  display: grid;
  place-items: center;
  font-size: 28px;
  font-weight: 800;
}

.brand h1 {
  font-size: 38px;
  margin: 28px 0 12px;
}

.brand p {
  font-size: 16px;
  line-height: 1.8;
  opacity: .9;
}

.role-hints {
  display: grid;
  gap: 10px;
  margin-top: 28px;
}

.role-hints button {
  border: 1px solid rgba(255,255,255,.26);
  background: rgba(255,255,255,.12);
  color: #fff;
  border-radius: 8px;
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
  transition: .2s ease;
}

.role-hints button:hover,
.role-hints button.active {
  transform: translateX(4px);
  background: rgba(255,255,255,.24);
}

.role-hints span {
  display: block;
  margin-top: 3px;
  font-size: 12px;
  opacity: .78;
}

.form {
  padding: 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form h2 {
  margin: 0 0 8px;
}

.form p {
  color: #667085;
  margin: 0 0 22px;
}

.form .el-button {
  width: 100%;
  margin-top: 18px;
  height: 44px;
}

@media (max-width: 760px) {
  .panel {
    grid-template-columns: 1fr;
  }

  .brand {
    padding: 32px;
  }
}
</style>
