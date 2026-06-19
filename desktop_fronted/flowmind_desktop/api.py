from __future__ import annotations

import json
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
            data = {
                "token": "mock-jwt.demo",
                "user": {
                    "id": 1,
                    "username": username or "admin",
                    "nickname": "FlowMind Admin",
                    "role": "ADMIN",
                },
            }
        if isinstance(data, dict) and data.get("token"):
            self.set_token(str(data["token"]))
        return data or {}

    def me(self) -> dict[str, Any]:
        try:
            return self.request("GET", "/api/users/me") or {}
        except Exception:
            return {"id": 1, "username": "admin", "nickname": "FlowMind Admin", "role": "ADMIN", "workspace": "桌面端 Demo 工作空间"}

    def routes(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/gateway/routes") or []
        except Exception:
            return fallback.copy(fallback.ROUTES)

    def list_agents(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/agents") or []
        except Exception:
            return fallback.copy(fallback.AGENTS)

    def new_session(self) -> str:
        try:
            data = self.request("POST", "/api/agents/conversations/new") or {}
        except Exception:
            data = {"sessionId": f"demo-session-{fallback.next_id(fallback.SESSIONS):03d}"}
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
            return {"ok": True}

    def history(self, agent_type: str, session_id: str) -> list[dict[str, Any]]:
        try:
            return self.request("GET", f"/api/agents/conversations/{agent_type}/{session_id}") or []
        except Exception:
            rows = [row for row in fallback.CONVERSATION_HISTORY if row.get("sessionId") == session_id]
            return fallback.copy(rows or fallback.CONVERSATION_HISTORY)

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
        url = self.base_url + "/api/agents/chat/stream"
        headers = self.headers({"Accept": "text/event-stream"})
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
            return {
                "kind": kind,
                "input": payload,
                "steps": ["结构检索", "模板压缩", "多版本生成", "入库沉淀"],
                "drafts": [
                    {
                        "title": f"{payload.get('topic') or payload.get('scene') or '内容'}：桌面端离线生成示例",
                        "content": "后端启动后这里会展示真实 SOP 生成结果。",
                    }
                ],
            }

    def content_architecture(self) -> Any:
        try:
            return self.request("GET", "/api/content/sop/architecture")
        except Exception:
            return {"stages": ["触发生成", "知识检索", "内容生成", "飞书沉淀", "本地入库"]}

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
        return self.request("PUT", f"/api/knowledge/docs/{doc_id}/tags", json_body={"tags": tags})

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
        return self.request("PUT", f"/api/students/{student_id}", json_body=payload)

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
            return {"studentId": student_id, "risk": "中风险", "summary": "离线 Demo 分析：建议补充科研经历证据，并优先准备材料和面试表达。"}

    def schools(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/schools") or []
        except Exception:
            return fallback.copy(fallback.SCHOOLS)

    def projects(self) -> list[dict[str, Any]]:
        try:
            return self.request("GET", "/api/school-projects") or []
        except Exception:
            return fallback.copy(fallback.PROJECTS)

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
            return {"action": action, "payload": payload, "status": "offline-demo"}

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
            return {"roleCode": role_code, "permissionCodes": [row["code"] for row in fallback.PERMISSIONS]}

    def update_role_permissions(self, role_code: str, permission_codes: list[str]) -> Any:
        return self.request(
            "PUT",
            f"/api/roles/{role_code}/permissions",
            json_body={"permissionCodes": permission_codes},
        )


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
