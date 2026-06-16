# 内容运营服务数据库设计

内容运营模块归属 `content-service`，使用 MySQL 数据库 `FlowMind`。表统一使用 `content_` 前缀，便于后续拆分为独立内容微服务。

## 表数量

当前内容运营板块共 8 张表：

1. `content_theme`
2. `content_copy`
3. `content_copy_image`
4. `content_calendar`
5. `content_tag`
6. `content_theme_tag`
7. `content_generation_record`
8. `content_publish_metric`

## 表职责

### content_theme

主题库主表。存放一个内容选题或运营主题。

核心字段：

- `title`：主题标题
- `topic`：主题关键词，如保研简历、导师套磁
- `platform`：默认平台，小红书、朋友圈、公众号
- `type`：主题类型，如爆款仿写、经验干货、人设表达
- `status`：待创作、已生成、待发布、已发布
- `heat`：选题热度
- `planned_date`：计划日期
- `summary`：主题摘要
- `source_type`：mock、agent、feishu、manual
- `external_ref`：飞书或外部系统引用

### content_copy

文案库主表。一个主题下面可以有多条历史文案。

核心字段：

- `theme_id`：关联主题
- `title`：文案标题
- `channel`：小红书、朋友圈、公众号
- `version`：干货版、情绪增强版、专业理性版等
- `style`：风格，如干货、温柔、专业
- `content`：正文
- `usage_status`：未使用、已使用、已归档
- `used_date`：实际使用日期
- `generated_at`：生成时间
- `owner`：负责人
- `feedback`：发布反馈或备注
- `image_suggestion`：没有图片时的配图建议
- `generation_source`：mock、agent、manual、import
- `prompt_snapshot`：生成时的 Prompt 参数快照，JSON
- `llm_trace_id`：未来接入真实 LLM 后的调用链路 ID

### content_copy_image

文案图片表。用于存储文案配图元信息。

核心字段：

- `copy_id`：关联文案
- `file_name`：文件名
- `url`：图片访问地址
- `storage_provider`：minio、feishu、local、mock
- `object_key`：对象存储 key
- `sort_order`：排序

### content_calendar

内容日历表。用于标记某一天计划发布或已经发布哪些内容。

核心字段：

- `theme_id`：关联主题
- `copy_id`：关联文案
- `publish_date`：发布或计划发布日期
- `channel`：渠道
- `publish_status`：待创作、已生成、待发布、已发布
- `usage_status`：未使用、已使用、已归档
- `feishu_task_id`：未来同步飞书任务 ID

### content_tag

标签字典表。

核心字段：

- `name`：标签名称
- `category`：topic、purpose、style、conversion
- `color`：前端展示颜色

### content_theme_tag

主题标签关联表。一个主题可以绑定多个标签。

核心字段：

- `theme_id`：主题 ID
- `tag_id`：标签 ID

### content_generation_record

AI 生成流水表。用于记录小红书、朋友圈、知识资产智能体的生成结果。

核心字段：

- `agent_type`：xiaohongshu、moments、asset
- `input_json`：输入参数
- `output_json`：生成结果
- `status`：success、failed、running
- `feishu_doc_token`：飞书文档 token
- `feishu_bitable_record_id`：飞书多维表格记录 ID
- `vector_record_id`：向量库记录 ID

### content_publish_metric

内容发布表现表。为后续数据分析、内容复盘预留。

核心字段：

- `copy_id`：关联文案
- `publish_date`：发布日期
- `view_count`：浏览数
- `like_count`：点赞数
- `comment_count`：评论数
- `collect_count`：收藏数
- `conversion_count`：转化数
- `remark`：复盘备注

## 初始化方式

后端启动时，`ContentDatabaseInitializer` 会执行：

1. `CREATE TABLE IF NOT EXISTS`
2. 检查 `content_theme` 是否为空
3. 如果为空，写入 Demo mock 数据

也可以手动执行：

```bash
mysql -h127.0.0.1 -P3306 -uroot -p123456 --default-character-set=utf8mb4 < backend/sql/content-service-init.sql
```

Windows PowerShell 如果遇到中文编码问题，建议进入 MySQL 后使用：

```sql
source backend/sql/content-service-init.sql;
```
