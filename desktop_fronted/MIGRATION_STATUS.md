# FlowMind 桌面端迁移进度

目标：尽可能 100% 复刻当前 Vue Web 前端，并使用 Python 实现一版可运行的电脑客户端。

迁移方式采用“保留后端、重写桌面端界面”的方案。现有 Java 后端继续提供 REST 接口和 SSE 流式接口，桌面端通过统一 API 客户端访问业务能力，不直接连接 MySQL、Weaviate 或飞书 CLI。这样既能减少重复开发，也能让 Web 端和桌面端共享同一套业务规则。

界面复刻重点放在页面结构、交互流程和数据展示方式上。桌面端保留 Web 端的侧边栏导航、顶部用户区、统计卡片、图表、表格、详情弹窗、标签页和 AI 工作台上下文面板，并根据 PySide6 的组件能力进行等价实现。部分 Web 端依赖浏览器生态的视觉效果，在桌面端采用 QSS、自绘图表和组件封装进行替代。

为了便于课程展示和离线演示，桌面端加入 `offline://demo` 模式。该模式内置 Mock 数据，可以在后端未启动、ngrok 不稳定或网络不可用时继续展示核心页面。离线模式也覆盖登录角色、权限菜单、流式对话、内容运营、知识库、学员、院校和飞书状态等数据，便于验证客户端功能是否完整。

当前目录：

```text
desktop_fronted/
```

## 已完成工程结构

- `run.py`：PySide6 高保真桌面端入口。
- `run_tk.py`：Tkinter 兜底桌面端入口。
- `requirements.txt`：桌面端依赖清单，当前固定 `PySide6==6.7.3`。
- `README.md`：运行方式、目录说明和迁移说明。
- `MIGRATION_STATUS.md`：迁移状态、已覆盖功能和后续补齐清单。
- `smoke_test.py`：不启动 GUI 的 API/fallback 自检脚本。
- `pyside_smoke_test.py`：PySide6 离屏窗口创建和页面切换自检脚本。
- `check_desktop.py`：一键执行编译、API fallback smoke、PySide 离屏 smoke。
- `start_desktop.ps1`：Windows 下一键启动 PySide 桌面端。
- `check_desktop.ps1`：Windows 下一键执行桌面端自检。
- `flowmind_desktop/api.py`：统一后端 API 客户端。
- `flowmind_desktop/fallback_data.py`：离线 Demo 数据。
- `flowmind_desktop/main.py`：PySide6 应用启动逻辑。
- `flowmind_desktop/styles.py`：PySide6 QSS 风格文件。
- `flowmind_desktop/views.py`：PySide6 主要页面实现。
- `flowmind_desktop/widgets.py`：PySide6 通用组件、线程、表格和可视化组件。
- `flowmind_desktop/tk_app.py`：Tkinter 备用页面实现。

## 已覆盖 API 模块

- 用户认证：`/api/auth/login`
- 当前用户：`/api/users/me`
- 网关路由：`/api/gateway/routes`
- AI 工作台：`/api/agents/**`
- Prompt 模板：`/api/prompts`
- 内容运营：`/api/content/**`
- 知识库：`/api/knowledge/**`
- 向量检索：`/api/knowledge/vector/search`
- 学员管理：`/api/students/**`
- 院校情报：`/api/schools`、`/api/school-projects`
- 数据分析：`/api/analytics/**`
- 飞书同步：`/api/feishu/**`
- 角色权限：`/api/roles`、`/api/permissions`、`/api/roles/{roleCode}/permissions`

## 已覆盖页面

- 登录页：负责 Base URL 配置、账号登录、离线 Demo 入口、演示账号快捷填充和角色信息初始化。
- Dashboard 首页：负责展示系统概览、关键统计、图表趋势和进入 AI 工作台的快捷入口。
- AI 工作台：负责会话管理、Agent 选择、普通对话、SSE 流式对话、工具调用过程和 Thinking 信息展示。
- 内容运营：负责主题库、文案库、内容日历、SOP 生成、文案详情、图片引用和 AI 生成入口。
- 知识库：负责文档列表、标签管理、同步状态、同步日志、语义检索和向量结果展示。
- 学员管理：负责学员画像、申请阶段、风险等级、进度展示、增删改和 AI 分析结果展示。
- 院校情报：负责院校项目、截止时间、报名条件、材料要求、匹配分和 AI 推荐结果展示。
- 数据分析：负责学员分布、内容统计、申请阶段漏斗、院校截止趋势和关键指标计算。
- 飞书同步：负责飞书连接状态、同步日志、文档创建、文档读取和知识库共享文件列表展示。
- 系统设置：负责 Prompt 模板、角色权限、连接测试、模型配置占位和系统日志展示。

## 可写入课程设计文档的说明

本桌面端迁移可以在课程设计文档中作为“系统客户端实现”章节展开。该部分可以先说明原系统采用前后端分离结构，Web 前端负责用户交互，Spring Boot 后端负责业务接口；然后说明本次迁移没有重写后端，而是用 Python/PySide6 重写客户端界面，从而降低迁移风险。

在“需求分析”部分，可以把桌面端需求拆成可运行性需求、功能完整性需求、界面一致性需求和接口复用需求。可运行性需求强调客户端可以在 Windows 桌面环境启动；功能完整性需求强调核心业务模块都需要保留；界面一致性需求强调页面布局和操作流程参考原 Vue 前端；接口复用需求强调桌面端通过 HTTP 和 SSE 调用后端能力。

在“概要设计”部分，可以描述桌面端由启动入口、API 客户端、页面视图、通用组件、离线数据和自检脚本组成。启动入口负责读取配置并创建窗口，API 客户端负责封装后端接口，页面视图负责组织业务界面，通用组件负责复用卡片、标签、表格、图表和弹窗，离线数据负责演示和容错，自检脚本负责验证项目可运行。

在“详细设计”部分，可以按页面逐个展开。例如 AI 工作台页面可以说明会话列表、输入框、流式输出、Thinking 面板和工具调用 trace；内容运营页面可以说明主题库、文案库、内容日历和 SOP 生成；知识库页面可以说明文档列表、标签编辑、同步状态和向量检索；学员与院校页面可以说明画像、风险、进度、项目匹配和推荐结果。

在“测试验证”部分，可以写明项目提供了 `smoke_test.py`、`pyside_smoke_test.py` 和 `check_desktop.py`。其中 `smoke_test.py` 主要验证 API 客户端和离线数据，`pyside_smoke_test.py` 主要验证 PySide 页面创建、页面切换和关键控件渲染，`check_desktop.py` 用于一键执行编译检查和两类自检，适合在提交前统一确认桌面端状态。

## 通用组件进度

- `Badge`：用于复刻 Web 端标签、状态和风险颜色。
- `StarRating`：用于主题评分和文案评分。
- `InfoRow`：用于详情面板中的键值信息展示。
- `MarkdownPanel`：用于摘要、正文、报名条件和材料要求展示。
- `JsonPanel`：用于工具调用、同步状态和原始响应展示。
- `PromptChip`：用于 AI 工作台推荐问题。
- `SectionHeader`：用于卡片内部标题和说明。
- `TextCard`：用于主题、文案和结构化文本卡片。
- `TraceListPanel`：用于 AI 工具调用 trace 卡片化展示。
- `CalendarGrid`：用于内容日历的月视图展示。
- `ChartCard`：用于 Dashboard 和数据分析页面的轻量图表展示。

## 本轮高保真增强

### 登录与主布局

- 登录页从单卡片升级为 Web 端同类“双栏面板”。
- 登录页左侧增加 FlowMind Agent 品牌区。
- 登录页左侧增加平台定位文案。
- 登录页增加团队管理员、内容运营人员、教育咨询老师、个人 IP 运营者、学员用户 5 个演示账号快捷入口。
- 点击演示账号会自动填入对应用户名和 `123456` 密码。
- 登录表单保留 Base URL 输入，方便切换本地后端、ngrok 后端和离线 Demo。
- 登录表单支持回车登录。
- 离线 Demo 登录会按账号返回不同角色，而不是所有账号都伪装成管理员。
- 离线 Demo 用户补齐 `roles`、`permissions`、`workspace` 字段。
- 登录后会把用户信息写入桌面端配置，下次启动可继续显示当前用户。
- 主布局顶部增加当前角色标签。
- 主布局顶部增加 API Ready 标签。
- 主布局顶部增加通知按钮占位，对齐 Web 端顶栏信息结构。
- 主布局顶部增加用户头像缩写。
- 主布局副标题增加当前工作空间。
- 侧边栏菜单接入前端路由权限判断。
- 管理员可访问全部页面。
- 非管理员会根据 `permissions` 中的接口路径或前端路由判断可访问页面。
- 无权限菜单会禁用并显示“无权限”。
- 无权限菜单带原因提示，文案对齐 Web 端“当前角色暂未开放该页面”的逻辑。
- 进入主界面时会自动跳转到当前角色第一个可访问页面。
- Dashboard 的“打开 AI 工作台”按钮也会经过权限判断。

### Dashboard 与数据分析

- Dashboard 增加 Web 端同款“AI 工作流总览”hero 区。
- Hero 区包含说明文案和“打开 AI 工作台”按钮。
- “打开 AI 工作台”按钮可直接切换到 AI 工作台页面。
- 将原本偏文本的统计区域改成轻量图表卡片。
- 图表卡片补齐 Web 端 `ChartCard` 的标题、说明和刷新按钮层级。
- Dashboard 图表补齐“内容选题类型”“申请阶段漏斗”等 Web 端同款说明文案。
- 覆盖学员分布、内容统计、申请阶段漏斗和院校截止趋势。
- 数据分析页统计卡补齐 Web 端同款指标：平均 GPA、高风险学员、本月截止和内容热度。
- 数据分析页会从学员列表计算平均 GPA 和高风险学员数。
- 数据分析页会从项目列表计算当月截止项目数。
- 数据分析页会从内容主题计算内容热度均值。
- 数据分析页新增 GPA 分布数据组装，图表标签为 3.2、3.4、3.6、3.8、4.0。
- 保留离线 Mock 数据，后端不可用时仍可展示完整页面。
- 图表采用 PySide 自绘组件，不额外引入 matplotlib，减少依赖风险。

### AI 工作台

- 保留会话列表、Agent 列表、推荐 prompt 和流式对话入口。
- AI 工作台顶部补齐 Web 端 `AgentTabs` 同类总智能体概览条。
- 总智能体概览条展示“FlowMind 总智能体”说明和 ContentAgent、KnowledgeAgent、StudentAgent、SchoolAgent、FeishuAgent 五个专业 Agent chip。
- 总智能体概览条使用浅色渐变背景和主圆点，强调自动路由与多 Agent 协作结构。
- 总智能体概览条已纳入 PySide 离屏自检，校验容器和 5 个专业 Agent chip 是否渲染。
- 会话列表支持新建、刷新、删除、清空当前会话历史。
- 会话列表从纯文本项升级为 Web 端 `SessionList` 同类结构，展示会话标题、相对时间、Agent 类型和行内删除入口。
- 会话列表条目使用自定义 PySide item widget，保留点击切换会话的行为。
- 会话列表行内删除会调用同一删除会话接口，删除当前会话后自动清空聊天区并刷新列表。
- 会话相对时间支持刚刚、分钟、小时、天和日期兜底展示。
- 会话列表自定义条目和相对时间格式化已纳入 PySide 离屏自检。
- 支持停止当前流式输出。
- 对话区增加 Agent 选择器，可在 FlowMindAgent、ContentAgent、KnowledgeAgent、StudentAgent、SchoolAgent、FeishuAgent 间切换。
- 发送消息时会使用当前选择的 Agent 类型，不再只能固定 `auto`。
- 会话切换时会同步当前会话的 Agent 类型。
- 左侧 Agent 能力区补齐 Web 端 `AgentSidebar` 的可点击切换行为。
- 点击左侧 Agent 能力卡中的“使用”按钮会同步切换当前 Agent 选择器。
- 当前 Agent 能力按钮会显示“当前”并高亮，便于识别当前对话发送时使用的 Agent 类型。
- Agent 能力区切换、选择器同步和高亮状态已纳入 PySide 离屏自检。
- 对话区增加当前会话 badge，展示 Agent 类型和会话短 ID。
- 聊天消息从整行卡片改为左右气泡布局。
- 用户消息使用右侧蓝色气泡，Agent 消息使用左侧白色气泡。
- 气泡顶部增加角色标签，贴近 Web 端 ChatMessage 的阅读层级。
- 历史 AI 消息气泡内新增 Web 端 `ChatMessage` 同类内联面板。
- AI 气泡内联面板支持展示模型 Thinking、处理过程和工具调用过程。
- 处理过程会按顺序去重展示，避免同一状态重复刷屏。
- 工具调用过程会过滤 `skipped` 项，只展示真实使用或失败的工具。
- 内联过程面板保留下方总调试面板，两者分别满足阅读和排查需求。
- AI 气泡内联过程面板已纳入 PySide 离屏自检。
- 推荐 prompt 与 Web 端一致，只在空会话或欢迎态显示，已有对话后自动隐藏。
- 将 Thinking、工具调用、模型 Thinking 分成独立标签页。
- 工具调用 trace 从纯 JSON 改成卡片列表展示。
- 保留原始 trace 查看能力，便于调试后端返回。
- 增加右侧上下文面板，补齐 Web 端 `ContextPanel` 的自动路由说明和快捷动作。
- 自动路由面板展示 ContentAgent、KnowledgeAgent、StudentAgent、SchoolAgent、FeishuAgent 的能力说明。
- 快捷动作补齐创建飞书文档、生成选题、整理资料、分析学员、推荐院校。
- 点击快捷动作会直接进入当前会话的流式对话入口。
- 增加右侧对话历史摘要面板，补齐 Web 端 `ConversationHistory` 的核心功能。
- 对话历史摘要展示用户轮次统计。
- 对话历史摘要按 U / AI 前缀展示每条消息预览。
- 对话历史面板支持清空当前会话。
- 空会话时展示欢迎提示，避免页面显得空白。
- 离线 Demo 模式支持完整 SSE 事件链：`session`、`thinking`、`trace`、`reasoning`、`delta`、`done`。
- 离线流式对话会写回会话历史，方便验证历史加载、删除和清空。

### 内容运营

- 主题库增加统计卡片。
- 页面顶部补齐 Web 端“去 AI 工作台生成”入口。
- “去 AI 工作台生成”入口会直接切换到 AI 工作台页面。
- 主题库增加卡片视图和表格视图。
- 主题卡片补齐 Web 端同类删除入口。
- 主题卡片删除会弹出确认框，并提示该主题下的文案和日历排期会一起删除。
- 主题详情显示平台、状态、评分、主题、热度、计划日期、标签和摘要。
- 主题详情增加“查看完整详情”弹窗。
- 主题详情弹窗展示 ID、主题、平台、类型、状态、热度、评分、计划日期、标签和摘要。
- 主题详情弹窗支持复制主题摘要。
- 主题详情弹窗支持复制主题标签。
- 主题详情弹窗支持复制完整主题卡片文本。
- 主题详情弹窗展示该主题下已加载的历史文案列表。
- 主题详情弹窗保留主题和文案原始数据，方便验收时对照接口字段。
- 主题表格支持双击打开详情弹窗。
- 文案库增加卡片视图和表格视图。
- 文案编辑区覆盖标题、渠道/版本、评分、使用状态、使用日期、反馈、配图建议和正文。
- 文案编辑区增加“查看完整详情”和“复制正文”操作。
- 文案卡片补齐 Web 端同类操作区。
- 文案卡片支持“历史”，可按文案所属主题打开该主题历史文案详情。
- 文案卡片支持“编辑”，点击后会把文案加载到右侧编辑区，便于继续修改标题、正文、使用状态、使用日期、反馈和配图建议。
- 文案卡片支持“详情”，可直接打开文案详情弹窗。
- 文案卡片支持“复制正文”。
- 文案卡片支持“删除”，删除前会出现确认框，避免误删历史文案。
- 文案所属主题查找逻辑已纳入离屏自检。
- 文案卡片编辑入口已纳入离屏自检，会校验当前文案 ID 和编辑区标题是否正确同步。
- 文案详情弹窗展示 ID、主题 ID、渠道、版本、风格、负责人、使用状态、使用日期、反馈和配图建议。
- 文案详情弹窗使用独立正文预览区域展示完整文案。
- 文案详情弹窗支持复制标题和复制正文。
- 文案详情弹窗增加图片引用页签。
- 文案详情弹窗图片引用页签展示 ID、名称、URL、存储来源和对象 Key。
- 文案详情弹窗支持复制选中图片 URL。
- 文案详情弹窗支持打开选中图片链接。
- 文案详情弹窗保留原始 JSON 页签，方便核对后端返回。
- 文案表格支持双击打开详情弹窗。
- 文案库新增图片引用入口。
- 图片引用弹窗覆盖图片名称、图片 URL、存储来源和对象 Key。
- 图片引用接口已接入 `/api/content/drafts/{draftId}/images`。
- 离线 fallback 模式下，新增图片引用会写回内存数据并刷新数量。
- 文案编辑区增加图片引用表格，展示 ID、名称、URL 和存储来源。
- 图片引用表格支持复制选中图片 URL。
- 图片引用表格支持打开选中图片链接。
- 内容日历增加月历网格视图。
- SOP 生成结果从纯 JSON 升级为“结构化卡片 + 原始 JSON”双页签。
- SOP 结构化卡片展示概览、流程步骤、生成草稿和内容资产。
- SOP 离线 Demo 返回三版草稿，覆盖干货版、情绪增强版和转化引导版。

### 知识库

- 文档列表从纯表格升级为“卡片视图 + 表格视图”双页签。
- 文档卡片按三列网格展示标题、摘要、类型、更新时间和标签。
- 文档卡片点击后会同步右侧详情面板，并打开详情弹窗，贴近 Web 端 `openDetail(doc)` 行为。
- 文档类型 badge 补齐 docx、doc、sheet、bitable、pdf、file、folder 的中文展示。
- 空筛选结果会展示空状态提示。
- 文档详情从纯 JSON 改成结构化信息面板。
- 支持查看标题、标签、来源、飞书链接、摘要和正文片段。
- 文档详情支持弹窗预览，贴近 Web 端文档详情 Dialog。
- 弹窗内支持查看元信息、正文预览、编辑标签和查看原始数据。
- 弹窗内标签编辑从单行文本升级为“标签列表 + 新增标签 + 删除选中”。
- 标签列表会同步到标签文本框，保存时会去重合并列表标签和文本标签。
- 弹窗保存后会调用标签保存接口并刷新列表。
- 文档详情支持复制摘要。
- 文档详情支持复制飞书链接。
- 文档详情支持直接打开飞书链接。
- 增加 Web 端同类统计卡片：文档数、标签数、同步数、来源。
- 增加标签筛选条，支持点击标签过滤文档。
- 增加文档标签编辑输入框和保存按钮。
- 接入 `/api/knowledge/docs/{id}/tags`，离线 Demo 模式也会写回内存数据。
- 增加同步状态页签，展示 `/api/knowledge/sync-status`。
- 增加同步日志表格，展示 `/api/knowledge/sync-logs`。
- 页面顶部同步入口文案调整为 Web 端同款“同步飞书”。
- 点击“同步飞书”后按钮会进入“同步中...”状态并临时禁用，避免重复触发同步。
- 同步完成后会展示新增、更新、跳过和错误数量摘要，并刷新文档列表、同步状态和同步日志。
- 同步失败时会恢复按钮状态，并在同步摘要区展示错误信息。
- 文档列表空状态拆分为“尚未同步飞书文档”和“没有匹配的文档”两类提示，更贴近 Web 端空状态语义。
- “同步飞书”按钮状态切换已纳入 PySide 离屏自检。
- 保留原始数据标签页，便于核对后端字段。
- 向量检索结果从纯 JSON 输出升级为“结果卡片 + 原始 JSON”双页签。
- 向量结果卡片展示来源、距离、飞书类型、标签、片段和飞书链接。
- 向量结果卡片支持复制片段。
- 向量结果卡片支持打开飞书链接。

### 学员管理

- 增加学员总数、低风险、中风险、高风险统计卡片。
- 学员列表从纯表格升级为“卡片视图 + 表格视图”双页签。
- 学员卡片展示姓名、目标院校、本科学校、专业、GPA、排名、申请阶段、风险等级和申请进度。
- 学员卡片中的申请阶段使用 badge 展示。
- 学员卡片中的风险等级使用低/中/高对应的成功、警告、危险色展示。
- 学员卡片中的申请进度使用进度条展示，贴近 Web 端 `el-progress`。
- 学员卡片支持点击后同步右侧学员画像。
- 学员卡片内支持直接触发 AI 分析。
- 学员表格中的阶段列从纯文本升级为 badge。
- 学员表格中的风险列从纯文本升级为按风险等级着色的 badge。
- 学员表格中的进度列从纯文本升级为进度条。
- 编辑、删除、AI 分析统一支持来自卡片或表格的当前选中学员。
- 右侧增加学员画像详情。
- 学员画像展示学校、专业、成绩、目标、阶段、风险和进度。
- 学员画像中的申请进度从纯文本升级为进度条。
- AI 分析结果从纯 JSON 升级为结构化卡片。
- AI 分析卡片展示风险等级、分析摘要和下一步动作。
- AI 分析保留原始结果页签，便于核对后端返回。
- 新增学员弹窗已接入 `/api/students`。
- 编辑学员弹窗已接入 `/api/students/{id}`。
- 删除学员和 AI 分析保持可用。
- 离线 Demo 模式下，新增、编辑、删除都会写回内存数据。

### 院校情报

- 增加学校数、项目数、平均匹配、最近截止统计卡片。
- 项目页增加卡片视图和表格视图。
- 项目卡片复刻 Web 端 `SchoolProjectCard` 的核心结构：学校、匹配分、项目名、截止时间、条件、材料和推荐按钮。
- 项目卡片布局从双列调整为三列，更贴近 Web 端 `grid three`。
- 点击项目卡片可更新右侧项目详情。
- 项目卡片中的“AI 匹配推荐”按钮会把当前项目送入推荐结果区，并自动切换到推荐页签。
- 项目列表右侧增加详情卡片。
- 项目详情展示院校、截止时间、匹配分、报名条件和材料要求。
- 项目详情区增加“详情弹窗”“复制条件”“复制材料”“AI 匹配推荐”操作。
- 院校项目增加独立详情弹窗，展示院校、项目名、项目类型、截止时间、匹配分、报名条件、材料要求和原始数据。
- 院校项目详情弹窗支持复制报名条件。
- 院校项目详情弹窗支持复制材料要求。
- 院校项目详情弹窗支持复制完整项目卡片文本。
- 项目表格中的匹配分从纯文本升级为进度条，贴近 Web 端匹配分强调方式。
- 新增学校弹窗已接入 `/api/schools`。
- 新增院校项目弹窗已接入 `/api/school-projects`。
- 院校推荐结果从纯 JSON 输出升级为推荐卡片列表。
- 推荐页仍保留原始结果页签，便于核对后端返回。

### 飞书同步

- 增加 docs、bitable、tasks、bot、larkCli 状态卡片。
- 同步状态使用 badge 颜色区分。
- 状态卡片从单状态码升级为 Web 端同类信息层级。
- 状态卡片展示文档同步、多维表格、任务同步、群机器人、lark-cli 的中文模块名。
- 文档/表格/任务/机器人状态卡展示总数、新增、更新、跳过、错误和最近同步时间。
- 状态卡片会同时读取知识库同步状态和飞书连接状态，兼容 `/api/knowledge/sync-status` 与 `/api/feishu/sync/status`。
- 状态码会转换为中文展示，例如 `SUCCESS` 显示为“正常”，`WAITING` 显示为“等待中”。
- 页面顶部刷新入口文案调整为 Web 端同款“刷新状态”。
- 刷新状态时按钮会进入“刷新中...”状态并临时禁用，避免重复请求。
- 刷新流程改为一次性加载同步状态和同步日志，更贴近 Web 端 `Promise.all` 的刷新语义。
- 操作结果使用结构化 JSON 面板展示。
- 增加飞书文档创建表单，覆盖标题、父文件夹 token、Markdown 内容和 `as=user`。
- 增加飞书文档读取表单，输入 docToken 后通过后端读取。
- 同步日志表格补齐新增、更新、跳过、错误等字段。
- 同步日志表格中的类型列从纯文本升级为中文 badge。
- 同步日志表格中的状态列从纯文本升级为中文状态 badge。
- 日志类型 badge 覆盖文档、多维表格、任务和机器人。
- 同步日志为空时展示“暂无同步记录，请先执行同步”空状态，而不是只显示空表格。
- 刷新按钮状态和同步日志空状态已纳入 PySide 离屏自检。
- `knowledge_files` 离线 Demo 会返回知识库文件清单，便于无后端时演示。
- 知识库文件结果从纯 JSON 输出升级为结构化表格。
- 知识库文件表格展示 ID、标题、类型、标签和飞书链接。
- 知识库文件表格支持复制选中文件链接。
- 知识库文件表格支持打开选中文件链接。
- 仍通过后端 API 访问飞书能力，桌面端不直接持有飞书凭证。

### 系统设置

- 增加 AI 模型设置页签，保持 Provider 选择和 API Key 防呆说明。
- 连接页增加后端连接测试，展示成功状态、Base URL、耗时、用户信息或错误详情。
- Prompt 页签从只读表格扩展为可新增 Prompt 模板。
- Prompt 模板从纯表格升级为“卡片视图 + 表格视图”双页签。
- Prompt 卡片展示 Agent、模板名和模板正文预览。
- Prompt 卡片支持复制模板正文。
- Prompt 卡片支持打开详情弹窗。
- Prompt 详情弹窗展示 ID、Agent、模板名、正文预览和原始数据。
- Prompt 详情弹窗支持复制模板名称。
- Prompt 详情弹窗支持复制完整模板正文。
- 增加飞书应用配置占位表单，说明密钥由后端本地配置维护。
- 权限页从原始 JSON 查询升级为角色卡片。
- 角色权限按 Dashboard、AI 工作台、内容运营、知识库、学员管理、院校情报等模块分组。
- 每个角色支持全选、清空和保存。
- 权限页增加搜索框，可按权限名称、路径或权限代码过滤。
- 角色卡片支持按权限勾选并保存到 `/api/roles/{roleCode}/permissions`。
- 权限页接入当前登录用户角色，页头展示当前角色标签。
- 非团队管理员进入权限页时会进入只读模式，刷新角色权限、查询角色权限和保存修改入口会被禁用或拦截。
- 非团队管理员只读模式会展示说明文案，避免误以为权限保存失败。
- 团队管理员 `TEAM_ADMIN` 继续保持受保护状态，复刻 Web 端“避免锁死后台”的防呆逻辑。
- 保存角色权限时继续校验至少保留一个权限，避免角色完全无法使用系统。
- 非管理员权限页只读状态已纳入 PySide 离屏自检。
- 权限页保留原始响应页签，便于和后端返回字段核对。
- 增加系统日志页签，用时间线卡片展示关键演示与初始化事件。

## 已验证命令

```powershell
.\.venv\Scripts\python.exe -m compileall -q .
.\.venv\Scripts\python.exe .\smoke_test.py
.\.venv\Scripts\python.exe .\pyside_smoke_test.py
.\.venv\Scripts\python.exe .\check_desktop.py
.\.venv\Scripts\python.exe -c "from flowmind_desktop.tk_app import FlowMindTkApp; app=FlowMindTkApp(); app.withdraw(); print(app.title()); app.destroy()"
.\.venv\Scripts\python.exe -c "from flowmind_desktop.api import ApiClient; c=ApiClient('offline://demo'); print(len(c.knowledge_docs())); print(c.knowledge_sync_status()['docs']['count'])"
.\.venv\Scripts\python.exe -c "from flowmind_desktop.api import ApiClient; c=ApiClient('offline://demo'); s=c.create_student({'name':'测试学员'}); print(c.update_student(s['id'], {'name':'更新学员'})['name']); print(c.create_school({'name':'测试大学'})['name']); print(c.create_project({'schoolName':'测试大学','projectName':'测试项目'})['projectName'])"
.\.venv\Scripts\python.exe -c "from flowmind_desktop.api import ApiClient; c=ApiClient('offline://demo'); sid=c.new_session(); events=list(c.stream_chat({'sessionId':sid,'message':'帮我生成小红书文案'})); print([e.event for e in events]); print(len(c.history('auto', sid))); print(c.clear_history('auto', sid)['ok'])"
.\.venv\Scripts\python.exe -c "from flowmind_desktop.api import ApiClient; c=ApiClient('offline://demo'); print(len(c.vector_search('课程论文', 5))); print(c.feishu_action('knowledge_files')['name'])"
.\.venv\Scripts\python.exe -c "from flowmind_desktop.api import ApiClient; c=ApiClient('offline://demo'); print(c.test_connection()['ok'])"
.\.venv\Scripts\python.exe -c "from flowmind_desktop.api import ApiClient; c=ApiClient('offline://demo'); print(len(c.permissions())); print(len(c.roles()))"
.\.venv\Scripts\python.exe -c "from flowmind_desktop.api import ApiClient; c=ApiClient('offline://demo'); print(c.knowledge_docs()[0]['title'])"
.\.venv\Scripts\python.exe -c "from flowmind_desktop.api import ApiClient; c=ApiClient('offline://demo'); d=c.drafts()[0]; print(c.add_draft_image(d['id'], {'name':'demo.png','url':'https://example.com/demo.png'})['url'])"
.\.venv\Scripts\python.exe -c "from flowmind_desktop.api import ApiClient; c=ApiClient('offline://demo'); s=c.students()[0]; print(c.analyze_student(s['id'])['risk'])"
$env:QT_QPA_PLATFORM='offscreen'; .\.venv\Scripts\python.exe -c "from PySide6.QtWidgets import QApplication; from flowmind_desktop.api import ApiClient; from flowmind_desktop.views import DashboardPage, AnalyticsPage; app=QApplication([]); c=ApiClient('offline://demo'); dashboard=DashboardPage(c); dashboard.render({'overview': c.analytics_overview(), 'distribution': c.analytics_distribution(), 'content': c.analytics_content_stats(), 'funnel': c.analytics_funnel(), 'deadlines': c.analytics_deadlines()}); analytics=AnalyticsPage(c); analytics.render({'overview': c.analytics_overview(), 'student': c.analytics_distribution(), 'funnel': c.analytics_funnel(), 'deadlines': c.analytics_deadlines(), 'studentsRows': c.students(), 'projects': c.projects(), 'themes': c.content_themes()}); print(dashboard.content_stats.canvas.labels[:2], analytics.overview_cards[0].value_label.text(), analytics.content_chart.canvas.labels, analytics.content_chart.canvas.values); dashboard.close(); analytics.close(); app.quit()"
$env:QT_QPA_PLATFORM='offscreen'; .\.venv\Scripts\python.exe -c "from PySide6.QtWidgets import QApplication; from flowmind_desktop.api import ApiClient; from flowmind_desktop.views import ThemeDetailDialog, DraftDetailDialog; app=QApplication([]); c=ApiClient('offline://demo'); theme=c.content_themes()[0]; drafts=c.theme_drafts(theme['id']); d1=ThemeDetailDialog(None, theme, drafts); d2=DraftDetailDialog(None, drafts[0]); print(d1.windowTitle()); print(d2.windowTitle()); d1.close(); d2.close(); app.quit()"
$env:QT_QPA_PLATFORM='offscreen'; .\.venv\Scripts\python.exe -c "from PySide6.QtWidgets import QApplication; from flowmind_desktop.api import ApiClient; from flowmind_desktop.views import MainWindow; app=QApplication([]); c=ApiClient('offline://demo'); w=MainWindow(c, {'token':'mock-jwt.demo','base_url':'offline://demo'}, lambda config: None); shell=w.centralWidget(); page=shell.pages[3]; themes=c.content_themes(); drafts=c.drafts(); page.render_themes(themes); page.render_drafts(drafts); print(page.theme_card_grid.count(), page.draft_card_grid.count(), page.theme_for_draft(drafts[0])['title']); page.agent_hint_button.click(); print(shell.stack.currentIndex()); w.close(); app.quit()"
$env:QT_QPA_PLATFORM='offscreen'; .\.venv\Scripts\python.exe -c "from PySide6.QtWidgets import QApplication; from flowmind_desktop.api import ApiClient; from flowmind_desktop.views import LoginPage, ShellWidget; app=QApplication([]); c=ApiClient('offline://demo'); login=LoginPage(c, {'base_url':'offline://demo'}); login.fill_account('content'); data=c.login('content','123456'); shell=ShellWidget(c, data['user'], lambda: None); disabled=sum(1 for b in shell.nav_group.buttons() if not b.isEnabled()); print(login.username.text(), data['user']['role'], disabled, shell.role_tag.text()); login.close(); shell.close(); app.quit()"
$env:QT_QPA_PLATFORM='offscreen'; .\.venv\Scripts\python.exe -c "from PySide6.QtWidgets import QApplication; from flowmind_desktop.api import ApiClient; from flowmind_desktop.views import AgentPage; app=QApplication([]); c=ApiClient('offline://demo'); page=AgentPage(c); sid=c.sessions()[0]['id']; page.current_session_id=sid; page.render_history(c.history('auto', sid)); print(page.context_tabs.count(), page.agent_selector.currentData(), page.history_preview_list.count(), page.prompts_widget.isVisible()); page.close(); app.quit()"
$env:QT_QPA_PLATFORM='offscreen'; .\.venv\Scripts\python.exe -c "from PySide6.QtWidgets import QApplication; from flowmind_desktop.api import ApiClient; from flowmind_desktop.views import KnowledgePage, KnowledgeDocDialog; app=QApplication([]); c=ApiClient('offline://demo'); page=KnowledgePage(c); docs=c.knowledge_docs(); page.render_docs(docs); page.render_doc_detail(docs[0]); dialog=KnowledgeDocDialog(page, docs[0], docs[0]['tags']); dialog.new_tag.setText('新增标签'); dialog.add_tag(); print(page.doc_card_grid.count(), page.doc_title.text(), dialog.tags()[-1]); dialog.close(); page.close(); app.quit()"
$env:QT_QPA_PLATFORM='offscreen'; .\.venv\Scripts\python.exe -c "from PySide6.QtWidgets import QApplication, QProgressBar; from flowmind_desktop.api import ApiClient; from flowmind_desktop.views import StudentsPage; app=QApplication([]); c=ApiClient('offline://demo'); page=StudentsPage(c); rows=c.students(); page.render(rows); page.open_student_card(rows[0]); page.render_student_analysis(c.analyze_student(rows[0]['id'])); widget=page.table.cellWidget(0, 10); print(page.student_card_grid.count(), page.student_name.text(), isinstance(widget, QProgressBar), page.analysis_risk_badge.text()); page.close(); app.quit()"
$env:QT_QPA_PLATFORM='offscreen'; .\.venv\Scripts\python.exe -c "from PySide6.QtWidgets import QApplication, QProgressBar; from flowmind_desktop.api import ApiClient; from flowmind_desktop.views import SchoolsPage, SchoolProjectDetailDialog; app=QApplication([]); c=ApiClient('offline://demo'); page=SchoolsPage(c); projects=c.projects(); page.render_projects(projects); page.render_project_detail(projects[0]); page.recommend_project_payload(projects[0]); dialog=SchoolProjectDetailDialog(page, projects[0]); widget=page.project_table.cellWidget(0, 7); print(page.project_card_grid.count(), page.project_title.text(), isinstance(widget, QProgressBar), page.rec_card_layout.count(), dialog.windowTitle()); dialog.close(); page.close(); app.quit()"
$env:QT_QPA_PLATFORM='offscreen'; .\.venv\Scripts\python.exe -c "from PySide6.QtWidgets import QApplication; from flowmind_desktop.api import ApiClient; from flowmind_desktop.views import FeishuPage; app=QApplication([]); c=ApiClient('offline://demo'); page=FeishuPage(c); page.render_status({'sync': c.knowledge_sync_status(), 'feishu': c.feishu_status()}); page.render_logs(c.feishu_logs()); page.render_feishu_files(c.feishu_action('knowledge_files')); print(page.feishu_badges['docs'].text(), page.feishu_detail_labels['docs'].text(), page.log_table.cellWidget(0, 1).text(), page.feishu_files_table.rowCount()); page.close(); app.quit()"
$env:QT_QPA_PLATFORM='offscreen'; .\.venv\Scripts\python.exe -c "from PySide6.QtWidgets import QApplication; from flowmind_desktop.api import ApiClient; from flowmind_desktop.views import SettingsPage, PromptDetailDialog; app=QApplication([]); c=ApiClient('offline://demo'); page=SettingsPage(c); prompts=c.prompts(); page.render_prompts(prompts); dialog=PromptDetailDialog(page, prompts[0]); print(page.prompt_card_grid.count(), dialog.windowTitle(), page.prompt_table.rowCount()); dialog.close(); page.close(); app.quit()"
```

验证结果：

- Python 语法编译通过。
- API/fallback smoke test 通过。
- PySide6 9 个页面创建与切换通过。
- 一键自检脚本 `check_desktop.py` 通过。
- Tkinter 备用窗口可创建。
- 知识库标签保存、Prompt 新增、飞书文档创建等离线接口路径通过定向检查。
- 学员新增/编辑、学校新增、项目新增、院校推荐等离线接口路径通过定向检查。
- 学员页和院校页 PySide 离屏加载与推荐卡片渲染通过定向检查。
- AI 工作台离线 SSE、历史写回、清空历史通过定向检查。
- 内容 SOP 结构化卡片渲染通过 PySide 离屏检查。
- 聊天气泡、知识库向量结果卡片、飞书文件表格通过 PySide 离屏检查。
- 连接测试、链接操作相关控件通过 PySide 离屏检查。
- 知识库详情快捷操作、角色权限模块分组、全选/清空通过 PySide 离屏检查。
- 知识库详情弹窗、标签编辑控件、权限搜索过滤通过 PySide 离屏检查。
- 文案图片引用表格、离线新增图片写回和 URL 操作通过 PySide 离屏检查。
- Dashboard hero 跳转信号和院校项目卡片渲染通过 PySide 离屏检查。
- 学员进度条和 AI 分析结构化卡片通过 PySide 离屏检查。

## 当前依赖说明

PySide6 6.11.1 在当前机器上曾出现 Qt DLL 导入失败。

已处理方式：

```powershell
.\.venv\Scripts\python.exe -m pip install --force-reinstall "PySide6==6.7.3"
```

当前固定依赖：

```text
PySide6==6.7.3
httpx>=0.27
```

PySide6 是后续高保真复刻主线。

Tkinter 版本保留为额外可运行兜底入口。

## 后续补齐清单

### AI 工作台

- 继续复刻 Web 端 ChatMessage 的气泡宽度、头像、折叠面板和 Markdown 表格样式。
- SSE 中断、错误、重试和结束状态需要更细化。
- 工具调用详情可以继续从 trace 卡片跳转到原始 JSON。
- 会话列表视觉可以继续向 Web 端靠齐，包括 hover 删除图标、相对时间和选中态细节。

### 内容运营

- 图片引用后续可以扩展为真实本地文件上传。
- 文案图片列表可以继续增加缩略图预览和删除入口。
- 主题与文案筛选条件可以继续对齐 Web 端。
- SOP 结构化结果可以继续增加“一键入库”“创建飞书文档”等快捷动作。

### 知识库

- 文档详情弹窗可以继续增强 Markdown/HTML 渲染和图片预览。
- 标签新增体验可以继续从文本输入升级为 chip 输入。
- 向量结果卡片可以继续增加“加入回答上下文”等快捷动作。

### 学员管理

- 申请阶段进度条和风险颜色可以继续细化。
- AI 分析结果可以继续增加待办清单、时间节点和一键生成跟进任务。
- 新增/编辑表单可以继续加入必填校验和字段下拉。

### 院校情报

- 截止时间、材料要求和匹配分可以继续视觉增强。
- 学校和项目后续可继续补编辑、删除与搜索筛选。
- 项目卡片可以继续加入“加入项目库”“生成材料清单”等快捷动作。

### 飞书同步

- 同步日志状态颜色可以继续复刻 Web 端。
- 真实飞书文档创建成功后可以自动复制链接或展示“在飞书中打开”。
- 知识库文件列表可以继续增加文件类型图标、预览详情和批量同步入口。

### 系统设置

- 角色权限可以继续增加按模块折叠和差异高亮。
- 系统日志可以继续接入真实后端日志或飞书同步日志。

## 运行命令

运行 PySide6 高保真版本：

```powershell
cd D:\Desktop\agent\FlowMind-Agent\desktop_fronted
.\.venv\Scripts\python.exe .\run.py
```

Windows 一键启动：

```powershell
cd D:\Desktop\agent\FlowMind-Agent\desktop_fronted
.\start_desktop.ps1
```

一键自检：

```powershell
cd D:\Desktop\agent\FlowMind-Agent\desktop_fronted
.\.venv\Scripts\python.exe .\check_desktop.py
```

Windows 一键自检：

```powershell
cd D:\Desktop\agent\FlowMind-Agent\desktop_fronted
.\check_desktop.ps1
```

运行 Tkinter 兜底版本：

```powershell
cd D:\Desktop\agent\FlowMind-Agent\desktop_fronted
.\.venv\Scripts\python.exe .\run_tk.py
```
