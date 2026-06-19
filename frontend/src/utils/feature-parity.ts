export type FeatureEndpoint = {
  name: string
  method: 'GET' | 'POST' | 'PUT' | 'DELETE'
  path: string
  permission: string
  description: string
  streaming?: boolean
  mutating?: boolean
}

export type FeatureScene = {
  route: string
  title: string
  subtitle: string
  permission: string
  emptyTitle: string
  emptyMessage: string
  recoveryActions: string[]
  endpoints: FeatureEndpoint[]
}

export const permissions = {
  agent: '/api/agents/**',
  prompt: '/api/prompts/**',
  knowledge: '/api/knowledge/**',
  content: '/api/content/**',
  students: '/api/students/**',
  schools: '/api/schools/**',
  schoolProjects: '/api/school-projects/**',
  analytics: '/api/analytics/**',
  feishu: '/api/feishu/**',
  users: '/api/users/**',
  roles: '/api/roles/**',
  permissionView: '/api/permissions/**'
}

export const scenes: FeatureScene[] = [
  {
    route: '/agent',
    title: 'AI 工作台',
    subtitle: '总智能体自动选择知识库、飞书、内容或院校能力。',
    permission: permissions.agent,
    emptyTitle: '还没有对话',
    emptyMessage: '可以从快捷指令开始，也可以直接输入你的任务。',
    recoveryActions: ['检查后端 Base URL', '确认 DeepSeek 或 MockLLM 可用', '优先尝试知识库检索类问题'],
    endpoints: [
      { name: '流式对话', method: 'POST', path: '/api/agents/chat/stream', permission: permissions.agent, description: 'SSE 流式回复、工具调用和 Thinking 展示', streaming: true, mutating: true },
      { name: '智能体清单', method: 'GET', path: '/api/agents', permission: permissions.agent, description: '读取当前启用的 Agent 能力' },
      { name: 'Prompt 模板', method: 'GET', path: '/api/prompts', permission: permissions.prompt, description: '读取 Prompt 模板' }
    ]
  },
  {
    route: '/knowledge',
    title: '知识库',
    subtitle: '文档、标签、向量检索、飞书同步状态聚合展示。',
    permission: permissions.knowledge,
    emptyTitle: '暂无知识文档',
    emptyMessage: '知识库为空时，建议先在飞书同步页触发同步，再回到这里检索。',
    recoveryActions: ['检查 Weaviate 是否启动', '检查飞书知识库 folderToken', '尝试使用更短的关键词'],
    endpoints: [
      { name: '知识库统计', method: 'GET', path: '/api/knowledge/stats', permission: permissions.knowledge, description: '读取文档数、标签数等指标' },
      { name: '文档列表', method: 'GET', path: '/api/knowledge/docs', permission: permissions.knowledge, description: '知识库文档卡片数据' },
      { name: '向量检索', method: 'GET', path: '/api/knowledge/vector/search', permission: permissions.knowledge, description: '按关键词检索 Weaviate 知识片段' },
      { name: '知识库同步', method: 'POST', path: '/api/knowledge/sync', permission: permissions.knowledge, description: '触发飞书文档同步到本地知识库', mutating: true }
    ]
  },
  {
    route: '/content',
    title: '内容运营',
    subtitle: '主题库、文案库、日历、评分、图片引用和历史文案。',
    permission: permissions.content,
    emptyTitle: '暂无内容资产',
    emptyMessage: '可以在 AI 工作台生成选题，或在内容页手动登记主题和文案。',
    recoveryActions: ['检查当前角色是否有内容权限', '刷新主题库', '确认 MySQL 内容表已初始化'],
    endpoints: [
      { name: '主题库', method: 'GET', path: '/api/content/themes', permission: permissions.content, description: '展示主题卡片' },
      { name: '新增主题', method: 'POST', path: '/api/content/themes', permission: permissions.content, description: '手动创建主题', mutating: true },
      { name: '主题评分', method: 'PUT', path: '/api/content/themes/{id}/rating', permission: permissions.content, description: '五星评分', mutating: true },
      { name: '历史文案', method: 'GET', path: '/api/content/themes/{themeId}/drafts', permission: permissions.content, description: '查看主题下历史文案' },
      { name: '文案库', method: 'GET', path: '/api/content/drafts', permission: permissions.content, description: '展示所有文案' },
      { name: '编辑文案', method: 'PUT', path: '/api/content/drafts/{draftId}', permission: permissions.content, description: '编辑文案标题和正文', mutating: true },
      { name: '文案图片', method: 'POST', path: '/api/content/drafts/{draftId}/images', permission: permissions.content, description: '登记图片 URL 或配图建议', mutating: true },
      { name: '内容日历', method: 'GET', path: '/api/content/calendar', permission: permissions.content, description: '按月展示发布标记' }
    ]
  },
  {
    route: '/schools',
    title: '院校情报',
    subtitle: '学校列表、夏令营项目、截止趋势和 AI 匹配推荐。',
    permission: permissions.schools,
    emptyTitle: '暂无院校项目',
    emptyMessage: '院校项目为空时，可以先检查后端 Mock 数据或新增项目。',
    recoveryActions: ['检查 school-service 是否启动', '查看 /api/school-projects', '尝试使用 AI 匹配推荐'],
    endpoints: [
      { name: '学校列表', method: 'GET', path: '/api/schools', permission: permissions.schools, description: '读取学校基础信息' },
      { name: '项目列表', method: 'GET', path: '/api/school-projects', permission: permissions.schoolProjects, description: '读取夏令营和预推免项目' },
      { name: '院校推荐', method: 'POST', path: '/api/schools/recommend', permission: permissions.schools, description: '基于学生画像推荐项目', mutating: true }
    ]
  },
  {
    route: '/settings',
    title: '系统设置',
    subtitle: '后端地址、登录状态、权限状态和飞书状态。',
    permission: permissions.users,
    emptyTitle: '暂无设置项',
    emptyMessage: '设置页始终保留基础入口，便于用户修复后端地址或退出登录。',
    recoveryActions: ['确认 Base URL 没有多余斜杠', '检查 token 是否过期', '无法访问时退出重新登录'],
    endpoints: [
      { name: '当前用户', method: 'GET', path: '/api/users/me', permission: permissions.users, description: '读取角色、权限和工作空间' },
      { name: '飞书状态', method: 'GET', path: '/api/feishu/sync/status', permission: permissions.feishu, description: '检查飞书同步能力' },
      { name: '角色清单', method: 'GET', path: '/api/roles', permission: permissions.roles, description: '团队管理员查看角色权限' },
      { name: '权限清单', method: 'GET', path: '/api/permissions', permission: permissions.permissionView, description: '团队管理员查看权限点' }
    ]
  }
]

export const roleProfiles = [
  { code: 'TEAM_ADMIN', name: '团队管理员', description: '可查看和配置所有模块', protectedRole: true },
  { code: 'CONTENT_OPERATOR', name: '内容运营人员', description: '暂定可查看全部内容运营和知识资产', protectedRole: false },
  { code: 'EDU_CONSULTANT', name: '教育咨询老师', description: '暂定可查看全部咨询服务内容', protectedRole: false },
  { code: 'IP_OPERATOR', name: '个人IP运营者', description: '暂定可查看全部内容和运营数据', protectedRole: false },
  { code: 'STUDENT_USER', name: '学员用户', description: '仅可查看院校情报、知识库和 AI 工作台', protectedRole: false }
]

export function sceneByRoute(route: string) {
  return scenes.find(scene => route.startsWith(scene.route))
}

export function sceneByPermission(permission: string) {
  return scenes.find(scene => scene.permission === permission || scene.endpoints.some(endpoint => endpoint.permission === permission))
}

export function endpointByPath(path: string) {
  return scenes.flatMap(scene => scene.endpoints).find(endpoint => {
    const normalized = endpoint.path.replace(/\{[^}]+\}/g, '')
    return path.startsWith(normalized.split('{')[0])
  })
}

export function canOpenScene(route: string, userPermissions: string[], admin = false) {
  if (admin) return true
  const scene = sceneByRoute(route)
  if (!scene) return false
  if (userPermissions.includes(scene.permission)) return true
  return scene.endpoints.some(endpoint => userPermissions.includes(endpoint.permission))
}

export function deniedSceneMessage(route: string, roleText = '当前角色') {
  const scene = sceneByRoute(route)
  if (!scene) return `${roleText}暂不能访问该页面。`
  return `${roleText}暂不能访问「${scene.title}」，请联系团队管理员调整角色权限。`
}

export function endpointChecklist(scene: FeatureScene) {
  return scene.endpoints.map(endpoint => ({
    title: endpoint.name,
    method: endpoint.method,
    path: endpoint.path,
    description: endpoint.description,
    type: endpoint.mutating ? 'warning' : endpoint.streaming ? 'success' : 'info'
  }))
}

export function paritySummary() {
  const totalEndpoints = scenes.reduce((sum, scene) => sum + scene.endpoints.length, 0)
  const streamingEndpoints = scenes.flatMap(scene => scene.endpoints).filter(endpoint => endpoint.streaming).length
  const mutatingEndpoints = scenes.flatMap(scene => scene.endpoints).filter(endpoint => endpoint.mutating).length
  return {
    scenes: scenes.length,
    endpoints: totalEndpoints,
    streamingEndpoints,
    mutatingEndpoints,
    roles: roleProfiles.length
  }
}

export function routeRecovery(route: string) {
  return sceneByRoute(route)?.recoveryActions || ['刷新页面', '检查后端服务', '重新登录']
}

export function sceneEmptyState(route: string) {
  const scene = sceneByRoute(route)
  return {
    title: scene?.emptyTitle || '暂无数据',
    message: scene?.emptyMessage || '当前页面没有可展示的数据。',
    actions: scene?.recoveryActions || ['刷新页面']
  }
}
