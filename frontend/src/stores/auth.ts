import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import http from '@/api/client'

export type FlowMindUser = {
  id?: number
  username: string
  nickname: string
  role: string
  roles: string[]
  permissions: string[]
  workspace?: string
}

const defaultUser: FlowMindUser = {
  username: '',
  nickname: '未登录用户',
  role: '',
  roles: [],
  permissions: []
}

const allRoutes = ['/dashboard', '/agent', '/knowledge', '/content', '/students', '/schools', '/analytics', '/feishu', '/settings']
const routeFallbackByPermission: Record<string, string[]> = {
  '/dashboard': ['/api/analytics/**'],
  '/agent': ['/api/agents/**'],
  '/knowledge': ['/api/knowledge/**'],
  '/content': ['/api/content/**'],
  '/students': ['/api/students/**'],
  '/schools': ['/api/schools/**', '/api/school-projects/**'],
  '/analytics': ['/api/analytics/**'],
  '/feishu': ['/api/feishu/**'],
  '/settings': ['/api/users/**']
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: loadUser(),
    hydrated: false
  }),
  getters: {
    token: () => localStorage.getItem('flowmind-token') || '',
    roleText: (state) => roleLabel(state.user.role),
    isAdmin: (state) => state.user.roles?.includes('TEAM_ADMIN') || state.user.role === 'TEAM_ADMIN' || state.user.role === 'ADMIN',
    allowedRoutes(): string[] {
      if (this.isAdmin) return allRoutes
      const permissions = this.user.permissions || []
      return allRoutes.filter(route => (routeFallbackByPermission[route] || []).some(pattern => permissions.includes(pattern)))
    }
  },
  actions: {
    async login(username: string, password = '123456') {
      const res = await http.post('/auth/login', { username, password })
      if (res.data?.code !== 200) throw new Error(res.data?.message || '登录失败')
      const data = res.data.data
      localStorage.setItem('flowmind-token', data.token)
      this.setUser(normalizeUser(data.user))
      this.hydrated = true
    },
    async hydrate(force = false) {
      if (this.hydrated && !force) return
      if (!localStorage.getItem('flowmind-token')) {
        this.user = defaultUser
        this.hydrated = true
        return
      }
      try {
        const res = await http.get('/users/me')
        if (res.data?.code === 200) this.setUser(normalizeUser(res.data.data))
      } catch (error: any) {
        const status = error?.response?.status
        if (status === 401) {
          this.logout(false)
          ElMessage.warning('登录已失效，请重新登录')
        }
      } finally {
        this.hydrated = true
      }
    },
    canVisit(path: string) {
      if (this.isAdmin) return true
      if (path === '/login') return true
      return this.allowedRoutes.includes(path)
    },
    firstAllowedRoute() {
      return this.allowedRoutes[0] || '/agent'
    },
    permissionReason(path: string) {
      if (this.canVisit(path)) return ''
      return `当前角色「${roleLabel(this.user.role)}」暂未开放该页面`
    },
    setUser(user: FlowMindUser) {
      this.user = user
      localStorage.setItem('flowmind-user', JSON.stringify(user))
    },
    logout(showTip = true) {
      localStorage.removeItem('flowmind-token')
      localStorage.removeItem('flowmind-user')
      this.user = defaultUser
      this.hydrated = false
      if (showTip) ElMessage.success('已退出登录')
    }
  }
})

function loadUser(): FlowMindUser {
  try {
    const raw = localStorage.getItem('flowmind-user')
    if (raw) return normalizeUser(JSON.parse(raw))
  } catch {}
  return defaultUser
}

function normalizeUser(input: any): FlowMindUser {
  const roles = Array.isArray(input?.roles) ? input.roles : input?.role ? [input.role] : []
  const permissions = Array.isArray(input?.permissions) ? input.permissions : []
  return {
    id: input?.id,
    username: input?.username || '',
    nickname: input?.nickname || input?.username || '未命名用户',
    role: input?.role || roles[0] || '',
    roles,
    permissions,
    workspace: input?.workspace
  }
}

function roleLabel(role: string) {
  const map: Record<string, string> = {
    CONTENT_OPERATOR: '内容运营人员',
    EDU_CONSULTANT: '教育咨询老师',
    IP_OPERATOR: '个人IP运营者',
    TEAM_ADMIN: '团队管理员',
    STUDENT_USER: '学员用户',
    ADMIN: '团队管理员'
  }
  return map[role] || role || '未分配角色'
}
