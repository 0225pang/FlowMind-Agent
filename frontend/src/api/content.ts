import http from './client'

export type ContentStatus = '待创作' | '已生成' | '待发布' | '已发布'
export type CopyUsageStatus = '未使用' | '已使用' | '已归档'
export type ContentChannel = '小红书' | '朋友圈' | '公众号'

export interface CopyImage {
  id: number
  name: string
  url: string
}

export interface CopyDraft {
  id: number
  themeId: number
  title: string
  channel: ContentChannel
  version: string
  style: string
  content: string
  usageStatus: CopyUsageStatus
  usedDate?: string
  generatedAt: string
  owner: string
  feedback: string
  rating?: number
  images: CopyImage[]
  imageSuggestion: string
}

export interface ContentTheme {
  id: number
  title: string
  topic: string
  platform: ContentChannel
  type: string
  status: ContentStatus
  heat: number
  rating?: number
  tags: string[]
  plannedDate: string
  summary: string
  drafts: CopyDraft[]
}

export interface ContentCalendarItem {
  id: number
  draftId: number
  themeId: number
  date: string
  title: string
  channel: ContentChannel
  status: ContentStatus
  usageStatus: CopyUsageStatus
}

export interface ThemeCreatePayload {
  title: string
  topic: string
  platform: ContentChannel
  type: string
  status: ContentStatus
  heat: number
  plannedDate: string
  summary: string
  tags: string[]
}

export interface CopyCreatePayload {
  title: string
  channel: ContentChannel
  version: string
  style: string
  content: string
  owner: string
}

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

function mockImage(label: string, color = '#5b6cff') {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="720" height="420" viewBox="0 0 720 420"><defs><linearGradient id="g" x1="0" x2="1" y1="0" y2="1"><stop stop-color="${color}"/><stop offset="1" stop-color="#8b5cf6"/></linearGradient></defs><rect width="720" height="420" rx="28" fill="url(#g)"/><circle cx="590" cy="80" r="96" fill="rgba(255,255,255,.16)"/><circle cx="120" cy="335" r="128" fill="rgba(255,255,255,.12)"/><text x="56" y="92" fill="white" font-family="Arial" font-size="34" font-weight="700">FlowMind</text><text x="56" y="220" fill="white" font-family="Arial" font-size="56" font-weight="800">${label}</text><text x="60" y="286" fill="rgba(255,255,255,.82)" font-family="Arial" font-size="22">AI content asset</text></svg>`
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}

export const contentThemes: ContentTheme[] = [
  {
    id: 1,
    title: '保研简历怎么写才像有科研潜力',
    topic: '保研简历',
    platform: '小红书',
    type: '爆款仿写',
    status: '已生成',
    heat: 96,
    rating: 4,
    tags: ['保研简历', '科研潜力', '材料优化'],
    plannedDate: '2026-06-18',
    summary: '围绕简历结构、科研表达和材料证据生成多版本笔记，适合做保研材料系列。',
    drafts: [
      {
        id: 101,
        themeId: 1,
        title: '保研er别再这样写简历了',
        channel: '小红书',
        version: '干货版',
        style: '干货',
        content: '保研简历不是经历堆砌，而是让老师快速看到你的科研潜力。建议每段经历都写清楚：你做了什么、用了什么方法、最后产出了什么证据。',
        usageStatus: '已使用',
        usedDate: '2026-06-12',
        generatedAt: '2026-06-10 10:30',
        owner: '内容运营',
        feedback: '收藏率高，适合继续拆成简历模板系列。',
        rating: 4,
        images: [{ id: 1001, name: 'resume-cover.svg', url: mockImage('Resume', '#2563eb') }],
        imageSuggestion: '三栏式简历改前改后对比图，突出科研经历、成果证据、匹配方向。'
      },
      {
        id: 102,
        themeId: 1,
        title: '简历没亮点？先改这 4 个位置',
        channel: '小红书',
        version: '情绪增强版',
        style: '学姐风',
        content: '很多同学不是经历不够，而是不知道怎么把经历讲成优势。尤其是科研、竞赛、课程项目这三块，写法不同，老师看到的信息完全不同。',
        usageStatus: '未使用',
        generatedAt: '2026-06-10 10:34',
        owner: '内容运营',
        feedback: '适合配案例图发布。',
        rating: 3,
        images: [],
        imageSuggestion: '配一张“简历亮点提取清单”长图，包含问题意识、方法、结果、证据四列。'
      },
      {
        id: 103,
        themeId: 1,
        title: '我会这样帮学员改保研简历',
        channel: '朋友圈',
        version: '专业理性版',
        style: '专业',
        content: '今天复盘一份简历，重点不是把语言修得更漂亮，而是把科研经历里的问题意识、方法动作和结果证据补清楚。材料的可信度，往往来自这些细节。',
        usageStatus: '已使用',
        usedDate: '2026-06-14',
        generatedAt: '2026-06-11 16:20',
        owner: '主理人',
        feedback: '私信咨询 3 条，适合继续扩写成长文。',
        rating: 5,
        images: [],
        imageSuggestion: '配会议白板或简历批注局部图，弱化营销感，强化真实服务场景。'
      }
    ]
  },
  {
    id: 2,
    title: '导师套磁邮件怎么写不尴尬',
    topic: '导师套磁',
    platform: '小红书',
    type: '经验干货',
    status: '待发布',
    heat: 91,
    rating: 5,
    tags: ['导师套磁', '邮件模板', '面试准备'],
    plannedDate: '2026-06-20',
    summary: '拆解套磁邮件结构，强调研究兴趣、匹配理由和克制表达。',
    drafts: [
      {
        id: 201,
        themeId: 2,
        title: '套磁邮件别一上来就求机会',
        channel: '小红书',
        version: '干货版',
        style: '干货',
        content: '一封好的套磁邮件，核心不是表达“我很想来”，而是说明你为什么匹配这位老师：研究方向、已有经历、能继续推进的问题。',
        usageStatus: '未使用',
        generatedAt: '2026-06-13 09:12',
        owner: '内容运营',
        feedback: '待配套邮件模板图。',
        rating: 0,
        images: [],
        imageSuggestion: '配“邮件结构拆解图”：称呼、研究匹配、经历证据、请教问题、克制结尾。'
      },
      {
        id: 202,
        themeId: 2,
        title: '给学生讲套磁，我最常提醒这一点',
        channel: '朋友圈',
        version: '学姐温和版',
        style: '温柔',
        content: '套磁不是打扰老师，而是一次简洁的自我说明。把自己的研究兴趣、已有准备和想请教的问题讲清楚，就已经比空泛表达好很多。',
        usageStatus: '已使用',
        usedDate: '2026-06-15',
        generatedAt: '2026-06-13 09:20',
        owner: '主理人',
        feedback: '适合继续扩写成长文。',
        rating: 4,
        images: [{ id: 2002, name: 'email-template.svg', url: mockImage('Email', '#7c3aed') }],
        imageSuggestion: '配一张简洁邮件模板截图，重点标出“匹配理由”段落。'
      }
    ]
  },
  {
    id: 3,
    title: '低年级保研规划清单',
    topic: '保研规划',
    platform: '朋友圈',
    type: '人设表达',
    status: '待创作',
    heat: 88,
    rating: 3,
    tags: ['低年级', '规划', '时间线'],
    plannedDate: '2026-06-22',
    summary: '强调早规划不是焦虑，而是把不确定拆成可执行任务。',
    drafts: [
      {
        id: 301,
        themeId: 3,
        title: '大一大二做保研规划，重点不是焦虑',
        channel: '朋友圈',
        version: '专业理性版',
        style: '克制',
        content: '低年级做规划，不是为了提前焦虑，而是知道 GPA、英语、科研、竞赛分别在什么时候该补到什么程度。节奏清楚，反而会轻松很多。',
        usageStatus: '未使用',
        generatedAt: '2026-06-14 21:10',
        owner: '主理人',
        feedback: '可作为招生转化铺垫。',
        rating: 0,
        images: [],
        imageSuggestion: '配一张低年级三阶段时间轴：基础分、项目经历、材料准备。'
      },
      {
        id: 302,
        themeId: 3,
        title: '低年级保研准备，不要只盯着绩点',
        channel: '小红书',
        version: '转化引导版',
        style: '干货',
        content: '绩点是门槛，但不是全部。低年级更应该同步搭建英语、科研、竞赛和表达能力，这些会在夏令营材料里一起被看见。',
        usageStatus: '未使用',
        generatedAt: '2026-06-15 11:30',
        owner: '内容运营',
        feedback: '标题可继续 A/B 测试。',
        rating: 2,
        images: [],
        imageSuggestion: '配“四象限能力地图”：绩点、英语、科研、表达。'
      }
    ]
  },
  {
    id: 4,
    title: '夏令营面试自我介绍模板',
    topic: '夏令营面试',
    platform: '小红书',
    type: '模板',
    status: '已发布',
    heat: 93,
    rating: 5,
    tags: ['面试', '自我介绍', '模板'],
    plannedDate: '2026-06-08',
    summary: '围绕背景、科研、目标方向和结尾承接设计面试表达。',
    drafts: [
      {
        id: 401,
        themeId: 4,
        title: '夏令营自我介绍照这个顺序讲',
        channel: '小红书',
        version: '转化引导版',
        style: '干货',
        content: '自我介绍建议按“基础背景-核心经历-研究兴趣-项目匹配”来讲。不要背简历，要让老师听到一条清楚的成长线。',
        usageStatus: '已使用',
        usedDate: '2026-06-08',
        generatedAt: '2026-06-06 13:00',
        owner: '内容运营',
        feedback: '互动率 12%，评论区追问较多。',
        rating: 5,
        images: [{ id: 4001, name: 'interview-flow.svg', url: mockImage('Interview', '#0891b2') }],
        imageSuggestion: '配一张自我介绍流程卡，四段结构用不同色块区分。'
      }
    ]
  },
  {
    id: 5,
    title: '收到 offer 后如何发朋友圈不浮夸',
    topic: '成功案例',
    platform: '朋友圈',
    type: '成果型人设',
    status: '已生成',
    heat: 90,
    rating: 4,
    tags: ['offer', '成功案例', '朋友圈'],
    plannedDate: '2026-06-24',
    summary: '用克制语气表达结果、过程和方法，避免强营销感。',
    drafts: [
      {
        id: 501,
        themeId: 5,
        title: '今天收到一个很踏实的好消息',
        channel: '朋友圈',
        version: '学姐温和版',
        style: '温柔',
        content: '今天收到学员的录取反馈，开心之外更多是替她松一口气。前期把材料反复打磨，面试前又做了三轮模拟，结果其实是一步步攒出来的。',
        usageStatus: '未使用',
        generatedAt: '2026-06-16 09:45',
        owner: '主理人',
        feedback: '适合等下一条真实反馈后发布。',
        rating: 4,
        images: [],
        imageSuggestion: '配模糊处理后的 offer 截图或聊天反馈局部，注意隐私遮挡。'
      },
      {
        id: 502,
        themeId: 5,
        title: '一个 offer 背后，最值得复盘的是过程',
        channel: '朋友圈',
        version: '专业理性版',
        style: '专业',
        content: '比起单纯晒结果，我更想记录这次申请里做对的几件事：材料定位更聚焦，项目表达更具体，面试回答有证据链。这些才是真正可复用的经验。',
        usageStatus: '已使用',
        usedDate: '2026-06-17',
        generatedAt: '2026-06-16 10:05',
        owner: '主理人',
        feedback: '获得 5 条咨询，语气克制有效。',
        rating: 5,
        images: [{ id: 5002, name: 'offer-story.svg', url: mockImage('Offer', '#16a34a') }],
        imageSuggestion: '配复盘清单图，弱化炫耀，突出方法论。'
      }
    ]
  },
  {
    id: 6,
    title: '科研小白如何找到第一个项目',
    topic: '科研入门',
    platform: '小红书',
    type: '爆款干货',
    status: '待发布',
    heat: 89,
    rating: 0,
    tags: ['科研入门', '项目经历', '保研'],
    plannedDate: '2026-06-25',
    summary: '把找项目拆成课程项目、导师课题、竞赛延展和论文复现四条路径。',
    drafts: [
      {
        id: 601,
        themeId: 6,
        title: '科研小白别只问老师有没有项目',
        channel: '小红书',
        version: '干货版',
        style: '干货',
        content: '第一个科研项目不一定从正式课题开始。你可以从课程论文、文献复现、竞赛选题和实验室小任务切入，先证明自己能稳定推进。',
        usageStatus: '未使用',
        generatedAt: '2026-06-16 15:20',
        owner: '内容运营',
        feedback: '需要做成可收藏长图。',
        rating: 3,
        images: [],
        imageSuggestion: '配“科研入门四路径”信息图：课程、复现、竞赛、实验室任务。'
      },
      {
        id: 602,
        themeId: 6,
        title: '科研经历不是等来的，是一点点搭出来的',
        channel: '朋友圈',
        version: '稍带传播版',
        style: '轻传播',
        content: '很多同学卡在“我没有科研经历，所以不敢联系老师”。但第一段经历往往不是等来的，而是从能完成的小任务开始搭出来的。',
        usageStatus: '已归档',
        generatedAt: '2026-06-16 15:32',
        owner: '主理人',
        feedback: '语气可保留，暂不发布。',
        rating: 0,
        images: [],
        imageSuggestion: '配书桌、文献、任务清单类真实工作场景图。'
      }
    ]
  }
]

export const copyLibrary: CopyDraft[] = contentThemes.flatMap(theme => theme.drafts)

export const contentCalendar: ContentCalendarItem[] = [
  { id: 1, draftId: 401, themeId: 4, date: '2026-06-08', title: '夏令营自我介绍照这个顺序讲', channel: '小红书', status: '已发布', usageStatus: '已使用' },
  { id: 2, draftId: 101, themeId: 1, date: '2026-06-12', title: '保研er别再这样写简历了', channel: '小红书', status: '已发布', usageStatus: '已使用' },
  { id: 3, draftId: 103, themeId: 1, date: '2026-06-14', title: '我会这样帮学员改保研简历', channel: '朋友圈', status: '已发布', usageStatus: '已使用' },
  { id: 4, draftId: 202, themeId: 2, date: '2026-06-15', title: '给学生讲套磁，我最常提醒这一点', channel: '朋友圈', status: '已发布', usageStatus: '已使用' },
  { id: 5, draftId: 502, themeId: 5, date: '2026-06-17', title: '一个 offer 背后，最值得复盘的是过程', channel: '朋友圈', status: '已发布', usageStatus: '已使用' },
  { id: 6, draftId: 201, themeId: 2, date: '2026-06-20', title: '套磁邮件别一上来就求机会', channel: '小红书', status: '待发布', usageStatus: '未使用' },
  { id: 7, draftId: 301, themeId: 3, date: '2026-06-22', title: '大一大二做保研规划，重点不是焦虑', channel: '朋友圈', status: '待创作', usageStatus: '未使用' },
  { id: 8, draftId: 501, themeId: 5, date: '2026-06-24', title: '今天收到一个很踏实的好消息', channel: '朋友圈', status: '已生成', usageStatus: '未使用' },
  { id: 9, draftId: 601, themeId: 6, date: '2026-06-25', title: '科研小白别只问老师有没有项目', channel: '小红书', status: '待发布', usageStatus: '未使用' }
]

async function requestData<T>(request: () => Promise<{ data: ApiResponse<T> }>, fallback: T) {
  try {
    const response = await request()
    return response.data.data
  } catch {
    return fallback
  }
}

function upsertDraft(draft: CopyDraft) {
  const libraryIndex = copyLibrary.findIndex(item => item.id === draft.id)
  if (libraryIndex >= 0) copyLibrary[libraryIndex] = draft
  else copyLibrary.unshift(draft)

  const theme = contentThemes.find(item => item.id === draft.themeId)
  if (theme) {
    const draftIndex = theme.drafts.findIndex(item => item.id === draft.id)
    if (draftIndex >= 0) theme.drafts[draftIndex] = draft
    else theme.drafts.unshift(draft)
  }
}

// Content-service REST API:
// GET    /api/content/themes?keyword=&status=&channel=
// POST   /api/content/themes
// DELETE /api/content/themes/{id}
// PUT    /api/content/themes/{id}/rating
// GET    /api/content/themes/{themeId}/drafts
// POST   /api/content/themes/{themeId}/drafts
// GET    /api/content/drafts?keyword=&channel=&usageStatus=
// PUT    /api/content/drafts/{draftId}
// DELETE /api/content/drafts/{draftId}
// PUT    /api/content/drafts/{draftId}/rating
// POST   /api/content/drafts/{draftId}/images
// GET    /api/content/calendar?month=YYYY-MM
export const contentApi = {
  async getThemes() {
    return requestData(() => http.get<ApiResponse<ContentTheme[]>>('/content/themes'), contentThemes)
  },
  async getThemeDrafts(themeId: number) {
    return requestData(
      () => http.get<ApiResponse<CopyDraft[]>>(`/content/themes/${themeId}/drafts`),
      copyLibrary.filter(item => item.themeId === themeId)
    )
  },
  async getDrafts() {
    return requestData(() => http.get<ApiResponse<CopyDraft[]>>('/content/drafts'), copyLibrary)
  },
  async getCalendar() {
    return requestData(() => http.get<ApiResponse<ContentCalendarItem[]>>('/content/calendar'), contentCalendar)
  },
  async createTheme(data: ThemeCreatePayload) {
    try {
      const response = await http.post<ApiResponse<ContentTheme>>('/content/themes', data)
      contentThemes.unshift(response.data.data)
      return response.data.data
    } catch {
      const newTheme: ContentTheme = { id: Date.now(), rating: 0, drafts: [], ...data }
      contentThemes.unshift(newTheme)
      return newTheme
    }
  },
  async deleteTheme(id: number) {
    try {
      await http.delete(`/content/themes/${id}`)
    } catch {
      // Local mock mode.
    }
    const themeIndex = contentThemes.findIndex(item => item.id === id)
    if (themeIndex >= 0) contentThemes.splice(themeIndex, 1)
    for (let i = copyLibrary.length - 1; i >= 0; i--) if (copyLibrary[i].themeId === id) copyLibrary.splice(i, 1)
    for (let i = contentCalendar.length - 1; i >= 0; i--) if (contentCalendar[i].themeId === id) contentCalendar.splice(i, 1)
  },
  async createCopy(themeId: number, data: CopyCreatePayload) {
    try {
      const response = await http.post<ApiResponse<CopyDraft>>(`/content/themes/${themeId}/drafts`, data)
      upsertDraft(response.data.data)
      return response.data.data
    } catch {
      const now = new Date().toISOString().slice(0, 16).replace('T', ' ')
      const draft: CopyDraft = {
        id: Date.now(),
        themeId,
        usageStatus: '未使用',
        generatedAt: now,
        feedback: '',
        rating: 0,
        images: [],
        imageSuggestion: '建议使用真实咨询场景、材料截图或清单长图作为配图。',
        ...data
      }
      upsertDraft(draft)
      return draft
    }
  },
  async updateDraft(draft: CopyDraft) {
    try {
      const response = await http.put<ApiResponse<CopyDraft>>(`/content/drafts/${draft.id}`, {
        title: draft.title,
        content: draft.content,
        usageStatus: draft.usageStatus,
        usedDate: draft.usedDate,
        feedback: draft.feedback,
        imageSuggestion: draft.imageSuggestion
      })
      upsertDraft(response.data.data)
      return response.data.data
    } catch {
      upsertDraft(draft)
      const item = contentCalendar.find(row => row.draftId === draft.id)
      if (item) {
        item.title = draft.title
        item.channel = draft.channel
        item.usageStatus = draft.usageStatus
        if (draft.usedDate) item.date = draft.usedDate
      }
      return draft
    }
  },
  async deleteCopy(id: number) {
    try {
      await http.delete(`/content/drafts/${id}`)
    } catch {
      // Local mock mode.
    }
    const copyIndex = copyLibrary.findIndex(item => item.id === id)
    const themeId = copyIndex >= 0 ? copyLibrary[copyIndex].themeId : undefined
    if (copyIndex >= 0) copyLibrary.splice(copyIndex, 1)
    if (themeId) {
      const theme = contentThemes.find(item => item.id === themeId)
      if (theme) theme.drafts = theme.drafts.filter(item => item.id !== id)
    }
    for (let i = contentCalendar.length - 1; i >= 0; i--) if (contentCalendar[i].draftId === id) contentCalendar.splice(i, 1)
  },
  async rateTheme(id: number, rating: number) {
    try {
      const response = await http.put<ApiResponse<ContentTheme>>(`/content/themes/${id}/rating`, { rating })
      const index = contentThemes.findIndex(item => item.id === id)
      if (index >= 0) contentThemes[index] = response.data.data
      return response.data.data
    } catch {
      const theme = contentThemes.find(item => item.id === id)
      if (theme) theme.rating = rating
      return theme!
    }
  },
  async rateCopy(id: number, rating: number) {
    try {
      const response = await http.put<ApiResponse<CopyDraft>>(`/content/drafts/${id}/rating`, { rating })
      upsertDraft(response.data.data)
      return response.data.data
    } catch {
      const draft = copyLibrary.find(item => item.id === id)
      if (draft) {
        draft.rating = rating
        upsertDraft(draft)
      }
      return draft!
    }
  },
  async uploadDraftImage(draftId: number, image: CopyImage) {
    return requestData(
      () => http.post<ApiResponse<CopyImage>>(`/content/drafts/${draftId}/images`, {
        name: image.name,
        url: image.url,
        storageProvider: 'local',
        objectKey: image.name
      }),
      image
    )
  }
}
