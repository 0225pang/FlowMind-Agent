# 小红书 SOP Skill 与 MCP 适配说明

本模块把 `xpzouying/xiaohongshu-mcp` 的能力适配进 FlowMind Agent。

在课程设计中，小红书内容搜索、爆款结构拆解、SOP 仿写、内容资产沉淀都属于 FlowMind 的内容运营智能体能力，因此业务适配代码直接放在 `ai-agent-service` 里。

结构划分如下：

- FlowMind 自己的业务能力代码：`backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/xiaohongshu/`
- 小红书 MCP 运行时代码：`backend/ai-agent-service/integrations/xiaohongshu-mcp/`

智能体不会直接调用第三方高风险工具。大模型只看到 FlowMind Java 适配层整理后的安全能力和 SOP 上下文。

## 当前开放范围

已开放给智能体：

- 检查 MCP 是否可用。
- 按主题搜索小红书热点笔记。
- 在真实 MCP 可用时读取公开笔记详情。
- 抽取爆款标题、开头钩子、正文结构、互动信号和转化结尾。
- 将“小红书 SOP Skill”上下文注入总智能体。

已登记但暂不开放：

- 发布图文笔记。
- 发布视频笔记。
- 发表评论。
- 回复评论。
- 点赞。
- 收藏。

这些高风险能力现在不暴露给智能体，防止误发布、误评论或误操作真实账号。

## 相关文件

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/xiaohongshu/XiaohongshuMcpToolRegistry.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/xiaohongshu/XiaohongshuMcpClient.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/xiaohongshu/XiaohongshuMcpProcessManager.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/xiaohongshu/XiaohongshuSopSkillExtension.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/xiaohongshu/XiaohongshuMcpController.java
backend/ai-agent-service/integrations/xiaohongshu-mcp/
backend/ai-agent-service/integrations/README.md
```

说明：

- `XiaohongshuMcpToolRegistry`：登记小红书能力，并区分安全读能力和高风险写能力。
- `XiaohongshuMcpClient`：调用小红书 MCP 的 HTTP API，并把结果标准化成 FlowMind 热帖结构。
- `XiaohongshuMcpProcessManager`：Spring Boot 启动时自动检查并启动 MCP 进程。
- `XiaohongshuSopSkillExtension`：识别“小红书、爆款、仿写、选题”等意图，并把 SOP 上下文注入总智能体。
- `XiaohongshuMcpController`：提供前端和调试用接口。

## 配置位置

默认配置在：

```text
backend/app-service/src/main/resources/application.yml
```

关键配置如下：

```yaml
flowmind:
  tools:
    xiaohongshu-mcp:
      enabled: true
      agent-enabled: true
      auto-start: true
      command: ${FLOWMIND_XHS_MCP_COMMAND:go}
      working-dir: ${FLOWMIND_XHS_MCP_WORKING_DIR:ai-agent-service/integrations/xiaohongshu-mcp}
      port: ${FLOWMIND_XHS_MCP_PORT:18060}
      headless: ${FLOWMIND_XHS_MCP_HEADLESS:true}
      go-cache: ${FLOWMIND_XHS_MCP_GO_CACHE:.gocache/xiaohongshu-mcp}
      base-url: ${FLOWMIND_XHS_MCP_BASE_URL:http://localhost:18060}
      search-path: /api/v1/feeds/search
      detail-path: /api/v1/feeds/detail
      timeout-seconds: 20
      default-limit: 8
      mock-fallback: true
```

字段含义：

- `enabled`：是否启用真实小红书 MCP 调用。
- `agent-enabled`：是否允许总智能体使用这个 skill。
- `auto-start`：后端启动时是否自动启动 MCP 进程。
- `command`：启动 MCP 使用的命令，默认是 `go`。
- `working-dir`：MCP 源码目录。
- `port`：MCP 服务端口，默认 `18060`。
- `headless`：浏览器是否无头运行。
- `go-cache`：MCP 编译缓存目录，默认放在项目 `.gocache/xiaohongshu-mcp`。
- `base-url`：FlowMind 调用 MCP 的地址。
- `search-path`：搜索笔记接口。
- `detail-path`：笔记详情接口。
- `mock-fallback`：真实 MCP 不可用时是否回退到 mock 数据。

## 后端启动时是否会自动启动 MCP

是的，使用下面这条命令启动后端时，后端会尝试自动启动小红书 MCP：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend"
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

启动流程是：

1. Spring Boot 启动 `app-service`。
2. `XiaohongshuMcpProcessManager` 检查 `http://localhost:18060/health`。
3. 如果 MCP 已经运行，就直接复用。
4. 如果 MCP 没运行，并且 `auto-start=true`，后端执行：

```powershell
go run . -port :18060 -headless=true
```

5. 如果本机没有 Go，或 MCP 启动失败，后端不会崩溃，会记录 warning，并回退到 mock 热帖结构。

## 前置条件

当前 MCP 是 Go 项目，本机需要安装 Go，并且终端能执行：

```powershell
go version
```

如果提示 `go` 不是可识别命令，说明还不能自动启动真实 MCP。

该 MCP 项目要求 Go 1.24+。

## 如何确认 MCP 是否启动成功

### 方式一：检查 MCP 健康接口

后端启动后，另开 PowerShell：

```powershell
Invoke-WebRequest http://localhost:18060/health
```

成功时会看到类似内容：

```json
{
  "success": true,
  "data": {
    "status": "healthy",
    "service": "xiaohongshu-mcp"
  },
  "message": "服务正常"
}
```

### 方式二：检查 FlowMind 代理接口

```powershell
Invoke-WebRequest "http://localhost:8080/api/agents/xiaohongshu/search?topic=保研简历&limit=5"
```

如果响应里是：

```json
"mode": "real"
```

说明 FlowMind 已经调到了真实 MCP。

如果响应里是：

```json
"mode": "mock"
```

说明真实 MCP 没有可用，当前使用的是 FlowMind mock 数据。

### 方式三：看后端日志

后端启动日志中如果出现：

```text
Started Xiaohongshu MCP from ... on port 18060
```

说明后端已经尝试启动 MCP。

如果出现：

```text
Failed to start Xiaohongshu MCP
```

通常是 Go 未安装、Go 版本过低、依赖下载失败，或本地浏览器环境问题。

### Windows Err:225 / leakless.exe

如果二维码接口报错：

```text
Path: ...\leakless.exe
Err: 225
```

这是 Windows Defender 或安全软件拦截了 go-rod 的 `leakless.exe`。项目已通过本地模块：

```text
backend/ai-agent-service/integrations/xiaohongshu-mcp/localmods/headless_browser/
```

禁用 `Leakless`，避免执行该临时程序。修改后需要重启后端和 MCP 进程才会生效。

## 测试接口

查看 FlowMind 暴露给智能体的工具：

```http
GET /api/agents/xiaohongshu/tools
```

搜索小红书热点笔记：

```http
GET /api/agents/xiaohongshu/search?topic=保研简历&limit=8
```

读取笔记详情：

```http
GET /api/agents/xiaohongshu/detail?id={feedId|xsecToken}
```

注意：小红书详情接口需要 `feedId` 和 `xsecToken`，所以推荐先通过搜索接口拿到结果里的 `id` 字段。

## AI 工作台触发方式

在 AI 工作台输入：

```text
搜索小红书关于“保研简历”的热点帖子，并按爆款结构仿写三版笔记。
```

统一总智能体会自动识别这些意图：

```text
小红书、爆款、高赞、热点帖子、仿写、笔记、选题、内容运营
```

触发后，Skill 会向大模型注入：

1. 热点笔记样本。
2. 点赞、收藏、评论等互动信号。
3. 标题钩子和结构线索。
4. 小红书爆款仿写 SOP。
5. 安全规则。

期望模型输出：

- 爆款结构分析。
- 可复用结构模板。
- 原创小红书笔记正文。
- 10 个标题备选。
- 干货版、情绪增强版、转化引导版三种文案。
- 可写入飞书文档、多维表格和内容库的字段。

## 安全规则

当前不要把发布、点赞、收藏、评论等能力开放给 `AgentTraceService` 或总智能体。

除非系统已经具备：

- 用户确认弹窗。
- 角色权限控制。
- 操作审计日志。
- 频率限制。
- 人工复核与撤销流程。

## 为什么这部分可以算项目业务代码

课程设计里可以这样描述：

FlowMind 并不是简单把第三方 MCP 工具丢给大模型，而是在 `ai-agent-service` 内实现了一层面向业务的智能体能力适配，包括：

- 小红书内容运营意图识别。
- 热点笔记结果标准化。
- 爆款热度评分。
- SOP 上下文构造。
- 工具风险分级与隔离。
- 前端和 App 可调用的调试接口。
- 面向内容资产沉淀的输出字段设计。

第三方 MCP 运行时代码在 `integrations/xiaohongshu-mcp` 中，属于 FlowMind Agent 服务集成的一部分；真正和业务流程绑定的是 `extension/xiaohongshu` 下的 Java 适配层。
