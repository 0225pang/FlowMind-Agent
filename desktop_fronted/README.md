# FlowMind Agent Python 桌面客户端

这是 FlowMind Agent 的 Python/PySide6 桌面端复刻工程，目标是尽可能按现有 Vue Web 前端复刻页面结构和功能，并复用当前 Java 后端 REST/SSE API。

## 迁移思路

桌面端采用“前端重写、后端复用”的迁移方式。原有 Spring Boot 后端仍然负责用户认证、AI 会话、知识库检索、内容运营、学员管理、院校情报、飞书同步和数据分析等业务能力，Python 桌面端只负责界面呈现、用户交互和 HTTP/SSE 调用。

这种方案的优点是改动范围比较清晰。后端数据库、Weaviate、飞书 CLI、LLM Key 和本地配置都不需要迁移到客户端，桌面端也不直接持有敏感配置。用户在桌面端看到的页面和 Web 端接近，但数据仍然通过统一接口获得，后续如果 Web 端和桌面端并行维护，也更容易保证功能一致。

界面层主要参考现有 Vue 前端的页面结构，包括左侧导航、顶部用户信息、数据卡片、标签页、表格、详情弹窗、AI 工作台上下文面板、内容日历和系统设置页。PySide6 负责实现高保真桌面界面，Tkinter 版本保留为兜底入口，方便在 PySide6/Qt 环境异常时仍能运行一个基础演示版本。

离线 Demo 数据用于课程展示和开发自测。没有启动 Java 后端时，桌面端可以切换到 `offline://demo`，继续演示登录、角色权限、页面渲染、AI 流式输出、内容运营、知识库检索和增删改操作。这样可以降低演示时对网络、ngrok 和本地后端状态的依赖。

## 技术栈

- Python 3.10+
- PySide6
- httpx

## 安装依赖

```powershell
cd D:\Desktop\agent\FlowMind-Agent\desktop_fronted
python -m pip install -r requirements.txt
```

## 运行

先启动 Java 后端，默认接口地址为：

```text
http://localhost:8080
```

然后运行桌面端：

```powershell
python run.py
```

Windows 下也可以直接执行：

```powershell
.\start_desktop.ps1
```

登录页提供 5 个演示账号快捷入口：

- `admin / 123456`：团队管理员，可访问全部页面。
- `content / 123456`：内容运营人员，侧重内容运营、知识库、AI 工作台和飞书。
- `teacher / 123456`：教育咨询老师，侧重学员管理、院校情报、知识库和 AI 工作台。
- `ip / 123456`：个人 IP 运营者，侧重内容运营、飞书同步和知识沉淀。
- `student / 123456`：学员用户，仅开放基础 AI 工作台和知识库。

没有启动 Java 后端时，可以点击“使用离线 Demo 数据”。桌面端会切换到 `offline://demo`，并用内置 Mock 数据演示角色权限、页面数据、流式对话和增删改操作。

如果当前机器 PySide6/Qt DLL 导入失败，可以先运行 Tkinter 兜底版。它不需要额外 GUI 依赖，仍然复用同一套 API 客户端和页面结构：

```powershell
python run_tk.py
```

## Smoke Test

不启动 GUI，只验证 API 客户端和离线 fallback 数据：

```powershell
python smoke_test.py
```

验证 PySide6 页面可以创建和切换：

```powershell
python pyside_smoke_test.py
```

一键执行编译、API fallback smoke 和 PySide 离屏 smoke：

```powershell
python check_desktop.py
```

Windows 下也可以直接执行：

```powershell
.\check_desktop.ps1
```

如果后端在 ngrok 或其他地址，登录页可以修改 Base URL，例如：

```text
https://gracious-justifier-espresso.ngrok-free.dev
```

## 当前页面覆盖

- 登录：支持 Base URL 设置、离线 Demo 登录、演示账号快捷填充和角色信息展示。
- Dashboard：首页保留统计卡片、图表概览和跳转 AI 工作台的入口。
- AI 工作台：支持会话列表、Agent 选择、流式对话、工具调用 trace、Thinking 面板和历史摘要。
- 知识库：支持文档列表、标签筛选、详情弹窗、标签编辑、同步状态、同步日志和向量检索结果展示。
- 内容运营：支持主题库、文案库、内容日历、SOP 生成、文案详情、图片引用和跳转 AI 工作台生成内容。
- 学员管理：支持学员卡片、表格视图、画像详情、进度展示、增删改和 AI 分析。
- 院校情报：支持学校与项目数据展示、项目详情、匹配分进度条和 AI 推荐结果。
- 数据分析：支持学员分布、内容统计、申请阶段漏斗、院校截止趋势和关键指标统计。
- 飞书同步：支持同步状态、同步日志、文档创建、文档读取和知识库共享文件列表。
- 系统设置/权限接口：支持连接测试、Prompt 模板、角色权限、模型配置占位和系统日志展示。

## 报告撰写提示

如果需要把桌面端迁移过程写入课程设计文档，可以按“迁移背景、技术选型、界面设计、接口复用、离线容错、测试验证、后续优化”几个小节展开。每个页面都可以单独说明迁移目标、复刻内容和对应接口，这样正文会更完整，也更容易体现工作量。

功能实现部分可以重点描述 AI 工作台、内容运营、知识库、学员管理和院校情报五个核心模块。它们既能体现面向对象界面组件设计，也能体现客户端与后端 API 的协作关系，适合作为课程设计报告中的主要篇幅。

## 设计约束

桌面端只访问 HTTP/SSE 接口，不直接读取 MySQL、Weaviate、飞书 CLI 或后端本地配置。
