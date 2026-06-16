# FlowMind Agent Backend

Spring Boot 3.x 多模块 Maven Demo。当前采用 `app-service` 聚合启动，内部保留 `gateway-service`、`user-service`、`ai-agent-service`、`knowledge-service`、`content-service`、`student-service`、`school-service`、`analytics-service`、`feishu-service` 等服务边界，后续可以拆成真实微服务。

## 启动 MySQL

如果你已经创建过 `mysql9` 容器：

```powershell
docker start mysql9
```

如果容器不存在，先创建：

```powershell
docker run -d --name mysql9 -p 3306:3306 -v H:\Docker\mysql:/var/lib/mysql -e MYSQL_ROOT_PASSWORD=123456 mysql:9.7.0
```

应用会连接本机 MySQL，并自动创建 `FlowMind` 数据库需要的内容运营表。配置文件在：

```text
app-service/src/main/resources/application.yml
```

## 启动后端

在 PowerShell 中进入后端目录：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend"
```

推荐使用项目内 Maven 仓库启动，避免 Windows 用户目录 `.m2` 权限问题：

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

如果提示 `Port 8080 was already in use`，说明 8080 端口被占用，可以临时换端口：

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run "-Dspring-boot.run.arguments=--server.port=18080"
```

## 访问地址

默认端口：

```text
API: http://localhost:8080
Swagger: http://localhost:8080/swagger-ui.html
```

如果使用 `18080` 端口：

```text
API: http://localhost:18080
Swagger: http://localhost:18080/swagger-ui.html
```

Demo 账号：

```text
admin / 123456
```

除 `/api/auth/**` 和 Swagger 外，接口需要请求头：

```text
Authorization: Bearer mock-jwt.demo
```

## 常见问题

如果看到 `Unable to find a suitable main class`，不要在父工程直接执行 `spring-boot:run`，请使用：

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

如果看到 `AccessDeniedException: C:\Users\Lenovo\.m2\repository...`，说明 Maven 访问用户目录仓库被拒绝，请保留 `-s maven-settings.xml`。

如果看到 `Port 8080 was already in use`，可以查看占用进程：

```powershell
Get-NetTCPConnection -LocalPort 8080 | Select-Object LocalAddress,LocalPort,State,OwningProcess
```

然后选择停止占用进程，或直接使用上面的 `18080` 端口启动命令。

## 模块结构

| 模块 | 说明 |
| --- | --- |
| `common-core` | 共享工具类，如 `ApiResponse`、`IdGenerator` |
| `common-security` | Mock Token 和简单鉴权 |
| `common-web` | CORS、Swagger 等 Web 配置 |
| `gateway-service` | 网关边界预留 |
| `user-service` | 用户登录、注册、用户信息 |
| `ai-agent-service` | AI Agent 路由和 Mock LLM |
| `knowledge-service` | 知识库管理 |
| `content-service` | 内容运营主题库、文案库、内容日历、评分和图片记录 |
| `student-service` | 学员管理 |
| `school-service` | 院校情报 |
| `analytics-service` | 数据分析 |
| `feishu-service` | 飞书 Mock 同步 |
| `app-service` | 聚合启动入口 `FlowMindApplication` |

## DeepSeek API / OpenAI-Compatible LLM

AI 工作台后端没有绑定 DeepSeek SDK，而是使用 OpenAI-compatible HTTP Client。DeepSeek、OpenAI、通义/豆包兼容网关、本地兼容接口，后续都可以通过配置切换。

配置文件：

```text
app-service/src/main/resources/application-deepseek.yml
```

把你的 DeepSeek API Token 填到这里：

```yaml
flowmind:
  llm:
    provider: deepseek
    base-url: https://api.deepseek.com
    chat-path: /chat/completions
    model: deepseek-chat
    api-key: PUT_YOUR_DEEPSEEK_API_TOKEN_HERE
```

使用 DeepSeek API 启动后端：

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=deepseek"
```

如果 8080 端口被占用，同时指定 18080：

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=deepseek --server.port=18080"
```

流式对话接口：

```text
POST /api/agents/chat/stream
Content-Type: application/json
Accept: text/event-stream
```

请求体示例：

```json
{
  "agentType": "content",
  "message": "帮我生成10个保研小红书选题",
  "context": {}
}
```

后续扩展 MCP / Skill：

- MCP 能力实现 `McpToolProvider`
- Skill 能力实现 `SkillProvider`
- 注册成 Spring Bean 后，会自动进入 Agent 系统提示词

## Lark CLI / Feishu Skill Bridge

Your local machine already has `lark-cli` installed if this command works:

```powershell
lark-cli --version
```

Current verified version on this machine:

```text
lark-cli version 1.0.53
```

FlowMind registers it as an MCP-style extension through:

```text
ai-agent-service/src/main/java/com/flowmind/agent/extension/LarkCliMcpExtension.java
```

Config:

```yaml
flowmind:
  tools:
    lark-cli:
      enabled: true
      command: lark-cli
      timeout-seconds: 5
```

If `lark-cli` is not in the PATH used by Maven/Spring Boot, change `command` to the real executable path. On this machine PowerShell resolves it to:

```text
D:\Node.js\node_global\lark-cli.ps1
```

Important: the bridge executes only whitelisted Feishu document operations through `LarkCliToolService`. Do not let the LLM execute arbitrary shell commands directly. Add future Feishu capabilities, such as bitable record creation, task creation and bot push, as explicit whitelist methods.

### Feishu Doc Creation From AI Workbench

FlowMind now supports a whitelisted Feishu document tool:

- Tool service: `ai-agent-service/src/main/java/com/flowmind/agent/service/LarkCliToolService.java`
- Agent: `ai-agent-service/src/main/java/com/flowmind/agent/core/FeishuAgent.java`
- Auto route: `ai-agent-service/src/main/java/com/flowmind/agent/service/AgentRouter.java`

When the AI workbench receives a request like:

```text
给我创建一个飞书文档，里面写上我喜欢刘昌乐
```

the backend routes it to `FeishuAgent`, calls `lark-cli docs +create`, and returns the real document URL. The model should not fake `CREATE_DOC` success.

If the backend machine has an invalid proxy such as `HTTPS_PROXY=http://127.0.0.1:9`, set:

```powershell
$env:LARK_CLI_NO_PROXY='1'
```

The Java tool also sets `LARK_CLI_NO_PROXY=1` before executing `lark-cli`.

Recommended backend startup:

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend"
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

Then restart the frontend dev server if it is already open.
