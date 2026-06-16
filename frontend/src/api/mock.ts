export const agents = [
  { type: 'content', name: 'ContentAgent', desc: '小红书爆款仿写与朋友圈人设文案', color: '#5b6cff' },
  { type: 'knowledge', name: 'KnowledgeAgent', desc: '内容资产拆解、模板沉淀与知识检索', color: '#19b37b' },
  { type: 'student', name: 'StudentAgent', desc: '学员画像与风险判断', color: '#f59e0b' },
  { type: 'school', name: 'SchoolAgent', desc: '院校项目与匹配推荐', color: '#ef4444' },
  { type: 'feishu', name: 'FeishuAgent', desc: '飞书文档创建、同步与消息推送', color: '#8b5cf6' }
]

export const contentAgents = [
  {
    key: 'xiaohongshu',
    name: '小红书内容智能体',
    subtitle: '爆款结构检索 + 模板压缩 + 仿写生成',
    input: '主题 / 目标人群 / 风格',
    output: '3 个版本笔记 + 10 条标题 + 结构模板'
  },
  {
    key: 'moments',
    name: '朋友圈内容智能体',
    subtitle: '场景识别 + 人设映射 + 克制表达',
    input: '场景 / 身份标签 / 情绪方向',
    output: '专业理性版 / 学姐温和版 / 稍带传播版'
  },
  {
    key: 'asset',
    name: '知识库/内容资产智能体',
    subtitle: '内容拆解 + 标签体系 + 模板库沉淀',
    input: '历史笔记 / 朋友圈 / 话术 / SOP',
    output: '标题模板、钩子库、结构库、转化话术库'
  }
]

export const sopSteps = {
  xiaohongshu: [
    '爆款结构检索：模拟同主题 Top 20 高赞笔记，抽取标题、开头、分段和结尾转化',
    '结构压缩成模板：沉淀标题类型、开头钩子、正文层级和行动引导',
    '内容生成：保留结构但替换案例、表达和逻辑顺序，加入关键词与情绪词',
    '标题生成：焦虑型、结果型、对比型、数字型、经验总结型共 10 条',
    '输出 3 个版本：干货版、情绪增强版、转化引导版',
    '自动入库：同步到飞书多维表格、本地数据库和内容资产库'
  ],
  moments: [
    '场景识别：区分成果型、过程型、思考型',
    '人设映射：绑定专业感、可信度和克制情绪基调',
    '朋友圈结构生成：场景描述、专业动作、反馈、方法提炼、克制收尾',
    '生成 3 种风格：专业理性版、学姐温和版、稍带传播版',
    '自动优化：删除营销感词汇，增强真实细节，控制 100-300 字',
    '自动入库：记录场景标签、使用版本和转化效果'
  ],
  asset: [
    '自动分类：小红书笔记、朋友圈内容、招生话术、方法论总结',
    '结构拆解：提取标题结构、开头模板、爆款钩子和转化话术',
    '生成可复用模板库：标题模板、结构模板、朋友圈模板、话术库',
    '标签体系：主题、目的、风格、转化标签',
    '向量化入库：为后续相似主题检索和 RAG 生成提供上下文',
    '飞书沉淀：写入多维表格并关联飞书文档'
  ]
}

export const pipelineNodes = [
  { name: '触发生成', items: ['前端界面', '飞书机器人'], color: '#5b6cff' },
  { name: '知识检索', items: ['向量数据库', '爆款结构库', '历史内容库'], color: '#19b37b' },
  { name: '内容生成', items: ['LLM API', 'Prompt 模板', 'Agent Router'], color: '#8b5cf6' },
  { name: '内容输出', items: ['飞书文档', '本地基础数据库'], color: '#f59e0b' },
  { name: '数据沉淀', items: ['飞书多维表格', '飞书文档', '本地基础数据库'], color: '#ef4444' }
]

export const topics = [
  '保研简历怎么写才像有科研潜力',
  '保研边缘人如何逆袭夏令营',
  '三分钟讲清预推免和夏令营区别',
  '低年级保研规划清单',
  '导师套磁邮件怎么写不尴尬',
  '科研小白第一段项目怎么找',
  '排名证明材料避坑',
  '夏令营面试自我介绍模板',
  '目标院校梯度怎么定',
  '普通双非的保研路线'
].map((title, i) => ({
  id: i + 1,
  title,
  platform: i % 3 === 0 ? '朋友圈' : i % 2 ? '公众号' : '小红书',
  type: ['经验', '科普', '转化', '材料'][i % 4],
  status: ['待创作', '已生成', '待发布', '已发布'][i % 4],
  heat: 72 + i * 2
}))

export const moments = [
  '今天和一个大二同学聊完规划，最感慨的是：保研不是临门一脚，而是每个学期的小积累。',
  '把排名、科研、英语和项目经历放到一张表里，焦虑会少一半，行动会多一倍。',
  '越早开始做院校情报，越能避开“错过截止时间”的遗憾。',
  '保研规划不是制造焦虑，而是把不确定拆成能执行的小任务。',
  '如果你现在只有 GPA，没有科研，暑假前还有补强窗口。',
  '简历不是经历堆砌，而是把优势证据讲清楚。',
  '今天整理了 15 个经管夏令营项目，适合排名前 30% 的同学重点关注。',
  '面试准备最重要的不是背答案，是把自己的研究线讲顺。'
]

export const docs = [
  '2026 保研夏令营时间线',
  '院校项目材料清单模板',
  '朋友圈转化文案案例库',
  '经管类面试高频问题',
  '保研咨询 SOP',
  '低 GPA 补强策略',
  '院校情报采集模板',
  '飞书多维表格字段设计',
  '公众号标题库',
  '学员周报模板'
].map((title, i) => ({
  id: i + 1,
  title,
  category: ['政策资料', '申请材料', '内容运营', '面试资料'][i % 4],
  summary: 'AI 摘要：该资料可拆分为选题卡片、学员提醒任务和知识库问答片段。',
  tags: ['夏令营', '材料', '运营', '面试'].slice(0, i % 4 + 1),
  updatedAt: `2026-06-${String(14 - i).padStart(2, '0')}`
}))

export const students = Array.from({ length: 20 }, (_, i) => ({
  id: i + 1,
  name: `学员${String(i + 1).padStart(2, '0')}`,
  school: `示例大学${i % 5 + 1}`,
  major: ['金融学', '会计学', '经济学', '新闻学'][i % 4],
  gpa: (3.2 + (i % 7) * 0.1).toFixed(2),
  rank: `${i % 20 + 1}/120`,
  english: i % 2 ? '雅思 6.5' : '六级 560',
  targetSchool: ['985 经管', '211 金融', '教育学强校'][i % 3],
  stage: ['初筛', '材料准备', '夏令营报名', '面试中', '拟录取'][i % 5],
  risk: ['低', '中', '高'][i % 3],
  progress: 40 + (i % 6) * 10
}))

export const projects = Array.from({ length: 15 }, (_, i) => ({
  id: i + 1,
  schoolName: ['复旦大学', '华东师范大学', '中南财经政法大学', '上海财经大学', '厦门大学'][i % 5],
  projectName: ['经管学院夏令营', '教育学部预推免', '金融专硕项目'][i % 3],
  deadline: `2026-${String(7 + i % 4).padStart(2, '0')}-${String(10 + i).padStart(2, '0')}`,
  requirements: '排名前30%，六级500+，有科研或竞赛经历优先',
  materials: '成绩单、排名证明、简历、个人陈述、科研证明',
  matchScore: 74 + i
}))

export const feishuLogs = Array.from({ length: 10 }, (_, i) => ({
  id: i + 1,
  type: ['docs', 'bitable', 'tasks', 'bot'][i % 4],
  status: i % 5 === 0 ? 'WAITING' : 'SUCCESS',
  message: `Mock 同步记录 ${i + 1}：已完成数据校验与写入`,
  time: `2026-06-14 ${String(10 + i).padStart(2, '0')}:20`
}))

export function mockChat(agentType: string, message: string) {
  return new Promise(resolve => setTimeout(() => resolve({
    agentType,
    reply: `已调用 ${agentType} 智能体处理：“${message}”。我将按 SOP 检索知识库、生成内容版本，并沉淀到飞书多维表格。`,
    cards: [
      { title: '核心建议', content: '把“爆款结构拆解 + 人设表达 + 知识沉淀”作为内容生产主链路。' },
      { title: '下一步', content: '生成内容文档、写入多维表格、记录标签与转化反馈。' }
    ]
  }), 450))
}
