# FlowMind Agent 扩展开发指南

这份文档专门说明：后续如果要给 FlowMind Agent 添加新的智能体能力，应该改哪些文件、怎么接后端路由、怎么接真实工具、怎么让前端知道。

当前项目采用“总智能体入口 + 自动路由 + 专业 Agent + 白名单工具”的方式：

```text
前端 AI 工作台
  -> POST /api/agents/chat/stream
  -> AgentRouter 自动判断意图
  -> ContentAgent / KnowledgeAgent / StudentAgent / SchoolAgent / FeishuAgent
  -> 可选：调用 LarkCliToolService / LLMClient / 数据库 Service
```

核心原则：

- 前端默认只发给 `agentType: auto`，不要再要求用户手动选择 Agent。
- `AgentRouter` 负责判断应该启用哪个 Agent。
- Agent 负责业务理解、组织 Prompt、决定是否调用工具。
- 真实外部操作必须走后端白名单工具，不要让大模型直接执行 shell 命令。
- 飞书、数据库、向量库、MCP、Skill 都应该包装成明确的 Service 或 Extension。

---

## 一、最常改的文件

### 后端 Agent 核心

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/core/Agent.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/core/BaseAgent.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/core/ContentAgent.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/core/KnowledgeAgent.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/core/StudentAgent.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/core/SchoolAgent.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/core/FeishuAgent.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/service/AgentRouter.java
```

### 后端工具和扩展

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/service/LarkCliToolService.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/AgentExtension.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/McpToolProvider.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/SkillProvider.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/LarkCliMcpExtension.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/RuntimeToolExtensions.java
```

### 后端配置

```text
backend/app-service/src/main/resources/application.yml
backend/app-service/src/main/resources/application-deepseek.yml
```

### 前端 AI 工作台

```text
frontend/src/views/AgentWorkspaceView.vue
frontend/src/components/AgentChat.vue
frontend/src/components/AgentTabs.vue
frontend/src/components/ContextPanel.vue
frontend/src/api/agent.ts
frontend/src/utils/markdown.ts
```

---

## 二、新增一个“纯 LLM 智能体”

适合：只需要大模型生成内容，不需要真实调用飞书、数据库、外部 API。

例子：新增 `ResearchAgent`，负责科研选题分析。

### 1. 新建 Agent 类

目录：

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/core/ResearchAgent.java
```

参考写法：

```java
package com.flowmind.agent.core;

import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.dto.AgentResponse;
import com.flowmind.agent.extension.AgentExtension;
import com.flowmind.agent.llm.LLMClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ResearchAgent extends BaseAgent {
    public ResearchAgent(LLMClient llm, List<AgentExtension> extensions) {
        super(llm, extensions);
    }

    @Override
    public String getName() {
        return "ResearchAgent";
    }

    @Override
    public String getDescription() {
        return "科研选题、论文结构、文献综述和研究计划智能体。";
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
        return response("research", request.getMessage(), List.of(
                Map.of("title", "研究计划", "content", "可生成研究问题、方法路线和论文结构")
        ));
    }
}
```

只要加了 `@Component`，Spring 会自动注册，`AgentRouter` 的 `agents` 列表会自动拿到它。

### 2. 修改自动路由

文件：

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/service/AgentRouter.java
```

在 `inferAgentType()` 里加关键词：

```java
if (containsAny(text, "科研", "论文", "文献综述", "研究计划", "课题")) {
    return "research";
}
```

注意：返回值必须等于 Agent 名去掉 `Agent` 后的小写形式。

```text
ResearchAgent -> research
FeishuAgent   -> feishu
ContentAgent  -> content
```

### 3. 前端是否需要改？

如果只是让总智能体自动调用，不一定要改前端。

如果要在右侧面板展示能力说明，改：

```text
frontend/src/components/ContextPanel.vue
frontend/src/components/AgentTabs.vue
```

---

## 三、新增一个“真实工具能力”

适合：需要真的调用外部系统或本机能力，比如飞书、网页搜索、文件读取、向量数据库、数据库写入。

不要让 LLM 直接输出：

```json
{"action":"CREATE_DOC"}
```

然后假装成功。正确做法是：

```text
用户请求
  -> Agent 判断意图
  -> 调用后端 Tool Service
  -> Tool Service 执行真实操作
  -> Agent 把真实返回结果展示给用户
```

### 1. 新建 Tool Service

例子：新增一个 `VectorSearchToolService`。

目录：

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/service/VectorSearchToolService.java
```

建议结构：

```java
@Service
public class VectorSearchToolService {
    public List<SearchResult> search(String query, int topK) {
        // 未来可以接 Milvus / Qdrant / pgvector / Elasticsearch
        return List.of();
    }
}
```

### 2. 在 Agent 中注入工具

例子：给 `KnowledgeAgent` 注入。

```java
private final VectorSearchToolService vectorSearchToolService;

public KnowledgeAgent(
        LLMClient llm,
        List<AgentExtension> extensions,
        VectorSearchToolService vectorSearchToolService
) {
    super(llm, extensions);
    this.vectorSearchToolService = vectorSearchToolService;
}
```

然后在 `execute()` 或 `stream()` 里判断：

```java
if (message.contains("检索") || message.contains("知识库")) {
    var results = vectorSearchToolService.search(message, 5);
    // 把真实结果组织成 AgentResponse
}
```

### 3. 注册工具上下文

如果你希望 LLM 知道这个工具存在，新增一个 Extension：

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/VectorSearchMcpExtension.java
```

实现：

```java
@Component
public class VectorSearchMcpExtension implements McpToolProvider {
    public String name() { return "vector-search"; }
    public String description() { return "知识库向量检索工具，支持相似内容召回。"; }
    public boolean supports(String agentType) {
        return "knowledge".equals(agentType) || "content".equals(agentType);
    }
}
```

`BaseAgent` 会自动把 Extension 注入系统提示词。

---

## 四、新增飞书能力

当前飞书相关能力已经有基础结构：

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/service/LarkCliToolService.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/core/FeishuAgent.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/LarkCliMcpExtension.java
backend/feishu-service/src/main/java/com/flowmind/feishu/FeishuController.java
backend/app-service/src/main/resources/application.yml
```

当前已经支持：

- 创建飞书文档
- 获取飞书文档
- 读取“保研知识库”共享文件夹直接子项
- 在“保研知识库”下创建文档

### 1. 飞书固定资源配置

放在：

```text
backend/app-service/src/main/resources/application.yml
```

例子：

```yaml
flowmind:
  feishu:
    knowledge-base:
      name: 保研知识库
      folder-token: KELsfW0jvlHcVqdiuTncQ66Lnnc
      url: https://ycn1ft0idflw.feishu.cn/drive/folder/KELsfW0jvlHcVqdiuTncQ66Lnnc
```

如果以后你有新的共享文件夹，比如“内容素材库”，可以加：

```yaml
flowmind:
  feishu:
    content-base:
      name: 内容素材库
      folder-token: xxxxx
      url: https://xxx.feishu.cn/drive/folder/xxxxx
```

然后在对应 Agent 中用 `@Value` 注入。

### 2. 新增一个飞书白名单命令

修改：

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/service/LarkCliToolService.java
```

例如新增移动文件：

```java
public JsonNode moveFile(String fileToken, String targetFolderToken, String asIdentity) throws Exception {
    List<String> args = baseArgs();
    args.add("drive");
    args.add("+move");
    // 按 lark-cli help 要求补参数
    return execute(null, args, null);
}
```

注意：

- 先用 `lark-cli <command> --help` 看参数。
- 高风险写操作必须做白名单，不要拼接用户任意命令。
- PowerShell 下复杂 JSON 参数优先用 stdin，也就是 `--params -`。

当前 `listFolder()` 就是这样做的。

### 3. 在 FeishuAgent 中调用

修改：

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/core/FeishuAgent.java
```

添加意图判断：

```java
if (isMoveFileRequest(message)) {
    return moveFileResponse(...);
}
```

建议顺序：

```text
创建 / 写入类意图
读取 / 列表类意图
搜索类意图
普通 LLM 回答
```

之前的问题就是“读取文件夹”优先级高于“创建文档”，导致“在保研知识库中创建文档”被误判成“列目录”。以后新增能力时，要特别注意这种优先级。

### 4. 如果缺飞书权限怎么办？

先运行对应命令，CLI 会返回 missing scope，例如：

```text
missing_scopes: ["space:document:retrieve"]
```

然后发起授权：

```powershell
$env:LARK_CLI_NO_PROXY='1'
lark-cli auth login --scope "space:document:retrieve" --no-wait --json
```

拿到 `verification_url` 后生成二维码：

```powershell
lark-cli auth qrcode "<verification_url>" --output flowmind_lark_auth_qr.png
```

用户授权完成后执行：

```powershell
lark-cli auth login --device-code "<device_code>"
```

如果保存 token 时报 Windows `Access is denied`，用管理员权限或提升权限执行同一条 `--device-code` 命令。

---

## 五、让总智能体知道新能力

总智能体的核心是：

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/service/AgentRouter.java
```

新增能力时一定要改 `inferAgentType()`。

示例：

```java
if (containsAny(text, "合同", "法务", "协议", "条款")) {
    return "legal";
}
```

同时新建：

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/core/LegalAgent.java
```

规则：

- 路由关键词要尽量具体。
- 外部系统操作要优先路由到工具型 Agent。
- 关键词冲突时，写操作优先级高于读取操作。
- 飞书相关请求优先路由到 `feishu`。
- 无法判断时默认 `content`。

---

## 六、前端怎么配合

现在前端是统一入口：

```text
frontend/src/views/AgentWorkspaceView.vue
frontend/src/components/AgentChat.vue
frontend/src/api/agent.ts
```

发送请求时：

```ts
agentType: 'auto'
```

所以新增后端 Agent 后，前端通常不用改。

需要改前端的情况：

### 1. 想在顶部展示新 Agent

改：

```text
frontend/src/components/AgentTabs.vue
```

增加一个 chip：

```ts
{ type: 'research', name: 'ResearchAgent', color: '#0ea5e9' }
```

### 2. 想在右侧面板展示新能力

改：

```text
frontend/src/components/ContextPanel.vue
```

增加 route：

```ts
{ name: 'ResearchAgent', desc: '科研选题、论文结构、文献综述', color: '#0ea5e9' }
```

增加快捷动作：

```ts
{ text: '生成研究计划', prompt: '帮我生成一个关于保研辅导平台的研究计划' }
```

### 3. 想让回复支持新格式

改：

```text
frontend/src/components/ChatMessage.vue
frontend/src/utils/markdown.ts
```

当前已经支持基础 Markdown 渲染。如果未来要支持工具卡片、飞书文档卡片、表格预览，可以在 `ChatMessage.vue` 根据 `cards` 渲染。

---

## 七、数据库能力怎么加

如果新增一个业务模块，比如“任务 Agent”，推荐新增一个独立 service module：

```text
backend/task-service/
```

典型结构：

```text
task-service/src/main/java/com/flowmind/task/controller/
task-service/src/main/java/com/flowmind/task/service/
task-service/src/main/java/com/flowmind/task/entity/
task-service/src/main/java/com/flowmind/task/dto/
task-service/src/main/java/com/flowmind/task/mapper/
```

然后：

1. 在 `backend/pom.xml` 加 module。
2. 在 `app-service/pom.xml` 引入依赖。
3. 在 `schema.sql` 加表。
4. 在 `mock-data.sql` 加 Demo 数据。
5. Agent 调用对应 Service，而不是直接写 SQL。

已有内容运营模块可以参考：

```text
backend/content-service/
```

---

## 八、接入新的大模型

当前 LLM 是 OpenAI-compatible 风格：

```text
backend/ai-agent-service/src/main/java/com/flowmind/agent/llm/LLMClient.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/llm/OpenAiCompatibleLLMClient.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/llm/MockLLMClient.java
backend/ai-agent-service/src/main/java/com/flowmind/agent/llm/LlmProperties.java
```

配置：

```text
backend/app-service/src/main/resources/application-deepseek.yml
```

如果换 OpenAI、通义、豆包兼容网关，只要改：

```yaml
flowmind:
  llm:
    provider: deepseek
    base-url: https://api.deepseek.com
    chat-path: /chat/completions
    model: deepseek-chat
    api-key: YOUR_TOKEN
```

如果厂商不是 OpenAI-compatible，再新增一个实现：

```java
public class DoubaoLLMClient implements LLMClient {
    public String complete(String systemPrompt, String userPrompt) {}
    public void stream(String systemPrompt, String userPrompt, Consumer<String> onDelta) {}
}
```

---

## 九、推荐的扩展步骤清单

新增能力时按这个顺序做：

```text
1. 明确能力类型
   - 纯 LLM 生成？
   - 需要数据库？
   - 需要飞书？
   - 需要互联网？
   - 需要向量检索？

2. 新建或修改 Agent
   - core/xxxAgent.java

3. 如果需要真实操作，新建 Tool Service
   - service/xxxToolService.java

4. 如果需要让 LLM 知道工具存在，新建 Extension
   - extension/xxxMcpExtension.java
   - extension/xxxSkillExtension.java

5. 修改总路由
   - service/AgentRouter.java

6. 加配置
   - app-service/src/main/resources/application.yml

7. 如有 REST 测试入口，加 Controller
   - feishu-service / content-service / new-service

8. 前端如需展示，改
   - AgentTabs.vue
   - ContextPanel.vue
   - AgentChat.vue

9. 编译验证
   - 后端：.\mvnw.cmd -s maven-settings.xml -q -DskipTests package
   - 前端：npm run build
```

---

## 十、运行命令

后端：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend"
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

前端：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\frontend"
npm run dev
```

如果用 DeepSeek API：

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend"
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=deepseek"
```

---

## 十一、当前已知关键配置

### 飞书知识库

```text
名称：保研知识库
Folder Token：KELsfW0jvlHcVqdiuTncQ66Lnnc
URL：https://ycn1ft0idflw.feishu.cn/drive/folder/KELsfW0jvlHcVqdiuTncQ66Lnnc
```

### 飞书 CLI

验证：

```powershell
lark-cli --version
```

如果本机有坏代理：

```powershell
$env:LARK_CLI_NO_PROXY='1'
```

读取保研知识库文件夹的稳定命令：

```powershell
'{"folder_token":"KELsfW0jvlHcVqdiuTncQ66Lnnc","page_size":200}' | lark-cli drive files list --params - --format json --as user
```

---

## 十二、最容易踩的坑

### 1. 只在提示词里写“可以调用工具”

不够。必须有后端 Service 真正执行。

正确做法：

```text
Agent -> ToolService -> 外部系统 -> 真实返回
```

### 2. 意图判断优先级写错

例如：

```text
在保研知识库中创建一个飞书文档
```

如果先判断“保研知识库读取”，就会只列目录，不创建文档。

正确顺序：

```text
创建 / 写入
读取 / 列表
搜索 / 查询
普通回答
```

### 3. 让 LLM 编造工具结果

禁止。飞书链接、token、数据库 ID 必须来自真实工具返回。

### 4. PowerShell JSON 参数被转义破坏

复杂 JSON 参数优先用 stdin：

```powershell
'{"folder_token":"xxx","page_size":200}' | lark-cli drive files list --params -
```

### 5. 飞书权限缺失

缺 scope 时，先按 CLI 返回的 `missing_scopes` 授权，不要猜。

---

## 十三、推荐的下一步架构升级

现在项目已经能跑 Demo。后续如果继续扩展，建议优先做：

1. `ToolCall` 统一协议  
   用 Java DTO 表示工具调用，而不是在 Agent 里各写各的判断。

2. `ToolRegistry`  
   所有工具注册为 Spring Bean，例如 `create_feishu_doc`、`list_feishu_folder`、`search_knowledge_base`。

3. 工具调用日志表  
   记录每次工具调用：用户请求、工具名、参数、结果、耗时、是否成功。

4. 前端工具结果卡片  
   飞书文档返回时展示“打开文档”按钮，而不是只显示纯文本链接。

5. 向量数据库接入  
   `KnowledgeAgent` 从“列文件”升级到“读取文档 -> 切片 -> embedding -> 检索 -> 回答”。

