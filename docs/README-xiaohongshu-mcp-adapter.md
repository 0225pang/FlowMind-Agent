# Xiaohongshu MCP Adapter

This module adapts the idea of `xpzouying/xiaohongshu-mcp` into FlowMind Agent.
It is not copied as a standalone MCP server. FlowMind keeps a thin adapter layer
inside `ai-agent-service` so the main agent can use Xiaohongshu content research
as a content-operation skill.

## Current Scope

Enabled for the agent:

- Check MCP availability/login status metadata.
- Search Xiaohongshu-style hot notes by topic.
- Read public note detail when a real MCP endpoint is configured.
- Extract hot-note structures.
- Generate Xiaohongshu SOP context for the LLM.

Registered but hidden from the agent:

- Publish image note.
- Publish video note.
- Create comment.
- Reply to comment.
- Like note.
- Collect note.

These high-risk actions are intentionally not exposed to the agent in the current
demo.

## Files

- `backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/xiaohongshu/XiaohongshuMcpToolRegistry.java`
- `backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/xiaohongshu/XiaohongshuMcpClient.java`
- `backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/xiaohongshu/XiaohongshuSopSkillExtension.java`
- `backend/ai-agent-service/src/main/java/com/flowmind/agent/extension/xiaohongshu/XiaohongshuMcpController.java`

## Configuration

Default configuration is in:

`backend/app-service/src/main/resources/application.yml`

```yaml
flowmind:
  tools:
    xiaohongshu-mcp:
      enabled: false
      agent-enabled: true
      base-url: ${FLOWMIND_XHS_MCP_BASE_URL:}
      search-path: /api/xiaohongshu/search
      detail-path: /api/xiaohongshu/detail
      timeout-seconds: 20
      default-limit: 8
      mock-fallback: true
```

When `enabled=false`, the skill still works with FlowMind mock hot-note
structures. This is useful for demos and offline development.

When a real MCP HTTP wrapper is available, set:

```powershell
$env:FLOWMIND_XHS_MCP_BASE_URL="http://localhost:xxxx"
```

Then set `enabled: true` in a local profile.

## Test APIs

List visible and hidden tools:

```http
GET /api/agents/xiaohongshu/tools
```

Search topic:

```http
GET /api/agents/xiaohongshu/search?topic=保研简历&limit=8
```

Read note detail:

```http
GET /api/agents/xiaohongshu/detail?id={noteIdOrUrl}
```

## Chat Trigger

In AI Workspace, ask:

```text
搜索小红书关于“保研简历”的热点帖子，并按爆款结构仿写三版笔记。
```

The extension will inject runtime context into the content/auto agent:

1. Hot note samples.
2. Engagement signals.
3. Structure clues.
4. Required Xiaohongshu SOP.
5. Safety rules.

The LLM should then output:

- Hot structure analysis.
- Reusable structure template.
- Original note draft.
- 10 title options.
- Dry-guide, emotion-enhanced and conversion-guided versions.
- Fields suitable for Feishu/Base storage.

## Safety Rule

Do not expose high-risk tools to `AgentTraceService` until the product has:

- User confirmation UI.
- Account permission model.
- Audit logs.
- Rate limits.
- Clear rollback/manual review workflow.

