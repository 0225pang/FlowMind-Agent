# 数据库设计

SQL 位于 `backend/sql/schema.sql` 与 `backend/sql/mock-data.sql`。表包含用户、角色、Agent 会话、Prompt 模板、知识库、内容选题、内容日历、学员画像、申请进度、院校信息、院校项目、飞书同步记录和系统日志。

每张表均包含 `id`、`created_at`、`updated_at`、`deleted` 字段，便于后续做逻辑删除、审计和数据同步。

核心表：
- `sys_user`, `sys_role`, `sys_user_role`
- `agent_session`, `agent_message`, `prompt_template`
- `knowledge_doc`, `knowledge_tag`, `knowledge_doc_tag`
- `content_topic`, `content_calendar`
- `student_profile`, `student_application_progress`
- `school_info`, `school_project`
- `feishu_sync_record`, `system_log`
