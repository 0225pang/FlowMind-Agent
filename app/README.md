# FlowMind Agent Android App

这是 FlowMind Agent 的原生 Android 移动端 Demo。App 不使用 WebView，而是通过 HTTP/SSE 直接复用现有后端接口。

默认后端地址：

```text
https://gracious-justifier-espresso.ngrok-free.dev
```

映射关系：

```text
https://gracious-justifier-espresso.ngrok-free.dev -> http://localhost:8080
```

## 1. 当前 App 形态

新版移动端已经改成传统 App 结构：

- 底部五栏导航
  - AI 工作台
  - 知识库
  - 内容创作
  - 院校情报
  - 设置
- 页面之间有淡入、上滑和缩放动画。
- 卡片、标签、分段控制、详情弹窗、评分星级、迷你图表和内容日历都使用原生 Android View 实现。
- AI 工作台支持 SSE 流式展示。
- AI 回复、知识片段、文案内容支持基础 Markdown 渲染。

## 2. 功能覆盖

### AI 工作台

- 调用：

```http
POST /api/agents/chat/stream
```

- 支持解析 SSE 事件：
  - `session`
  - `thinking`
  - `reasoning`
  - `trace`
  - `delta`
  - `done`
  - `error`
- 支持展示工具调用。
- 支持点击展开模型 Thinking 与工具调用详情。
- 支持快捷指令：查知识库、生成选题、院校推荐、飞书能力。

### 知识库

- 调用：

```http
GET /api/knowledge/stats
GET /api/knowledge/docs
GET /api/knowledge/vector/search?q=关键词&topK=8
```

- 支持知识库统计卡片。
- 支持向量语义检索。
- 支持文档与知识片段详情弹窗。

### 内容创作

- 调用：

```http
GET /api/content/themes
GET /api/content/drafts
GET /api/content/calendar?month=yyyy-MM
GET /api/content/themes/{themeId}/drafts
PUT /api/content/themes/{id}/rating
PUT /api/content/drafts/{id}/rating
PUT /api/content/drafts/{draftId}
POST /api/content/drafts/{draftId}/images
```

- 页面包含三个分段：
  - 主题库
  - 文案库
  - 日历
- 主题卡片支持查看详情、星级评分、查看该主题下历史文案。
- 文案卡片支持查看详情、星级评分、编辑文案、登记图片 URL。
- 如果文案没有图片，会展示“配图建议”。
- 日历用原生 Canvas 绘制，有发布内容的日期会有标记，下方展示发布列表。

### 院校情报

- 调用：

```http
GET /api/schools
GET /api/school-projects
POST /api/schools/recommend
```

- 支持学校列表。
- 支持夏令营/预推免项目列表。
- 支持项目详情弹窗。
- 支持输入学生画像并调用 AI 推荐。
- 顶部包含原生迷你柱状图，用于展示院校项目趋势。

### 设置

- 调用：

```http
GET /api/users/me
GET /api/feishu/sync/status
```

- 支持修改后端 Base URL。
- 支持检查当前用户。
- 支持检查飞书同步状态。
- 支持退出登录。

## 3. 项目结构

```text
app/
  README.md
  settings.gradle
  build.gradle
  FlowMindAgent-mobile-debug.apk
  mobile/
    build.gradle
    src/main/
      AndroidManifest.xml
      java/com/flowmind/mobile/
        ApiClient.java
        MainActivity.java
      res/values/
        styles.xml
```

## 4. 技术方案

当前移动端采用：

- 原生 Android Java
- Android SDK 34
- Gradle / Android Gradle Plugin
- `HttpURLConnection` 调用 REST API
- 手写 SSE 解析
- 原生 View 动态构建 UI
- 原生 Canvas 绘制迷你图表与内容日历
- 无 AndroidX、无 Retrofit、无第三方 UI 组件

这样做的好处是：可以在当前本地环境稳定离线打包 APK，不需要重新下载依赖。

## 5. 构建 APK

进入移动端目录：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\app"
```

设置 Android SDK 路径：

```powershell
$env:ANDROID_HOME='C:\Users\Lenovo\AppData\Local\Android\Sdk'
```

构建 Debug APK：

```powershell
& 'C:\Users\Lenovo\.gradle\wrapper\dists\gradle-8.14.3-bin\cv11ve7ro1n3o1j4so8xd9n66\gradle-8.14.3\bin\gradle.bat' assembleDebug
```

构建产物位置：

```text
app/mobile/build/outputs/apk/debug/mobile-debug.apk
```

当前已复制一份到：

```text
app/FlowMindAgent-mobile-debug.apk
```

## 6. 安装到 Android 手机

直接把这个文件发到手机安装：

```text
flowmind-agent/app/FlowMindAgent-mobile-debug.apk
```

如果使用 ADB：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\app"
adb install -r .\FlowMindAgent-mobile-debug.apk
```

如果提示签名冲突：

```powershell
adb uninstall com.flowmind.mobile
adb install .\FlowMindAgent-mobile-debug.apk
```

## 7. 后端启动要求

移动端默认访问 ngrok 地址：

```text
https://gracious-justifier-espresso.ngrok-free.dev
```

请确保本地后端正在运行：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend"
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

并确保 ngrok 穿透仍然指向：

```text
http://localhost:8080
```

## 8. 登录方式

推荐 Demo 账号：

```text
admin / 123456
```

也可以直接点击：

```text
使用 Demo Token
```

移动端会默认使用：

```http
Authorization: Bearer mock-jwt.demo
```

## 9. ngrok 注意事项

移动端请求会自动带：

```http
ngrok-skip-browser-warning: true
```

这是为了绕过 ngrok 免费域名的浏览器提示页，否则 App 可能拿到 HTML，而不是后端 JSON。

如果 ngrok 地址变了，可以在 App 的“设置”页面修改 Base URL。

如果想修改 APK 默认地址，改：

```text
mobile/build.gradle
```

字段：

```gradle
buildConfigField "String", "DEFAULT_BASE_URL", "\"https://你的新地址\""
```

然后重新构建 APK。

## 10. 已验证构建

本机已执行：

```powershell
$env:ANDROID_HOME='C:\Users\Lenovo\AppData\Local\Android\Sdk'
& 'C:\Users\Lenovo\.gradle\wrapper\dists\gradle-8.14.3-bin\cv11ve7ro1n3o1j4so8xd9n66\gradle-8.14.3\bin\gradle.bat' assembleDebug
```

结果：

```text
BUILD SUCCESSFUL
```

新版 APK：

```text
flowmind-agent/app/FlowMindAgent-mobile-debug.apk
```

