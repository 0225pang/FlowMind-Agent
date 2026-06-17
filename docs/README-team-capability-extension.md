# 小组成员能力扩展说明

目标：小组成员新增功能时，尽量只新增文件，不修改现有智能体、路由、业务模块代码。

## 统一扩展目录

后续组员新增后端能力，统一放到：

```text
backend/app-service/src/main/java/com/flowmind/contrib/capability/
```

示例：

```text
backend/app-service/src/main/java/com/flowmind/contrib/capability/vectorsearch/
backend/app-service/src/main/java/com/flowmind/contrib/capability/websearch/
backend/app-service/src/main/java/com/flowmind/contrib/capability/feishubase/
```

不要再让组员自己判断应该放到 `knowledge-service`、`content-service`、`ai-agent-service` 还是其他模块。

## 为什么放在 app-service

`app-service` 是当前 Demo 的聚合启动模块，它依赖所有业务模块。

所以放在这里有三个好处：

- 可以注入现有业务服务，例如知识库、内容运营、飞书工具、学生管理服务。
- Spring Boot 会自动扫描 `com.flowmind` 下的 Bean。
- 不需要改 Maven 模块依赖，也不需要改现有微服务边界。

这适合课程设计阶段的小组协作。等某个能力稳定后，再由负责人把它迁移到正式业务模块。

## 一个能力一般写几个文件

推荐 2 到 3 个文件。

### 1. `XxxToolService.java`

放真实功能逻辑。

例如：

```text
VectorSearchToolService.java
WebSearchToolService.java
FeishuBaseToolService.java
```

职责：

- 调用数据库、飞书、向量库、第三方 API。
- 做参数校验和结果封装。
- 不关心前端页面。
- 不直接修改 `AgentRouter`。

### 2. `XxxController.java`

放 REST 接口。

例如：

```text
VectorSearchController.java
WebSearchController.java
FeishuBaseController.java
```

职责：

- 给前端或测试工具调用。
- 返回统一 `ApiResponse.success(...)`。
- 让能力可以独立测试。

示例接口风格：

```http
GET /api/capabilities/vector-search?q=保研简历&topK=5
POST /api/capabilities/feishu-docs
POST /api/capabilities/web-search
```

### 3. `XxxExtension.java`，可选

放智能体能力说明。

例如：

```text
VectorSearchExtension.java
WebSearchExtension.java
FeishuDocExtension.java
```

职责：

- 告诉总智能体“系统有这个能力”。
- 描述这个能力怎么调用。
- 不写具体业务逻辑。

如果是 MCP 类工具，实现：

```java
McpToolProvider
```

如果是 Prompt / SOP / 固定工作流能力，实现：

```java
SkillProvider
```

## 推荐目录模板

以“向量检索能力”为例：

```text
backend/app-service/src/main/java/com/flowmind/contrib/capability/vectorsearch/
  VectorSearchToolService.java
  VectorSearchController.java
  VectorSearchExtension.java
```

Java package：

```java
package com.flowmind.contrib.capability.vectorsearch;
```

## 组员不能改哪些文件

默认不要改：

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/service/AgentRouter.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/core/*.java
backend/*-service/pom.xml
backend/app-service/pom.xml
backend/app-service/src/main/java/com/flowmind/app/FlowMindApplication.java
```

这些文件属于主流程。多人同时改，容易冲突。

## 什么时候才需要接入现有智能体

先不要接。

新增能力的第一步是让它能独立通过 REST API 跑通。

只有当能力已经稳定，并且确实要进入主对话链路时，再由负责人统一改：

```text
AgentRouter.java
某个 XxxAgent.java
```

## 最小开发流程

1. 在 `contrib/capability/` 下新建自己的能力目录。
2. 新增 `XxxToolService.java`。
3. 新增 `XxxController.java`。
4. 可选新增 `XxxExtension.java`。
5. 本地启动后端。
6. 用 Swagger 或 curl 测试自己的接口。
7. 提交代码，不修改主流程文件。

## 判断是否合格

一个组员提交的能力，至少要满足：

- 后端能启动。
- 有独立接口可以测试。
- 不影响 AI 工作台现有对话。
- 不修改主路由。
- 不把所有逻辑写进 Controller。
- 不让 LLM 直接执行任意 shell 命令。

## 和正式微服务结构的关系

这个目录是“课程设计协作缓冲区”。

短期目标：

- 降低小组协作冲突。
- 让能力能快速独立开发。
- 让负责人后续容易筛选和整合。

长期目标：

- 稳定的知识库能力迁移到 `knowledge-service`。
- 稳定的内容能力迁移到 `content-service`。
- 稳定的飞书能力迁移到 `feishu-service`。
- 稳定的智能体工具说明迁移到 `ai-agent-service`。

也就是说，组员先在 `contrib/capability` 写，负责人后续再决定是否正式纳入对应微服务。
