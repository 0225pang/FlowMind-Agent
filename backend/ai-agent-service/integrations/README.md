# Agent Service 集成能力目录

这个目录用于存放属于 `ai-agent-service` 边界内的外部能力运行时、MCP 服务、协议适配器和本地工具。

建议约定：

- FlowMind 自己的智能体业务能力代码放在：`src/main/java/com/flowmind/agent/extension`
- 第三方 MCP 服务、HTTP 包装器、本地工具运行时放在：`backend/ai-agent-service/integrations`
- 智能体优先调用 FlowMind 自己的 Java 扩展类，不直接把高风险第三方工具暴露给大模型。

这样做的好处是：小组成员可以把能力放进统一目录，但主智能体仍通过 FlowMind 的适配层调用，后续维护和权限控制会更清楚。

## 小红书 MCP

小红书 MCP 运行时代码已放在：

```text
backend/ai-agent-service/integrations/xiaohongshu-mcp/
```

FlowMind 面向智能体的业务适配代码在：

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/xiaohongshu/
```

当前 FlowMind 只开放内容运营链路：

- 按主题搜索小红书热点帖子。
- 在真实 MCP 可用时读取公开笔记详情。
- 提取标题钩子、开头钩子、正文结构和转化结尾。
- 将“小红书爆款仿写 SOP”上下文注入总智能体。
- 由 LLM 生成原创小红书文案、标题和内容资产字段。

以下高风险动作已登记，但默认不开放给智能体：

- 发布图文笔记。
- 发布视频笔记。
- 发表评论。
- 回复评论。
- 点赞。
- 收藏。

这些能力必须等到系统具备确认弹窗、角色权限、审计日志、频率限制和人工复核流程后，才能逐步开放。

## 后端启动时自动启动 MCP

自动启动逻辑在：

```text
src/main/java/com/flowmind/agent/extension/xiaohongshu/XiaohongshuMcpProcessManager.java
```

当 Spring Boot 后端启动时，它会先检查：

```http
GET http://localhost:18060/health
```

如果这个地址没有响应，并且配置中开启了 `auto-start: true`，后端会在小红书 MCP 目录下自动执行：

```powershell
go run . -port :18060 -headless=true
```

也就是说，正常情况下你只需要启动后端：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend"
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

后端会尝试自动拉起小红书 MCP 服务。

## 前置条件

小红书 MCP 是 Go 项目，因此本机必须能执行：

```powershell
go version
```

该 MCP 项目要求 Go 1.24+。如果当前终端提示 `go` 不是可识别命令，说明 Go 没有安装或没有加入 PATH。此时后端仍然能正常启动，但小红书能力会回退到 FlowMind mock 热帖数据。

## 如何确认 MCP 是否启动成功

后端启动后，另开一个 PowerShell：

```powershell
Invoke-WebRequest http://localhost:18060/health
```

成功时会返回 JSON，里面通常包含：

```json
{
  "success": true,
  "data": {
    "status": "healthy",
    "service": "xiaohongshu-mcp"
  }
}
```

也可以测试 FlowMind 的代理接口：

```powershell
Invoke-WebRequest "http://localhost:8080/api/agents/xiaohongshu/search?topic=保研简历&limit=5"
```

如果返回中的 `mode` 是 `real`，说明已经调用真实 MCP。

如果返回中的 `mode` 是 `mock`，说明 MCP 没有启动成功或没有返回有效数据，系统正在使用 mock 热帖结构兜底。
