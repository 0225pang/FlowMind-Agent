# API 设计

统一返回：

```json
{ "code": 200, "message": "success", "data": {} }
```

## 内容 SOP 新增接口

- `POST /api/content/sop/xiaohongshu/generate`
  - 输入：主题、目标人群、风格。
  - 输出：SOP 步骤、结构模板、10 条标题、3 个版本、标签、入库记录。

- `POST /api/content/sop/moments/generate`
  - 输入：场景、身份标签、情绪方向。
  - 输出：朋友圈结构、专业理性版、学姐温和版、稍带传播版、入库记录。

- `POST /api/content/sop/assets/extract`
  - 输入：历史内容、SOP、话术、爆款结构。
  - 输出：分类结果、标题模板、钩子库、转化话术、标签和向量入库占位信息。

- `GET /api/content/sop/architecture`
  - 输出：触发生成、知识检索、内容生成、内容输出、数据沉淀五段式架构。

## 原有接口保留

- 用户：`POST /api/auth/login`, `POST /api/auth/register`, `GET /api/users/me`
- AI：`POST /api/agents/chat`, `GET /api/agents`, `GET /api/prompts`, `POST /api/prompts`
- 内容：`GET /api/content/topics`, `POST /api/content/topics/generate`, `POST /api/content/moments/generate`, `PUT /api/content/topics/{id}/status`
- 知识库：`GET /api/knowledge/docs`, `POST /api/knowledge/docs`, `POST /api/knowledge/docs/{id}/summarize`, `GET /api/knowledge/tags`
- 学员：`GET /api/students`, `POST /api/students`, `POST /api/students/{id}/analyze`
- 院校：`GET /api/schools`, `GET /api/school-projects`, `POST /api/schools/recommend`
- 分析：`GET /api/analytics/overview`, `GET /api/analytics/student-distribution`, `GET /api/analytics/content-stats`
- 飞书：`GET /api/feishu/sync/status`, `POST /api/feishu/sync/docs`, `POST /api/feishu/sync/bitable`, `POST /api/feishu/bot/push`, `GET /api/feishu/logs`
