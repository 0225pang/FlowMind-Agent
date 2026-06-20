# FlowMind Agent 后端启动说明

本后端是 Spring Boot 3.x 多模块 Maven 工程。当前 Demo 使用 `app-service` 作为聚合启动服务，同时保留这些模块边界：

```text
gateway-service
user-service
ai-agent-service
knowledge-service
content-service
student-service
school-service
analytics-service
feishu-service
```

## 1. 启动 MySQL

如果容器已经存在：

```powershell
docker start mysql9
```

如果容器不存在：

```powershell
docker run -d --name mysql9 -p 3306:3306 -v H:\Docker\mysql:/var/lib/mysql -e MYSQL_ROOT_PASSWORD=123456 mysql:9.7.0
```

后端默认连接：

```text
数据库：jdbc:mysql://localhost:3306/FlowMind
用户名：root
密码：123456
```

## 2. 启动后端

进入后端目录：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend"
```

启动命令：

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

注意：不要直接在父工程运行普通的 `spring-boot:run`，否则 Maven 会尝试启动父 POM，导致找不到主类。

如果 `8080` 端口被占用：

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run "-Dspring-boot.run.arguments=--server.port=18080"
```

## 3. DeepSeek / MockLLM 规则

默认配置中：

- `flowmind.llm.provider` 是 `deepseek`。
- 如果没有配置 API Key，系统会自动使用 `MockLLMClient`。
- 如果配置了 API Key，系统会使用 `OpenAiCompatibleLLMClient` 调用 DeepSeek API。

推荐把私有 Key 放到：

```text
app-service/src/main/resources/application-local.yml
```

创建方式：

```powershell
Copy-Item .\app-service\src\main\resources\application-local.template.yml .\app-service\src\main\resources\application-local.yml
```

然后填写：


```yaml
flowmind:
  llm:
    api-key: your_deepseek_api_key
  embedding:
    api-key: your_embedding_api_key
```

`application-local.yml` 已在 `.gitignore` 中，不应该提交到 GitHub。

## 4. 小红书 MCP 是否会随后端自动启动

会，但需要满足前置条件。

你现在使用的启动命令：

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

会启动 Spring Boot 后端。后端启动过程中，`XiaohongshuMcpProcessManager` 会自动检查：

```http
GET http://localhost:18060/health
```

如果小红书 MCP 没有运行，并且配置中是：

```yaml
flowmind:
  tools:
    xiaohongshu-mcp:
      enabled: true
      auto-start: true
```

后端会进入：

```text
ai-agent-service/integrations/xiaohongshu-mcp
```

并自动执行：

```powershell
go run . -port :18060 -headless=true
```

### 前置条件

因为小红书 MCP 是 Go 项目，所以本机必须安装 Go，并且 PowerShell 能执行：

```powershell
go version
```

该 MCP 要求 Go 1.24+。

如果 `go version` 报错，后端仍然会启动成功，但小红书 MCP 不会真实启动，系统会回退到 mock 热帖结构。

## 5. 小红书 MCP 配置在哪里确认

配置文件：

```text
app-service/src/main/resources/application.yml
```

关键配置：

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

字段说明：

- `enabled`：是否启用真实小红书 MCP 调用。
- `agent-enabled`：是否允许智能体使用小红书 SOP Skill。
- `auto-start`：后端启动时是否自动启动 MCP 进程。
- `command`：启动命令，默认 `go`。
- `working-dir`：MCP 源码目录。
- `port`：MCP 服务端口，默认 `18060`。
- `go-cache`：MCP 编译缓存目录，默认放在项目 `.gocache/xiaohongshu-mcp`，避免 Windows 默认 Go 缓存目录权限问题。
- `base-url`：FlowMind 调用 MCP 的地址。
- `search-path`：搜索接口路径。
- `detail-path`：详情接口路径。
- `mock-fallback`：真实 MCP 不可用时是否回退到 mock 数据。

## 6. 怎么确认 MCP 服务好了

### 方法一：检查健康接口

后端启动后，另开 PowerShell：

```powershell
Invoke-WebRequest http://localhost:18060/health
```

如果成功，会看到类似：

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

### 方法二：检查 FlowMind 代理接口

```powershell
Invoke-WebRequest "http://localhost:8080/api/agents/xiaohongshu/search?topic=保研简历&limit=5"
```

判断返回结果：

```text
mode = real：已经调用真实小红书 MCP。
mode = mock：真实 MCP 不可用，当前使用 mock 热帖结构。
```

### 方法三：看后端启动日志

如果看到：

```text
Started Xiaohongshu MCP from ... on port 18060
```

说明后端已经尝试启动小红书 MCP。

如果看到：

```text
Failed to start Xiaohongshu MCP
```

通常是 Go 没安装、Go 版本不够、依赖下载失败，或本地浏览器环境没有准备好。

## 7. 访问地址

默认后端：

```text
API：http://localhost:8080
Swagger：http://localhost:8080/swagger-ui.html
```

小红书 MCP：

```text
健康检查：http://localhost:18060/health
MCP HTTP：http://localhost:18060/mcp
FlowMind 代理搜索：http://localhost:8080/api/agents/xiaohongshu/search?topic=保研简历&limit=8
```

如果后端改为 `18080` 端口：

```text
API：http://localhost:18080
Swagger：http://localhost:18080/swagger-ui.html
FlowMind 代理搜索：http://localhost:18080/api/agents/xiaohongshu/search?topic=保研简历&limit=8
```

## 8. Demo 账号

Demo 账号会自动初始化到 MySQL：

```text
admin   / 123456  团队管理员
content / 123456  内容运营人员
teacher / 123456  教育咨询老师
ip      / 123456  个人 IP 运营者
student / 123456  学员用户
```

权限相关表：

```text
sys_user
sys_role
sys_user_role
sys_permission
sys_role_permission
```

多数接口需要登录后带上 token：

```text
Authorization: Bearer mock-jwt.xxxxxx
```

权限管理接口：

```http
GET /api/roles
GET /api/permissions
GET /api/roles/{roleCode}/permissions
PUT /api/roles/{roleCode}/permissions
```

## 9. 常见问题

### 找不到主类

不要直接启动父工程，使用：

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

### 8080 端口被占用

查看端口占用：

```powershell
Get-NetTCPConnection -LocalPort 8080 | Select-Object LocalAddress,LocalPort,State,OwningProcess
```

换端口启动：

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run "-Dspring-boot.run.arguments=--server.port=18080"
```

### Maven 仓库权限问题

启动命令中保留：

```text
-s maven-settings.xml
```

项目使用本地 Maven 配置，避免 Windows 用户目录权限问题。

### 小红书 MCP 没启动

先检查：

```powershell
go version
```

如果没有 Go，安装 Go 1.24+ 并重新打开 PowerShell。

再检查：

```powershell
Invoke-WebRequest http://localhost:18060/health
```

如果仍然失败，可以手动进入目录测试：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend\ai-agent-service\integrations\xiaohongshu-mcp"
go run . -port :18060 -headless=true
```

### 小红书二维码接口返回 Err:225 / leakless.exe

如果调用：

```powershell
Invoke-RestMethod http://localhost:18060/api/v1/login/qrcode -TimeoutSec 60
```

返回类似：

```text
Path: ...\leakless.exe
Err: 225
```

通常是 Windows Defender 或安全软件拦截了 go-rod 默认释放到临时目录的 `leakless.exe`。

项目已经内置了修复：`xiaohongshu-mcp/go.mod` 使用本地 `localmods/headless_browser`，启动浏览器时禁用 `Leakless`，避免执行这个临时程序。

处理方式：

1. 停掉当前后端和小红书 MCP 进程。
2. 重新启动后端：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend"
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

3. 再重新获取二维码。

## 10. 飞书 CLI

检查 `lark-cli`：

```powershell
lark-cli --version
```

配置：

```yaml
flowmind:
  tools:
    lark-cli:
      enabled: true
      command: lark-cli
      timeout-seconds: 30
```

Java 桥接层：

```text
ai-agent-service/src/main/java/com/flowmind/agent/service/LarkCliToolService.java
```

不要让大模型直接执行任意 shell 命令。

## 11. 向量检索接口

The decoupled vector retrieval example is available at:

```http
GET /api/knowledge/vector/search?q=保研简历&topK=5
```

说明文档：

```text
docs/README-vector-search-extension.md
```
