<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="logo">
        <div class="logo-mark">F</div>
        <div>
          <strong>FlowMind</strong>
          <span>Agent Platform</span>
        </div>
      </div>
      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item v-for="item in nav" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <section class="main">
      <header class="topbar">
        <div>
          <h1>{{ $route.meta.title }}</h1>
          <span>保研内容运营工作空间</span>
        </div>
        <div class="top-actions">
          <el-tag effect="plain" type="success">API Ready</el-tag>
          <el-button circle aria-label="通知"><Bell /></el-button>
          <el-avatar :size="36">FM</el-avatar>
        </div>
      </header>

      <main :class="['content', { 'content-agent': $route.path === '/agent' }]">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
const nav = [
  { path: '/dashboard', label: 'Dashboard', icon: 'DataBoard' },
  { path: '/agent', label: 'AI 工作台', icon: 'ChatLineRound' },
  { path: '/knowledge', label: '知识库', icon: 'Collection' },
  { path: '/content', label: '内容运营', icon: 'EditPen' },
  { path: '/students', label: '学员管理', icon: 'User' },
  { path: '/schools', label: '院校情报', icon: 'School' },
  { path: '/analytics', label: '数据分析', icon: 'TrendCharts' },
  { path: '/feishu', label: '飞书同步', icon: 'Connection' },
  { path: '/settings', label: '系统设置', icon: 'Setting' }
]
</script>

<style scoped>
.layout {
  height: 100vh;
  display: flex;
  overflow: hidden;
}

.sidebar {
  width: 248px;
  flex: none;
  background: #fff;
  border-right: 1px solid #e7ebf3;
  padding: 18px 12px;
  height: 100vh;
  overflow: auto;
}

.logo {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 4px 10px 20px;
}

.logo-mark {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 800;
  background: linear-gradient(135deg, #5b6cff, #19b37b);
}

.logo span {
  display: block;
  color: #667085;
  font-size: 12px;
  margin-top: 2px;
}

.menu {
  border: 0;
}

.menu :deep(.el-menu-item) {
  border-radius: 8px;
  margin: 4px 0;
  height: 44px;
}

.menu :deep(.is-active) {
  background: linear-gradient(90deg, rgba(91,108,255,.14), rgba(25,179,123,.10));
  color: #4c5cff;
}

.main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 72px;
  flex: none;
  background: rgba(255,255,255,.82);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid #e7ebf3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 10;
}

.topbar h1 {
  font-size: 20px;
  margin: 0 0 4px;
}

.topbar span {
  font-size: 13px;
  color: #667085;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 22px;
  width: 100%;
  max-width: 1480px;
  margin: 0 auto;
}

.content-agent {
  max-width: none;
  margin: 0;
  overflow: hidden;
}

@media (max-width: 900px) {
  .sidebar {
    width: 78px;
  }

  .logo div:last-child,
  .menu span {
    display: none;
  }

  .content {
    padding: 14px;
  }

  .topbar {
    padding: 0 14px;
  }
}
</style>
