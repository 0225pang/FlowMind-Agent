import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

const routes = [
  { path: '/login', component: () => import('@/views/LoginView.vue') },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: 'Dashboard' } },
      { path: 'agent', component: () => import('@/views/AgentWorkspaceView.vue'), meta: { title: 'AI 工作台' } },
      { path: 'knowledge', component: () => import('@/views/KnowledgeView.vue'), meta: { title: '知识库' } },
      { path: 'content', component: () => import('@/views/ContentView.vue'), meta: { title: '内容运营' } },
      { path: 'students', component: () => import('@/views/StudentsView.vue'), meta: { title: '学员管理' } },
      { path: 'schools', component: () => import('@/views/SchoolsView.vue'), meta: { title: '院校情报' } },
      { path: 'analytics', component: () => import('@/views/AnalyticsView.vue'), meta: { title: '数据分析' } },
      { path: 'feishu', component: () => import('@/views/FeishuView.vue'), meta: { title: '飞书同步' } },
      { path: 'settings', component: () => import('@/views/SettingsView.vue'), meta: { title: '系统设置' } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (to.path !== '/login' && !localStorage.getItem('flowmind-token')) return '/login'
})

export default router
