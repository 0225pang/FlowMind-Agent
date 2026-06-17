# 向量检索能力扩展示例

这份文档说明如何用最少改动新增一个“能力”。示例能力是“向量检索”，它不直接修改 `AgentRouter`，也不侵入现有 Agent。

## 本次新增文件

- `backend/knowledge-service/src/main/java/com/flowmind/knowledge/vector/VectorSearchToolService.java`
- `backend/knowledge-service/src/main/java/com/flowmind/knowledge/vector/VectorSearchController.java`
- `backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/SemanticVectorSearchExtension.java`
- `backend/app-service/src/main/java/com/flowmind/contrib/capability/vectorsearch/VectorSearchRuntimeExtension.java`

## 能力边界

`VectorSearchToolService` 负责真正执行检索：

- 优先使用 `EmbeddingService` 生成 query 向量。
- 如果 Weaviate 可用，调用 `WeaviateClientService.search(...)`。
- 如果 Weaviate 或 embedding 不可用，回退到 MySQL 的 `KnowledgeService.searchDocs(...)`。

`VectorSearchController` 负责把能力暴露成接口：

```http
GET /api/knowledge/vector/search?q=保研简历&topK=5
```

`SemanticVectorSearchExtension` 只负责告诉总智能体“系统存在这个能力”，不负责实际调用。

`VectorSearchRuntimeExtension` 负责在对话运行时真实调用向量检索。它放在 `app-service`，因为 `app-service` 同时依赖 `ai-agent-service` 和 `knowledge-service`，可以把 Agent 扩展接口和知识库检索服务连接起来。

## 配置

配置位于：

```text
backend/app-service/src/main/resources/application.yml
```

相关配置：

```yaml
flowmind:
  vector-demo:
    enabled: true
    fallback-enabled: true
    max-top-k: 20
    max-distance: 0.65
    agent-enabled: true
    agent-top-k: 5
    always-search: true
```

含义：

- `enabled`：是否启用这个示例能力。
- `fallback-enabled`：向量库不可用时，是否回退到 MySQL 关键词检索。
- `max-top-k`：接口允许返回的最大条数，避免前端误传过大值。
- `max-distance`：向量距离阈值。距离超过该值的结果会被视为不够相关，不注入给智能体。
- `agent-enabled`：是否允许 AI 工作台对话时自动调用向量检索。
- `agent-top-k`：AI 工作台每次检索注入给模型的结果条数。
- `always-search`：是否每个用户问题都优先查询向量数据库。当前默认 `true`。

## 如何测试

启动后端后访问：

```powershell
curl "http://localhost:8080/api/knowledge/vector/search?q=保研简历&topK=5"
```

如果 Weaviate 和 embedding 配置完整，返回 `source = weaviate`。

如果向量库不可用但 MySQL 有知识库数据，返回 `source = mysql-fallback`。

## 如何让智能体真实调用

现在已经接入。

当前策略是：AI 工作台里的每个问题都会优先查询向量数据库。

如果关闭 `always-search`，才会退回到关键词触发模式。关键词包括：

```text
知识库、向量、检索、搜索、查找、资料、文档、RAG、根据知识、根据资料、保研知识库
```

后端会执行：

```text
Agent -> VectorSearchRuntimeExtension -> VectorSearchToolService -> Weaviate / MySQL fallback
```

然后把检索结果注入到模型的 system prompt 中。

注意：如果当前 `flowmind.llm.provider=mock`，MockLLM 仍然会返回固定文案，看不出检索结果。要观察真实 RAG 效果，需要用 DeepSeek 或其他真实 OpenAI-compatible LLM 启动。

## 后续成员新增能力的最小模板

一个独立能力一般只需要 2 到 3 个文件：

1. `XxxToolService.java`
   - 放真实业务逻辑。
   - 不依赖 `AgentRouter`。
   - 能独立单测或通过接口测试。

2. `XxxController.java`
   - 把能力暴露为 REST API。
   - 前端、Agent、定时任务都可以复用。

3. `XxxExtension.java`，可选
   - 只写能力名称、描述、适用 Agent。
   - 用来让总智能体知道“有什么工具可以调用”。

## 什么时候需要改 AgentRouter

默认不改。当前向量检索就是通过 `VectorSearchRuntimeExtension` 接入的，没有修改 `AgentRouter`。

只有当你要把能力强绑定到某个 Agent 的固定流程中，比如“每次知识库问答前必须先检索”，才需要改：

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/service/AgentRouter.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/core/KnowledgeAgent.java
```

更推荐先保持能力独立，等接口稳定后再接入总智能体。

## 推荐协作规则

- 成员优先在自己的 service 包里新增能力，不改公共路由。
- 先提供 REST API，再考虑接入 Agent。
- 配置统一放到 `application.yml`，并提供默认值。
- 能力输出尽量用稳定 DTO 或 record，不直接返回第三方 SDK 对象。
