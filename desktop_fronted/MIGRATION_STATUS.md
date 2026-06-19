# FlowMind 桌面端迁移进度

目标：尽可能 100% 复刻当前 Vue Web 前端，并使用 Python 实现一版可运行的电脑客户端。

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

- 登录页。
- Dashboard 首页。
- AI 工作台。
- 内容运营。
- 知识库。
- 学员管理。
- 院校情报。
- 数据分析。
- 飞书同步。
- 系统设置。

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

### Dashboard 与数据分析

- 将原本偏文本的统计区域改成轻量图表卡片。
- 覆盖学员分布、内容统计、申请阶段漏斗和院校截止趋势。
- 保留离线 Mock 数据，后端不可用时仍可展示完整页面。
- 图表采用 PySide 自绘组件，不额外引入 matplotlib，减少依赖风险。

### AI 工作台

- 保留会话列表、Agent 列表、推荐 prompt 和流式对话入口。
- 将 Thinking、工具调用、模型 Thinking 分成独立标签页。
- 工具调用 trace 从纯 JSON 改成卡片列表展示。
- 保留原始 trace 查看能力，便于调试后端返回。
- 空会话时展示欢迎提示，避免页面显得空白。

### 内容运营

- 主题库增加统计卡片。
- 主题库增加卡片视图和表格视图。
- 主题详情显示平台、状态、评分、主题、热度、计划日期、标签和摘要。
- 文案库增加卡片视图和表格视图。
- 文案编辑区覆盖标题、渠道/版本、评分、使用状态、使用日期、反馈、配图建议和正文。
- 文案库新增图片引用入口。
- 图片引用弹窗覆盖图片名称、图片 URL、存储来源和对象 Key。
- 图片引用接口已接入 `/api/content/drafts/{draftId}/images`。
- 离线 fallback 模式下，新增图片引用会写回内存数据并刷新数量。
- 内容日历增加月历网格视图。
- SOP 生成结果使用结构化 JSON 面板展示。

### 知识库

- 文档详情从纯 JSON 改成结构化信息面板。
- 支持查看标题、标签、来源、飞书链接、摘要和正文片段。
- 保留原始数据标签页，便于核对后端字段。
- 向量检索结果保留独立标签页。

### 学员管理

- 增加学员总数、低风险、中风险、高风险统计卡片。
- 右侧增加学员画像详情。
- 学员画像展示学校、专业、成绩、目标、阶段、风险和进度。
- AI 分析结果保留独立标签页。

### 院校情报

- 增加学校数、项目数、平均匹配、最近截止统计卡片。
- 项目列表右侧增加详情卡片。
- 项目详情展示院校、截止时间、匹配分、报名条件和材料要求。
- 院校推荐结果仍保留 JSON 输出，后续可继续卡片化。

### 飞书同步

- 增加 docs、bitable、tasks、bot、larkCli 状态卡片。
- 同步状态使用 badge 颜色区分。
- 操作结果使用结构化 JSON 面板展示。
- 仍通过后端 API 访问飞书能力，桌面端不直接持有飞书凭证。

## 已验证命令

```powershell
.\.venv\Scripts\python.exe -m compileall -q .
.\.venv\Scripts\python.exe .\smoke_test.py
.\.venv\Scripts\python.exe .\pyside_smoke_test.py
.\.venv\Scripts\python.exe -c "from flowmind_desktop.tk_app import FlowMindTkApp; app=FlowMindTkApp(); app.withdraw(); print(app.title()); app.destroy()"
```

验证结果：

- Python 语法编译通过。
- API/fallback smoke test 通过。
- PySide6 9 个页面创建与切换通过。
- Tkinter 备用窗口可创建。

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
- 会话删除、清空历史和刷新状态需要做成完整按钮组。
- SSE 中断、错误、重试和结束状态需要更细化。
- 工具调用详情可以继续从 trace 卡片跳转到原始 JSON。

### 内容运营

- SOP 生成结果从 JSON 面板进一步拆成标题、正文、素材建议和发布步骤卡片。
- 图片引用后续可以扩展为真实本地文件上传。
- 文案图片列表可以增加预览、复制 URL 和删除入口。
- 主题与文案筛选条件可以继续对齐 Web 端。

### 知识库

- 标签编辑接口需要接入。
- 同步日志和同步状态可以拆成独立页签。
- 向量检索结果可以进一步卡片化展示来源、距离、飞书链接和片段。

### 学员管理

- 新增、编辑、删除表单需要补齐字段校验。
- 申请阶段进度条和风险颜色可以继续细化。
- AI 分析结果可以拆成风险、摘要、建议动作和待办清单。

### 院校情报

- 学校和项目新增表单需要继续增强。
- 项目推荐结果可以从 JSON 改为卡片视图。
- 截止时间、材料要求和匹配分可以继续视觉增强。

### 飞书同步

- 创建飞书文档和读取飞书文档表单可以继续补齐。
- 共享知识库文件列表可以结构化展示。
- 同步日志状态颜色可以继续复刻 Web 端。

### 系统设置

- Prompt 新增表单需要继续完善。
- 角色权限勾选编辑需要继续细化。
- 后端连接测试按钮可以显示延迟、状态码和错误详情。

## 运行命令

运行 PySide6 高保真版本：

```powershell
cd D:\Desktop\agent\FlowMind-Agent\desktop_fronted
.\.venv\Scripts\python.exe .\run.py
```

运行 Tkinter 兜底版本：

```powershell
cd D:\Desktop\agent\FlowMind-Agent\desktop_fronted
.\.venv\Scripts\python.exe .\run_tk.py
```
