from __future__ import annotations

import json
import threading
import tkinter as tk
from pathlib import Path
from tkinter import messagebox, ttk
from typing import Any, Callable

from .api import ApiClient, safe_json_dumps


CONFIG_DIR = Path.home() / ".flowmind_desktop"
CONFIG_FILE = CONFIG_DIR / "tk_config.json"


COLORS = {
    "bg": "#f6f8fc",
    "card": "#ffffff",
    "line": "#e7ebf3",
    "primary": "#5b6cff",
    "success": "#19b37b",
    "text": "#152033",
    "muted": "#667085",
}


def load_config() -> dict[str, str]:
    if CONFIG_FILE.exists():
        try:
            return json.loads(CONFIG_FILE.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            return {}
    return {}


def save_config(config: dict[str, str]) -> None:
    CONFIG_DIR.mkdir(parents=True, exist_ok=True)
    CONFIG_FILE.write_text(json.dumps(config, ensure_ascii=False, indent=2), encoding="utf-8")


def run_async(root: tk.Misc, fn: Callable[[], Any], on_done: Callable[[Any], None], on_error: Callable[[str], None] | None = None) -> None:
    def work() -> None:
        try:
            result = fn()
            root.after(0, lambda: on_done(result))
        except Exception as exc:  # noqa: BLE001
            root.after(0, lambda: (on_error or (lambda msg: messagebox.showwarning("FlowMind", msg)))(str(exc)))

    threading.Thread(target=work, daemon=True).start()


def clear(container: tk.Misc) -> None:
    for child in container.winfo_children():
        child.destroy()


def add_title(parent: tk.Misc, title: str, subtitle: str = "") -> None:
    tk.Label(parent, text=title, bg=COLORS["bg"], fg=COLORS["text"], font=("Microsoft YaHei", 20, "bold")).pack(anchor="w")
    if subtitle:
        tk.Label(parent, text=subtitle, bg=COLORS["bg"], fg=COLORS["muted"], font=("Microsoft YaHei", 10)).pack(anchor="w", pady=(2, 16))


def card(parent: tk.Misc) -> tk.Frame:
    frame = tk.Frame(parent, bg=COLORS["card"], highlightbackground=COLORS["line"], highlightthickness=1, padx=14, pady=12)
    return frame


def make_table(parent: tk.Misc, columns: list[str]) -> ttk.Treeview:
    table = ttk.Treeview(parent, columns=columns, show="headings", height=14)
    for col in columns:
        table.heading(col, text=col)
        table.column(col, width=120, anchor="w")
    y = ttk.Scrollbar(parent, orient="vertical", command=table.yview)
    table.configure(yscrollcommand=y.set)
    table.pack(side="left", fill="both", expand=True)
    y.pack(side="right", fill="y")
    return table


def set_rows(table: ttk.Treeview, rows: list[dict[str, Any]], keys: list[str]) -> None:
    table.delete(*table.get_children())
    for row in rows:
        values = []
        for key in keys:
            value = row.get(key, "")
            if isinstance(value, list):
                value = "、".join(map(str, value))
            values.append("" if value is None else str(value))
        item_id = table.insert("", "end", values=values)
        table.set(item_id, "#0", json.dumps(row, ensure_ascii=False))


def selected_row(table: ttk.Treeview) -> dict[str, Any] | None:
    selection = table.selection()
    if not selection:
        return None
    values = table.item(selection[0], "values")
    columns = table["columns"]
    return dict(zip(columns, values))


class LoginFrame(tk.Frame):
    def __init__(self, master: tk.Misc, client: ApiClient, on_login: Callable[[], None]):
        super().__init__(master, bg=COLORS["bg"])
        self.client = client
        self.on_login = on_login
        self.pack(fill="both", expand=True)

        box = card(self)
        box.place(relx=0.5, rely=0.5, anchor="center", width=430)
        tk.Label(box, text="FlowMind", bg=COLORS["card"], fg=COLORS["text"], font=("Microsoft YaHei", 24, "bold")).pack(anchor="w")
        tk.Label(box, text="Agent Platform 桌面客户端", bg=COLORS["card"], fg=COLORS["muted"]).pack(anchor="w", pady=(0, 18))
        self.base_url = self.input(box, "Base URL", self.client.base_url)
        self.username = self.input(box, "用户名", "admin")
        self.password = self.input(box, "密码", "123456", show="*")
        self.status = tk.Label(box, text="后端未启动时会自动使用离线 Demo 数据。", bg=COLORS["card"], fg=COLORS["muted"])
        self.status.pack(anchor="w", pady=(8, 0))
        ttk.Button(box, text="登录", command=self.login).pack(fill="x", pady=(16, 6))
        ttk.Button(box, text="使用 Demo Token 进入", command=self.demo).pack(fill="x")

    def input(self, parent: tk.Misc, label: str, value: str, show: str | None = None) -> tk.Entry:
        tk.Label(parent, text=label, bg=COLORS["card"], fg=COLORS["text"]).pack(anchor="w")
        entry = ttk.Entry(parent, show=show)
        entry.insert(0, value)
        entry.pack(fill="x", pady=(4, 10))
        return entry

    def login(self) -> None:
        self.client.set_base_url(self.base_url.get().strip())
        self.status.config(text="正在登录...")
        run_async(self, lambda: self.client.login(self.username.get(), self.password.get()), self.login_ok, self.login_fail)

    def login_ok(self, result: dict[str, Any]) -> None:
        save_config({"base_url": self.client.base_url, "token": self.client.token})
        self.on_login()

    def login_fail(self, message: str) -> None:
        self.status.config(text=f"登录失败：{message}")

    def demo(self) -> None:
        self.client.set_base_url(self.base_url.get().strip())
        self.client.set_token("mock-jwt.demo")
        save_config({"base_url": self.client.base_url, "token": self.client.token})
        self.on_login()


class ShellFrame(tk.Frame):
    NAV = [
        ("Dashboard", "dashboard"),
        ("AI 工作台", "agent"),
        ("知识库", "knowledge"),
        ("内容运营", "content"),
        ("学员管理", "students"),
        ("院校情报", "schools"),
        ("数据分析", "analytics"),
        ("飞书同步", "feishu"),
        ("系统设置", "settings"),
    ]

    def __init__(self, master: tk.Misc, client: ApiClient, on_logout: Callable[[], None]):
        super().__init__(master, bg=COLORS["bg"])
        self.client = client
        self.on_logout = on_logout
        self.pack(fill="both", expand=True)

        self.sidebar = tk.Frame(self, bg=COLORS["card"], width=248, highlightbackground=COLORS["line"], highlightthickness=1)
        self.sidebar.pack(side="left", fill="y")
        self.sidebar.pack_propagate(False)
        tk.Label(self.sidebar, text="F", bg=COLORS["primary"], fg="white", font=("Microsoft YaHei", 18, "bold"), width=3).pack(anchor="w", padx=18, pady=(18, 4))
        tk.Label(self.sidebar, text="FlowMind", bg=COLORS["card"], fg=COLORS["text"], font=("Microsoft YaHei", 13, "bold")).pack(anchor="w", padx=18)
        tk.Label(self.sidebar, text="Agent Platform", bg=COLORS["card"], fg=COLORS["muted"]).pack(anchor="w", padx=18, pady=(0, 18))

        self.buttons: dict[str, ttk.Button] = {}
        for label, key in self.NAV:
            btn = ttk.Button(self.sidebar, text=label, command=lambda k=key: self.show_page(k))
            btn.pack(fill="x", padx=12, pady=3)
            self.buttons[key] = btn
        ttk.Button(self.sidebar, text="退出登录", command=self.logout).pack(side="bottom", fill="x", padx=12, pady=18)

        self.main = tk.Frame(self, bg=COLORS["bg"])
        self.main.pack(side="left", fill="both", expand=True)

        self.pages = {
            "dashboard": DashboardTk(self.main, client),
            "agent": AgentTk(self.main, client),
            "knowledge": KnowledgeTk(self.main, client),
            "content": ContentTk(self.main, client),
            "students": StudentsTk(self.main, client),
            "schools": SchoolsTk(self.main, client),
            "analytics": AnalyticsTk(self.main, client),
            "feishu": FeishuTk(self.main, client),
            "settings": SettingsTk(self.main, client),
        }
        self.show_page("dashboard")

    def show_page(self, key: str) -> None:
        for page in self.pages.values():
            page.pack_forget()
        page = self.pages[key]
        page.pack(fill="both", expand=True)
        page.load()

    def logout(self) -> None:
        save_config({"base_url": self.client.base_url})
        self.on_logout()


class Page(tk.Frame):
    def __init__(self, master: tk.Misc, client: ApiClient, title: str, subtitle: str):
        super().__init__(master, bg=COLORS["bg"], padx=22, pady=22)
        self.client = client
        add_title(self, title, subtitle)

    def load(self) -> None:
        pass


class DashboardTk(Page):
    def __init__(self, master: tk.Misc, client: ApiClient):
        super().__init__(master, client, "Dashboard", "保研运营核心指标、内容选题和申请趋势")
        self.stats = tk.Frame(self, bg=COLORS["bg"])
        self.stats.pack(fill="x")
        self.charts = tk.Text(self, height=22, wrap="word")
        self.charts.pack(fill="both", expand=True, pady=16)

    def load(self) -> None:
        def fetch() -> dict[str, Any]:
            return {
                "overview": self.client.analytics_overview(),
                "student": self.client.analytics_distribution(),
                "content": self.client.analytics_content_stats(),
                "funnel": self.client.analytics_funnel(),
                "deadlines": self.client.analytics_deadlines(),
            }

        run_async(self, fetch, self.render)

    def render(self, data: dict[str, Any]) -> None:
        clear(self.stats)
        overview = data["overview"]
        for label, key in [("学员数", "students"), ("活跃项目", "activeProjects"), ("内容选题", "contentTopics"), ("任务完成率", "taskRate")]:
            box = card(self.stats)
            box.pack(side="left", fill="x", expand=True, padx=(0, 12))
            tk.Label(box, text=label, bg=COLORS["card"], fg=COLORS["muted"]).pack(anchor="w")
            tk.Label(box, text=str(overview.get(key, "-")), bg=COLORS["card"], fg=COLORS["text"], font=("Microsoft YaHei", 22, "bold")).pack(anchor="w")
        self.charts.delete("1.0", "end")
        self.charts.insert("end", safe_json_dumps(data))


class AgentTk(Page):
    def __init__(self, master: tk.Misc, client: ApiClient):
        super().__init__(master, client, "AI 工作台", "自动路由、多 Agent 对话与工具调用")
        body = tk.PanedWindow(self, orient="horizontal", bg=COLORS["bg"])
        body.pack(fill="both", expand=True)
        left = tk.Frame(body, bg=COLORS["card"], padx=10, pady=10)
        right = tk.Frame(body, bg=COLORS["bg"])
        body.add(left, width=260)
        body.add(right)
        ttk.Button(left, text="新建会话", command=self.new_session).pack(fill="x")
        self.sessions = tk.Listbox(left)
        self.sessions.pack(fill="both", expand=True, pady=8)
        self.sessions.bind("<<ListboxSelect>>", lambda event: self.load_history())
        self.chat = tk.Text(right, wrap="word")
        self.chat.pack(fill="both", expand=True)
        row = tk.Frame(right, bg=COLORS["bg"])
        row.pack(fill="x", pady=(8, 0))
        self.input = ttk.Entry(row)
        self.input.pack(side="left", fill="x", expand=True)
        ttk.Button(row, text="发送", command=self.send).pack(side="left", padx=8)
        self.current_session = ""
        self.session_rows: list[dict[str, Any]] = []

    def load(self) -> None:
        run_async(self, self.client.sessions, self.render_sessions)

    def render_sessions(self, rows: list[dict[str, Any]]) -> None:
        self.session_rows = rows
        self.sessions.delete(0, "end")
        for row in rows:
            self.sessions.insert("end", row.get("title") or row.get("id"))
        if rows and not self.current_session:
            self.sessions.selection_set(0)
            self.current_session = rows[0].get("id", "")
            self.load_history()

    def new_session(self) -> None:
        run_async(self, self.client.new_session, lambda sid: (setattr(self, "current_session", sid), self.load()))

    def load_history(self) -> None:
        selection = self.sessions.curselection()
        if selection:
            self.current_session = self.session_rows[selection[0]].get("id", "")
        if self.current_session:
            run_async(self, lambda: self.client.history("auto", self.current_session), self.render_history)

    def render_history(self, rows: list[dict[str, Any]]) -> None:
        self.chat.delete("1.0", "end")
        for row in rows:
            name = "你" if row.get("role") == "user" else "FlowMind"
            self.chat.insert("end", f"{name}: {row.get('content', '')}\n\n")

    def send(self) -> None:
        text = self.input.get().strip()
        if not text:
            return
        self.input.delete(0, "end")
        self.chat.insert("end", f"你: {text}\n")
        payload = {"agentType": "auto", "message": text, "sessionId": self.current_session, "context": {}}

        def on_result(result: dict[str, Any]) -> None:
            self.current_session = result.get("sessionId") or self.current_session
            self.chat.insert("end", f"FlowMind: {result.get('reply', '')}\n\n")
            self.load()

        run_async(self, lambda: self.client.chat(payload), on_result)


class KnowledgeTk(Page):
    def __init__(self, master: tk.Misc, client: ApiClient):
        super().__init__(master, client, "知识库", "文档、标签、同步与向量检索")
        tools = tk.Frame(self, bg=COLORS["bg"])
        tools.pack(fill="x")
        self.keyword = ttk.Entry(tools)
        self.keyword.pack(side="left", fill="x", expand=True)
        ttk.Button(tools, text="搜索", command=self.load).pack(side="left", padx=8)
        ttk.Button(tools, text="向量检索", command=self.vector_search).pack(side="left")
        wrap = tk.Frame(self)
        wrap.pack(fill="both", expand=True, pady=10)
        self.table = make_table(wrap, ["id", "title", "tags", "updatedAt"])
        self.detail = tk.Text(self, height=8, wrap="word")
        self.detail.pack(fill="x")

    def load(self) -> None:
        run_async(self, lambda: self.client.knowledge_docs(self.keyword.get().strip()), self.render)

    def render(self, rows: list[dict[str, Any]]) -> None:
        set_rows(self.table, rows, ["id", "title", "tags", "updatedAt"])
        self.detail.delete("1.0", "end")
        self.detail.insert("end", safe_json_dumps(rows[:1]))

    def vector_search(self) -> None:
        run_async(self, lambda: self.client.vector_search(self.keyword.get().strip() or "课程论文"), lambda rows: self.detail.replace("1.0", "end", safe_json_dumps(rows)))


class ContentTk(Page):
    def __init__(self, master: tk.Misc, client: ApiClient):
        super().__init__(master, client, "内容运营", "主题库、文案库、内容日历和 SOP 生成")
        self.tabs = ttk.Notebook(self)
        self.tabs.pack(fill="both", expand=True)
        self.theme_tab = tk.Frame(self.tabs)
        self.draft_tab = tk.Frame(self.tabs)
        self.calendar_tab = tk.Frame(self.tabs)
        self.sop_tab = tk.Frame(self.tabs)
        self.tabs.add(self.theme_tab, text="主题库")
        self.tabs.add(self.draft_tab, text="文案库")
        self.tabs.add(self.calendar_tab, text="内容日历")
        self.tabs.add(self.sop_tab, text="内容 SOP")
        self.theme_table = make_table(self.theme_tab, ["id", "title", "topic", "platform", "status", "heat", "rating"])
        self.draft_table = make_table(self.draft_tab, ["id", "themeId", "title", "channel", "version", "usageStatus", "rating"])
        self.calendar_table = make_table(self.calendar_tab, ["id", "date", "channel", "title", "status", "usageStatus"])
        sop_tools = tk.Frame(self.sop_tab)
        sop_tools.pack(fill="x")
        self.sop_input = ttk.Entry(sop_tools)
        self.sop_input.insert(0, "保研简历")
        self.sop_input.pack(side="left", fill="x", expand=True)
        ttk.Button(sop_tools, text="生成小红书 SOP", command=self.generate_sop).pack(side="left", padx=8)
        self.sop_output = tk.Text(self.sop_tab, wrap="word")
        self.sop_output.pack(fill="both", expand=True)

    def load(self) -> None:
        run_async(self, self.client.content_themes, lambda rows: set_rows(self.theme_table, rows, ["id", "title", "topic", "platform", "status", "heat", "rating"]))
        run_async(self, self.client.drafts, lambda rows: set_rows(self.draft_table, rows, ["id", "themeId", "title", "channel", "version", "usageStatus", "rating"]))
        run_async(self, self.client.calendar, lambda rows: set_rows(self.calendar_table, rows, ["id", "date", "channel", "title", "status", "usageStatus"]))

    def generate_sop(self) -> None:
        payload = {"agentType": "content", "topic": self.sop_input.get(), "audience": "保研er", "style": "干货", "extra": {}}
        run_async(self, lambda: self.client.content_generate("xiaohongshu", payload), lambda result: self.sop_output.replace("1.0", "end", safe_json_dumps(result)))


class StudentsTk(Page):
    def __init__(self, master: tk.Misc, client: ApiClient):
        super().__init__(master, client, "学员管理", "学员画像、风险标签、申请阶段和进度")
        tools = tk.Frame(self, bg=COLORS["bg"])
        tools.pack(fill="x")
        ttk.Button(tools, text="AI 分析选中学员", command=self.analyze).pack(side="left")
        wrap = tk.Frame(self)
        wrap.pack(fill="both", expand=True, pady=10)
        self.table = make_table(wrap, ["id", "name", "school", "major", "gpa", "rank", "english", "stage", "risk", "progress"])
        self.output = tk.Text(self, height=8)
        self.output.pack(fill="x")

    def load(self) -> None:
        run_async(self, self.client.students, lambda rows: set_rows(self.table, rows, ["id", "name", "school", "major", "gpa", "rank", "english", "stage", "risk", "progress"]))

    def analyze(self) -> None:
        row = selected_row(self.table)
        if not row:
            return
        run_async(self, lambda: self.client.analyze_student(int(row["id"])), lambda result: self.output.replace("1.0", "end", safe_json_dumps(result)))


class SchoolsTk(Page):
    def __init__(self, master: tk.Misc, client: ApiClient):
        super().__init__(master, client, "院校情报", "院校项目库与推荐")
        self.tabs = ttk.Notebook(self)
        self.tabs.pack(fill="both", expand=True)
        school_tab = tk.Frame(self.tabs)
        project_tab = tk.Frame(self.tabs)
        rec_tab = tk.Frame(self.tabs)
        self.tabs.add(school_tab, text="学校")
        self.tabs.add(project_tab, text="项目")
        self.tabs.add(rec_tab, text="推荐")
        self.school_table = make_table(school_tab, ["id", "name", "region", "level", "disciplineTags"])
        self.project_table = make_table(project_tab, ["id", "schoolName", "projectName", "deadline", "requirements", "materials", "matchScore"])
        ttk.Button(rec_tab, text="生成推荐", command=self.recommend).pack(anchor="w")
        self.rec_output = tk.Text(rec_tab, wrap="word")
        self.rec_output.pack(fill="both", expand=True)

    def load(self) -> None:
        run_async(self, self.client.schools, lambda rows: set_rows(self.school_table, rows, ["id", "name", "region", "level", "disciplineTags"]))
        run_async(self, self.client.projects, lambda rows: set_rows(self.project_table, rows, ["id", "schoolName", "projectName", "deadline", "requirements", "materials", "matchScore"]))

    def recommend(self) -> None:
        payload = {"gpa": "3.80", "rank": "5/120", "english": "六级 580", "targetMajor": "食品科学", "riskPreference": "稳妥"}
        run_async(self, lambda: self.client.recommend_schools(payload), lambda result: self.rec_output.replace("1.0", "end", safe_json_dumps(result)))


class AnalyticsTk(DashboardTk):
    def __init__(self, master: tk.Misc, client: ApiClient):
        Page.__init__(self, master, client, "数据分析", "院校分布、GPA、申请漏斗和截止趋势")
        self.stats = tk.Frame(self, bg=COLORS["bg"])
        self.stats.pack(fill="x")
        self.charts = tk.Text(self, height=24, wrap="word")
        self.charts.pack(fill="both", expand=True, pady=16)


class FeishuTk(Page):
    def __init__(self, master: tk.Misc, client: ApiClient):
        super().__init__(master, client, "飞书同步", "飞书状态、文档、同步与日志")
        actions = tk.Frame(self, bg=COLORS["bg"])
        actions.pack(fill="x")
        for label, action in [("同步文档", "sync_docs"), ("同步多维表格", "sync_bitable"), ("同步任务", "sync_tasks"), ("lark-cli 状态", "lark_status")]:
            ttk.Button(actions, text=label, command=lambda a=action: self.action(a)).pack(side="left", padx=(0, 8))
        self.status = tk.Text(self, height=8, wrap="word")
        self.status.pack(fill="x", pady=10)
        wrap = tk.Frame(self)
        wrap.pack(fill="both", expand=True)
        self.logs = make_table(wrap, ["id", "syncType", "targetName", "status", "message", "createdAt"])

    def load(self) -> None:
        run_async(self, self.client.feishu_status, lambda result: self.status.replace("1.0", "end", safe_json_dumps(result)))
        run_async(self, self.client.feishu_logs, lambda rows: set_rows(self.logs, rows, ["id", "syncType", "targetName", "status", "message", "createdAt"]))

    def action(self, action: str) -> None:
        run_async(self, lambda: self.client.feishu_action(action), lambda result: self.status.replace("1.0", "end", safe_json_dumps(result)))


class SettingsTk(Page):
    def __init__(self, master: tk.Misc, client: ApiClient):
        super().__init__(master, client, "系统设置", "后端连接、Prompt、路由和权限")
        self.tabs = ttk.Notebook(self)
        self.tabs.pack(fill="both", expand=True)
        self.connection = tk.Frame(self.tabs)
        self.routes = tk.Frame(self.tabs)
        self.permissions = tk.Frame(self.tabs)
        self.tabs.add(self.connection, text="连接")
        self.tabs.add(self.routes, text="路由")
        self.tabs.add(self.permissions, text="权限")
        self.base_url = ttk.Entry(self.connection)
        self.base_url.insert(0, self.client.base_url)
        self.base_url.pack(fill="x", pady=8)
        self.token = ttk.Entry(self.connection)
        self.token.insert(0, self.client.token)
        self.token.pack(fill="x", pady=8)
        ttk.Button(self.connection, text="应用", command=self.save).pack(anchor="w")
        self.routes_table = make_table(self.routes, ["service", "path"])
        self.permissions_output = tk.Text(self.permissions)
        self.permissions_output.pack(fill="both", expand=True)
        ttk.Button(self.permissions, text="加载角色与权限", command=self.load_permissions).pack(anchor="w")

    def load(self) -> None:
        run_async(self, self.client.routes, lambda rows: set_rows(self.routes_table, rows, ["service", "path"]))

    def save(self) -> None:
        self.client.set_base_url(self.base_url.get().strip())
        self.client.set_token(self.token.get().strip())
        save_config({"base_url": self.client.base_url, "token": self.client.token})
        messagebox.showinfo("FlowMind", "已保存当前连接信息")

    def load_permissions(self) -> None:
        def fetch() -> dict[str, Any]:
            return {"roles": self.client.roles(), "permissions": self.client.permissions()}

        run_async(self, fetch, lambda result: self.permissions_output.replace("1.0", "end", safe_json_dumps(result)))


class FlowMindTkApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("FlowMind Agent Desktop")
        self.geometry("1380x860")
        self.configure(bg=COLORS["bg"])
        config = load_config()
        self.client = ApiClient(config.get("base_url", "http://localhost:8080"), config.get("token", "mock-jwt.demo"))
        self.current: tk.Widget | None = None
        if config.get("token"):
            self.show_shell()
        else:
            self.show_login()

    def reset(self) -> None:
        if self.current is not None:
            self.current.destroy()
            self.current = None

    def show_login(self) -> None:
        self.reset()
        self.current = LoginFrame(self, self.client, self.show_shell)

    def show_shell(self) -> None:
        self.reset()
        self.current = ShellFrame(self, self.client, self.show_login)


def main() -> None:
    app = FlowMindTkApp()
    app.mainloop()


if __name__ == "__main__":
    main()

