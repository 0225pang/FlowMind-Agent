# FlowMind Agent 桌面端复用接口文档

本文档面向新的桌面应用客户端，用于复用当前 FlowMind Agent 后端 REST/SSE 接口。

> 当前后端采用 Spring Boot 多模块聚合启动。桌面端只需要访问 HTTP 接口，不需要直接访问 MySQL、Weaviate、飞书 CLI 或任何本地配置文件。

## 1. 基础约定

### 1.1 Base URL

本地默认：

```text
http://localhost:8080
```

如果后端改端口，例如 `18080`：

```text
http://localhost:18080
```

### 1.2 统一返回格式

除流式 SSE 接口外，所有接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

失败时通常为：

```json
{
  "code": 500,
  "message": "错误信息",
  "data": null
}
```

桌面端建议判断：

- HTTP 状态码是否为 `2xx`
- `body.code === 200`
- `body.data` 是否存在

### 1.3 鉴权 Header

当前 Demo 使用 Mock Token。登录后保存 `token`，后续请求带：

```http
Authorization: Bearer mock-jwt.demo
Content-Type: application/json
```

如果桌面端暂时不做登录，也可以在 Demo 阶段默认带：

```http
Authorization: Bearer mock-jwt.demo
```

### 1.4 时间与编码

- 请求体和响应体使用 UTF-8 JSON。
- 日期一般使用：
  - 日期：`yyyy-MM-dd`
  - 时间：`yyyy-MM-dd HH:mm:ss` 或 ISO 字符串
- 现有部分 Mock 文案在源码中存在历史编码残留，但接口字段名稳定。桌面端应以字段名为准。

### 1.5 Swagger

后端启动后可访问：

```text
http://localhost:8080/swagger-ui.html
```

## 2. 接口总览

| 模块 | 接口前缀 | 说明 |
|---|---|---|
| 网关 | `/api/gateway` | 路由清单 |
| 用户认证 | `/api/auth` | 登录、注册 |
| 用户信息 | `/api/users` | 当前用户 |
| AI 工作台 | `/api/agents` | Agent 列表、会话、普通对话、流式对话 |
| Prompt | `/api/prompts` | Prompt 模板列表与新增 |
| 内容运营 | `/api/content` | 主题库、文案库、日历、内容 SOP |
| 知识库 | `/api/knowledge` | 文档、标签、同步、搜索 |
| 向量检索 | `/api/knowledge/vector` | 解耦向量检索接口 |
| 学员管理 | `/api/students` | 学员 CRUD 与分析 |
| 院校情报 | `/api/schools`, `/api/school-projects` | 院校、项目、推荐 |
| 数据分析 | `/api/analytics` | ECharts 数据 |
| 飞书 | `/api/feishu` | 飞书状态、文档、同步、日志 |

## 3. 用户认证接口

### 3.1 登录

```http
POST /api/auth/login
```

请求体：

```json
{
  "username": "admin",
  "password": "123456"
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "mock-jwt.admin",
    "user": {
      "id": 1,
      "username": "admin",
      "nickname": "FlowMind Admin",
      "role": "ADMIN"
    }
  }
}
```

桌面端处理建议：

- 将 `data.token` 保存到本地安全存储或应用状态中。
- 后续接口统一添加 `Authorization: Bearer ${token}`。

### 3.2 注册

```http
POST /api/auth/register
```

请求体：

```json
{
  "username": "operator01",
  "password": "123456"
}
```

响应：用户对象。

### 3.3 当前用户信息

```http
GET /api/users/me
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "FlowMind Admin",
    "role": "ADMIN",
    "workspace": "默认工作空间"
  }
}
```

## 4. 网关接口

### 4.1 路由清单

```http
GET /api/gateway/routes
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "service": "ai-agent-service",
      "path": "/api/agents/**,/api/prompts/**"
    }
  ]
}
```

用途：

- 桌面端调试时可用于确认后端模块暴露路径。

## 5. AI 工作台接口

### 5.1 Agent 列表

```http
GET /api/agents
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "name": "FlowMindAgent",
      "type": "auto",
      "description": "Unified agent entry..."
    },
    {
      "name": "ContentAgent",
      "type": "content",
      "description": "..."
    }
  ]
}
```

桌面端建议：

- 默认使用 `agentType: "auto"`。
- 不强制用户手动选择 Agent。

### 5.2 新建会话

```http
POST /api/agents/conversations/new
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": "a1b2c3d4"
  }
}
```

### 5.3 会话列表

```http
GET /api/agents/sessions
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "a1b2c3d4",
      "title": "根据保研知识库总结...",
      "agentType": "auto",
      "turnCount": 4,
      "createdAt": "2026-06-18T10:00:00",
      "updatedAt": "2026-06-18T10:05:00"
    }
  ]
}
```

### 5.4 删除会话

```http
DELETE /api/agents/sessions/{sessionId}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "ok": true
  }
}
```

### 5.5 加载会话历史

```http
GET /api/agents/conversations/{agentType}/{sessionId}
```

路径参数：

| 参数 | 示例 | 说明 |
|---|---|---|
| `agentType` | `auto` / `content` / `knowledge` | 会话所属 Agent 类型 |
| `sessionId` | `a1b2c3d4` | 会话 ID |

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 101,
      "agentType": "auto",
      "sessionId": "a1b2c3d4",
      "turnIndex": 0,
      "role": "user",
      "content": "根据保研知识库，期末如何速成课程论文？",
      "metadata": null,
      "createdAt": "2026-06-18T10:00:00"
    },
    {
      "id": 102,
      "agentType": "auto",
      "sessionId": "a1b2c3d4",
      "turnIndex": 0,
      "role": "assistant",
      "content": "回答正文...",
      "metadata": "{\"traceItems\":[],\"modelThinking\":\"...\"}",
      "createdAt": "2026-06-18T10:00:20"
    }
  ]
}
```

`metadata` 说明：

```json
{
  "version": 1,
  "agentType": "knowledge",
  "traceItems": [
    {
      "name": "SemanticVectorSearch",
      "type": "mcp",
      "status": "used",
      "durationMs": 132,
      "summary": "检索到相关知识库片段",
      "detail": "完整工具结果..."
    }
  ],
  "thinking": "回答完成，可展开查看本次可见处理过程。",
  "thinkingHistory": [
    "已选择 knowledge，正在查询向量知识库和可用工具。",
    "已调用 1 个工具，正在生成回答。",
    "回答完成，可展开查看本次可见处理过程。"
  ],
  "modelThinking": "模型返回的 reasoning/thinking 内容；如果模型不支持则为空",
  "createdAt": "2026-06-18T10:00:20"
}
```

兼容性：

- 老消息可能 `metadata = null`。
- 桌面端解析 `metadata` 前必须判空，并捕获 JSON parse 错误。

### 5.6 清空会话历史

```http
DELETE /api/agents/conversations/{agentType}/{sessionId}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "ok": true
  }
}
```

### 5.7 普通对话

```http
POST /api/agents/chat
```

请求体：

```json
{
  "agentType": "auto",
  "message": "帮我生成 10 个保研小红书选题",
  "sessionId": "a1b2c3d4",
  "context": {}
}
```

字段说明：

| 字段 | 必填 | 说明 |
|---|---|---|
| `agentType` | 否 | 推荐传 `auto`；也可传 `content`、`knowledge`、`student`、`school`、`feishu` |
| `message` | 是 | 用户输入 |
| `sessionId` | 否 | 不传则后端创建新会话；桌面端建议先调用新建会话 |
| `context` | 否 | 额外上下文 |

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "agentType": "content",
    "reply": "回答正文",
    "sessionId": "a1b2c3d4",
    "cards": [
      {
        "type": "trace",
        "title": "工具调用",
        "items": []
      }
    ]
  }
}
```

### 5.8 流式对话 SSE

```http
POST /api/agents/chat/stream
Accept: text/event-stream
Content-Type: application/json
```

请求体同普通对话：

```json
{
  "agentType": "auto",
  "message": "根据保研知识库，期末如何速成课程论文？",
  "sessionId": "a1b2c3d4",
  "context": {}
}
```

SSE 事件类型：

| event | data 示例 | 说明 |
|---|---|---|
| `session` | `{"sessionId":"a1b2c3d4"}` | 返回会话 ID |
| `thinking` | `{"content":"已选择 knowledge..."}` | 系统可见处理状态 |
| `trace` | `{"items":[...],"agentType":"knowledge"}` | 实际工具调用结果 |
| `reasoning` | `{"content":"模型思考增量"}` | 模型 API 返回的 reasoning/thinking 增量 |
| `delta` | `{"content":"回答正文增量"}` | 最终回答正文增量 |
| `done` | `{"ok":true,"sessionId":"a1b2c3d4"}` | 流结束 |
| `error` | `{"message":"错误信息"}` | 错误 |

桌面端解析示例伪代码：

```ts
for await (const event of sseStream) {
  if (event.name === 'delta') appendAnswer(event.data.content)
  if (event.name === 'reasoning') appendModelThinking(event.data.content)
  if (event.name === 'trace') setTraceItems(event.data.items)
  if (event.name === 'thinking') setProcessStatus(event.data.content)
  if (event.name === 'done') finish()
}
```

桌面端 UI 建议：

- `delta` 显示为 AI 正文。
- `reasoning` 显示为“模型 Thinking”，支持折叠。
- `trace.items` 显示为“工具调用过程”，只展示 `status !== skipped` 的工具。
- `thinking` 显示为浅色状态行。

## 6. Prompt 模板接口

### 6.1 Prompt 列表

```http
GET /api/prompts
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "agentType": "content",
      "name": "小红书爆款结构仿写",
      "template": "围绕 {theme} ..."
    }
  ]
}
```

### 6.2 新增 Prompt

```http
POST /api/prompts
```

请求体：

```json
{
  "id": 10,
  "agentType": "content",
  "name": "自定义内容生成模板",
  "template": "请围绕 {topic} 生成..."
}
```

响应：原样返回新增对象。

## 7. 内容运营接口

### 7.1 主题列表

```http
GET /api/content/themes?keyword=简历&status=已生成&channel=小红书
```

查询参数均可选：

| 参数 | 说明 |
|---|---|
| `keyword` | 按标题/主题搜索 |
| `status` | 状态筛选 |
| `channel` | 平台/渠道筛选 |

响应字段常见包含：

```json
{
  "id": 1,
  "title": "保研简历怎么写才像有科研潜力",
  "topic": "保研简历",
  "platform": "小红书",
  "type": "爆款仿写",
  "status": "已生成",
  "heat": 96,
  "rating": 4,
  "plannedDate": "2026-06-18",
  "summary": "主题摘要",
  "tags": ["保研简历", "科研潜力"]
}
```

### 7.2 新增主题

```http
POST /api/content/themes
```

请求体：

```json
{
  "title": "导师套磁邮件怎么写不尴尬",
  "topic": "导师套磁",
  "platform": "小红书",
  "type": "经验干货",
  "status": "待创作",
  "heat": 80,
  "plannedDate": "2026-06-25",
  "summary": "围绕导师套磁邮件结构生成内容",
  "tags": ["导师套磁", "邮件模板"]
}
```

### 7.3 删除主题

```http
DELETE /api/content/themes/{id}
```

### 7.4 主题评分

```http
PUT /api/content/themes/{id}/rating
```

请求体：

```json
{
  "rating": 5
}
```

评分范围建议：`1-5`。

### 7.5 查询某主题下文案

```http
GET /api/content/themes/{themeId}/drafts
```

响应：文案数组。

### 7.6 给主题新增文案

```http
POST /api/content/themes/{themeId}/drafts
```

请求体：

```json
{
  "title": "保研简历别再这样写了",
  "channel": "小红书",
  "version": "干货版",
  "style": "干货",
  "content": "正文内容...",
  "owner": "内容运营"
}
```

### 7.7 文案列表

```http
GET /api/content/drafts?keyword=简历&channel=小红书&usageStatus=已使用
```

查询参数均可选：

| 参数 | 说明 |
|---|---|
| `keyword` | 标题/正文搜索 |
| `channel` | 小红书、朋友圈、公众号 |
| `usageStatus` | 未使用、已使用、已归档 |

响应字段常见包含：

```json
{
  "id": 101,
  "themeId": 1,
  "title": "保研er别再这样写简历了",
  "channel": "小红书",
  "version": "干货版",
  "style": "干货",
  "content": "正文内容...",
  "usageStatus": "已使用",
  "usedDate": "2026-06-12",
  "feedback": "收藏率较高",
  "rating": 4,
  "imageSuggestion": "配图建议...",
  "images": [
    {
      "id": 1001,
      "name": "resume-cover.svg",
      "url": "/mock-assets/content/resume-cover.svg"
    }
  ]
}
```

### 7.8 编辑文案

```http
PUT /api/content/drafts/{draftId}
```

请求体：

```json
{
  "title": "新的文案标题",
  "content": "新的正文",
  "usageStatus": "已使用",
  "usedDate": "2026-06-18",
  "feedback": "发布后咨询较多",
  "imageSuggestion": "建议使用材料清单长图"
}
```

### 7.9 删除文案

```http
DELETE /api/content/drafts/{draftId}
```

### 7.10 文案评分

```http
PUT /api/content/drafts/{id}/rating
```

请求体：

```json
{
  "rating": 5
}
```

### 7.11 添加文案图片

```http
POST /api/content/drafts/{draftId}/images
```

请求体：

```json
{
  "name": "resume-cover.png",
  "url": "https://example.com/resume-cover.png",
  "storageProvider": "minio",
  "objectKey": "content/resume-cover.png"
}
```

说明：

- 当前接口保存图片引用，不负责真实上传文件。
- 桌面端如要上传本地图片，需要后续补文件上传接口或先上传到对象存储再传 URL。

### 7.12 内容日历

```http
GET /api/content/calendar?month=2026-06
```

响应：日历项数组，常见字段：

```json
{
  "id": 1,
  "themeId": 4,
  "copyId": 401,
  "publishDate": "2026-06-08",
  "channel": "小红书",
  "publishStatus": "已发布",
  "usageStatus": "已使用",
  "title": "夏令营自我介绍照这个顺序说"
}
```

### 7.13 兼容旧主题接口

```http
GET /api/content/topics
```

等价于主题列表基础查询。

### 7.14 生成选题

```http
POST /api/content/topics/generate
```

请求体：

```json
{
  "theme": "保研简历",
  "style": "干货"
}
```

响应：生成的主题/选题数据。

### 7.15 生成朋友圈文案

```http
POST /api/content/moments/generate
```

请求体：

```json
{
  "scene": "带学员复盘申请节奏"
}
```

响应：朋友圈文案数组。

### 7.16 生成公众号标题

```http
POST /api/content/articles/generate
```

请求体：

```json
{
  "topic": "保研规划"
}
```

响应：标题数组。

### 7.17 更新主题状态

```http
PUT /api/content/topics/{id}/status
```

请求体：

```json
{
  "status": "已发布"
}
```

### 7.18 小红书 SOP 生成

```http
POST /api/content/sop/xiaohongshu/generate
```

请求体：

```json
{
  "agentType": "content",
  "topic": "保研简历",
  "audience": "保研er",
  "style": "干货",
  "extra": {
    "keywords": ["保研", "简历", "科研潜力"]
  }
}
```

### 7.19 朋友圈 SOP 生成

```http
POST /api/content/sop/moments/generate
```

请求体：

```json
{
  "agentType": "moments",
  "scene": "收到学员 offer",
  "style": "专业理性",
  "extra": {
    "persona": "学姐规划师"
  }
}
```

### 7.20 内容资产提取

```http
POST /api/content/sop/assets/extract
```

请求体：

```json
{
  "agentType": "asset",
  "topic": "保研内容资产沉淀",
  "extra": {
    "content": "历史文案或SOP文本"
  }
}
```

### 7.21 内容 SOP 架构说明

```http
GET /api/content/sop/architecture
```

响应：内容生成、知识检索、飞书沉淀等架构说明。

## 8. 知识库接口

### 8.1 文档列表/搜索

```http
GET /api/knowledge/docs?keyword=课程论文
```

`keyword` 可选。

响应：

```json
{
  "id": 1,
  "feishuToken": "doc_token",
  "title": "期末如何速成课程论文",
  "content": "正文",
  "summary": "摘要",
  "tags": ["论文", "期末"],
  "feishuUrl": "https://...",
  "feishuType": "docx",
  "createdAt": "2026-06-18 10:00:00",
  "updatedAt": "2026-06-18 10:00:00"
}
```

### 8.2 文档详情

```http
GET /api/knowledge/docs/{id}
```

### 8.3 更新文档标签

```http
PUT /api/knowledge/docs/{id}/tags
```

请求体：

```json
{
  "tags": ["保研", "课程论文", "写作模板"]
}
```

### 8.4 从飞书同步知识库

```http
POST /api/knowledge/sync
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "added": 1,
    "updated": 2,
    "skipped": 0,
    "errors": 0
  }
}
```

### 8.5 同步日志

```http
GET /api/knowledge/sync-logs
```

响应：最近 50 条同步日志。

### 8.6 同步状态

```http
GET /api/knowledge/sync-status
```

响应：

```json
{
  "docs": {
    "status": "success",
    "lastSync": "2026-06-18 10:00:00",
    "added": 1,
    "updated": 2,
    "skipped": 0,
    "errors": 0,
    "count": 3
  },
  "bitable": {},
  "tasks": {},
  "bot": {}
}
```

### 8.7 知识库统计

```http
GET /api/knowledge/stats
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "docCount": 10
  }
}
```

### 8.8 标签列表

```http
GET /api/knowledge/tags
```

响应：字符串数组。

### 8.9 语义搜索（直接 Weaviate）

```http
GET /api/knowledge/search?q=课程论文&topK=10
```

说明：

- 该接口要求 Weaviate 启用。
- 如果 Weaviate 未启用，会返回失败。

### 8.10 解耦向量检索接口（推荐桌面端使用）

```http
GET /api/knowledge/vector/search?q=课程论文&topK=5
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "source": "weaviate",
      "mysqlId": 1,
      "title": "期末如何速成课程论文",
      "chunkText": "相关片段...",
      "feishuToken": "doc_token",
      "feishuUrl": "https://...",
      "feishuType": "docx",
      "tags": ["论文"],
      "distance": 0.23
    }
  ]
}
```

推荐原因：

- 会优先查 Weaviate。
- 无结果或不可用时可 fallback 到 MySQL。
- 返回结构更适合桌面端展示来源。

## 9. 飞书接口

### 9.1 飞书同步状态

```http
GET /api/feishu/sync/status
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "docs": "CONNECTED",
    "bitable": "WAITING",
    "tasks": "WAITING",
    "bot": "READY",
    "larkCli": "AVAILABLE"
  }
}
```

### 9.2 创建飞书文档

```http
POST /api/feishu/docs/create
```

请求体：

```json
{
  "title": "文档标题",
  "content": "# Markdown 内容\n\n正文",
  "parentToken": "目标文件夹 token，可为空",
  "as": "user"
}
```

说明：

- `parentToken` 为空时由飞书工具默认创建位置决定。
- 如果要创建到共享文件夹，需要传目标文件夹 token。
- `as` 当前建议传 `"user"`。

### 9.3 读取飞书文档

```http
POST /api/feishu/docs/fetch
```

请求体：

```json
{
  "docToken": "飞书文档 token",
  "as": "user"
}
```

### 9.4 lark-cli 状态

```http
GET /api/feishu/lark-cli/status
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "available": true,
    "version": "..."
  }
}
```

### 9.5 知识库共享文件夹文件列表

```http
GET /api/feishu/knowledge-base/files
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "name": "保研知识库",
    "folderToken": "KELsfW0jvlHcVqdiuTncQ66Lnnc",
    "result": {}
  }
}
```

### 9.6 Mock 同步飞书文档

```http
POST /api/feishu/sync/docs
```

### 9.7 Mock 同步多维表格

```http
POST /api/feishu/sync/bitable
```

### 9.8 Mock 同步任务

```http
POST /api/feishu/sync/tasks
```

### 9.9 Mock 群机器人推送

```http
POST /api/feishu/bot/push
```

请求体：

```json
{
  "message": "推送内容"
}
```

### 9.10 飞书同步日志

```http
GET /api/feishu/logs
```

响应：同步日志数组。

## 10. 学员管理接口

### 10.1 学员列表

```http
GET /api/students
```

响应：

```json
{
  "id": 1,
  "name": "学员01",
  "school": "示例大学",
  "major": "金融学",
  "gpa": "3.60",
  "rank": "3/120",
  "english": "六级 560",
  "targetSchool": "985/211 经管项目",
  "stage": "材料准备",
  "risk": "中",
  "progress": 70
}
```

### 10.2 新增学员

```http
POST /api/students
```

请求体可传任意学员字段：

```json
{
  "name": "学员21",
  "school": "示例大学",
  "major": "食品科学",
  "gpa": "3.80",
  "rank": "5/120",
  "english": "六级 580",
  "targetSchool": "985 食品项目",
  "stage": "材料准备",
  "risk": "低",
  "progress": 60
}
```

### 10.3 学员详情

```http
GET /api/students/{id}
```

### 10.4 更新学员

```http
PUT /api/students/{id}
```

请求体：完整学员对象。

### 10.5 删除学员

```http
DELETE /api/students/{id}
```

### 10.6 学员 AI 分析

```http
POST /api/students/{id}/analyze
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1,
    "risk": "中风险",
    "summary": "申请建议..."
  }
}
```

## 11. 院校情报接口

### 11.1 学校列表

```http
GET /api/schools
```

### 11.2 新增学校

```http
POST /api/schools
```

请求体示例：

```json
{
  "name": "示例大学",
  "region": "北京",
  "level": "985"
}
```

### 11.3 院校项目列表

```http
GET /api/school-projects
```

响应字段：

```json
{
  "id": 1,
  "schoolName": "示例大学",
  "projectName": "2026 夏令营项目",
  "deadline": "2026-07-01",
  "requirements": "报名条件",
  "materials": "材料要求",
  "matchScore": 88
}
```

### 11.4 新增院校项目

```http
POST /api/school-projects
```

请求体：

```json
{
  "schoolName": "示例大学",
  "projectName": "2026 夏令营项目",
  "deadline": "2026-07-01",
  "requirements": "专业排名前30%",
  "materials": "简历、成绩单、个人陈述",
  "matchScore": 85
}
```

### 11.5 院校推荐

```http
POST /api/schools/recommend
```

请求体：

```json
{
  "gpa": "3.80",
  "rank": "5/120",
  "english": "六级 580",
  "targetMajor": "食品科学",
  "riskPreference": "稳妥"
}
```

响应：推荐项目数组。

## 12. 数据分析接口

### 12.1 概览数据

```http
GET /api/analytics/overview
```

响应：

```json
{
  "students": 20,
  "activeProjects": 15,
  "contentTopics": 48,
  "taskRate": "86%"
}
```

### 12.2 学员分布

```http
GET /api/analytics/student-distribution
```

响应：

```json
{
  "labels": ["985", "211", "双非", "其他"],
  "values": [8, 5, 4, 3]
}
```

### 12.3 内容统计

```http
GET /api/analytics/content-stats
```

响应：

```json
{
  "labels": ["简历", "套磁", "规划"],
  "values": [18, 12, 9]
}
```

### 12.4 申请阶段漏斗

```http
GET /api/analytics/application-funnel
```

响应：

```json
[
  {
    "name": "初筛",
    "value": 20
  }
]
```

### 12.5 院校截止趋势

```http
GET /api/analytics/school-deadlines
```

响应：

```json
{
  "labels": ["6月", "7月", "8月", "9月", "10月"],
  "values": [3, 7, 12, 9, 4]
}
```

## 13. 桌面端推荐数据模型

### 13.1 ApiResponse

```ts
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}
```

### 13.2 AgentRequest

```ts
interface AgentRequest {
  agentType?: 'auto' | 'content' | 'knowledge' | 'student' | 'school' | 'feishu' | string
  message: string
  sessionId?: string
  context?: Record<string, unknown>
}
```

### 13.3 AgentTraceItem

```ts
interface AgentTraceItem {
  name: string
  type: string
  status: 'used' | 'failed' | 'skipped' | string
  durationMs?: number
  summary?: string
  detail?: string
}
```

### 13.4 MessageMetadata

```ts
interface MessageMetadata {
  version?: number
  agentType?: string
  traceItems?: AgentTraceItem[]
  thinking?: string
  thinkingHistory?: string[]
  modelThinking?: string
  createdAt?: string
}
```

### 13.5 ConversationItem

```ts
interface ConversationItem {
  id: number
  agentType: string
  sessionId: string
  turnIndex: number
  role: 'user' | 'assistant' | string
  content: string
  metadata?: string | MessageMetadata | null
  createdAt: string
}
```

### 13.6 ContentTheme

```ts
interface ContentTheme {
  id: number
  title: string
  topic: string
  platform: string
  type: string
  status: string
  heat?: number
  rating?: number | null
  plannedDate?: string | null
  summary?: string
  tags?: string[]
}
```

### 13.7 CopyDraft

```ts
interface CopyDraft {
  id: number
  themeId: number
  title: string
  channel: string
  version: string
  style: string
  content: string
  usageStatus: string
  usedDate?: string | null
  feedback?: string
  rating?: number | null
  imageSuggestion?: string
  images?: CopyImage[]
}

interface CopyImage {
  id: number
  name: string
  url: string
}
```

## 14. 桌面端对接建议

### 14.1 首屏流程

推荐桌面端启动流程：

1. 检查本地是否已有 token。
2. 如果没有 token，打开登录页。
3. 登录成功后调用 `/api/users/me`。
4. 并行加载：
   - `/api/agents/sessions`
   - `/api/analytics/overview`
   - `/api/content/themes`
   - `/api/knowledge/stats`

### 14.2 AI 工作台流程

推荐：

1. 新建或选择 `sessionId`。
2. 调用 `/api/agents/chat/stream`。
3. 解析 SSE：
   - `delta` 进入正文区域。
   - `reasoning` 进入模型 Thinking 面板。
   - `trace` 进入工具调用面板。
   - `thinking` 进入状态行。
4. 流结束后刷新会话列表。
5. 切换会话时调用历史接口，并解析 `metadata`。

### 14.3 内容运营流程

推荐：

1. 主题页调用 `/api/content/themes`。
2. 点击主题后调用 `/api/content/themes/{themeId}/drafts`。
3. 文案库页调用 `/api/content/drafts`。
4. 日历页调用 `/api/content/calendar?month=yyyy-MM`。
5. 评分调用对应 `/rating` 接口。
6. 图片引用调用 `/api/content/drafts/{draftId}/images`。

### 14.4 错误处理

桌面端建议统一处理：

- HTTP 非 2xx：网络或服务异常。
- `code !== 200`：业务异常，展示 `message`。
- SSE `error` 事件：结束本次生成，保留已生成内容并展示错误。
- `metadata` JSON 解析失败：忽略 metadata，不影响正文显示。
- 飞书接口失败：提示用户检查 `lark-cli` 授权和飞书权限。

## 15. 当前接口注意事项

1. 部分 Demo 数据是内存 Mock，重启后可能恢复默认数据。
2. 内容运营核心数据已接 MySQL 初始化表，适合桌面端重点复用。
3. 飞书接口依赖本机 `lark-cli` 和用户授权。
4. `reasoning` 事件只有模型 API 返回 thinking/reasoning 字段时才会出现。
5. 如果使用 `deepseek-chat`，通常没有模型 Thinking；如果使用 reasoner 类模型才可能有。
6. 图片接口当前保存 URL，不负责二进制上传。
7. 桌面端不要直接读取 `application-local.yml`，也不要持有 LLM API Key。
8. 所有外部能力应通过后端接口访问。

