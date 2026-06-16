# 架构设计

FlowMind Agent 采用前后端分离：前端是 AI 工作台 + 后台管理系统，后端是 Spring Boot 多模块项目。Demo 阶段使用 `app-service` 聚合启动，其余模块体现服务边界，后续可拆为独立进程并接入注册中心、配置中心和网关。

## 内容运营核心链路

当前重点抽象为 3 个内容智能体：

- 小红书内容智能体：爆款结构检索、结构模板压缩、仿写生成、标题生成、三版输出、自动入库。
- 朋友圈内容智能体：场景识别、人设映射、朋友圈结构生成、三种风格、去营销化优化、效果记录。
- 知识库/内容资产智能体：内容分类、结构拆解、模板库生成、标签体系、向量化入库、飞书沉淀。

## 飞书生态落地结构

系统按端口适配方式拆分外部能力，避免业务逻辑直接依赖某个具体 SDK：

- 触发生成：飞书机器人、前端界面。
- 知识检索：向量数据库、爆款结构库、历史内容库。
- 内容生成：LLM API、Prompt 模板、Agent Router。
- 内容输出：飞书文档、本地基础数据库。
- 数据沉淀：飞书多维表格、飞书文档、本地基础数据库。

对应后端接口位于 `content-service`：

- `KnowledgeRetriever`：后续替换为向量数据库/RAG 检索。
- `ContentGenerationClient`：后续替换为 DeepSeek、豆包、通义千问或 OpenAI。
- `ContentDocumentPublisher`：后续替换为飞书文档 API。
- `BitableContentRepository`：后续替换为飞书多维表格 API。
- `LocalContentRepository`：后续替换为 MySQL/MyBatis/JPA。

这样新增接口时只需要新增一个 port + adapter，不必改动 Controller 和 SOP 主流程。
