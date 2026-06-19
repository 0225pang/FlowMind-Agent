from __future__ import annotations

import json
from typing import Any, Callable

from PySide6.QtCore import Qt, Signal
from PySide6.QtWidgets import (
    QButtonGroup,
    QComboBox,
    QDateEdit,
    QDialog,
    QDialogButtonBox,
    QFrame,
    QGridLayout,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QListWidget,
    QListWidgetItem,
    QMainWindow,
    QMessageBox,
    QPlainTextEdit,
    QPushButton,
    QScrollArea,
    QSizePolicy,
    QSplitter,
    QSpinBox,
    QStackedWidget,
    QTableWidget,
    QTableWidgetItem,
    QTabWidget,
    QTextBrowser,
    QTextEdit,
    QVBoxLayout,
    QWidget,
)

from .api import ApiClient, safe_json_dumps
from .widgets import (
    ApiWorker,
    Badge,
    BarList,
    CalendarGrid,
    Card,
    ChartCard,
    DataTable,
    EmptyState,
    InfoRow,
    JsonPanel,
    MarkdownPanel,
    PageHeader,
    PromptChip,
    SectionHeader,
    StarRating,
    StatCard,
    StreamWorker,
    TextCard,
    TraceListPanel,
    clear_layout,
    danger_button,
    primary_button,
)


def value(row: dict[str, Any], *keys: str, default: str = "") -> Any:
    for key in keys:
        if key in row and row[key] not in (None, ""):
            return row[key]
    return default


def show_error(parent: QWidget, message: str) -> None:
    QMessageBox.warning(parent, "FlowMind", message)


def compact_json(value_: Any) -> str:
    if value_ is None:
        return ""
    if isinstance(value_, str):
        return value_
    return json.dumps(value_, ensure_ascii=False)


class BasePage(QWidget):
    status = Signal(str)

    def __init__(self, client: ApiClient):
        super().__init__()
        self.client = client
        self._workers: list[ApiWorker] = []

    def run_api(
        self,
        fn: Callable[..., Any],
        on_ok: Callable[[Any], None],
        *args: Any,
        on_fail: Callable[[str], None] | None = None,
        **kwargs: Any,
    ) -> None:
        worker = ApiWorker(fn, *args, **kwargs)
        self._workers.append(worker)

        def finish() -> None:
            if worker in self._workers:
                self._workers.remove(worker)

        worker.ok.connect(lambda result: (finish(), on_ok(result)))
        worker.fail.connect(lambda error: (finish(), (on_fail or (lambda msg: show_error(self, msg)))(error)))
        worker.start()


class LoginPage(QWidget):
    logged_in = Signal(dict)

    def __init__(self, client: ApiClient, config: dict[str, str]):
        super().__init__()
        self.client = client
        self.worker: ApiWorker | None = None

        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)
        root.addStretch(1)

        card = Card()
        card.setFixedWidth(420)
        layout = card.layout
        title = QLabel("FlowMind")
        title.setObjectName("PageTitle")
        sub = QLabel("Agent Platform 桌面客户端")
        sub.setObjectName("Muted")
        self.base_url = QLineEdit(config.get("base_url", "http://localhost:8080"))
        self.username = QLineEdit("admin")
        self.password = QLineEdit("123456")
        self.password.setEchoMode(QLineEdit.Password)
        self.login_button = primary_button("登录")
        self.demo_button = QPushButton("使用 Demo Token 进入")
        self.status = QLabel("复用后端 REST/SSE API，不直接访问数据库或本地配置。")
        self.status.setObjectName("Muted")

        layout.addWidget(title)
        layout.addWidget(sub)
        layout.addSpacing(10)
        layout.addWidget(QLabel("Base URL"))
        layout.addWidget(self.base_url)
        layout.addWidget(QLabel("用户名"))
        layout.addWidget(self.username)
        layout.addWidget(QLabel("密码"))
        layout.addWidget(self.password)
        layout.addWidget(self.login_button)
        layout.addWidget(self.demo_button)
        layout.addWidget(self.status)

        row = QHBoxLayout()
        row.addStretch(1)
        row.addWidget(card)
        row.addStretch(1)
        root.addLayout(row)
        root.addStretch(1)

        self.login_button.clicked.connect(self.login)
        self.demo_button.clicked.connect(self.demo)

    def login(self) -> None:
        self.client.set_base_url(self.base_url.text().strip())
        self.login_button.setEnabled(False)
        self.status.setText("正在登录...")
        self.worker = ApiWorker(self.client.login, self.username.text().strip(), self.password.text())
        self.worker.ok.connect(self.on_login_ok)
        self.worker.fail.connect(self.on_login_fail)
        self.worker.start()

    def on_login_ok(self, data: Any) -> None:
        self.login_button.setEnabled(True)
        self.status.setText("登录成功")
        self.logged_in.emit({"base_url": self.base_url.text().strip(), "token": self.client.token, "user": data.get("user", {})})

    def on_login_fail(self, message: str) -> None:
        self.login_button.setEnabled(True)
        self.status.setText(f"登录失败：{message}")

    def demo(self) -> None:
        self.client.set_base_url(self.base_url.text().strip())
        self.client.set_token("mock-jwt.demo")
        self.logged_in.emit({"base_url": self.base_url.text().strip(), "token": "mock-jwt.demo", "user": {"nickname": "FM"}})


class MainWindow(QMainWindow):
    def __init__(self, client: ApiClient, config: dict[str, str], save_config: Callable[[dict[str, str]], None]):
        super().__init__()
        self.client = client
        self.config = config
        self.save_config = save_config
        self.setWindowTitle("FlowMind Agent Desktop")
        if config.get("token"):
            self.show_shell()
        else:
            self.show_login()

    def show_login(self) -> None:
        login = LoginPage(self.client, self.config)
        login.logged_in.connect(self.on_logged_in)
        self.setCentralWidget(login)

    def on_logged_in(self, data: dict[str, Any]) -> None:
        self.config.update({"base_url": data.get("base_url", self.client.base_url), "token": data.get("token", self.client.token)})
        self.save_config(self.config)
        self.show_shell()

    def show_shell(self) -> None:
        shell = ShellWidget(self.client, self.logout)
        self.setCentralWidget(shell)

    def logout(self) -> None:
        self.config.pop("token", None)
        self.save_config(self.config)
        self.client.set_token("mock-jwt.demo")
        self.show_login()


class ShellWidget(QWidget):
    NAV = [
        ("Dashboard", "Dashboard", "保研内容运营工作空间"),
        ("AI 工作台", "AI 工作台", "自动路由、多 Agent 对话与工具调用"),
        ("知识库", "知识库", "文档、标签、同步与向量检索"),
        ("内容运营", "内容运营", "主题库、文案库、日历与 SOP 生成"),
        ("学员管理", "学员管理", "学员画像、申请进度与 AI 分析"),
        ("院校情报", "院校情报", "院校项目库与推荐"),
        ("数据分析", "数据分析", "运营与申请趋势图表"),
        ("飞书同步", "飞书同步", "飞书状态、文档、同步与日志"),
        ("系统设置", "系统设置", "后端连接、接口和权限管理"),
    ]

    def __init__(self, client: ApiClient, logout: Callable[[], None]):
        super().__init__()
        self.client = client
        self.logout = logout
        self.setObjectName("Root")
        root = QHBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)
        root.setSpacing(0)

        sidebar = QFrame()
        sidebar.setObjectName("Sidebar")
        sidebar.setFixedWidth(248)
        sidebar_layout = QVBoxLayout(sidebar)
        sidebar_layout.setContentsMargins(12, 18, 12, 18)
        sidebar_layout.setSpacing(6)

        logo = QHBoxLayout()
        logo_mark = QLabel("F")
        logo_mark.setObjectName("LogoMark")
        logo_mark.setFixedSize(40, 40)
        logo_mark.setAlignment(Qt.AlignCenter)
        logo_text = QVBoxLayout()
        logo_title = QLabel("FlowMind")
        logo_title.setStyleSheet("font-weight: 800;")
        logo_sub = QLabel("Agent Platform")
        logo_sub.setObjectName("Muted")
        logo_text.addWidget(logo_title)
        logo_text.addWidget(logo_sub)
        logo.addWidget(logo_mark)
        logo.addLayout(logo_text)
        sidebar_layout.addLayout(logo)
        sidebar_layout.addSpacing(12)

        self.nav_group = QButtonGroup(self)
        self.nav_group.setExclusive(True)
        for index, (label, _, _) in enumerate(self.NAV):
            button = QPushButton(label)
            button.setObjectName("NavButton")
            button.setCheckable(True)
            button.clicked.connect(lambda checked=False, i=index: self.switch_page(i))
            self.nav_group.addButton(button, index)
            sidebar_layout.addWidget(button)
        sidebar_layout.addStretch(1)

        main = QVBoxLayout()
        main.setContentsMargins(0, 0, 0, 0)
        main.setSpacing(0)

        topbar = QFrame()
        topbar.setObjectName("Topbar")
        topbar.setFixedHeight(72)
        topbar_layout = QHBoxLayout(topbar)
        topbar_layout.setContentsMargins(24, 0, 24, 0)
        title_col = QVBoxLayout()
        self.title = QLabel()
        self.title.setObjectName("PageTitle")
        self.subtitle = QLabel()
        self.subtitle.setObjectName("Muted")
        title_col.addWidget(self.title)
        title_col.addWidget(self.subtitle)
        topbar_layout.addLayout(title_col, 1)
        self.api_tag = QLabel("API Ready")
        self.api_tag.setObjectName("Tag")
        logout_button = QPushButton("退出")
        logout_button.clicked.connect(self.logout)
        topbar_layout.addWidget(self.api_tag)
        topbar_layout.addWidget(logout_button)

        self.stack = QStackedWidget()
        self.stack.setObjectName("ContentArea")
        self.pages: list[QWidget] = [
            DashboardPage(client),
            AgentPage(client),
            KnowledgePage(client),
            ContentPage(client),
            StudentsPage(client),
            SchoolsPage(client),
            AnalyticsPage(client),
            FeishuPage(client),
            SettingsPage(client),
        ]
        for page in self.pages:
            self.stack.addWidget(page)

        main.addWidget(topbar)
        main.addWidget(self.stack, 1)
        root.addWidget(sidebar)
        root.addLayout(main, 1)
        self.switch_page(0)

    def switch_page(self, index: int) -> None:
        self.stack.setCurrentIndex(index)
        label, title, subtitle = self.NAV[index]
        self.title.setText(title)
        self.subtitle.setText(subtitle)
        button = self.nav_group.button(index)
        if button:
            button.setChecked(True)
        page = self.pages[index]
        if hasattr(page, "load"):
            page.load()


class DashboardPage(BasePage):
    def __init__(self, client: ApiClient):
        super().__init__(client)
        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        refresh = primary_button("刷新")
        refresh.clicked.connect(self.load)
        root.addWidget(PageHeader("Dashboard", "保研运营核心指标、内容选题和申请趋势", [refresh]))

        grid = QGridLayout()
        self.stats = [
            StatCard("学员数", "-"),
            StatCard("活跃项目", "-"),
            StatCard("内容选题", "-"),
            StatCard("任务完成率", "-"),
        ]
        for i, card in enumerate(self.stats):
            grid.addWidget(card, 0, i)
        root.addLayout(grid)

        charts = QGridLayout()
        self.distribution = ChartCard("学员分布", "#5b6cff")
        self.content_stats = ChartCard("内容统计", "#8b5cf6")
        self.funnel = ChartCard("申请阶段漏斗", "#19b37b")
        self.deadlines = ChartCard("院校截止趋势", "#f59e0b")
        charts.addWidget(self.distribution, 0, 0)
        charts.addWidget(self.content_stats, 0, 1)
        charts.addWidget(self.funnel, 1, 0)
        charts.addWidget(self.deadlines, 1, 1)
        root.addLayout(charts, 1)

    def load(self) -> None:
        def fetch_all() -> dict[str, Any]:
            return {
                "overview": self.client.analytics_overview(),
                "distribution": self.client.analytics_distribution(),
                "content": self.client.analytics_content_stats(),
                "funnel": self.client.analytics_funnel(),
                "deadlines": self.client.analytics_deadlines(),
            }

        self.run_api(fetch_all, self.render)

    def render(self, data: dict[str, Any]) -> None:
        overview = data.get("overview") or {}
        self.stats[0].set_value(value(overview, "students", default="-"), "当前管理学员")
        self.stats[1].set_value(value(overview, "activeProjects", default="-"), "院校项目")
        self.stats[2].set_value(value(overview, "contentTopics", default="-"), "内容主题")
        self.stats[3].set_value(value(overview, "taskRate", default="-"), "任务进度")
        self._set_series(self.distribution, data.get("distribution"))
        self._set_series(self.content_stats, data.get("content"))
        funnel = data.get("funnel") or []
        self.funnel.set_data([item.get("name") for item in funnel], [item.get("value") for item in funnel])
        self._set_series(self.deadlines, data.get("deadlines"))

    @staticmethod
    def _set_series(widget: BarList, data: Any) -> None:
        data = data or {}
        widget.set_data(data.get("labels") or [], data.get("values") or [])


class AgentPage(BasePage):
    PROMPTS = [
        "根据保研知识库，总结期末如何速成课程论文",
        "帮我生成 10 个保研小红书选题，并给出爆款结构",
        "联网搜索今天最新的保研通知，并说明来源可信度",
        "在保研知识库中创建一篇飞书文档",
        "分析学员01的申请风险并给出下一步动作",
    ]

    AGENT_CAPABILITIES = [
        ("auto", "FlowMindAgent", "统一入口，自动选择专业 Agent", "primary"),
        ("content", "ContentAgent", "小红书、朋友圈、SOP 和文案资产", "purple"),
        ("knowledge", "KnowledgeAgent", "知识库检索、摘要和 RAG 回答", "success"),
        ("student", "StudentAgent", "学员风险分析和下一步动作", "warning"),
        ("school", "SchoolAgent", "院校项目匹配和推荐", "info"),
        ("feishu", "FeishuAgent", "飞书文档、同步和机器人推送", "primary"),
    ]

    def __init__(self, client: ApiClient):
        super().__init__(client)
        self.current_session_id = ""
        self.stream_worker: StreamWorker | None = None
        self.assistant_markdown = ""
        self.assistant_bubble: QTextBrowser | None = None

        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        root.addWidget(PageHeader("AI 工作台", "默认使用 auto，由 AgentRouter 自动选择专业智能体"))

        splitter = QSplitter()
        left = Card("会话")
        left.setMinimumWidth(210)
        row = QHBoxLayout()
        self.new_button = primary_button("新建")
        self.refresh_button = QPushButton("刷新")
        self.delete_button = danger_button("删除")
        row.addWidget(self.new_button)
        row.addWidget(self.refresh_button)
        row.addWidget(self.delete_button)
        left.layout.addLayout(row)
        self.session_list = QListWidget()
        left.layout.addWidget(self.session_list, 1)
        left.layout.addWidget(SectionHeader("Agent 能力", "自动路由会优先匹配下面的专业能力"))
        for agent_type, name, desc, kind in self.AGENT_CAPABILITIES:
            cap = QFrame()
            cap.setObjectName("Card")
            cap_layout = QVBoxLayout(cap)
            cap_layout.setContentsMargins(10, 8, 10, 8)
            cap_layout.setSpacing(4)
            top = QHBoxLayout()
            top.addWidget(Badge(agent_type, kind))
            top.addStretch(1)
            cap_layout.addLayout(top)
            cap_layout.addWidget(QLabel(name))
            muted = QLabel(desc)
            muted.setObjectName("Muted")
            muted.setWordWrap(True)
            cap_layout.addWidget(muted)
            left.layout.addWidget(cap)

        right = QSplitter(Qt.Vertical)
        chat_card = Card()
        chat_card.layout.addWidget(SectionHeader("对话", "支持 SSE 流式输出、模型 Thinking 和工具调用过程"))
        self.chat_area = QScrollArea()
        self.chat_area.setWidgetResizable(True)
        self.chat_inner = QWidget()
        self.chat_layout = QVBoxLayout(self.chat_inner)
        self.chat_layout.setAlignment(Qt.AlignTop)
        self.chat_area.setWidget(self.chat_inner)
        chat_card.layout.addWidget(self.chat_area, 1)

        self.prompts_widget = QWidget()
        prompts_layout = QHBoxLayout(self.prompts_widget)
        prompts_layout.setContentsMargins(0, 0, 0, 0)
        prompts_layout.setSpacing(8)
        for prompt in self.PROMPTS:
            chip = PromptChip(prompt)
            chip.clicked.connect(lambda checked=False, text=prompt: self.send_prompt(text))
            prompts_layout.addWidget(chip)
        prompts_layout.addStretch(1)
        chat_card.layout.addWidget(self.prompts_widget)

        input_row = QHBoxLayout()
        self.input = QTextEdit()
        self.input.setPlaceholderText("输入你的问题，例如：帮我生成 10 个保研小红书选题")
        self.input.setFixedHeight(92)
        self.send_button = primary_button("发送")
        input_row.addWidget(self.input, 1)
        input_row.addWidget(self.send_button)
        chat_card.layout.addLayout(input_row)

        trace_card = Card("Thinking / 工具调用")
        self.trace_tabs = QTabWidget()
        self.thinking_output = QPlainTextEdit()
        self.thinking_output.setReadOnly(True)
        self.trace_panel = TraceListPanel()
        self.trace_output = QPlainTextEdit()
        self.trace_output.setReadOnly(True)
        self.reasoning_output = QPlainTextEdit()
        self.reasoning_output.setReadOnly(True)
        self.trace_tabs.addTab(self.thinking_output, "处理过程")
        self.trace_tabs.addTab(self.trace_panel, "工具调用")
        self.trace_tabs.addTab(self.reasoning_output, "模型 Thinking")
        self.trace_tabs.addTab(self.trace_output, "原始 trace")
        trace_card.layout.addWidget(self.trace_tabs)
        right.addWidget(chat_card)
        right.addWidget(trace_card)
        right.setSizes([640, 170])

        splitter.addWidget(left)
        splitter.addWidget(right)
        splitter.setSizes([230, 950])
        root.addWidget(splitter, 1)

        self.new_button.clicked.connect(self.create_session)
        self.refresh_button.clicked.connect(self.load)
        self.delete_button.clicked.connect(self.delete_session)
        self.send_button.clicked.connect(self.send)
        self.session_list.currentItemChanged.connect(self.switch_session)

    def load(self) -> None:
        self.run_api(self.client.sessions, self.render_sessions, on_fail=lambda _: self.render_sessions([]))

    def render_sessions(self, sessions: list[dict[str, Any]]) -> None:
        self.session_list.clear()
        for session in sessions:
            item = QListWidgetItem(session.get("title") or "新会话")
            item.setData(Qt.UserRole, session)
            item.setToolTip(session.get("updatedAt") or session.get("createdAt") or "")
            self.session_list.addItem(item)
        if sessions and not self.current_session_id:
            self.session_list.setCurrentRow(0)
        elif not sessions:
            self.create_session()

    def create_session(self) -> None:
        self.run_api(self.client.new_session, self.on_new_session)

    def on_new_session(self, session_id: str) -> None:
        self.current_session_id = session_id
        self.clear_chat()
        self.load()

    def delete_session(self) -> None:
        if not self.current_session_id:
            return
        sid = self.current_session_id
        self.run_api(lambda: self.client.delete_session(sid), lambda _: (self.clear_chat(), setattr(self, "current_session_id", ""), self.load()))

    def switch_session(self, current: QListWidgetItem | None, previous: QListWidgetItem | None) -> None:
        if not current:
            return
        session = current.data(Qt.UserRole) or {}
        sid = session.get("id")
        if not sid or sid == self.current_session_id:
            return
        self.current_session_id = str(sid)
        self.load_history()

    def load_history(self) -> None:
        sid = self.current_session_id
        self.run_api(lambda: self.client.history("auto", sid), self.render_history, on_fail=lambda _: self.render_history([]))

    def render_history(self, rows: list[dict[str, Any]]) -> None:
        self.clear_chat()
        if not rows:
            self.add_message("assistant", "新会话已创建。直接输入你的任务，我会自动选择合适的 Agent，并展示工具调用过程。")
            return
        for row in rows:
            self.add_message(row.get("role", "assistant"), row.get("content", ""))
            meta = row.get("metadata")
            if meta:
                parsed = self.parse_metadata(meta)
                self.render_metadata(parsed)

    def clear_chat(self) -> None:
        while self.chat_layout.count():
            item = self.chat_layout.takeAt(0)
            widget = item.widget()
            if widget:
                widget.deleteLater()
        self.chat_layout.addStretch(1)
        self.thinking_output.clear()
        self.trace_output.clear()
        self.trace_panel.set_items([])
        self.reasoning_output.clear()

    def parse_metadata(self, meta: Any) -> dict[str, Any]:
        if isinstance(meta, dict):
            return meta
        if isinstance(meta, str):
            try:
                parsed = json.loads(meta)
                return parsed if isinstance(parsed, dict) else {}
            except json.JSONDecodeError:
                return {"raw": meta}
        return {}

    def render_metadata(self, meta: dict[str, Any]) -> None:
        thinking_history = meta.get("thinkingHistory") or []
        if thinking_history:
            self.thinking_output.setPlainText("\n".join(map(str, thinking_history)))
        elif meta.get("thinking"):
            self.thinking_output.setPlainText(str(meta["thinking"]))
        if meta.get("traceItems"):
            self.render_trace_items(meta["traceItems"])
        if meta.get("modelThinking"):
            self.reasoning_output.setPlainText(str(meta["modelThinking"]))

    def render_trace_items(self, items: list[dict[str, Any]]) -> None:
        self.trace_panel.set_items(items)
        self.trace_output.setPlainText(safe_json_dumps(items))

    def add_message(self, role: str, content: str) -> QTextBrowser:
        if self.chat_layout.count() and self.chat_layout.itemAt(self.chat_layout.count() - 1).spacerItem():
            self.chat_layout.takeAt(self.chat_layout.count() - 1)
        frame = QFrame()
        frame.setObjectName("Card")
        frame.setStyleSheet("QFrame#Card { background: %s; }" % ("#eef2ff" if role == "user" else "#ffffff"))
        layout = QVBoxLayout(frame)
        layout.setContentsMargins(14, 10, 14, 10)
        name = QLabel("你" if role == "user" else "FlowMind Agent")
        name.setObjectName("Muted")
        body = QTextBrowser()
        body.setOpenExternalLinks(True)
        body.setMarkdown(content or "")
        body.setMinimumHeight(54)
        body.setSizePolicy(QSizePolicy.Expanding, QSizePolicy.Minimum)
        layout.addWidget(name)
        layout.addWidget(body)
        self.chat_layout.addWidget(frame)
        self.chat_layout.addStretch(1)
        self.chat_area.verticalScrollBar().setValue(self.chat_area.verticalScrollBar().maximum())
        return body

    def send(self) -> None:
        text = self.input.toPlainText().strip()
        self.send_prompt(text)

    def send_prompt(self, text: str) -> None:
        text = text.strip()
        if not text:
            return
        self.input.clear()
        self.add_message("user", text)
        self.assistant_markdown = ""
        self.assistant_bubble = self.add_message("assistant", "")
        payload = {"agentType": "auto", "message": text, "sessionId": self.current_session_id, "context": {}}
        self.send_button.setEnabled(False)
        self.stream_worker = StreamWorker(lambda: self.client.stream_chat(payload))
        self.stream_worker.session.connect(self.on_stream_session)
        self.stream_worker.thinking.connect(lambda text_: self.thinking_output.appendPlainText(text_))
        self.stream_worker.trace.connect(self.render_trace_items)
        self.stream_worker.reasoning.connect(lambda text_: self.reasoning_output.appendPlainText(text_))
        self.stream_worker.delta.connect(self.on_delta)
        self.stream_worker.done.connect(self.on_stream_done)
        self.stream_worker.fail.connect(self.on_stream_fail)
        self.stream_worker.start()

    def on_stream_session(self, session_id: str) -> None:
        if session_id:
            self.current_session_id = session_id

    def on_delta(self, text: str) -> None:
        self.assistant_markdown += text
        if self.assistant_bubble:
            self.assistant_bubble.setMarkdown(self.assistant_markdown)
        self.chat_area.verticalScrollBar().setValue(self.chat_area.verticalScrollBar().maximum())

    def on_stream_done(self, session_id: str) -> None:
        if session_id:
            self.current_session_id = session_id
        self.send_button.setEnabled(True)
        self.thinking_output.appendPlainText("回答完成，可展开查看本次处理过程。")
        self.load()

    def on_stream_fail(self, message: str) -> None:
        self.send_button.setEnabled(True)
        self.trace_output.appendPlainText(f"[error] {message}")


class KnowledgePage(BasePage):
    def __init__(self, client: ApiClient):
        super().__init__(client)
        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        refresh = primary_button("刷新")
        refresh.clicked.connect(self.load)
        sync = QPushButton("飞书同步")
        sync.clicked.connect(self.sync)
        root.addWidget(PageHeader("知识库", "资料分类、标签、摘要和语义检索", [sync, refresh]))

        top = QHBoxLayout()
        self.keyword = QLineEdit()
        self.keyword.setPlaceholderText("搜索知识文档")
        self.search_button = primary_button("搜索")
        self.vector_query = QLineEdit()
        self.vector_query.setPlaceholderText("语义检索，例如：课程论文怎么写")
        self.vector_button = QPushButton("向量检索")
        top.addWidget(self.keyword, 1)
        top.addWidget(self.search_button)
        top.addWidget(self.vector_query, 1)
        top.addWidget(self.vector_button)
        root.addLayout(top)

        splitter = QSplitter()
        self.table = DataTable(["ID", "标题", "标签", "来源", "更新时间"])
        detail_tabs = QTabWidget()
        detail_card = Card("文档详情")
        self.doc_title = QLabel("请选择左侧文档")
        self.doc_title.setObjectName("SectionTitle")
        self.doc_title.setWordWrap(True)
        self.doc_tags = InfoRow("标签", "-")
        self.doc_source = InfoRow("来源", "-")
        self.doc_url = InfoRow("飞书", "-")
        self.doc_summary = MarkdownPanel()
        self.doc_summary.setMinimumHeight(180)
        self.detail = JsonPanel()
        detail_card.layout.addWidget(self.doc_title)
        detail_card.layout.addWidget(self.doc_tags)
        detail_card.layout.addWidget(self.doc_source)
        detail_card.layout.addWidget(self.doc_url)
        detail_card.layout.addWidget(QLabel("摘要 / 正文片段"))
        detail_card.layout.addWidget(self.doc_summary, 1)
        detail_tabs.addTab(detail_card, "文档")
        detail_tabs.addTab(self.detail, "原始数据")
        self.vector_output = JsonPanel()
        detail_tabs.addTab(self.vector_output, "向量检索")
        splitter.addWidget(self.table)
        splitter.addWidget(detail_tabs)
        splitter.setSizes([760, 420])
        root.addWidget(splitter, 1)

        self.search_button.clicked.connect(self.load)
        self.vector_button.clicked.connect(self.vector_search)
        self.table.itemSelectionChanged.connect(self.show_selected)

    def load(self) -> None:
        keyword = self.keyword.text().strip()
        self.run_api(lambda: self.client.knowledge_docs(keyword), self.render_docs)

    def render_docs(self, rows: list[dict[str, Any]]) -> None:
        self.docs = rows
        normalized = []
        for row in rows:
            normalized.append(
                {
                    "id": value(row, "id"),
                    "title": value(row, "title"),
                    "tags": value(row, "tags", default=[]),
                    "source": value(row, "source", "feishuType", default=""),
                    "updatedAt": value(row, "updatedAt", "createdAt", default=""),
                    "_raw": row,
                }
            )
        self.table.set_rows(normalized, ["id", "title", "tags", "source", "updatedAt"])

    def show_selected(self) -> None:
        row = self.table.selected_row_data()
        if row:
            raw = row.get("_raw") or row
            self.doc_title.setText(str(value(raw, "title", default="未命名文档")))
            tags = value(raw, "tags", default=[])
            self.doc_tags.set_value("、".join(tags) if isinstance(tags, list) else tags)
            self.doc_source.set_value(value(raw, "source", "feishuType", default="-"))
            self.doc_url.set_value(value(raw, "feishuUrl", default="-"))
            self.doc_summary.set_text(str(value(raw, "summary", "content", default="")))
            self.detail.set_json(raw)

    def vector_search(self) -> None:
        query = self.vector_query.text().strip()
        if not query:
            return
        self.run_api(lambda: self.client.vector_search(query, 5), lambda rows: self.vector_output.set_json(rows))

    def sync(self) -> None:
        self.run_api(self.client.sync_knowledge, lambda result: (self.vector_output.set_json(result), self.load()))


class ContentPage(BasePage):
    def __init__(self, client: ApiClient):
        super().__init__(client)
        self.current_theme_id: int | None = None
        self.current_draft_id: int | None = None
        self.themes: list[dict[str, Any]] = []
        self.drafts: list[dict[str, Any]] = []
        self.calendar_rows: list[dict[str, Any]] = []

        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        refresh = primary_button("刷新")
        refresh.clicked.connect(self.load)
        root.addWidget(PageHeader("内容运营", "主题库、文案库、内容日历和 SOP 生成", [refresh]))

        stat_grid = QGridLayout()
        self.theme_count_card = StatCard("主题总数", "0", "3x2 分页展示")
        self.copy_count_card = StatCard("文案总数", "0", "支持图片和建议")
        self.rated_count_card = StatCard("已评分", "0", "星级越高越重要")
        self.calendar_count_card = StatCard("日历排期", "0", "点击日期展开")
        for index, card in enumerate([self.theme_count_card, self.copy_count_card, self.rated_count_card, self.calendar_count_card]):
            stat_grid.addWidget(card, 0, index)
        root.addLayout(stat_grid)

        tabs = QTabWidget()
        tabs.addTab(self.build_themes_tab(), "主题库")
        tabs.addTab(self.build_drafts_tab(), "文案库")
        tabs.addTab(self.build_calendar_tab(), "内容日历")
        tabs.addTab(self.build_sop_tab(), "内容 SOP")
        root.addWidget(tabs, 1)

    def build_themes_tab(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        tools = QHBoxLayout()
        self.theme_keyword = QLineEdit()
        self.theme_keyword.setPlaceholderText("关键词")
        self.theme_status = QComboBox()
        self.theme_status.addItems(["", "待创作", "已生成", "待发布", "已发布"])
        self.theme_channel = QComboBox()
        self.theme_channel.addItems(["", "小红书", "朋友圈", "公众号"])
        search = primary_button("查询")
        add = QPushButton("新增主题")
        delete = danger_button("删除主题")
        for w in [self.theme_keyword, self.theme_status, self.theme_channel, search, add, delete]:
            tools.addWidget(w)
        layout.addLayout(tools)
        splitter = QSplitter()
        theme_tabs = QTabWidget()
        theme_cards_page = QWidget()
        theme_cards_layout = QVBoxLayout(theme_cards_page)
        theme_cards_layout.setContentsMargins(0, 0, 0, 0)
        theme_cards_layout.addWidget(SectionHeader("主题卡片", "点击卡片查看该主题下的历史文案"))
        theme_card_scroll = QScrollArea()
        theme_card_scroll.setWidgetResizable(True)
        self.theme_card_host = QWidget()
        self.theme_card_grid = QGridLayout(self.theme_card_host)
        self.theme_card_grid.setSpacing(12)
        theme_card_scroll.setWidget(self.theme_card_host)
        theme_cards_layout.addWidget(theme_card_scroll, 1)
        self.theme_table = DataTable(["ID", "标题", "主题", "平台", "状态", "热度", "评分", "计划日期"])
        theme_tabs.addTab(theme_cards_page, "卡片")
        theme_tabs.addTab(self.theme_table, "表格")
        detail = Card("主题详情")
        detail.layout.addWidget(SectionHeader("主题卡片预览", "对应 Web 端主题卡片的核心信息"))
        tag_row = QHBoxLayout()
        self.theme_platform_badge = Badge("-", "primary")
        self.theme_status_badge = Badge("-", "info")
        tag_row.addWidget(self.theme_platform_badge)
        tag_row.addWidget(self.theme_status_badge)
        tag_row.addStretch(1)
        detail.layout.addLayout(tag_row)
        self.theme_title_label = QLabel("请选择左侧主题")
        self.theme_title_label.setObjectName("SectionTitle")
        self.theme_title_label.setWordWrap(True)
        self.theme_summary_label = QLabel("")
        self.theme_summary_label.setObjectName("Muted")
        self.theme_summary_label.setWordWrap(True)
        self.theme_rating = StarRating(0)
        self.theme_rating.changed.connect(self.rate_selected_theme)
        self.theme_topic_row = InfoRow("主题", "-")
        self.theme_heat_row = InfoRow("热度", "-")
        self.theme_date_row = InfoRow("计划", "-")
        self.theme_tags_row = InfoRow("标签", "-")
        self.theme_drafts_preview = QPlainTextEdit()
        self.theme_drafts_preview.setReadOnly(True)
        detail.layout.addWidget(self.theme_title_label)
        detail.layout.addWidget(self.theme_rating)
        detail.layout.addWidget(self.theme_summary_label)
        detail.layout.addWidget(self.theme_topic_row)
        detail.layout.addWidget(self.theme_heat_row)
        detail.layout.addWidget(self.theme_date_row)
        detail.layout.addWidget(self.theme_tags_row)
        detail.layout.addWidget(QLabel("历史文案"))
        detail.layout.addWidget(self.theme_drafts_preview, 1)
        splitter.addWidget(theme_tabs)
        splitter.addWidget(detail)
        splitter.setSizes([760, 420])
        layout.addWidget(splitter, 1)
        search.clicked.connect(self.load_themes)
        add.clicked.connect(self.add_theme)
        delete.clicked.connect(self.delete_theme)
        self.theme_table.itemSelectionChanged.connect(self.load_selected_theme_drafts)
        return page

    def build_drafts_tab(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        tools = QHBoxLayout()
        self.draft_keyword = QLineEdit()
        self.draft_keyword.setPlaceholderText("文案关键词")
        self.draft_channel = QComboBox()
        self.draft_channel.addItems(["", "小红书", "朋友圈", "公众号"])
        self.draft_status = QComboBox()
        self.draft_status.addItems(["", "未使用", "已使用", "已归档"])
        search = primary_button("查询")
        add = QPushButton("给选中主题新增文案")
        save = primary_button("保存编辑")
        delete = danger_button("删除文案")
        for w in [self.draft_keyword, self.draft_channel, self.draft_status, search, add, save, delete]:
            tools.addWidget(w)
        layout.addLayout(tools)

        splitter = QSplitter()
        draft_tabs = QTabWidget()
        draft_cards_page = QWidget()
        draft_cards_layout = QVBoxLayout(draft_cards_page)
        draft_cards_layout.setContentsMargins(0, 0, 0, 0)
        draft_cards_layout.addWidget(SectionHeader("文案卡片", "展示标题、摘要、状态、版本和评分"))
        draft_card_scroll = QScrollArea()
        draft_card_scroll.setWidgetResizable(True)
        self.draft_card_host = QWidget()
        self.draft_card_grid = QGridLayout(self.draft_card_host)
        self.draft_card_grid.setSpacing(12)
        draft_card_scroll.setWidget(self.draft_card_host)
        draft_cards_layout.addWidget(draft_card_scroll, 1)
        self.draft_table = DataTable(["ID", "主题ID", "标题", "渠道", "版本", "使用状态", "评分"])
        draft_tabs.addTab(draft_cards_page, "卡片")
        draft_tabs.addTab(self.draft_table, "表格")
        editor_card = Card("文案编辑")
        self.draft_title = QLineEdit()
        self.draft_usage = QComboBox()
        self.draft_usage.addItems(["未使用", "已使用", "已归档"])
        self.draft_feedback = QLineEdit()
        self.draft_content = QTextEdit()
        editor_card.layout.addWidget(QLabel("标题"))
        editor_card.layout.addWidget(self.draft_title)
        self.draft_meta_row = InfoRow("渠道/版本", "-")
        editor_card.layout.addWidget(self.draft_meta_row)
        self.draft_rating = StarRating(0)
        self.draft_rating.changed.connect(self.rate_selected_draft)
        editor_card.layout.addWidget(self.draft_rating)
        editor_card.layout.addWidget(QLabel("使用状态"))
        editor_card.layout.addWidget(self.draft_usage)
        self.draft_used_date = QLineEdit()
        self.draft_used_date.setPlaceholderText("使用日期 yyyy-MM-dd")
        editor_card.layout.addWidget(QLabel("使用日期"))
        editor_card.layout.addWidget(self.draft_used_date)
        editor_card.layout.addWidget(QLabel("反馈"))
        editor_card.layout.addWidget(self.draft_feedback)
        self.draft_image_suggestion = QLineEdit()
        editor_card.layout.addWidget(QLabel("配图建议"))
        editor_card.layout.addWidget(self.draft_image_suggestion)
        image_reference_row = QHBoxLayout()
        self.draft_image_count = Badge("图片 0", "info")
        add_image = QPushButton("添加图片引用")
        add_image.clicked.connect(self.add_draft_image)
        image_reference_row.addWidget(self.draft_image_count)
        image_reference_row.addStretch(1)
        image_reference_row.addWidget(add_image)
        editor_card.layout.addLayout(image_reference_row)
        editor_card.layout.addWidget(QLabel("正文"))
        editor_card.layout.addWidget(self.draft_content, 1)
        splitter.addWidget(draft_tabs)
        splitter.addWidget(editor_card)
        splitter.setSizes([760, 420])
        layout.addWidget(splitter, 1)
        search.clicked.connect(self.load_drafts)
        add.clicked.connect(self.add_draft)
        save.clicked.connect(self.save_draft)
        delete.clicked.connect(self.delete_draft)
        self.draft_table.itemSelectionChanged.connect(self.render_selected_draft)
        return page

    def build_calendar_tab(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        row = QHBoxLayout()
        self.calendar_month = QLineEdit()
        self.calendar_month.setPlaceholderText("yyyy-MM，例如 2026-06")
        self.calendar_month.setText("2026-06")
        load = primary_button("加载日历")
        row.addWidget(self.calendar_month)
        row.addWidget(load)
        layout.addLayout(row)
        splitter = QSplitter()
        calendar_card = Card("内容日历")
        self.calendar_grid = CalendarGrid()
        self.calendar_day_output = QPlainTextEdit()
        self.calendar_day_output.setReadOnly(True)
        calendar_card.layout.addWidget(self.calendar_grid)
        calendar_card.layout.addWidget(QLabel("选中日期内容"))
        calendar_card.layout.addWidget(self.calendar_day_output, 1)
        self.calendar_table = DataTable(["ID", "日期", "渠道", "标题", "发布状态", "使用状态"])
        splitter.addWidget(calendar_card)
        splitter.addWidget(self.calendar_table)
        splitter.setSizes([520, 680])
        layout.addWidget(splitter, 1)
        load.clicked.connect(self.load_calendar)
        self.calendar_grid.selected.connect(self.select_calendar_day)
        return page

    def build_sop_tab(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        form = QGridLayout()
        self.sop_kind = QComboBox()
        self.sop_kind.addItems(["xiaohongshu", "moments", "asset"])
        self.sop_topic = QLineEdit("保研简历")
        self.sop_audience = QLineEdit("保研er")
        self.sop_style = QLineEdit("干货")
        self.sop_scene = QLineEdit("收到学员 offer")
        self.sop_extra = QTextEdit()
        self.sop_extra.setPlaceholderText("历史文案、关键词或补充上下文")
        generate = primary_button("生成")
        architecture = QPushButton("查看架构说明")
        form.addWidget(QLabel("类型"), 0, 0)
        form.addWidget(self.sop_kind, 0, 1)
        form.addWidget(QLabel("主题"), 1, 0)
        form.addWidget(self.sop_topic, 1, 1)
        form.addWidget(QLabel("受众"), 2, 0)
        form.addWidget(self.sop_audience, 2, 1)
        form.addWidget(QLabel("风格"), 3, 0)
        form.addWidget(self.sop_style, 3, 1)
        form.addWidget(QLabel("场景"), 4, 0)
        form.addWidget(self.sop_scene, 4, 1)
        form.addWidget(QLabel("补充"), 5, 0)
        form.addWidget(self.sop_extra, 5, 1)
        form.addWidget(generate, 6, 0)
        form.addWidget(architecture, 6, 1)
        layout.addLayout(form)
        self.sop_output = JsonPanel()
        layout.addWidget(self.sop_output, 1)
        generate.clicked.connect(self.generate_sop)
        architecture.clicked.connect(lambda: self.run_api(self.client.content_architecture, lambda result: self.sop_output.setPlainText(safe_json_dumps(result))))
        return page

    def load(self) -> None:
        self.load_themes()
        self.load_drafts()
        self.load_calendar()

    def load_themes(self) -> None:
        self.run_api(
            lambda: self.client.content_themes(
                self.theme_keyword.text().strip(),
                self.theme_status.currentText(),
                self.theme_channel.currentText(),
            ),
            self.render_themes,
        )

    def render_themes(self, rows: list[dict[str, Any]]) -> None:
        self.themes = rows
        normalized = [
            {
                "id": value(row, "id"),
                "title": value(row, "title"),
                "topic": value(row, "topic"),
                "platform": value(row, "platform"),
                "status": value(row, "status"),
                "heat": value(row, "heat"),
                "rating": value(row, "rating"),
                "plannedDate": value(row, "plannedDate"),
                "_raw": row,
            }
            for row in rows
        ]
        self.theme_table.set_rows(normalized, ["id", "title", "topic", "platform", "status", "heat", "rating", "plannedDate"])
        self.render_theme_cards(rows)
        self.update_content_stats()

    def render_theme_cards(self, rows: list[dict[str, Any]]) -> None:
        clear_layout(self.theme_card_grid)
        if not rows:
            self.theme_card_grid.addWidget(EmptyState("暂无主题"), 0, 0)
            return
        for index, theme in enumerate(rows):
            title = str(value(theme, "title", default="未命名主题"))
            summary = str(value(theme, "summary", default=""))
            meta = f"{value(theme, 'platform')} / {value(theme, 'status')} / 热度 {value(theme, 'heat')} / 评分 {value(theme, 'rating', default=0)}"
            card = TextCard(title, summary, meta, theme)
            top = QHBoxLayout()
            top.addWidget(Badge(str(value(theme, "platform", default="-")), "primary"))
            status = str(value(theme, "status", default="-"))
            top.addWidget(Badge(status, self.status_kind(status)))
            top.addStretch(1)
            card.layout.insertLayout(0, top)
            card.clicked.connect(self.open_theme_card)
            self.theme_card_grid.addWidget(card, index // 2, index % 2)

    def open_theme_card(self, payload: object) -> None:
        if not isinstance(payload, dict):
            return
        self.current_theme_id = int(value(payload, "id", default=0) or 0)
        self.render_theme_detail(payload)
        if self.current_theme_id:
            self.run_api(lambda: self.client.theme_drafts(self.current_theme_id), self.render_drafts)

    def add_theme(self) -> None:
        dialog = ThemeDialog(self)
        if dialog.exec() == QDialog.Accepted:
            self.run_api(lambda: self.client.create_theme(dialog.payload()), lambda _: self.load_themes())

    def delete_theme(self) -> None:
        row = self.theme_table.selected_row_data()
        if not row:
            return
        self.run_api(lambda: self.client.delete_theme(int(row["id"])), lambda _: self.load())

    def load_selected_theme_drafts(self) -> None:
        row = self.theme_table.selected_row_data()
        if not row:
            return
        self.current_theme_id = int(row["id"])
        self.render_theme_detail(row.get("_raw") or row)
        self.run_api(lambda: self.client.theme_drafts(self.current_theme_id), self.render_drafts)

    def render_theme_detail(self, theme: dict[str, Any]) -> None:
        self.theme_title_label.setText(str(value(theme, "title", default="未命名主题")))
        self.theme_summary_label.setText(str(value(theme, "summary", default="")))
        platform = str(value(theme, "platform", default="-"))
        status = str(value(theme, "status", default="-"))
        self.theme_platform_badge.setText(platform)
        self.theme_status_badge.setText(status)
        self.theme_status_badge.set_kind(self.status_kind(status))
        self.theme_rating.blockSignals(True)
        self.theme_rating.set_rating(int(value(theme, "rating", default=0) or 0))
        self.theme_rating.blockSignals(False)
        self.theme_topic_row.set_value(value(theme, "topic", default="-"))
        self.theme_heat_row.set_value(value(theme, "heat", default="-"))
        self.theme_date_row.set_value(value(theme, "plannedDate", default="-"))
        tags = value(theme, "tags", default=[])
        self.theme_tags_row.set_value("、".join(tags) if isinstance(tags, list) else tags)

    def status_kind(self, status: str) -> str:
        if status in ("已发布", "已使用", "success"):
            return "success"
        if status in ("待发布", "待创作", "running"):
            return "warning"
        if status in ("已生成",):
            return "primary"
        if status in ("已归档",):
            return "info"
        return "info"

    def rate_selected_theme(self, rating: int) -> None:
        if not self.current_theme_id:
            return
        self.run_api(lambda: self.client.rate_theme(self.current_theme_id, rating), lambda _: self.load_themes())

    def load_drafts(self) -> None:
        self.run_api(
            lambda: self.client.drafts(self.draft_keyword.text().strip(), self.draft_channel.currentText(), self.draft_status.currentText()),
            self.render_drafts,
        )

    def render_drafts(self, rows: list[dict[str, Any]]) -> None:
        self.drafts = rows
        normalized = [
            {
                "id": value(row, "id"),
                "themeId": value(row, "themeId"),
                "title": value(row, "title"),
                "channel": value(row, "channel"),
                "version": value(row, "version"),
                "usageStatus": value(row, "usageStatus"),
                "rating": value(row, "rating"),
                "_raw": row,
            }
            for row in rows
        ]
        self.draft_table.set_rows(normalized, ["id", "themeId", "title", "channel", "version", "usageStatus", "rating"])
        self.render_draft_cards(rows)
        self.theme_drafts_preview.setPlainText("\n\n".join(f"{row.get('title', '')}\n{row.get('content', '')}" for row in rows[:5]))
        self.update_content_stats()

    def render_draft_cards(self, rows: list[dict[str, Any]]) -> None:
        clear_layout(self.draft_card_grid)
        if not rows:
            self.draft_card_grid.addWidget(EmptyState("暂无文案"), 0, 0)
            return
        for index, draft in enumerate(rows):
            title = str(value(draft, "title", default="未命名文案"))
            body = str(value(draft, "content", default=""))
            if len(body) > 130:
                body = f"{body[:130]}..."
            meta = f"{value(draft, 'channel')} / {value(draft, 'version')} / {value(draft, 'usageStatus')} / 评分 {value(draft, 'rating', default=0)}"
            card = TextCard(title, body, meta, draft)
            top = QHBoxLayout()
            status = str(value(draft, "usageStatus", default="-"))
            top.addWidget(Badge(str(value(draft, "channel", default="-")), "primary"))
            top.addWidget(Badge(status, self.status_kind(status)))
            top.addStretch(1)
            card.layout.insertLayout(0, top)
            card.clicked.connect(self.open_draft_card)
            self.draft_card_grid.addWidget(card, index // 2, index % 2)

    def open_draft_card(self, payload: object) -> None:
        if not isinstance(payload, dict):
            return
        row = {"_raw": payload, "id": value(payload, "id", default=0)}
        self.current_draft_id = int(value(payload, "id", default=0) or 0)
        self.draft_title.setText(str(value(payload, "title")))
        self.draft_meta_row.set_value(f"{value(payload, 'channel')} / {value(payload, 'version')} / {value(payload, 'style')}")
        self.draft_rating.blockSignals(True)
        self.draft_rating.set_rating(int(value(payload, "rating", default=0) or 0))
        self.draft_rating.blockSignals(False)
        self.draft_usage.setCurrentText(str(value(payload, "usageStatus", default="未使用")))
        self.draft_used_date.setText(str(value(payload, "usedDate", default="")))
        self.draft_feedback.setText(str(value(payload, "feedback")))
        self.draft_image_suggestion.setText(str(value(payload, "imageSuggestion")))
        self.update_draft_image_count(payload)
        self.draft_content.setPlainText(str(value(payload, "content")))

    def render_selected_draft(self) -> None:
        row = self.draft_table.selected_row_data()
        if not row:
            return
        draft = row.get("_raw") or row
        self.current_draft_id = int(value(draft, "id"))
        self.draft_title.setText(str(value(draft, "title")))
        self.draft_meta_row.set_value(f"{value(draft, 'channel')} / {value(draft, 'version')} / {value(draft, 'style')}")
        self.draft_rating.blockSignals(True)
        self.draft_rating.set_rating(int(value(draft, "rating", default=0) or 0))
        self.draft_rating.blockSignals(False)
        self.draft_usage.setCurrentText(str(value(draft, "usageStatus", default="未使用")))
        self.draft_used_date.setText(str(value(draft, "usedDate", default="")))
        self.draft_feedback.setText(str(value(draft, "feedback")))
        self.draft_image_suggestion.setText(str(value(draft, "imageSuggestion")))
        self.update_draft_image_count(draft)
        self.draft_content.setPlainText(str(value(draft, "content")))

    def add_draft(self) -> None:
        theme_id = self.current_theme_id
        if not theme_id:
            show_error(self, "请先在主题库选中一个主题")
            return
        dialog = DraftDialog(self)
        if dialog.exec() == QDialog.Accepted:
            self.run_api(lambda: self.client.create_draft(theme_id, dialog.payload()), lambda _: self.load_drafts())

    def save_draft(self) -> None:
        if not self.current_draft_id:
            return
        payload = {
            "title": self.draft_title.text().strip(),
            "content": self.draft_content.toPlainText(),
            "usageStatus": self.draft_usage.currentText(),
            "usedDate": self.draft_used_date.text().strip() or None,
            "feedback": self.draft_feedback.text().strip(),
            "imageSuggestion": self.draft_image_suggestion.text().strip(),
        }
        self.run_api(lambda: self.client.update_draft(self.current_draft_id, payload), lambda _: self.load_drafts())

    def rate_selected_draft(self, rating: int) -> None:
        if not self.current_draft_id:
            return
        self.run_api(lambda: self.client.rate_draft(self.current_draft_id, rating), lambda _: self.load_drafts())

    def delete_draft(self) -> None:
        if not self.current_draft_id:
            return
        self.run_api(lambda: self.client.delete_draft(self.current_draft_id), lambda _: self.load_drafts())

    def update_draft_image_count(self, draft: dict[str, Any]) -> None:
        images = draft.get("images") if isinstance(draft, dict) else []
        count = len(images) if isinstance(images, list) else 0
        self.draft_image_count.setText(f"图片 {count}")

    def add_draft_image(self) -> None:
        if not self.current_draft_id:
            show_error(self, "请先选择一条文案")
            return
        dialog = ImageReferenceDialog(self)
        if dialog.exec() == QDialog.Accepted:
            self.run_api(
                lambda: self.client.add_draft_image(self.current_draft_id, dialog.payload()),
                lambda _: self.load_drafts(),
            )

    def load_calendar(self) -> None:
        self.run_api(lambda: self.client.calendar(self.calendar_month.text().strip()), self.render_calendar)

    def render_calendar(self, rows: list[dict[str, Any]]) -> None:
        self.calendar_rows = rows
        normalized = [
            {
                "id": value(row, "id"),
                "date": value(row, "date", "publishDate"),
                "channel": value(row, "channel"),
                "title": value(row, "title"),
                "publishStatus": value(row, "publishStatus", "status"),
                "usageStatus": value(row, "usageStatus"),
            }
            for row in rows
        ]
        self.calendar_table.set_rows(normalized, ["id", "date", "channel", "title", "publishStatus", "usageStatus"])
        self.calendar_grid.set_month_items(self.calendar_month.text().strip() or "2026-06", rows)
        self.update_content_stats()

    def select_calendar_day(self, day: str) -> None:
        items = []
        for row in self.calendar_rows:
            date_text = str(value(row, "date", "publishDate", default=""))
            if date_text == day:
                items.append(row)
        if not items:
            self.calendar_day_output.setPlainText(f"{day}\n当天暂无发布内容。")
            return
        lines = [f"{day} 内容列表"]
        for item in items:
            lines.append(
                f"- {value(item, 'channel')} / {value(item, 'publishStatus', 'status')} / "
                f"{value(item, 'title')}"
            )
        self.calendar_day_output.setPlainText("\n".join(lines))

    def generate_sop(self) -> None:
        kind = self.sop_kind.currentText()
        payload = {
            "agentType": "content" if kind == "xiaohongshu" else kind,
            "topic": self.sop_topic.text().strip(),
            "audience": self.sop_audience.text().strip(),
            "style": self.sop_style.text().strip(),
            "scene": self.sop_scene.text().strip(),
            "extra": {"content": self.sop_extra.toPlainText()},
        }
        self.sop_output.setPlainText("生成中...")
        self.run_api(lambda: self.client.content_generate(kind, payload), lambda result: self.sop_output.set_json(result))

    def update_content_stats(self) -> None:
        rated = 0
        for row in [*self.themes, *self.drafts]:
            try:
                if int(row.get("rating") or 0) > 0:
                    rated += 1
            except (TypeError, ValueError):
                pass
        self.theme_count_card.set_value(len(self.themes))
        self.copy_count_card.set_value(len(self.drafts))
        self.rated_count_card.set_value(rated)
        self.calendar_count_card.set_value(len(self.calendar_rows))


class ThemeDialog(QDialog):
    def __init__(self, parent: QWidget):
        super().__init__(parent)
        self.setWindowTitle("新增主题")
        layout = QVBoxLayout(self)
        self.title = QLineEdit("导师套磁邮件怎么写不尴尬")
        self.topic = QLineEdit("导师套磁")
        self.platform = QComboBox()
        self.platform.addItems(["小红书", "朋友圈", "公众号"])
        self.type = QLineEdit("经验干货")
        self.status = QComboBox()
        self.status.addItems(["待创作", "已生成", "待发布", "已发布"])
        self.heat = QSpinBox()
        self.heat.setRange(0, 100)
        self.heat.setValue(80)
        self.planned = QLineEdit("2026-06-25")
        self.summary = QLineEdit("围绕导师套磁邮件结构生成内容")
        self.tags = QLineEdit("导师套磁,邮件模板")
        for label, widget in [
            ("标题", self.title),
            ("主题", self.topic),
            ("平台", self.platform),
            ("类型", self.type),
            ("状态", self.status),
            ("热度", self.heat),
            ("计划日期", self.planned),
            ("摘要", self.summary),
            ("标签", self.tags),
        ]:
            layout.addWidget(QLabel(label))
            layout.addWidget(widget)
        buttons = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def payload(self) -> dict[str, Any]:
        return {
            "title": self.title.text(),
            "topic": self.topic.text(),
            "platform": self.platform.currentText(),
            "type": self.type.text(),
            "status": self.status.currentText(),
            "heat": self.heat.value(),
            "plannedDate": self.planned.text(),
            "summary": self.summary.text(),
            "tags": [tag.strip() for tag in self.tags.text().split(",") if tag.strip()],
        }


class DraftDialog(QDialog):
    def __init__(self, parent: QWidget):
        super().__init__(parent)
        self.setWindowTitle("新增文案")
        layout = QVBoxLayout(self)
        self.title = QLineEdit("保研简历别再这样写了")
        self.channel = QComboBox()
        self.channel.addItems(["小红书", "朋友圈", "公众号"])
        self.version = QLineEdit("干货版")
        self.style = QLineEdit("干货")
        self.owner = QLineEdit("内容运营")
        self.content = QTextEdit()
        self.content.setPlainText("正文内容...")
        for label, widget in [
            ("标题", self.title),
            ("渠道", self.channel),
            ("版本", self.version),
            ("风格", self.style),
            ("负责人", self.owner),
            ("正文", self.content),
        ]:
            layout.addWidget(QLabel(label))
            layout.addWidget(widget)
        buttons = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def payload(self) -> dict[str, Any]:
        return {
            "title": self.title.text(),
            "channel": self.channel.currentText(),
            "version": self.version.text(),
            "style": self.style.text(),
            "content": self.content.toPlainText(),
            "owner": self.owner.text(),
        }


class ImageReferenceDialog(QDialog):
    def __init__(self, parent: QWidget):
        super().__init__(parent)
        self.setWindowTitle("添加图片引用")
        layout = QVBoxLayout(self)
        self.name = QLineEdit("resume-cover.png")
        self.url = QLineEdit("https://example.com/resume-cover.png")
        self.storage_provider = QLineEdit("local")
        self.object_key = QLineEdit("content/resume-cover.png")
        for label, widget in [
            ("图片名称", self.name),
            ("图片 URL", self.url),
            ("存储来源", self.storage_provider),
            ("对象 Key", self.object_key),
        ]:
            layout.addWidget(QLabel(label))
            layout.addWidget(widget)
        buttons = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def payload(self) -> dict[str, Any]:
        return {
            "name": self.name.text().strip(),
            "url": self.url.text().strip(),
            "storageProvider": self.storage_provider.text().strip(),
            "objectKey": self.object_key.text().strip(),
        }


class StudentsPage(BasePage):
    def __init__(self, client: ApiClient):
        super().__init__(client)
        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        refresh = primary_button("刷新")
        refresh.clicked.connect(self.load)
        add = QPushButton("新增学员")
        add.clicked.connect(self.add_student)
        analyze = QPushButton("AI 分析")
        analyze.clicked.connect(self.analyze_selected)
        delete = danger_button("删除")
        delete.clicked.connect(self.delete_selected)
        root.addWidget(PageHeader("学员管理", "学员画像、风险标签、申请阶段和进度条", [add, analyze, delete, refresh]))

        stat_grid = QGridLayout()
        self.student_total_card = StatCard("学员总数", "0", "当前列表")
        self.student_low_risk_card = StatCard("低风险", "0", "节奏稳定")
        self.student_mid_risk_card = StatCard("中风险", "0", "需要跟进")
        self.student_high_risk_card = StatCard("高风险", "0", "优先处理")
        for index, card in enumerate([self.student_total_card, self.student_low_risk_card, self.student_mid_risk_card, self.student_high_risk_card]):
            stat_grid.addWidget(card, 0, index)
        root.addLayout(stat_grid)

        splitter = QSplitter()
        self.table = DataTable(["ID", "姓名", "学校", "专业", "GPA", "排名", "英语", "目标", "阶段", "风险", "进度"])
        detail_tabs = QTabWidget()
        profile = Card("学员画像")
        self.student_name = QLabel("请选择学员")
        self.student_name.setObjectName("SectionTitle")
        self.student_school = InfoRow("学校/专业", "-")
        self.student_scores = InfoRow("成绩", "-")
        self.student_target = InfoRow("目标", "-")
        self.student_stage = InfoRow("阶段", "-")
        self.student_risk_badge = Badge("-", "info")
        self.student_progress = InfoRow("进度", "-")
        profile.layout.addWidget(self.student_name)
        profile.layout.addWidget(self.student_risk_badge)
        profile.layout.addWidget(self.student_school)
        profile.layout.addWidget(self.student_scores)
        profile.layout.addWidget(self.student_target)
        profile.layout.addWidget(self.student_stage)
        profile.layout.addWidget(self.student_progress)
        self.output = JsonPanel()
        detail_tabs.addTab(profile, "画像")
        detail_tabs.addTab(self.output, "AI 分析")
        splitter.addWidget(self.table)
        splitter.addWidget(detail_tabs)
        splitter.setSizes([900, 300])
        root.addWidget(splitter, 1)
        self.table.itemSelectionChanged.connect(self.render_selected_student)

    def load(self) -> None:
        self.run_api(self.client.students, self.render)

    def render(self, rows: list[dict[str, Any]]) -> None:
        normalized = [
            {
                "id": value(row, "id"),
                "name": value(row, "name"),
                "school": value(row, "school"),
                "major": value(row, "major"),
                "gpa": value(row, "gpa"),
                "rank": value(row, "rank", "ranking"),
                "english": value(row, "english", "englishScore"),
                "targetSchool": value(row, "targetSchool"),
                "stage": value(row, "stage", "applicationStage"),
                "risk": value(row, "risk", "riskLevel"),
                "progress": value(row, "progress"),
                "_raw": row,
            }
            for row in rows
        ]
        self.table.set_rows(normalized, ["id", "name", "school", "major", "gpa", "rank", "english", "targetSchool", "stage", "risk", "progress"])
        self.update_stats(normalized)

    def update_stats(self, rows: list[dict[str, Any]]) -> None:
        self.student_total_card.set_value(len(rows))
        self.student_low_risk_card.set_value(sum(1 for row in rows if row.get("risk") == "低"))
        self.student_mid_risk_card.set_value(sum(1 for row in rows if row.get("risk") == "中"))
        self.student_high_risk_card.set_value(sum(1 for row in rows if row.get("risk") == "高"))

    def render_selected_student(self) -> None:
        row = self.table.selected_row_data()
        if not row:
            return
        raw = row.get("_raw") or row
        self.student_name.setText(str(value(raw, "name", default="未命名学员")))
        self.student_school.set_value(f"{value(raw, 'school')} / {value(raw, 'major')}")
        self.student_scores.set_value(f"GPA {value(raw, 'gpa')}，排名 {value(raw, 'rank', 'ranking')}，英语 {value(raw, 'english', 'englishScore')}")
        self.student_target.set_value(value(raw, "targetSchool", default="-"))
        self.student_stage.set_value(value(raw, "stage", "applicationStage", default="-"))
        risk = str(value(raw, "risk", "riskLevel", default="-"))
        self.student_risk_badge.setText(f"风险：{risk}")
        self.student_risk_badge.set_kind("success" if risk == "低" else "warning" if risk == "中" else "danger")
        self.student_progress.set_value(f"{value(raw, 'progress', default='-')}%")

    def add_student(self) -> None:
        dialog = StudentDialog(self)
        if dialog.exec() == QDialog.Accepted:
            self.run_api(lambda: self.client.create_student(dialog.payload()), lambda _: self.load())

    def selected_id(self) -> int | None:
        row = self.table.selected_row_data()
        return int(row["id"]) if row and row.get("id") else None

    def analyze_selected(self) -> None:
        sid = self.selected_id()
        if not sid:
            return
        self.run_api(lambda: self.client.analyze_student(sid), lambda result: self.output.set_json(result))

    def delete_selected(self) -> None:
        sid = self.selected_id()
        if not sid:
            return
        self.run_api(lambda: self.client.delete_student(sid), lambda _: self.load())


class StudentDialog(QDialog):
    def __init__(self, parent: QWidget):
        super().__init__(parent)
        self.setWindowTitle("新增学员")
        layout = QVBoxLayout(self)
        self.fields: dict[str, QLineEdit] = {}
        defaults = {
            "name": "学员21",
            "school": "示例大学",
            "major": "食品科学",
            "gpa": "3.80",
            "rank": "5/120",
            "english": "六级 580",
            "targetSchool": "985 食品项目",
            "stage": "材料准备",
            "risk": "低",
            "progress": "60",
        }
        for key, default in defaults.items():
            layout.addWidget(QLabel(key))
            edit = QLineEdit(default)
            self.fields[key] = edit
            layout.addWidget(edit)
        buttons = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def payload(self) -> dict[str, Any]:
        payload = {key: edit.text() for key, edit in self.fields.items()}
        try:
            payload["progress"] = int(payload["progress"])
        except ValueError:
            payload["progress"] = 0
        return payload


class SchoolsPage(BasePage):
    def __init__(self, client: ApiClient):
        super().__init__(client)
        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        refresh = primary_button("刷新")
        refresh.clicked.connect(self.load)
        recommend = QPushButton("院校推荐")
        recommend.clicked.connect(self.recommend)
        root.addWidget(PageHeader("院校情报", "院校项目、截止时间、条件、材料和 AI 匹配", [recommend, refresh]))
        stat_grid = QGridLayout()
        self.school_count_card = StatCard("学校数", "0", "已收录院校")
        self.project_count_card = StatCard("项目数", "0", "夏令营/预推免")
        self.match_avg_card = StatCard("平均匹配", "0", "项目匹配分")
        self.deadline_card = StatCard("最近截止", "-", "需要优先跟进")
        for index, card in enumerate([self.school_count_card, self.project_count_card, self.match_avg_card, self.deadline_card]):
            stat_grid.addWidget(card, 0, index)
        root.addLayout(stat_grid)
        tabs = QTabWidget()
        tabs.addTab(self.build_schools(), "学校")
        tabs.addTab(self.build_projects(), "项目")
        tabs.addTab(self.build_recommend(), "推荐")
        root.addWidget(tabs, 1)

    def build_schools(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        self.school_table = DataTable(["ID", "学校", "地区", "层次", "标签"])
        layout.addWidget(self.school_table)
        return page

    def build_projects(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        splitter = QSplitter()
        self.project_table = DataTable(["ID", "学校", "项目", "类型", "截止", "要求", "材料", "匹配"])
        detail = Card("项目详情")
        self.project_title = QLabel("请选择项目")
        self.project_title.setObjectName("SectionTitle")
        self.project_school = InfoRow("院校", "-")
        self.project_deadline = InfoRow("截止", "-")
        self.project_match = Badge("匹配 -", "info")
        self.project_requirements = MarkdownPanel()
        self.project_requirements.setMinimumHeight(140)
        self.project_materials = MarkdownPanel()
        self.project_materials.setMinimumHeight(140)
        detail.layout.addWidget(self.project_title)
        detail.layout.addWidget(self.project_match)
        detail.layout.addWidget(self.project_school)
        detail.layout.addWidget(self.project_deadline)
        detail.layout.addWidget(QLabel("报名条件"))
        detail.layout.addWidget(self.project_requirements, 1)
        detail.layout.addWidget(QLabel("材料要求"))
        detail.layout.addWidget(self.project_materials, 1)
        splitter.addWidget(self.project_table)
        splitter.addWidget(detail)
        splitter.setSizes([780, 420])
        layout.addWidget(splitter)
        self.project_table.itemSelectionChanged.connect(self.render_selected_project)
        return page

    def build_recommend(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        form = QGridLayout()
        self.rec_gpa = QLineEdit("3.80")
        self.rec_rank = QLineEdit("5/120")
        self.rec_english = QLineEdit("六级 580")
        self.rec_major = QLineEdit("食品科学")
        self.rec_risk = QComboBox()
        self.rec_risk.addItems(["稳妥", "均衡", "冲刺"])
        form.addWidget(QLabel("GPA"), 0, 0)
        form.addWidget(self.rec_gpa, 0, 1)
        form.addWidget(QLabel("排名"), 1, 0)
        form.addWidget(self.rec_rank, 1, 1)
        form.addWidget(QLabel("英语"), 2, 0)
        form.addWidget(self.rec_english, 2, 1)
        form.addWidget(QLabel("目标专业"), 3, 0)
        form.addWidget(self.rec_major, 3, 1)
        form.addWidget(QLabel("风险偏好"), 4, 0)
        form.addWidget(self.rec_risk, 4, 1)
        layout.addLayout(form)
        self.rec_output = QPlainTextEdit()
        self.rec_output.setReadOnly(True)
        layout.addWidget(self.rec_output)
        return page

    def load(self) -> None:
        self.run_api(self.client.schools, self.render_schools)
        self.run_api(self.client.projects, self.render_projects)

    def render_schools(self, rows: list[dict[str, Any]]) -> None:
        self.school_rows = rows
        normalized = [
            {
                "id": value(row, "id"),
                "name": value(row, "name", "schoolName"),
                "region": value(row, "region"),
                "level": value(row, "level"),
                "disciplineTags": value(row, "disciplineTags", "tags"),
            }
            for row in rows
        ]
        self.school_table.set_rows(normalized, ["id", "name", "region", "level", "disciplineTags"])
        self.school_count_card.set_value(len(rows))

    def render_projects(self, rows: list[dict[str, Any]]) -> None:
        self.project_rows = rows
        normalized = [
            {
                "id": value(row, "id"),
                "schoolName": value(row, "schoolName"),
                "projectName": value(row, "projectName"),
                "projectType": value(row, "projectType"),
                "deadline": value(row, "deadline"),
                "requirements": value(row, "requirements"),
                "materials": value(row, "materials"),
                "matchScore": value(row, "matchScore"),
            }
            for row in rows
        ]
        self.project_table.set_rows(normalized, ["id", "schoolName", "projectName", "projectType", "deadline", "requirements", "materials", "matchScore"])
        self.project_count_card.set_value(len(rows))
        scores = []
        for row in rows:
            try:
                scores.append(float(value(row, "matchScore", default=0) or 0))
            except (TypeError, ValueError):
                pass
        self.match_avg_card.set_value(f"{sum(scores) / len(scores):.0f}" if scores else "0")
        deadlines = sorted([str(value(row, "deadline", default="")) for row in rows if value(row, "deadline", default="")])
        self.deadline_card.set_value(deadlines[0] if deadlines else "-")

    def render_selected_project(self) -> None:
        row = self.project_table.selected_row_data()
        if not row:
            return
        self.project_title.setText(str(value(row, "projectName", default="未命名项目")))
        self.project_school.set_value(value(row, "schoolName", default="-"))
        self.project_deadline.set_value(value(row, "deadline", default="-"))
        score = value(row, "matchScore", default="-")
        self.project_match.setText(f"匹配 {score}")
        try:
            numeric = float(score)
            self.project_match.set_kind("success" if numeric >= 85 else "warning" if numeric >= 70 else "info")
        except (TypeError, ValueError):
            self.project_match.set_kind("info")
        self.project_requirements.set_text(str(value(row, "requirements", default="")))
        self.project_materials.set_text(str(value(row, "materials", default="")))

    def recommend(self) -> None:
        payload = {
            "gpa": self.rec_gpa.text(),
            "rank": self.rec_rank.text(),
            "english": self.rec_english.text(),
            "targetMajor": self.rec_major.text(),
            "riskPreference": self.rec_risk.currentText(),
        }
        self.run_api(lambda: self.client.recommend_schools(payload), lambda result: self.rec_output.setPlainText(safe_json_dumps(result)))


class AnalyticsPage(BasePage):
    def __init__(self, client: ApiClient):
        super().__init__(client)
        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        refresh = primary_button("刷新")
        refresh.clicked.connect(self.load)
        root.addWidget(PageHeader("数据分析", "院校分布、GPA、申请漏斗和截止趋势", [refresh]))
        grid = QGridLayout()
        self.overview_cards = [
            StatCard("学员", "-"),
            StatCard("项目", "-"),
            StatCard("内容", "-"),
            StatCard("任务率", "-"),
        ]
        for i, card in enumerate(self.overview_cards):
            grid.addWidget(card, 0, i)
        self.student_chart = ChartCard("学员分布", "#5b6cff")
        self.content_chart = ChartCard("内容统计", "#8b5cf6")
        self.funnel_chart = ChartCard("申请阶段漏斗", "#19b37b")
        self.deadline_chart = ChartCard("截止趋势", "#f59e0b")
        grid.addWidget(self.student_chart, 1, 0, 1, 2)
        grid.addWidget(self.content_chart, 1, 2, 1, 2)
        grid.addWidget(self.funnel_chart, 2, 0, 1, 2)
        grid.addWidget(self.deadline_chart, 2, 2, 1, 2)
        root.addLayout(grid, 1)

    def load(self) -> None:
        def fetch() -> dict[str, Any]:
            return {
                "overview": self.client.analytics_overview(),
                "student": self.client.analytics_distribution(),
                "content": self.client.analytics_content_stats(),
                "funnel": self.client.analytics_funnel(),
                "deadlines": self.client.analytics_deadlines(),
            }

        self.run_api(fetch, self.render)

    def render(self, data: dict[str, Any]) -> None:
        overview = data.get("overview") or {}
        self.overview_cards[0].set_value(value(overview, "students", default="-"))
        self.overview_cards[1].set_value(value(overview, "activeProjects", default="-"))
        self.overview_cards[2].set_value(value(overview, "contentTopics", default="-"))
        self.overview_cards[3].set_value(value(overview, "taskRate", default="-"))
        DashboardPage._set_series(self.student_chart, data.get("student"))
        DashboardPage._set_series(self.content_chart, data.get("content"))
        funnel = data.get("funnel") or []
        self.funnel_chart.set_data([row.get("name") for row in funnel], [row.get("value") for row in funnel])
        DashboardPage._set_series(self.deadline_chart, data.get("deadlines"))


class FeishuPage(BasePage):
    def __init__(self, client: ApiClient):
        super().__init__(client)
        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        refresh = primary_button("刷新")
        refresh.clicked.connect(self.load)
        root.addWidget(PageHeader("飞书同步", "飞书文档、多维表格、机器人和同步日志", [refresh]))
        status_grid = QGridLayout()
        self.feishu_badges: dict[str, Badge] = {}
        for index, name in enumerate(["docs", "bitable", "tasks", "bot", "larkCli"]):
            box = Card()
            box.layout.addWidget(QLabel(name))
            badge = Badge("WAITING", "info")
            self.feishu_badges[name] = badge
            box.layout.addWidget(badge)
            status_grid.addWidget(box, 0, index)
        root.addLayout(status_grid)
        actions = QHBoxLayout()
        for label, action in [
            ("同步文档", "sync_docs"),
            ("同步多维表格", "sync_bitable"),
            ("同步任务", "sync_tasks"),
            ("lark-cli 状态", "lark_status"),
            ("知识库文件", "knowledge_files"),
        ]:
            button = QPushButton(label)
            button.clicked.connect(lambda checked=False, a=action: self.run_action(a))
            actions.addWidget(button)
        self.push_text = QLineEdit("FlowMind 桌面端测试推送")
        push = primary_button("机器人推送")
        push.clicked.connect(lambda: self.run_action("push", {"message": self.push_text.text()}))
        actions.addWidget(self.push_text)
        actions.addWidget(push)
        root.addLayout(actions)

        splitter = QSplitter()
        left = Card("同步状态")
        self.status_output = JsonPanel()
        left.layout.addWidget(self.status_output)
        right = Card("同步日志")
        self.log_table = DataTable(["ID", "类型", "目标", "状态", "消息", "时间"])
        right.layout.addWidget(self.log_table)
        splitter.addWidget(left)
        splitter.addWidget(right)
        splitter.setSizes([420, 780])
        root.addWidget(splitter, 1)

    def load(self) -> None:
        self.run_api(self.client.feishu_status, self.render_status)
        self.run_api(self.client.feishu_logs, self.render_logs)

    def render_status(self, result: dict[str, Any]) -> None:
        self.status_output.set_json(result)
        for key, badge in self.feishu_badges.items():
            current = str(result.get(key, "-"))
            badge.setText(current)
            badge.set_kind(self.feishu_status_kind(current))

    def feishu_status_kind(self, status: str) -> str:
        status = status.upper()
        if status in ("CONNECTED", "READY", "AVAILABLE", "SUCCESS"):
            return "success"
        if status in ("WAITING", "DEMO", "PENDING"):
            return "warning"
        if status in ("FAILED", "ERROR", "UNAVAILABLE"):
            return "danger"
        return "info"

    def run_action(self, action: str, payload: dict[str, Any] | None = None) -> None:
        self.run_api(lambda: self.client.feishu_action(action, payload), lambda result: (self.status_output.set_json(result), self.load()))

    def render_logs(self, rows: list[dict[str, Any]]) -> None:
        normalized = [
            {
                "id": value(row, "id"),
                "syncType": value(row, "syncType", "type"),
                "targetName": value(row, "targetName", "target"),
                "status": value(row, "status"),
                "message": value(row, "message"),
                "createdAt": value(row, "createdAt"),
            }
            for row in rows
        ]
        self.log_table.set_rows(normalized, ["id", "syncType", "targetName", "status", "message", "createdAt"])


class SettingsPage(BasePage):
    def __init__(self, client: ApiClient):
        super().__init__(client)
        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        root.addWidget(PageHeader("系统设置", "模型、Prompt、接口、后端地址与权限接口"))
        tabs = QTabWidget()
        tabs.addTab(self.build_connection(), "连接")
        tabs.addTab(self.build_prompts(), "Prompt")
        tabs.addTab(self.build_permissions(), "权限")
        tabs.addTab(self.build_routes(), "路由")
        root.addWidget(tabs, 1)

    def build_connection(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        self.base_url = QLineEdit(self.client.base_url)
        self.token = QLineEdit(self.client.token)
        save = primary_button("保存到当前进程")
        save.clicked.connect(self.save_connection)
        layout.addWidget(QLabel("Base URL"))
        layout.addWidget(self.base_url)
        layout.addWidget(QLabel("Token"))
        layout.addWidget(self.token)
        layout.addWidget(save)
        layout.addStretch(1)
        return page

    def build_prompts(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        load = primary_button("加载 Prompt")
        load.clicked.connect(self.load_prompts)
        self.prompt_table = DataTable(["ID", "Agent", "名称", "模板"])
        layout.addWidget(load)
        layout.addWidget(self.prompt_table)
        return page

    def build_permissions(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        row = QHBoxLayout()
        roles = QPushButton("角色")
        permissions = QPushButton("权限")
        role_perm = primary_button("查询角色权限")
        self.role_code = QLineEdit("ADMIN")
        row.addWidget(roles)
        row.addWidget(permissions)
        row.addWidget(self.role_code)
        row.addWidget(role_perm)
        layout.addLayout(row)
        self.permissions_output = QPlainTextEdit()
        self.permissions_output.setReadOnly(True)
        layout.addWidget(self.permissions_output)
        roles.clicked.connect(lambda: self.run_api(self.client.roles, lambda result: self.permissions_output.setPlainText(safe_json_dumps(result))))
        permissions.clicked.connect(lambda: self.run_api(self.client.permissions, lambda result: self.permissions_output.setPlainText(safe_json_dumps(result))))
        role_perm.clicked.connect(lambda: self.run_api(lambda: self.client.role_permissions(self.role_code.text().strip()), lambda result: self.permissions_output.setPlainText(safe_json_dumps(result))))
        return page

    def build_routes(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        load = primary_button("加载后端路由")
        load.clicked.connect(self.load_routes)
        self.routes_table = DataTable(["服务", "路径"])
        layout.addWidget(load)
        layout.addWidget(self.routes_table)
        return page

    def load(self) -> None:
        self.load_routes()

    def save_connection(self) -> None:
        self.client.set_base_url(self.base_url.text().strip())
        self.client.set_token(self.token.text().strip())
        QMessageBox.information(self, "FlowMind", "已更新当前进程连接信息。登录页保存的信息将在下次登录后写入。")

    def load_prompts(self) -> None:
        self.run_api(lambda: self.client.request("GET", "/api/prompts"), self.render_prompts)

    def render_prompts(self, rows: list[dict[str, Any]]) -> None:
        normalized = [
            {
                "id": value(row, "id"),
                "agentType": value(row, "agentType"),
                "name": value(row, "name"),
                "template": value(row, "template"),
            }
            for row in rows
        ]
        self.prompt_table.set_rows(normalized, ["id", "agentType", "name", "template"])

    def load_routes(self) -> None:
        self.run_api(self.client.routes, self.render_routes, on_fail=lambda _: None)

    def render_routes(self, rows: list[dict[str, Any]]) -> None:
        normalized = [{"service": value(row, "service"), "path": value(row, "path")} for row in rows]
        self.routes_table.set_rows(normalized, ["service", "path"])
