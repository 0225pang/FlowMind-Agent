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
      browser-bin: ${FLOWMIND_XHS_MCP_BROWSER_BIN:}
      chrome-user-data-dir: ${FLOWMIND_XHS_MCP_CHROME_USER_DATA_DIR:.runtime/xiaohongshu/chrome-profile}
      cookies-path: ${FLOWMIND_XHS_MCP_COOKIES_PATH:.runtime/xiaohongshu/cookies.json}
      go-cache: ${FLOWMIND_XHS_MCP_GO_CACHE:.gocache/xiaohongshu-mcp}
      base-url: ${FLOWMIND_XHS_MCP_BASE_URL:http://localhost:18060}
      search-path: /api/v1/feeds/search
      detail-path: /api/v1/feeds/detail
      timeout-seconds: 120
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
- `browser-bin`：浏览器二进制路径。默认留空，MCP 会自动探测系统 Chrome；只有需要强制指定浏览器时，才配置 `FLOWMIND_XHS_MCP_BROWSER_BIN`。
- `chrome-user-data-dir`：Chrome 自动化用户目录。当前默认使用项目内 `.runtime/xiaohongshu/chrome-profile`。
- `cookies-path`：小红书登录 cookies 文件。当前默认使用项目内 `.runtime/xiaohongshu/cookies.json`。
- `go-cache`：MCP 编译缓存目录，默认放在项目 `.gocache/xiaohongshu-mcp`。
- `base-url`：FlowMind 调用 MCP 的地址。
- `search-path`：搜索笔记接口。
- `detail-path`：笔记详情接口。
- `mock-fallback`：真实 MCP 不可用时是否回退到 mock 数据。

## 项目内 Chrome 运行环境

为了避免小红书 MCP 把浏览器 profile、cookies 或 Chromium 下载缓存散落到系统用户目录，FlowMind 统一使用项目根目录下的运行目录：

```text
flowmind-agent/
  .runtime/
    chrome/
      chrome.exe
      149.0.xxxx.x/
      ...
    xiaohongshu/
      chrome-profile/
      cookies.json
```

当前项目已经将本机 Chrome Application 目录复制到：

```text
flowmind-agent/.runtime/chrome/
```

后端默认配置为：

```yaml
flowmind:
  tools:
    xiaohongshu-mcp:
      browser-bin: ${FLOWMIND_XHS_MCP_BROWSER_BIN:}
      chrome-user-data-dir: ${FLOWMIND_XHS_MCP_CHROME_USER_DATA_DIR:.runtime/xiaohongshu/chrome-profile}
      cookies-path: ${FLOWMIND_XHS_MCP_COOKIES_PATH:.runtime/xiaohongshu/cookies.json}
```

说明：

- `.runtime/` 已加入 `.gitignore`，不会提交到 Git。
- 如果自动探测不到浏览器，临时用环境变量 `FLOWMIND_XHS_MCP_BROWSER_BIN` 指向本机 Chrome，例如 `C:\Program Files\Google\Chrome\Application\chrome.exe`。
- 登录态保存在 `.runtime/xiaohongshu/cookies.json`。如果要切换小红书账号，可以删除这个文件，或者调用 `DELETE /api/v1/login/cookies`。

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

## 如何登录或重新登录小红书 MCP

小红书 MCP 的搜索和详情读取依赖本机浏览器登录态。第一次使用、登录失效、换账号、或搜索结果异常时，可以按下面流程重新登录。

### 1. 先确认 MCP 服务正在运行

后端启动后，另开一个 PowerShell：

```powershell
Invoke-RestMethod http://localhost:18060/health
```

正常返回中应包含：

```json
{
  "success": true,
  "data": {
    "status": "healthy",
    "service": "xiaohongshu-mcp"
  }
}
```

如果无法连接，先启动后端：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend"
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

### 2. 查看当前登录状态

```powershell
Invoke-RestMethod http://localhost:18060/api/v1/login/status
```

如果返回 `is_logged_in: true`，说明当前已经登录，可以直接搜索和读取详情。

### 3. 推荐方式：使用可视化浏览器登录

如果 `/api/v1/login/qrcode` 超时，或者无头浏览器里二维码不显示，优先使用这个方式。它会直接打开一个真实浏览器窗口，你在窗口中扫码即可，比 HTTP 二维码接口更稳定。

先进入 MCP 目录：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend\ai-agent-service\integrations\xiaohongshu-mcp"
```

启动可视化登录：

```powershell
go run ./cmd/login
```

执行后会打开浏览器窗口。用小红书 App 扫码登录，登录成功后程序会自动保存 cookies。保存后，后续搜索和详情读取会复用这份登录态。

登录完成后，回到任意 PowerShell，确认状态：

```powershell
Invoke-RestMethod http://localhost:18060/api/v1/login/status
```

如果看到：

```json
{
  "is_logged_in": true
}
```

说明登录成功。

### 4. 备选方式：通过 HTTP 接口获取登录二维码

```powershell
$r = Invoke-RestMethod http://localhost:18060/api/v1/login/qrcode -TimeoutSec 60
$r.data
```

返回里通常会包含：

- `img`：二维码图片，格式一般是 `data:image/png;base64,...`
- `timeout`：二维码有效时间。
- `is_logged_in`：当前是否已经登录。

如果这里出现 `操作超时`，说明 MCP 在无头浏览器中生成二维码时卡住了。直接改用上面的“可视化浏览器登录”方式。

### 5. 把二维码保存成图片并打开

如果 `$r.data.img` 是 base64 图片，可以执行：

```powershell
$base64 = $r.data.img -replace '^data:image/[^;]+;base64,', ''
[IO.File]::WriteAllBytes("$PWD\xhs-login-qrcode.png", [Convert]::FromBase64String($base64))
Start-Process "$PWD\xhs-login-qrcode.png"
```

然后用小红书 App 扫码确认登录。

### 6. 扫码后再次确认登录状态

```powershell
Invoke-RestMethod http://localhost:18060/api/v1/login/status
```

如果看到 `is_logged_in: true`，说明登录成功。

### 7. 测试真实搜索

MCP 直连测试：

```powershell
$q = [uri]::EscapeDataString("保研")
Invoke-RestMethod "http://localhost:18060/api/v1/feeds/search?keyword=$q" -TimeoutSec 90
```

FlowMind 后端测试：

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/agents/xiaohongshu/search?topic=保研&limit=5" `
  -Headers @{ Authorization = "Bearer mock-jwt.admin" }
```

如果 FlowMind 返回 `mode = real`，说明已经调用真实小红书 MCP；如果返回 `mode = mock`，说明 MCP 不可用、登录态异常、接口超时或已回退 mock。

### 8. 重新登录或切换账号

如果需要清掉旧登录态，先删除 cookies：

```powershell
Invoke-RestMethod -Method Delete http://localhost:18060/api/v1/login/cookies
```

然后推荐重新执行可视化登录：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend\ai-agent-service\integrations\xiaohongshu-mcp"
go run ./cmd/login
```

如果你仍然想用 HTTP 二维码方式，也可以执行：

```powershell
$r = Invoke-RestMethod http://localhost:18060/api/v1/login/qrcode -TimeoutSec 60
$base64 = $r.data.img -replace '^data:image/[^;]+;base64,', ''
[IO.File]::WriteAllBytes("$PWD\xhs-login-qrcode.png", [Convert]::FromBase64String($base64))
Start-Process "$PWD\xhs-login-qrcode.png"
```

扫码完成后，再用 `/api/v1/login/status` 确认状态。

### 9. 常见问题

- `Invoke-RestMethod http://localhost:18060/health` 无法连接：MCP 没启动，先启动后端，或检查 Go 是否安装。
- `/api/v1/login/qrcode` 返回 `Err:225`：Windows Defender 拦截了浏览器辅助程序，重启后端，让项目内置的 `Leakless(false)` 修复生效。
- `/api/v1/login/qrcode` 操作超时：无头浏览器中二维码页面没有正常返回，直接使用 `go run ./cmd/login` 可视化登录。
- 扫码成功但搜索仍为空：重新查 `/api/v1/login/status`；如果未登录，删除 cookies 后重新扫码。
- 搜索很慢：这是浏览器自动化访问小红书 Web 页面，不是普通数据库查询，等待 30-90 秒属于正常范围。
- FlowMind 接口返回 401：调用 `localhost:8080` 的 FlowMind 接口时要带 `Authorization` 请求头；MCP 的 `localhost:18060` 调试接口不需要 FlowMind token。

## 测试接口

查看 FlowMind 暴露给智能体的工具：

```http
GET /api/agents/xiaohongshu/tools
```

搜索小红书热点笔记：

```http
GET /api/agents/xiaohongshu/search?topic=保研简历&limit=8
```

按时间和互动维度筛选：

```http
GET /api/agents/xiaohongshu/search?topic=保研&limit=10&publishTime=一周内&sortBy=最多收藏
```

支持的筛选参数：

- `sortBy`：`综合`、`最新`、`最多点赞`、`最多评论`、`最多收藏`。
- `noteType`：`不限`、`视频`、`图文`。
- `publishTime`：`不限`、`一天内`、`一周内`、`半年内`。
- `searchScope`：`不限`、`已看过`、`未看过`、`已关注`。
- `location`：`不限`、`同城`、`附近`。

说明：当前小红书 MCP 底层没有“三个月内”这个原生筛选项。FlowMind 中传 `三个月内`、`3个月`、`90天` 会近似映射为 `半年内`。如果后续要严格三个月，需要读取详情中的发布时间再做二次过滤。

围绕一个主题做多关键词聚合搜索，适合“保研主题搜 10 个热点帖子”这种场景：

```http
GET /api/agents/xiaohongshu/search/batch?topic=保研&limit=10&perTopicLimit=8
```

也可以手动指定关键词矩阵，多个关键词用英文逗号或中文逗号分隔：

```http
GET /api/agents/xiaohongshu/search/batch?topic=保研&topics=保研,保研经验,保研面试,保研简历,保研文书,推免&limit=10
```

让大模型先生成相近关键词，再聚合搜索：

```http
GET /api/agents/xiaohongshu/search/batch?topic=期末论文&keywordMode=llm&limit=10&publishTime=一周内&sortBy=最多收藏
```

`keywordMode` 可选值：

- `local`：默认值，用本地规则扩展关键词，稳定且不额外消耗 LLM。
- `llm` 或 `ai`：先调用当前配置的大模型生成相近关键词，再搜索。
- `none` 或 `single`：不扩词，只搜索 `topic` 本身。

批量搜索会返回：

- `keywords`：本次实际搜索的关键词列表。
- `keywordPlan`：关键词生成方式，包含是否使用 LLM、生成消息和关键词列表。
- `traces`：每个关键词的调用模式、返回数量和消息，方便排查是否真实调用 MCP。
- `notes`：去重并按互动热度排序后的帖子列表，每条包含 `url/detailUrl` 官方详情链接、`coverUrl` 封面图、互动数和 `sourceKeyword`。

读取笔记详情：

```http
GET /api/agents/xiaohongshu/detail?id={feedId|xsecToken}
```

注意：小红书详情接口需要 `feedId` 和 `xsecToken`，所以推荐先通过搜索接口拿到结果里的 `id` 字段。详情接口返回的是 MCP 原始详情结构，正文通常在 `data.data.note.desc` 或相邻的 `note.desc` 字段，图片在 `imageList`，评论在 `comments`。

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
