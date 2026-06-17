# 向量检索能力扩展示例

这份文档说明如何用最少改动新增一个“能力”。示例能力是“向量检索”，它不直接修改 `AgentRouter`，也不侵入现有 Agent。

## 本次新增文件

- `backend/knowledge-service/src/main/java/com/flowmind/knowledge/vector/VectorSearchToolService.java`
- `backend/knowledge-service/src/main/java/com/flowmind/knowledge/vector/VectorSearchController.java`
- `backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/SemanticVectorSearchExtension.java`

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
```

含义：

- `enabled`：是否启用这个示例能力。
- `fallback-enabled`：向量库不可用时，是否回退到 MySQL 关键词检索。
- `max-top-k`：接口允许返回的最大条数，避免前端误传过大值。

## 如何测试

启动后端后访问：

```powershell
curl "http://localhost:8080/api/knowledge/vector/search?q=保研简历&topK=5"
```

如果 Weaviate 和 embedding 配置完整，返回 `source = weaviate`。

如果向量库不可用但 MySQL 有知识库数据，返回 `source = mysql-fallback`。

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

默认不改。

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
