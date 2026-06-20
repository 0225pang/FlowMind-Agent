from __future__ import annotations

import json
import time
from dataclasses import dataclass
from typing import Any, Callable, Iterable

import httpx

from . import fallback_data as fallback


class ApiError(RuntimeError):
    pass


@dataclass
class SseEvent:
    event: str
    data: dict[str, Any]


class ApiClient:
    def __init__(self, base_url: str = "http://localhost:8080", token: str = "mock-jwt.demo"):
        self.base_url = base_url.rstrip("/")
        self.token = token
        self.timeout = httpx.Timeout(20.0, connect=8.0, read=60.0)
        self.fallback_enabled = True
        self.current_user: dict[str, Any] | None = None

    def set_base_url(self, base_url: str) -> None:
        self.base_url = base_url.rstrip("/")

    def set_token(self, token: str) -> None:
        self.token = token or "mock-jwt.demo"

    def headers(self, extra: dict[str, str] | None = None) -> dict[str, str]:
        headers = {
            "Authorization": f"Bearer {self.token or 'mock-jwt.demo'}",
            "Content-Type": "application/json",
            "ngrok-skip-browser-warning": "true",
        }
        if extra:
            headers.update(extra)
        return headers

    def request(
        self,
        method: str,
        path: str,
        *,
        params: dict[str, Any] | None = None,
        json_body: dict[str, Any] | None = None,
        unwrap: bool = True,
    ) -> Any:
        if self.base_url.startswith("offline://"):
            raise ApiError("offline demo mode")
        url = self.base_url + path
        with httpx.Client(timeout=self.timeout, follow_redirects=True) as client:
            response = client.request(method, url, params=params, json=json_body, headers=self.headers())
        if response.status_code // 100 != 2:
            raise ApiError(f"HTTP {response.status_code}: {response.text[:300]}")
        try:
            body = response.json()
        except json.JSONDecodeError as exc:
            raise ApiError(f"响应不是 JSON: {response.text[:300]}") from exc
        if not unwrap:
            return body
        if isinstance(body, dict) and "code" in body:
            if body.get("code") != 200:
                raise ApiError(str(body.get("message") or "业务接口返回失败"))
            return body.get("data")
        return body

    def login(self, username: str, password: str) -> dict[str, Any]:
        try:
            data = self.request("POST", "/api/auth/login", json_body={"username": username, "password": password})
        except Exception:
            data = {"token": f"mock-jwt.{username or 'admin'}", "user": self.offline_user(username or "admin")}
        if isinstance(data, dict) and data.get("token"):
            self.set_token(str(data["token"]))
        if isinstance(data, dict) and isinstance(data.get("user"), dict):
            self.current_user = data["user"]
        return data or {}

    def me(self) -> dict[str, Any]:
        try:
            return self.request("GET", "/api/users/me") or {}
        except Exception:
            if self.current_user:
                return self.current_user
            return self.offline_user("admin")

    def offline_user(self, username: str) -> dict[str, Any]:
        permission_sets = {
            "admin": ["*"],
            "content": ["/api/analytics/**", "/api/agents/**", "/api/content/**", "/api/knowledge/**", "/api/feishu/**"],
            "teacher": [
                "/api/analytics/**",
                "/api/agents/**",
                "/api/knowledge/**",
                "/api/students/**",
                "/api/schools/**",
                "/api/school-projects/**",
            ],
            "ip": ["/api/analytics/**", "/api/agents/**", "/api/content/**", "/api/knowledge/**", "/api/feishu/**"],
            "student": ["/api/agents/**", "/api/knowledge/**"],
        }
        users = {
            "admin": ("FlowMind Admin", "ADMIN", ["ADMIN", "TEAM_ADMIN"]),
            "content": ("内容运营人员", "CONTENT_OPERATOR", ["CONTENT_OPERATOR"]),
            "teacher": ("教育咨询老师", "EDU_CONSULTANT", ["EDU_CONSULTANT"]),
            "ip": ("个人IP运营者", "IP_OPERATOR", ["IP_OPERATOR"]),
            "student": ("学员用户", "STUDENT_USER", ["STUDENT_USER"]),
        }
        normalized = username.strip().lower() or "admin"
        nickname, role, roles = users.get(normalized, users["admin"])
        return {
            "id": list(users).index(normalized) + 1 if normalized in users else 1,
            "username": normalized,
            "nickname": nickname,
            "role": role,
            "roles": roles,
            "permissions": permission_sets.get(normalized, ["*"]),
            "workspace": "保研内容运营工作空间",
        }

    def routes(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/gateway/routes") or []
        except Exception:
            return fallback.copy(fallback.ROUTES)

    def test_connection(self) -> dict[str, Any]:
        started = time.perf_counter()
        if self.base_url.startswith("offline://"):
            return {
                "ok": True,
                "mode": "offline-demo",
                "baseUrl": self.base_url,
                "latencyMs": 0,
                "message": "当前使用离线 Demo 数据，不访问网络。",
            }
        try:
            data = self.request("GET", "/api/users/me")
            return {
                "ok": True,
                "baseUrl": self.base_url,
                "latencyMs": round((time.perf_counter() - started) * 1000),
                "user": data,
            }
        except Exception as exc:
            return {
                "ok": False,
                "baseUrl": self.base_url,
                "latencyMs": round((time.perf_counter() - started) * 1000),
                "error": str(exc),
            }

    def list_agents(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/agents") or []
        except Exception:
            return fallback.copy(fallback.AGENTS)

    def new_session(self) -> str:
        try:
            data = self.request("POST", "/api/agents/conversations/new") or {}
        except Exception:
            existing = []
            for row in fallback.SESSIONS:
                raw = str(row.get("id") or "")
                try:
                    existing.append(int(raw.rsplit("-", 1)[-1]))
                except ValueError:
                    continue
            data = {"sessionId": f"demo-session-{(max(existing or [0]) + 1):03d}"}
            fallback.SESSIONS.insert(
                0,
                {
                    "id": data["sessionId"],
                    "title": "新会话",
                    "agentType": "auto",
                    "turnCount": 0,
                    "createdAt": "2026-06-19T00:00:00",
                    "updatedAt": "2026-06-19T00:00:00",
                },
            )
        return str(data.get("sessionId") or "")

    def sessions(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/agents/sessions") or []
        except Exception:
            return fallback.copy(fallback.SESSIONS)

    def delete_session(self, session_id: str) -> Any:
        try:
            return self.request("DELETE", f"/api/agents/sessions/{session_id}")
        except Exception:
            fallback.SESSIONS[:] = [row for row in fallback.SESSIONS if row.get("id") != session_id]
            fallback.CONVERSATION_HISTORY[:] = [row for row in fallback.CONVERSATION_HISTORY if row.get("sessionId") != session_id]
            return {"ok": True}

    def history(self, agent_type: str, session_id: str) -> list[dict[str, Any]]:
        try:
            return self.request("GET", f"/api/agents/conversations/{agent_type}/{session_id}") or []
        except Exception:
            rows = [row for row in fallback.CONVERSATION_HISTORY if row.get("sessionId") == session_id]
            return fallback.copy(rows)

    def clear_history(self, agent_type: str, session_id: str) -> Any:
        try:
            return self.request("DELETE", f"/api/agents/conversations/{agent_type}/{session_id}")
        except Exception:
            fallback.CONVERSATION_HISTORY[:] = [row for row in fallback.CONVERSATION_HISTORY if row.get("sessionId") != session_id]
            return {"ok": True}

    def chat(self, payload: dict[str, Any]) -> dict[str, Any]:
        try:
            return self.request("POST", "/api/agents/chat", json_body=payload) or {}
        except Exception:
            return {
                "agentType": "content",
                "reply": f"离线 Demo 回复：已收到「{payload.get('message', '')}」。后端启动后这里会返回真实 Agent 结果。",
                "sessionId": payload.get("sessionId") or "demo-session-001",
                "cards": [{"type": "trace", "title": "离线模式", "items": []}],
            }

    def stream_chat(self, payload: dict[str, Any]) -> Iterable[SseEvent]:
        if self.base_url.startswith("offline://"):
            yield from self.offline_stream_chat(payload)
            return
        url = self.base_url + "/api/agents/chat/stream"
        headers = self.headers({"Accept": "text/event-stream"})
        try:
            with httpx.Client(timeout=self.timeout, follow_redirects=True) as client:
                with client.stream("POST", url, headers=headers, json=payload) as response:
                    if response.status_code // 100 != 2:
                        raise ApiError(f"HTTP {response.status_code}: {response.text[:300]}")
                    buffer = ""
                    for chunk in response.iter_text():
                        buffer += chunk
                        frames = buffer.split("\n\n")
                        buffer = frames.pop() or ""
                        for frame in frames:
                            event = parse_sse_frame(frame)
                            if event:
                                yield event
        except Exception:
            yield from self.offline_stream_chat(payload)

    def offline_stream_chat(self, payload: dict[str, Any]) -> Iterable[SseEvent]:
        session_id = str(payload.get("sessionId") or self.new_session())
        message = str(payload.get("message") or "")
        agent_type = "content" if any(word in message for word in ["文案", "小红书", "朋友圈", "SOP"]) else "knowledge" if "知识" in message else "auto"
        reply = (
            f"离线 Demo 回复：我已收到「{message}」。当前演示会展示 session、thinking、trace、delta、done "
            "这些 SSE 事件；后端启动后会替换成真实 Agent 推理和工具调用结果。"
        )
        trace_items = [
            {
                "name": "OfflineRouter",
                "type": "router",
                "status": "used",
                "durationMs": 12,
                "summary": f"选择 {agent_type} 作为本轮处理智能体",
                "detail": "离线模式未访问真实 LLM、Weaviate 或飞书。",
            }
        ]
        turn_index = len([row for row in fallback.CONVERSATION_HISTORY if row.get("sessionId") == session_id]) // 2
        fallback.CONVERSATION_HISTORY.append(
            {
                "id": fallback.next_id(fallback.CONVERSATION_HISTORY),
                "agentType": agent_type,
                "sessionId": session_id,
                "turnIndex": turn_index,
                "role": "user",
                "content": message,
                "metadata": None,
                "createdAt": "2026-06-19T00:00:00",
            }
        )
        fallback.CONVERSATION_HISTORY.append(
            {
                "id": fallback.next_id(fallback.CONVERSATION_HISTORY),
                "agentType": agent_type,
                "sessionId": session_id,
                "turnIndex": turn_index,
                "role": "assistant",
                "content": reply,
                "metadata": json.dumps({"traceItems": trace_items, "thinkingHistory": ["离线模式：已选择智能体。", "离线模式：正在生成演示回答。"]}, ensure_ascii=False),
                "createdAt": "2026-06-19T00:00:01",
            }
        )
        for session in fallback.SESSIONS:
            if session.get("id") == session_id:
                session["title"] = message[:28] or "离线会话"
                session["agentType"] = agent_type
                session["turnCount"] = turn_index + 1
                session["updatedAt"] = "2026-06-19T00:00:01"
                break
        yield SseEvent("session", {"sessionId": session_id})
        yield SseEvent("thinking", {"content": f"已选择 {agent_type}，正在准备离线演示回答。"})
        yield SseEvent("trace", {"items": trace_items, "agentType": agent_type})
        yield SseEvent("reasoning", {"content": "离线模式下展示可见处理过程，不包含真实模型隐藏思维。"})
        for start in range(0, len(reply), 18):
            yield SseEvent("delta", {"content": reply[start : start + 18]})
        yield SseEvent("done", {"ok": True, "sessionId": session_id})

    def analytics_overview(self) -> dict[str, Any]:
        try:
            return self.request("GET", "/api/analytics/overview") or {}
        except Exception:
            return fallback.copy(fallback.OVERVIEW)

    def analytics_distribution(self) -> dict[str, Any]:
        try:
            return self.request("GET", "/api/analytics/student-distribution") or {}
        except Exception:
            return fallback.copy(fallback.STUDENT_DISTRIBUTION)

    def analytics_content_stats(self) -> dict[str, Any]:
        try:
            return self.request("GET", "/api/analytics/content-stats") or {}
        except Exception:
            return fallback.copy(fallback.CONTENT_STATS)

    def analytics_funnel(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/analytics/application-funnel") or []
        except Exception:
            return fallback.copy(fallback.APPLICATION_FUNNEL)

    def analytics_deadlines(self) -> dict[str, Any]:
        try:
            return self.request("GET", "/api/analytics/school-deadlines") or {}
        except Exception:
            return fallback.copy(fallback.SCHOOL_DEADLINES)

    def content_themes(self, keyword: str = "", status: str = "", channel: str = "") -> list[dict[str, Any]]:
        params = {k: v for k, v in {"keyword": keyword, "status": status, "channel": channel}.items() if v}
        try:
            return self.request("GET", "/api/content/themes", params=params) or []
        except Exception:
            rows = fallback.filter_contains(fallback.CONTENT_THEMES, keyword)
            if status:
                rows = [row for row in rows if row.get("status") == status]
            if channel:
                rows = [row for row in rows if row.get("platform") == channel]
            return fallback.copy(rows)

    def create_theme(self, payload: dict[str, Any]) -> dict[str, Any]:
        try:
            return self.request("POST", "/api/content/themes", json_body=payload) or {}
        except Exception:
            row = {"id": fallback.next_id(fallback.CONTENT_THEMES), "rating": 0, **payload}
            fallback.CONTENT_THEMES.insert(0, row)
            return fallback.copy(row)

    def delete_theme(self, theme_id: int) -> Any:
        try:
            return self.request("DELETE", f"/api/content/themes/{theme_id}")
        except Exception:
            fallback.CONTENT_THEMES[:] = [row for row in fallback.CONTENT_THEMES if int(row.get("id") or 0) != theme_id]
            fallback.COPY_DRAFTS[:] = [row for row in fallback.COPY_DRAFTS if int(row.get("themeId") or 0) != theme_id]
            return {"ok": True}

    def rate_theme(self, theme_id: int, rating: int) -> dict[str, Any]:
        try:
            return self.request("PUT", f"/api/content/themes/{theme_id}/rating", json_body={"rating": rating}) or {}
        except Exception:
            for row in fallback.CONTENT_THEMES:
                if int(row.get("id") or 0) == theme_id:
                    row["rating"] = rating
                    return fallback.copy(row)
            return {}

    def theme_drafts(self, theme_id: int) -> list[dict[str, Any]]:
        try:
            return self.request("GET", f"/api/content/themes/{theme_id}/drafts") or []
        except Exception:
            return fallback.copy([row for row in fallback.COPY_DRAFTS if int(row.get("themeId") or 0) == theme_id])

    def drafts(self, keyword: str = "", channel: str = "", usage_status: str = "") -> list[dict[str, Any]]:
        params = {k: v for k, v in {"keyword": keyword, "channel": channel, "usageStatus": usage_status}.items() if v}
        try:
            return self.request("GET", "/api/content/drafts", params=params) or []
        except Exception:
            rows = fallback.filter_contains(fallback.COPY_DRAFTS, keyword)
            if channel:
                rows = [row for row in rows if row.get("channel") == channel]
            if usage_status:
                rows = [row for row in rows if row.get("usageStatus") == usage_status]
            return fallback.copy(rows)

    def create_content_draft(self, payload: dict[str, Any]) -> dict[str, Any]:
        """通用文案入库，不强制绑定 theme_id，适合 SOP 一键入库。"""
        try:
            return self.request("POST", "/api/content/drafts", json_body=payload) or {}
        except Exception:
            row = {
                "id": fallback.next_id(fallback.COPY_DRAFTS),
                "themeId": 0,
                "usageStatus": "未使用",
                "generatedAt": "2026-06-19 00:00",
                "feedback": "",
                "rating": 0,
                "imageSuggestion": "",
                **payload,
            }
            fallback.COPY_DRAFTS.insert(0, row)
            return fallback.copy(row)

    def create_draft(self, theme_id: int, payload: dict[str, Any]) -> dict[str, Any]:
        try:
            return self.request("POST", f"/api/content/themes/{theme_id}/drafts", json_body=payload) or {}
        except Exception:
            row = {
                "id": fallback.next_id(fallback.COPY_DRAFTS),
                "themeId": theme_id,
                "usageStatus": "未使用",
                "generatedAt": "2026-06-19 00:00",
                "feedback": "",
                "rating": 0,
                "imageSuggestion": "",
                **payload,
            }
            fallback.COPY_DRAFTS.insert(0, row)
            return fallback.copy(row)

    def update_draft(self, draft_id: int, payload: dict[str, Any]) -> dict[str, Any]:
        try:
            return self.request("PUT", f"/api/content/drafts/{draft_id}", json_body=payload) or {}
        except Exception:
            for row in fallback.COPY_DRAFTS:
                if int(row.get("id") or 0) == draft_id:
                    row.update(payload)
                    return fallback.copy(row)
            return {}

    def delete_draft(self, draft_id: int) -> Any:
        try:
            return self.request("DELETE", f"/api/content/drafts/{draft_id}")
        except Exception:
            fallback.COPY_DRAFTS[:] = [row for row in fallback.COPY_DRAFTS if int(row.get("id") or 0) != draft_id]
            return {"ok": True}

    def rate_draft(self, draft_id: int, rating: int) -> dict[str, Any]:
        try:
            return self.request("PUT", f"/api/content/drafts/{draft_id}/rating", json_body={"rating": rating}) or {}
        except Exception:
            for row in fallback.COPY_DRAFTS:
                if int(row.get("id") or 0) == draft_id:
                    row["rating"] = rating
                    return fallback.copy(row)
            return {}

    def add_draft_image(self, draft_id: int, payload: dict[str, Any]) -> dict[str, Any]:
        try:
            return self.request("POST", f"/api/content/drafts/{draft_id}/images", json_body=payload) or {}
        except Exception:
            for row in fallback.COPY_DRAFTS:
                if int(row.get("id") or 0) == draft_id:
                    images = row.setdefault("images", [])
                    image = {"id": fallback.next_id(images), **payload}
                    images.append(image)
                    return fallback.copy(image)
            return {"id": 1, **payload}

    def calendar(self, month: str = "") -> list[dict[str, Any]]:
        params = {"month": month} if month else None
        try:
            return self.request("GET", "/api/content/calendar", params=params) or []
        except Exception:
            rows = fallback.copy(fallback.CONTENT_CALENDAR)
            if month:
                rows = [row for row in rows if str(row.get("date", "")).startswith(month)]
            return rows

    def content_generate(self, kind: str, payload: dict[str, Any]) -> Any:
        path = {
            "xiaohongshu": "/api/content/sop/xiaohongshu/generate",
            "moments": "/api/content/sop/moments/generate",
            "asset": "/api/content/sop/assets/extract",
        }[kind]
        try:
            return self.request("POST", path, json_body=payload)
        except Exception:
            topic = payload.get("topic") or payload.get("scene") or "内容"
            return {
                "kind": kind,
                "input": payload,
                "steps": ["检索历史内容结构", "抽取可复用模板", "生成多版本草稿", "生成标题与标签", "写入飞书和本地内容库"],
                "drafts": [
                    {
                        "title": f"{topic}：干货版",
                        "style": "干货",
                        "content": "用问题切入，先讲常见误区，再给出可执行清单，最后引导用户保存或私信咨询。",
                    },
                    {
                        "title": f"{topic}：情绪增强版",
                        "style": "情绪增强",
                        "content": "先描述学生真实焦虑，再给出拆解后的方法，让内容更像经验复盘而不是硬广。",
                    },
                    {
                        "title": f"{topic}：转化引导版",
                        "style": "转化引导",
                        "content": "在结尾加入材料检查、时间规划或案例复盘入口，保持克制但明确下一步动作。",
                    },
                ],
                "assets": ["标题钩子", "正文结构", "配图建议", "飞书沉淀", "本地入库"],
            }

    def content_architecture(self) -> Any:
        try:
            return self.request("GET", "/api/content/sop/architecture")
        except Exception:
            return {
                "kind": "architecture",
                "stages": ["触发生成", "知识检索", "结构模板压缩", "LLM 多版本生成", "飞书文档沉淀", "多维表格记录", "本地内容库入库"],
                "assets": ["主题", "文案", "标题", "标签", "发布反馈", "图片引用"],
            }

    def knowledge_docs(self, keyword: str = "") -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/knowledge/docs", params={"keyword": keyword}) or []
        except Exception:
            return fallback.filter_contains(fallback.KNOWLEDGE_DOCS, keyword)

    def knowledge_doc(self, doc_id: int) -> dict[str, Any]:
        try:
            return self.request("GET", f"/api/knowledge/docs/{doc_id}") or {}
        except Exception:
            return fallback.copy(next((row for row in fallback.KNOWLEDGE_DOCS if int(row.get("id") or 0) == doc_id), {}))

    def update_doc_tags(self, doc_id: int, tags: list[str]) -> Any:
        try:
            return self.request("PUT", f"/api/knowledge/docs/{doc_id}/tags", json_body={"tags": tags})
        except Exception:
            for row in fallback.KNOWLEDGE_DOCS:
                if int(row.get("id") or 0) == doc_id:
                    row["tags"] = tags
                    return fallback.copy(row)
            return {"id": doc_id, "tags": tags}

    def sync_knowledge(self) -> Any:
        try:
            return self.request("POST", "/api/knowledge/sync")
        except Exception:
            return {"added": 0, "updated": len(fallback.KNOWLEDGE_DOCS), "skipped": 0, "errors": 0, "mode": "offline-demo"}

    def knowledge_stats(self) -> dict[str, Any]:
        try:
            return self.request("GET", "/api/knowledge/stats") or {}
        except Exception:
            return {"docCount": len(fallback.KNOWLEDGE_DOCS)}

    def knowledge_tags(self) -> list[str]:
        try:
            return self.request("GET", "/api/knowledge/tags") or []
        except Exception:
            tags = sorted({tag for row in fallback.KNOWLEDGE_DOCS for tag in row.get("tags", [])})
            return tags

    def knowledge_sync_status(self) -> dict[str, Any]:
        try:
            return self.request("GET", "/api/knowledge/sync-status") or {}
        except Exception:
            return {
                "docs": {
                    "status": "SUCCESS",
                    "lastSync": "2026-06-18 10:00:00",
                    "added": 0,
                    "updated": len(fallback.KNOWLEDGE_DOCS),
                    "skipped": 0,
                    "errors": 0,
                    "count": len(fallback.KNOWLEDGE_DOCS),
                },
                "bitable": {"status": "--", "lastSync": None, "added": 0, "updated": 0, "skipped": 0, "errors": 0, "count": 0},
                "tasks": {"status": "--", "lastSync": None, "added": 0, "updated": 0, "skipped": 0, "errors": 0, "count": 0},
                "bot": {"status": "--", "lastSync": None, "added": 0, "updated": 0, "skipped": 0, "errors": 0, "count": 0},
            }

    def knowledge_sync_logs(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/knowledge/sync-logs") or []
        except Exception:
            return fallback.copy(fallback.FEISHU_LOGS)

    def vector_search(self, query: str, top_k: int = 5) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/knowledge/vector/search", params={"q": query, "topK": top_k}) or []
        except Exception:
            rows = fallback.filter_contains(fallback.KNOWLEDGE_DOCS, query) or fallback.copy(fallback.KNOWLEDGE_DOCS)
            return [
                {
                    "source": "offline-demo",
                    "mysqlId": row.get("id"),
                    "title": row.get("title"),
                    "chunkText": row.get("summary") or row.get("content"),
                    "feishuToken": row.get("feishuToken"),
                    "feishuUrl": row.get("feishuUrl"),
                    "feishuType": row.get("feishuType"),
                    "tags": row.get("tags"),
                    "distance": 0.42,
                }
                for row in rows[:top_k]
            ]

    def students(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/students") or []
        except Exception:
            return fallback.copy(fallback.STUDENTS)

    def create_student(self, payload: dict[str, Any]) -> Any:
        try:
            return self.request("POST", "/api/students", json_body=payload)
        except Exception:
            row = {"id": fallback.next_id(fallback.STUDENTS), **payload}
            fallback.STUDENTS.insert(0, row)
            return fallback.copy(row)

    def update_student(self, student_id: int, payload: dict[str, Any]) -> Any:
        try:
            return self.request("PUT", f"/api/students/{student_id}", json_body=payload)
        except Exception:
            for row in fallback.STUDENTS:
                if int(row.get("id") or 0) == student_id:
                    row.update(payload)
                    row["id"] = student_id
                    return fallback.copy(row)
            return {"id": student_id, **payload}

    def delete_student(self, student_id: int) -> Any:
        try:
            return self.request("DELETE", f"/api/students/{student_id}")
        except Exception:
            fallback.STUDENTS[:] = [row for row in fallback.STUDENTS if int(row.get("id") or 0) != student_id]
            return {"ok": True}

    def analyze_student(self, student_id: int) -> Any:
        try:
            return self.request("POST", f"/api/students/{student_id}/analyze")
        except Exception:
            return {
                "studentId": student_id,
                "risk": "中风险",
                "summary": "离线 Demo 分析：建议补充科研经历证据，并优先准备材料和面试表达。",
                "actions": [
                    "补充 1 段科研经历的问题、方法、结果和证据。",
                    "本周完成简历和个人陈述初稿。",
                    "优先筛选 3 个匹配度较高的夏令营项目。",
                ],
            }

    def schools(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/schools") or []
        except Exception:
            return fallback.copy(fallback.SCHOOLS)

    def create_school(self, payload: dict[str, Any]) -> Any:
        try:
            return self.request("POST", "/api/schools", json_body=payload)
        except Exception:
            row = {"id": fallback.next_id(fallback.SCHOOLS), **payload}
            fallback.SCHOOLS.insert(0, row)
            return fallback.copy(row)

    def projects(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/school-projects") or []
        except Exception:
            return fallback.copy(fallback.PROJECTS)

    def create_project(self, payload: dict[str, Any]) -> Any:
        try:
            return self.request("POST", "/api/school-projects", json_body=payload)
        except Exception:
            row = {"id": fallback.next_id(fallback.PROJECTS), **payload}
            fallback.PROJECTS.insert(0, row)
            return fallback.copy(row)

    def recommend_schools(self, payload: dict[str, Any]) -> Any:
        try:
            return self.request("POST", "/api/schools/recommend", json_body=payload)
        except Exception:
            return fallback.copy(fallback.PROJECTS)

    def feishu_status(self) -> dict[str, Any]:
        try:
            return self.request("GET", "/api/feishu/sync/status") or {}
        except Exception:
            return fallback.copy(fallback.FEISHU_STATUS)

    def feishu_logs(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/feishu/logs") or []
        except Exception:
            return fallback.copy(fallback.FEISHU_LOGS)

    def feishu_action(self, action: str, payload: dict[str, Any] | None = None) -> Any:
        paths = {
            "sync_docs": ("POST", "/api/feishu/sync/docs"),
            "sync_bitable": ("POST", "/api/feishu/sync/bitable"),
            "sync_tasks": ("POST", "/api/feishu/sync/tasks"),
            "push": ("POST", "/api/feishu/bot/push"),
            "lark_status": ("GET", "/api/feishu/lark-cli/status"),
            "knowledge_files": ("GET", "/api/feishu/knowledge-base/files"),
            "create_doc": ("POST", "/api/feishu/docs/create"),
            "fetch_doc": ("POST", "/api/feishu/docs/fetch"),
        }
        method, path = paths[action]
        try:
            return self.request(method, path, json_body=payload)
        except Exception:
            if action == "create_doc":
                title = (payload or {}).get("title") or "FlowMind 桌面端文档"
                return {
                    "title": title,
                    "url": "https://example.feishu.cn/docx/offline-demo",
                    "token": "offline_demo_doc_token",
                    "mode": "offline-demo",
                }
            if action == "fetch_doc":
                return {
                    "docToken": (payload or {}).get("docToken") or "offline_demo_doc_token",
                    "title": "离线 Demo 飞书文档",
                    "content": "后端和 lark-cli 可用后，这里会展示真实飞书文档内容。",
                    "mode": "offline-demo",
                }
            if action == "knowledge_files":
                return {"name": "保研知识库", "folderToken": "offline-demo-folder", "files": fallback.copy(fallback.KNOWLEDGE_DOCS)}
            return {"action": action, "payload": payload, "status": "offline-demo"}

    def prompts(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/prompts") or []
        except Exception:
            return fallback.copy(fallback.PROMPTS)

    def create_prompt(self, payload: dict[str, Any]) -> Any:
        try:
            return self.request("POST", "/api/prompts", json_body=payload)
        except Exception:
            row = {"id": fallback.next_id(fallback.PROMPTS), **payload}
            fallback.PROMPTS.insert(0, row)
            return fallback.copy(row)

    def roles(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/roles") or []
        except Exception:
            return fallback.copy(fallback.ROLES)

    def permissions(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/permissions") or []
        except Exception:
            return fallback.copy(fallback.PERMISSIONS)

    def role_permissions(self, role_code: str) -> Any:
        try:
            return self.request("GET", f"/api/roles/{role_code}/permissions")
        except Exception:
            role = next((row for row in fallback.ROLES if row.get("roleCode") == role_code), None)
            permissions = role.get("permissions", []) if role else fallback.PERMISSIONS
            return {"roleCode": role_code, "permissionCodes": [row.get("permissionCode") or row.get("code") for row in permissions]}

    def update_role_permissions(self, role_code: str, permission_codes: list[str]) -> Any:
        try:
            return self.request(
                "PUT",
                f"/api/roles/{role_code}/permissions",
                json_body={"permissionCodes": permission_codes},
            )
        except Exception:
            for row in fallback.ROLES:
                if row.get("roleCode") == role_code:
                    row["permissions"] = [
                        permission
                        for permission in fallback.PERMISSIONS
                        if permission.get("permissionCode") in permission_codes or permission.get("code") in permission_codes
                    ]
                    return fallback.copy(row)
            return {"roleCode": role_code, "permissionCodes": permission_codes, "mode": "offline-demo"}


def parse_sse_frame(frame: str) -> SseEvent | None:
    event_name = "message"
    data_lines: list[str] = []
    for raw in frame.splitlines():
        line = raw.strip("\r")
        if line.startswith("event:"):
            event_name = line[6:].strip() or "message"
        elif line.startswith("data:"):
            data_lines.append(line[5:].strip())
    if not data_lines:
        return None
    data_text = "\n".join(data_lines)
    try:
        data = json.loads(data_text)
    except json.JSONDecodeError:
        data = {"content": data_text}
    if not isinstance(data, dict):
        data = {"content": data}
    return SseEvent(event=event_name, data=data)


def safe_json_dumps(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2)
