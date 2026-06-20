from __future__ import annotations

from datetime import datetime
import json
from typing import Any, Callable

from PySide6.QtCore import Qt, QUrl, Signal
from PySide6.QtGui import QDesktopServices
from PySide6.QtWidgets import (
    QApplication,
    QButtonGroup,
    QCheckBox,
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
    QProgressBar,
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


def copy_text(parent: QWidget, text: str, label: str = "内容") -> None:
    if not text:
        show_error(parent, f"没有可复制的{label}")
        return
    QApplication.clipboard().setText(text)
    QMessageBox.information(parent, "FlowMind", f"已复制{label}。")


def open_url(parent: QWidget, url: str) -> None:
    if not url:
        show_error(parent, "没有可打开的链接")
        return
    if not QDesktopServices.openUrl(QUrl(url)):
        show_error(parent, "无法打开该链接，请复制后在浏览器中访问。")


def compact_json(value_: Any) -> str:
    if value_ is None:
        return ""
    if isinstance(value_, str):
        return value_
    return json.dumps(value_, ensure_ascii=False)


ROLE_LABELS = {
    "CONTENT_OPERATOR": "内容运营人员",
    "EDU_CONSULTANT": "教育咨询老师",
    "IP_OPERATOR": "个人IP运营者",
    "TEAM_ADMIN": "团队管理员",
    "STUDENT_USER": "学员用户",
    "ADMIN": "团队管理员",
}

ROUTE_PERMISSION_FALLBACKS = {
    "/dashboard": ["/api/analytics/**"],
    "/agent": ["/api/agents/**"],
    "/knowledge": ["/api/knowledge/**"],
    "/content": ["/api/content/**"],
    "/students": ["/api/students/**"],
    "/schools": ["/api/schools/**", "/api/school-projects/**"],
    "/analytics": ["/api/analytics/**"],
    "/feishu": ["/api/feishu/**"],
    "/settings": ["/api/users/**"],
}


def normalize_user(input_user: Any) -> dict[str, Any]:
    user = input_user if isinstance(input_user, dict) else {}
    roles = user.get("roles")
    if not isinstance(roles, list):
        roles = [user.get("role")] if user.get("role") else []
    permissions = user.get("permissions")
    if not isinstance(permissions, list):
        permissions = []
    return {
        "id": user.get("id"),
        "username": user.get("username") or "",
        "nickname": user.get("nickname") or user.get("username") or "未命名用户",
        "role": user.get("role") or (roles[0] if roles else ""),
        "roles": roles,
        "permissions": permissions,
        "workspace": user.get("workspace") or "保研内容运营工作空间",
    }


def role_label(role: str) -> str:
    return ROLE_LABELS.get(role, role or "未分配角色")


def user_initials(user: dict[str, Any]) -> str:
    name = str(user.get("nickname") or user.get("username") or "FM")
    return name[:2].upper()


def permission_strings(user: dict[str, Any]) -> set[str]:
    result: set[str] = set()
    for item in user.get("permissions") or []:
        if isinstance(item, dict):
            for key in ["pathPattern", "frontendRoute", "permissionCode", "code"]:
                if item.get(key):
                    result.add(str(item[key]))
        elif item:
            result.add(str(item))
    return result


def is_admin_user(user: dict[str, Any]) -> bool:
    roles = {str(role) for role in user.get("roles") or []}
    role = str(user.get("role") or "")
    return bool({"ADMIN", "TEAM_ADMIN"} & ({role} | roles))


def can_visit_route(user: dict[str, Any], route_path: str) -> bool:
    if is_admin_user(user):
        return True
    permissions = permission_strings(user)
    if "*" in permissions or route_path in permissions:
        return True
    return any(pattern in permissions for pattern in ROUTE_PERMISSION_FALLBACKS.get(route_path, []))


def permission_reason(user: dict[str, Any], route_path: str) -> str:
    if can_visit_route(user, route_path):
        return "可访问"
    return f"当前角色「{role_label(str(user.get('role') or ''))}」暂未开放该页面"


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

    ACCOUNTS = [
        {"username": "admin", "label": "团队管理员"},
        {"username": "content", "label": "内容运营人员"},
        {"username": "teacher", "label": "教育咨询老师"},
        {"username": "ip", "label": "个人IP运营者"},
        {"username": "student", "label": "学员用户"},
    ]

    def __init__(self, client: ApiClient, config: dict[str, str]):
        super().__init__()
        self.client = client
        self.worker: ApiWorker | None = None

        root = QVBoxLayout(self)
        root.setContentsMargins(28, 28, 28, 28)
        root.addStretch(1)

        panel = QFrame()
        panel.setObjectName("LoginPanel")
        panel.setFixedWidth(980)
        panel_layout = QHBoxLayout(panel)
        panel_layout.setContentsMargins(0, 0, 0, 0)
        panel_layout.setSpacing(0)

        brand = QFrame()
        brand.setObjectName("LoginBrand")
        brand.setFixedWidth(520)
        brand_layout = QVBoxLayout(brand)
        brand_layout.setContentsMargins(52, 48, 52, 48)
        brand_layout.setSpacing(14)
        mark = QLabel("F")
        mark.setObjectName("LoginMark")
        mark.setFixedSize(56, 56)
        mark.setAlignment(Qt.AlignCenter)
        title = QLabel("FlowMind Agent")
        title.setObjectName("LoginTitle")
        sub = QLabel("基于角色权限的 AI 内容运营与知识管理智能体平台")
        sub.setObjectName("LoginSubtitle")
        sub.setWordWrap(True)
        brand_layout.addWidget(mark)
        brand_layout.addSpacing(8)
        brand_layout.addWidget(title)
        brand_layout.addWidget(sub)

        self.account_group = QButtonGroup(self)
        self.account_group.setExclusive(True)
        for index, account in enumerate(self.ACCOUNTS):
            button = QPushButton(f"{account['label']}\n{account['username']} / 123456")
            button.setObjectName("DemoAccountButton")
            button.setCheckable(True)
            button.setMinimumHeight(58)
            if account["username"] == "admin":
                button.setChecked(True)
            button.clicked.connect(lambda checked=False, name=account["username"]: self.fill_account(name))
            self.account_group.addButton(button, index)
            brand_layout.addWidget(button)
        brand_layout.addStretch(1)

        form = QWidget()
        form.setObjectName("LoginForm")
        layout = QVBoxLayout(form)
        layout.setContentsMargins(48, 48, 48, 48)
        layout.setSpacing(12)
        form_title = QLabel("登录工作空间")
        form_title.setObjectName("PageTitle")
        form_sub = QLabel("账号密码复用后端认证接口；离线演示会自动使用 Mock 角色和权限数据。")
        form_sub.setObjectName("Muted")
        form_sub.setWordWrap(True)
        self.base_url = QLineEdit(config.get("base_url", "http://localhost:8080"))
        self.username = QLineEdit("admin")
        self.password = QLineEdit("123456")
        self.password.setEchoMode(QLineEdit.Password)
        self.login_button = primary_button("进入工作台")
        self.demo_button = QPushButton("使用离线 Demo 数据")
        self.status = QLabel("团队管理员：admin / 123456；学员用户：student / 123456")
        self.status.setObjectName("Muted")

        layout.addWidget(form_title)
        layout.addWidget(form_sub)
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
        layout.addStretch(1)

        panel_layout.addWidget(brand)
        panel_layout.addWidget(form, 1)

        row = QHBoxLayout()
        row.addStretch(1)
        row.addWidget(panel)
        row.addStretch(1)
        root.addLayout(row)
        root.addStretch(1)

        self.login_button.clicked.connect(self.login)
        self.demo_button.clicked.connect(self.demo)
        self.password.returnPressed.connect(self.login)

    def fill_account(self, username: str) -> None:
        self.username.setText(username)
        self.password.setText("123456")
        self.status.setText(f"已选择 {username} / 123456")

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
        self.logged_in.emit({"base_url": self.base_url.text().strip(), "token": self.client.token, "user": normalize_user(data.get("user", {}))})

    def on_login_fail(self, message: str) -> None:
        self.login_button.setEnabled(True)
        self.status.setText(f"登录失败：{message}")

    def demo(self) -> None:
        self.base_url.setText("offline://demo")
        self.client.set_base_url("offline://demo")
        data = self.client.login(self.username.text().strip() or "admin", self.password.text() or "123456")
        self.logged_in.emit({"base_url": "offline://demo", "token": self.client.token, "user": normalize_user(data.get("user", {}))})


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
        user = normalize_user(data.get("user", {}))
        self.config.update({"base_url": data.get("base_url", self.client.base_url), "token": data.get("token", self.client.token), "user": user})
        self.save_config(self.config)
        self.show_shell()

    def show_shell(self) -> None:
        user = self.config.get("user") if isinstance(self.config.get("user"), dict) else None
        if not user:
            user = self.client.current_user or self.client.offline_user("admin")
        shell = ShellWidget(self.client, normalize_user(user), self.logout)
        self.setCentralWidget(shell)

    def logout(self) -> None:
        self.config.pop("token", None)
        self.config.pop("user", None)
        self.save_config(self.config)
        self.client.set_token("mock-jwt.demo")
        self.show_login()


class ShellWidget(QWidget):
    NAV = [
        ("/dashboard", "Dashboard", "Dashboard", "保研内容运营工作空间"),
        ("/agent", "AI 工作台", "AI 工作台", "自动路由、多 Agent 对话与工具调用"),
        ("/knowledge", "知识库", "知识库", "文档、标签、同步与向量检索"),
        ("/content", "内容运营", "内容运营", "主题库、文案库、日历与 SOP 生成"),
        ("/students", "学员管理", "学员管理", "学员画像、申请进度与 AI 分析"),
        ("/schools", "院校情报", "院校情报", "院校项目库与推荐"),
        ("/analytics", "数据分析", "数据分析", "运营与申请趋势图表"),
        ("/feishu", "飞书同步", "飞书同步", "飞书状态、文档、同步与日志"),
        ("/settings", "系统设置", "系统设置", "后端连接、接口和权限管理"),
    ]

    def __init__(self, client: ApiClient, user: dict[str, Any], logout: Callable[[], None]):
        super().__init__()
        self.client = client
        self.user = normalize_user(user)
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
        for index, (path, label, _, _) in enumerate(self.NAV):
            allowed = can_visit_route(self.user, path)
            button = QPushButton(label if allowed else f"{label}    无权限")
            button.setObjectName("NavButton")
            button.setCheckable(True)
            button.setEnabled(allowed)
            button.setToolTip(permission_reason(self.user, path))
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
        self.role_tag = QLabel(role_label(str(self.user.get("role") or "")))
        self.role_tag.setObjectName("Tag")
        notify_button = QPushButton("通知")
        notify_button.setToolTip("通知占位，与 Web 端顶部通知按钮对应")
        self.avatar = QLabel(user_initials(self.user))
        self.avatar.setObjectName("Avatar")
        self.avatar.setFixedSize(36, 36)
        self.avatar.setAlignment(Qt.AlignCenter)
        self.avatar.setToolTip(str(self.user.get("nickname") or ""))
        logout_button = QPushButton("退出")
        logout_button.clicked.connect(self.logout)
        topbar_layout.addWidget(self.role_tag)
        topbar_layout.addWidget(self.api_tag)
        topbar_layout.addWidget(notify_button)
        topbar_layout.addWidget(self.avatar)
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
            SettingsPage(client, self.user),
        ]
        for page in self.pages:
            self.stack.addWidget(page)
        dashboard = self.pages[0]
        if isinstance(dashboard, DashboardPage):
            dashboard.open_agent_requested.connect(lambda: self.switch_page(1))
        content = self.pages[3]
        if isinstance(content, ContentPage):
            content.open_agent_requested.connect(lambda: self.switch_page(1))

        main.addWidget(topbar)
        main.addWidget(self.stack, 1)
        root.addWidget(sidebar)
        root.addLayout(main, 1)
        self.switch_page(self.first_allowed_index(), force=True)

    def first_allowed_index(self) -> int:
        for index, (path, _, _, _) in enumerate(self.NAV):
            if can_visit_route(self.user, path):
                return index
        return 1

    def switch_page(self, index: int, force: bool = False) -> None:
        path, _label, title, subtitle = self.NAV[index]
        if not force and not can_visit_route(self.user, path):
            show_error(self, permission_reason(self.user, path))
            return
        self.stack.setCurrentIndex(index)
        self.title.setText(title)
        self.subtitle.setText(f"{self.user.get('workspace') or '保研内容运营工作空间'} · {subtitle}")
        button = self.nav_group.button(index)
        if button:
            button.setChecked(True)
        page = self.pages[index]
        if hasattr(page, "load"):
            page.load()


class DashboardPage(BasePage):
    open_agent_requested = Signal()

    def __init__(self, client: ApiClient):
        super().__init__(client)
        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        refresh = primary_button("刷新")
        refresh.clicked.connect(self.load)
        root.addWidget(PageHeader("Dashboard", "保研运营核心指标、内容选题和申请趋势", [refresh]))

        hero = QFrame()
        hero.setObjectName("DashboardHero")
        hero_layout = QHBoxLayout(hero)
        hero_layout.setContentsMargins(24, 22, 24, 22)
        hero_layout.setSpacing(18)
        hero_text = QVBoxLayout()
        hero_title = QLabel("AI 工作流总览")
        hero_title.setObjectName("HeroTitle")
        hero_subtitle = QLabel("从选题生成、资料沉淀、学员分析到飞书协同，一屏掌握内容运营与保研服务进展。")
        hero_subtitle.setObjectName("Muted")
        hero_subtitle.setWordWrap(True)
        hero_text.addWidget(hero_title)
        hero_text.addWidget(hero_subtitle)
        hero_layout.addLayout(hero_text, 1)
        open_agent = primary_button("打开 AI 工作台")
        open_agent.setMinimumHeight(42)
        open_agent.clicked.connect(self.open_agent_requested.emit)
        hero_layout.addWidget(open_agent)
        root.addWidget(hero)

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
        self.distribution = ChartCard("学员分布", "#5b6cff", "本科院校层次分布", self.load)
        self.content_stats = ChartCard("内容选题类型", "#8b5cf6", "近期内容运营主题分布", self.load)
        self.funnel = ChartCard("申请阶段漏斗", "#19b37b", "学员申请推进情况", self.load)
        self.deadlines = ChartCard("院校截止趋势", "#f59e0b", "项目时间趋势", self.load)
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

    QUICK_ACTIONS = [
        ("创建飞书文档", "创建一个飞书文档，里面写上我喜欢刘昌乐"),
        ("生成选题", "生成 10 个保研小红书选题，并给出爆款结构"),
        ("整理资料", "总结知识库里的夏令营资料，并提取标签"),
        ("分析学员", "分析学员01的申请风险并给出下一步动作"),
        ("推荐院校", "推荐适合经管学生的夏令营项目"),
    ]

    def __init__(self, client: ApiClient):
        super().__init__(client)
        self.current_session_id = ""
        self.current_agent_type = "auto"
        self.message_count = 0
        self.stream_worker: StreamWorker | None = None
        self.assistant_markdown = ""
        self.assistant_bubble: QTextBrowser | None = None
        self.agent_capability_buttons: dict[str, QPushButton] = {}

        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        root.addWidget(PageHeader("AI 工作台", "默认使用 auto，由 AgentRouter 自动选择专业智能体"))
        root.addWidget(self.build_agent_overview())

        splitter = QSplitter()
        left = Card("会话")
        left.setMinimumWidth(210)
        row = QHBoxLayout()
        self.new_button = primary_button("新建")
        self.refresh_button = QPushButton("刷新")
        self.clear_button = QPushButton("清空")
        self.delete_button = danger_button("删除")
        self.stop_button = QPushButton("停止")
        row.addWidget(self.new_button)
        row.addWidget(self.refresh_button)
        row.addWidget(self.clear_button)
        row.addWidget(self.delete_button)
        row.addWidget(self.stop_button)
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
            use_button = QPushButton("使用" if agent_type != "auto" else "默认")
            use_button.setObjectName("PromptChip")
            use_button.setCheckable(True)
            use_button.clicked.connect(lambda checked=False, at=agent_type: self.choose_agent_type(at))
            self.agent_capability_buttons[agent_type] = use_button
            cap_layout.addWidget(use_button)
            left.layout.addWidget(cap)

        center = QSplitter(Qt.Vertical)
        chat_card = Card()
        chat_card.layout.addWidget(SectionHeader("对话", "支持 SSE 流式输出、模型 Thinking 和工具调用过程"))
        agent_row = QHBoxLayout()
        agent_row.addWidget(QLabel("Agent"))
        self.agent_selector = QComboBox()
        for agent_type, name, _desc, _kind in self.AGENT_CAPABILITIES:
            self.agent_selector.addItem(f"{name} · {agent_type}", agent_type)
        self.agent_selector.setCurrentIndex(0)
        self.agent_selector.currentIndexChanged.connect(self.change_agent_type)
        self.session_badge = Badge("新会话", "info")
        agent_row.addWidget(self.agent_selector)
        agent_row.addWidget(self.session_badge)
        agent_row.addStretch(1)
        chat_card.layout.addLayout(agent_row)
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
        center.addWidget(chat_card)
        center.addWidget(trace_card)
        center.setSizes([640, 170])

        context = Card("上下文")
        context.setMinimumWidth(280)
        self.context_tabs = QTabWidget()
        self.context_tabs.addTab(self.build_route_context_tab(), "自动路由")
        self.context_tabs.addTab(self.build_history_context_tab(), "对话历史")
        context.layout.addWidget(self.context_tabs, 1)

        splitter.addWidget(left)
        splitter.addWidget(center)
        splitter.addWidget(context)
        splitter.setSizes([230, 760, 300])
        root.addWidget(splitter, 1)

        self.new_button.clicked.connect(self.create_session)
        self.refresh_button.clicked.connect(self.load)
        self.clear_button.clicked.connect(self.clear_current_history)
        self.delete_button.clicked.connect(self.delete_session)
        self.stop_button.clicked.connect(self.stop_stream)
        self.send_button.clicked.connect(self.send)
        self.session_list.currentItemChanged.connect(self.switch_session)
        self.update_agent_capability_buttons()

    def build_agent_overview(self) -> QWidget:
        overview = QFrame()
        overview.setObjectName("AgentOverview")
        overview_layout = QHBoxLayout(overview)
        overview_layout.setContentsMargins(14, 12, 14, 12)
        overview_layout.setSpacing(16)

        master = QHBoxLayout()
        master.setSpacing(12)
        dot = QLabel()
        dot.setObjectName("AgentMasterDot")
        dot.setFixedSize(14, 14)
        master.addWidget(dot)
        text = QVBoxLayout()
        text.setSpacing(2)
        title = QLabel("FlowMind 总智能体")
        title.setObjectName("SectionTitle")
        title.setStyleSheet("font-size: 15px;")
        desc = QLabel("已启用 5 个专业 Agent，输入后自动判断调用内容、知识库、学员、院校或飞书能力。")
        desc.setObjectName("Muted")
        desc.setWordWrap(True)
        text.addWidget(title)
        text.addWidget(desc)
        master.addLayout(text, 1)
        overview_layout.addLayout(master, 1)

        chips = QHBoxLayout()
        chips.setSpacing(8)
        self.agent_overview_chips: list[Badge] = []
        for agent_type, name, _desc, kind in self.AGENT_CAPABILITIES[1:]:
            chip = Badge(name, kind)
            chip.setToolTip(agent_type)
            self.agent_overview_chips.append(chip)
            chips.addWidget(chip)
        overview_layout.addLayout(chips)
        self.agent_overview = overview
        return overview

    def build_route_context_tab(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        layout.setContentsMargins(8, 8, 8, 8)
        layout.setSpacing(10)
        layout.addWidget(SectionHeader("自动路由", "与 Web 端 ContextPanel 对齐"))
        for agent_type, name, desc, kind in self.AGENT_CAPABILITIES[1:]:
            item = QFrame()
            item.setObjectName("Card")
            item_layout = QHBoxLayout(item)
            item_layout.setContentsMargins(10, 8, 10, 8)
            item_layout.setSpacing(8)
            item_layout.addWidget(Badge(agent_type, kind))
            text = QVBoxLayout()
            title = QLabel(name)
            title.setObjectName("SectionTitle")
            title.setStyleSheet("font-size: 13px;")
            subtitle = QLabel(desc)
            subtitle.setObjectName("Muted")
            subtitle.setWordWrap(True)
            text.addWidget(title)
            text.addWidget(subtitle)
            item_layout.addLayout(text, 1)
            layout.addWidget(item)

        layout.addWidget(SectionHeader("快捷动作", "点击后直接发送到当前会话"))
        for label, prompt in self.QUICK_ACTIONS:
            button = QPushButton(label)
            button.setToolTip(prompt)
            button.clicked.connect(lambda checked=False, text=prompt: self.send_prompt(text))
            layout.addWidget(button)
        layout.addStretch(1)
        return page

    def build_history_context_tab(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        layout.setContentsMargins(8, 8, 8, 8)
        header = QHBoxLayout()
        header.addWidget(QLabel("对话历史"))
        self.history_count_badge = Badge("0 轮", "info")
        header.addWidget(self.history_count_badge)
        header.addStretch(1)
        clear = QPushButton("清空历史")
        clear.clicked.connect(self.clear_current_history)
        header.addWidget(clear)
        layout.addLayout(header)
        self.history_preview_list = QListWidget()
        layout.addWidget(self.history_preview_list, 1)
        return page

    def load(self) -> None:
        self.run_api(self.client.sessions, self.render_sessions, on_fail=lambda _: self.render_sessions([]))

    def change_agent_type(self) -> None:
        selected = self.agent_selector.currentData()
        self.current_agent_type = str(selected or "auto")
        self.update_agent_capability_buttons()

    def choose_agent_type(self, agent_type: str) -> None:
        self.current_agent_type = agent_type or "auto"
        self.set_agent_selector(self.current_agent_type)
        self.update_session_badge()
        self.update_agent_capability_buttons()

    def update_agent_capability_buttons(self) -> None:
        if not hasattr(self, "agent_capability_buttons"):
            return
        for agent_type, button in self.agent_capability_buttons.items():
            active = agent_type == (self.current_agent_type or "auto")
            button.setText("当前" if active else ("使用" if agent_type != "auto" else "默认"))
            button.setChecked(active)
            button.setStyleSheet(
                "QPushButton#PromptChip {"
                f"background: {'#eef2ff' if active else '#ffffff'};"
                f"color: {'#4c5cff' if active else '#475467'};"
                f"border-color: {'#5b6cff' if active else '#dfe5f2'};"
                "}"
            )

    def render_sessions(self, sessions: list[dict[str, Any]]) -> None:
        self.session_list.clear()
        selected_index = -1
        for index, session in enumerate(sessions):
            item = QListWidgetItem()
            item.setData(Qt.UserRole, session)
            item.setToolTip(str(session.get("updatedAt") or session.get("createdAt") or ""))
            self.session_list.addItem(item)
            widget = self.session_item_widget(session)
            item.setSizeHint(widget.sizeHint())
            self.session_list.setItemWidget(item, widget)
            if session.get("id") == self.current_session_id:
                selected_index = index
        if sessions and not self.current_session_id:
            self.session_list.setCurrentRow(0)
        elif sessions and selected_index >= 0:
            self.session_list.setCurrentRow(selected_index)
        elif sessions:
            self.current_session_id = str(sessions[0].get("id") or "")
            self.session_list.setCurrentRow(0)
        elif not sessions:
            self.create_session()

    def create_session(self) -> None:
        self.run_api(self.client.new_session, self.on_new_session)

    def session_item_widget(self, session: dict[str, Any]) -> QWidget:
        widget = QFrame()
        widget.setObjectName("SessionItem")
        widget.setMinimumHeight(58)
        layout = QHBoxLayout(widget)
        layout.setContentsMargins(10, 7, 8, 7)
        layout.setSpacing(6)
        text = QVBoxLayout()
        text.setSpacing(2)
        title = QLabel(str(session.get("title") or "无标题"))
        title.setObjectName("SectionTitle")
        title.setStyleSheet("font-size: 13px;")
        title.setToolTip(str(session.get("title") or "无标题"))
        time_label = QLabel(self.session_time_label(str(session.get("updatedAt") or session.get("createdAt") or "")))
        time_label.setObjectName("Muted")
        time_label.setStyleSheet("font-size: 10px;")
        text.addWidget(title)
        text.addWidget(time_label)
        layout.addLayout(text, 1)
        agent = str(session.get("agentType") or "auto")
        layout.addWidget(Badge(agent, "primary" if agent == "auto" else "info"))
        delete = danger_button("×")
        delete.setFixedWidth(28)
        delete.setToolTip("删除会话")
        delete.clicked.connect(lambda checked=False, payload=session: self.delete_session_payload(payload))
        layout.addWidget(delete)
        return widget

    def session_time_label(self, raw: str) -> str:
        if not raw:
            return ""
        normalized = raw.replace("Z", "+00:00")
        try:
            dt = datetime.fromisoformat(normalized)
            now = datetime.now(dt.tzinfo) if dt.tzinfo else datetime.now()
            diff = now - dt
            seconds = max(0, int(diff.total_seconds()))
            if seconds < 60:
                return "刚刚"
            if seconds < 3600:
                return f"{seconds // 60} 分钟前"
            if seconds < 86400:
                return f"{seconds // 3600} 小时前"
            if seconds < 86400 * 7:
                return f"{seconds // 86400} 天前"
            return dt.strftime("%Y-%m-%d")
        except ValueError:
            return raw

    def on_new_session(self, session_id: str) -> None:
        self.current_session_id = session_id
        self.current_agent_type = "auto"
        self.set_agent_selector("auto")
        self.update_session_badge()
        self.clear_chat()
        self.load()

    def delete_session(self) -> None:
        if not self.current_session_id:
            return
        self.delete_session_payload({"id": self.current_session_id})

    def delete_session_payload(self, session: dict[str, Any]) -> None:
        sid = str(session.get("id") or "")
        if not sid:
            return
        was_current = sid == self.current_session_id
        self.run_api(
            lambda: self.client.delete_session(sid),
            lambda _: self.after_delete_session(sid, was_current),
        )

    def after_delete_session(self, session_id: str, was_current: bool) -> None:
        if was_current:
            self.clear_chat()
            self.current_session_id = ""
            self.current_agent_type = "auto"
            self.set_agent_selector("auto")
            self.update_session_badge()
        self.load()

    def clear_current_history(self) -> None:
        if not self.current_session_id:
            return
        sid = self.current_session_id
        agent_type = self.current_agent_type or "auto"
        self.run_api(
            lambda: self.client.clear_history(agent_type, sid),
            lambda _: (self.clear_chat(), self.render_history_preview([]), self.add_message("assistant", "当前会话历史已清空。")),
        )

    def stop_stream(self) -> None:
        if self.stream_worker:
            self.stream_worker.stop()
            self.stream_worker = None
        self.send_button.setEnabled(True)
        self.thinking_output.appendPlainText("已停止本次流式输出。")

    def switch_session(self, current: QListWidgetItem | None, previous: QListWidgetItem | None) -> None:
        if not current:
            return
        session = current.data(Qt.UserRole) or {}
        sid = session.get("id")
        if not sid or sid == self.current_session_id:
            return
        self.current_session_id = str(sid)
        self.current_agent_type = str(session.get("agentType") or "auto")
        self.set_agent_selector(self.current_agent_type)
        self.update_session_badge(session)
        self.load_history()

    def load_history(self) -> None:
        sid = self.current_session_id
        agent_type = self.current_agent_type or "auto"
        self.run_api(lambda: self.client.history(agent_type, sid), self.render_history, on_fail=lambda _: self.render_history([]))

    def render_history(self, rows: list[dict[str, Any]]) -> None:
        self.clear_chat()
        self.render_history_preview(rows)
        if not rows:
            self.add_message("assistant", "新会话已创建。直接输入你的任务，我会自动选择合适的 Agent，并展示工具调用过程。")
            return
        for row in rows:
            meta = row.get("metadata")
            parsed = self.parse_metadata(meta) if meta else {}
            self.add_message(row.get("role", "assistant"), row.get("content", ""), parsed)
            if meta:
                self.render_metadata(parsed)

    def clear_chat(self) -> None:
        while self.chat_layout.count():
            item = self.chat_layout.takeAt(0)
            widget = item.widget()
            if widget:
                widget.deleteLater()
        self.message_count = 0
        self.chat_layout.addStretch(1)
        self.thinking_output.clear()
        self.trace_output.clear()
        self.trace_panel.set_items([])
        self.reasoning_output.clear()
        self.update_prompt_visibility()

    def render_history_preview(self, rows: list[dict[str, Any]]) -> None:
        if not hasattr(self, "history_preview_list"):
            return
        self.history_preview_list.clear()
        user_turns = sum(1 for row in rows if row.get("role") == "user")
        self.history_count_badge.setText(f"{user_turns} 轮")
        if not rows:
            item = QListWidgetItem("暂无对话记录")
            item.setFlags(Qt.NoItemFlags)
            self.history_preview_list.addItem(item)
            return
        for row in rows:
            role = row.get("role") or "assistant"
            prefix = "U" if role == "user" else "AI"
            content = str(row.get("content") or "").replace("\n", " ").strip()
            if len(content) > 64:
                content = f"{content[:64]}..."
            item = QListWidgetItem(f"{prefix}  {content}")
            item.setToolTip(str(row.get("content") or ""))
            self.history_preview_list.addItem(item)

    def set_agent_selector(self, agent_type: str) -> None:
        index = self.agent_selector.findData(agent_type)
        if index < 0:
            index = 0
        self.agent_selector.blockSignals(True)
        self.agent_selector.setCurrentIndex(index)
        self.agent_selector.blockSignals(False)
        self.update_agent_capability_buttons()

    def update_session_badge(self, session: dict[str, Any] | None = None) -> None:
        if not hasattr(self, "session_badge"):
            return
        if not self.current_session_id:
            self.session_badge.setText("新会话")
            self.session_badge.set_kind("info")
            return
        agent_type = self.current_agent_type or str((session or {}).get("agentType") or "auto")
        short_id = self.current_session_id[:8]
        self.session_badge.setText(f"{agent_type} · {short_id}")
        self.session_badge.set_kind("primary")

    def update_prompt_visibility(self) -> None:
        if hasattr(self, "prompts_widget"):
            self.prompts_widget.setVisible(self.message_count <= 1)

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

    def normalized_thinking_history(self, meta: dict[str, Any]) -> list[str]:
        lines = meta.get("thinkingHistory") or []
        if not lines and meta.get("thinking"):
            lines = [meta.get("thinking")]
        result: list[str] = []
        seen: set[str] = set()
        if isinstance(lines, list):
            for line in lines:
                text = str(line or "").strip()
                if text and text not in seen:
                    seen.add(text)
                    result.append(text)
        return result

    def inline_panel(self, title: str) -> QFrame:
        panel = QFrame()
        panel.setObjectName("InlinePanel")
        panel_layout = QVBoxLayout(panel)
        panel_layout.setContentsMargins(10, 8, 10, 8)
        panel_layout.setSpacing(6)
        title_label = QLabel(title)
        title_label.setObjectName("InlinePanelTitle")
        panel_layout.addWidget(title_label)
        return panel

    def add_inline_agent_panels(self, layout: QVBoxLayout, meta: dict[str, Any]) -> None:
        if not meta:
            return
        model_thinking = str(meta.get("modelThinking") or "").strip()
        if model_thinking:
            panel = self.inline_panel("模型 Thinking")
            content = QPlainTextEdit()
            content.setReadOnly(True)
            content.setMaximumHeight(120)
            content.setPlainText(model_thinking)
            panel.layout().addWidget(content)
            layout.addWidget(panel)

        thinking_lines = self.normalized_thinking_history(meta)
        if thinking_lines:
            panel = self.inline_panel("处理过程")
            steps = QLabel("\n".join(f"{index}. {line}" for index, line in enumerate(thinking_lines, start=1)))
            steps.setObjectName("Muted")
            steps.setWordWrap(True)
            panel.layout().addWidget(steps)
            layout.addWidget(panel)

        trace_items = meta.get("traceItems") or []
        if isinstance(trace_items, list):
            visible = [item for item in trace_items if isinstance(item, dict) and item.get("status") != "skipped"]
            if visible:
                panel = self.inline_panel("工具调用过程")
                trace_panel = TraceListPanel()
                trace_panel.setMaximumHeight(260)
                trace_panel.set_items(visible)
                panel.layout().addWidget(trace_panel)
                layout.addWidget(panel)

    def add_message(self, role: str, content: str, meta: dict[str, Any] | None = None) -> QTextBrowser:
        if self.chat_layout.count() and self.chat_layout.itemAt(self.chat_layout.count() - 1).spacerItem():
            self.chat_layout.takeAt(self.chat_layout.count() - 1)
        wrapper = QWidget()
        wrapper_layout = QHBoxLayout(wrapper)
        wrapper_layout.setContentsMargins(0, 4, 0, 4)
        frame = QFrame()
        frame.setObjectName("Card")
        is_user = role == "user"
        frame.setMaximumWidth(780)
        frame.setStyleSheet(
            "QFrame#Card {"
            f"background: {'#eef2ff' if is_user else '#ffffff'};"
            f"border-color: {'#c7d2fe' if is_user else '#e7ebf3'};"
            "}"
        )
        layout = QVBoxLayout(frame)
        layout.setContentsMargins(14, 10, 14, 10)
        header = QHBoxLayout()
        name = Badge("你" if is_user else "FlowMind Agent", "primary" if is_user else "success")
        header.addWidget(name)
        header.addStretch(1)
        layout.addLayout(header)
        if not is_user and meta:
            self.add_inline_agent_panels(layout, meta)
        body = QTextBrowser()
        body.setOpenExternalLinks(True)
        body.setMarkdown(content or "")
        body.setMinimumHeight(54)
        body.setSizePolicy(QSizePolicy.Expanding, QSizePolicy.Minimum)
        layout.addWidget(body)
        if is_user:
            wrapper_layout.addStretch(1)
            wrapper_layout.addWidget(frame)
        else:
            wrapper_layout.addWidget(frame)
            wrapper_layout.addStretch(1)
        self.chat_layout.addWidget(wrapper)
        self.chat_layout.addStretch(1)
        self.message_count += 1
        self.update_prompt_visibility()
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
        payload = {"agentType": self.current_agent_type or "auto", "message": text, "sessionId": self.current_session_id, "context": {}}
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
            self.update_session_badge()

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
        self.load_history()

    def on_stream_fail(self, message: str) -> None:
        self.send_button.setEnabled(True)
        self.trace_output.appendPlainText(f"[error] {message}")


class KnowledgePage(BasePage):
    def __init__(self, client: ApiClient):
        super().__init__(client)
        self.docs: list[dict[str, Any]] = []
        self.filter_tag = ""
        self.current_doc_id: int | None = None
        self.current_doc: dict[str, Any] | None = None
        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        refresh = primary_button("刷新")
        refresh.clicked.connect(self.load)
        sync = QPushButton("同步飞书")
        sync.clicked.connect(self.sync)
        self.sync_button = sync
        root.addWidget(PageHeader("知识库", "资料分类、标签、摘要和语义检索", [sync, refresh]))

        stat_grid = QGridLayout()
        self.knowledge_doc_card = StatCard("文档", "0", "0 种类型")
        self.knowledge_tag_card = StatCard("标签", "0", "可点击筛选")
        self.knowledge_sync_card = StatCard("已同步", "0", "待同步")
        self.knowledge_source_card = StatCard("来源", "飞书", "保研知识库")
        for index, card in enumerate(
            [self.knowledge_doc_card, self.knowledge_tag_card, self.knowledge_sync_card, self.knowledge_source_card]
        ):
            stat_grid.addWidget(card, 0, index)
        root.addLayout(stat_grid)

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

        tag_filter_card = Card()
        tag_filter_card.layout.addWidget(SectionHeader("标签筛选", "点击标签筛选文档，再次点击可取消"))
        self.tag_filter_layout = QHBoxLayout()
        tag_filter_card.layout.addLayout(self.tag_filter_layout)
        root.addWidget(tag_filter_card)

        splitter = QSplitter()
        doc_tabs = QTabWidget()
        doc_cards_page = QWidget()
        doc_cards_layout = QVBoxLayout(doc_cards_page)
        doc_cards_layout.setContentsMargins(0, 0, 0, 0)
        doc_cards_layout.addWidget(SectionHeader("文档卡片", "点击卡片打开详情弹窗，贴近 Web 端知识库列表"))
        doc_card_scroll = QScrollArea()
        doc_card_scroll.setWidgetResizable(True)
        self.doc_card_host = QWidget()
        self.doc_card_grid = QGridLayout(self.doc_card_host)
        self.doc_card_grid.setSpacing(12)
        doc_card_scroll.setWidget(self.doc_card_host)
        doc_cards_layout.addWidget(doc_card_scroll, 1)
        self.table = DataTable(["ID", "标题", "标签", "来源", "更新时间"])
        doc_tabs.addTab(doc_cards_page, "卡片")
        doc_tabs.addTab(self.table, "表格")
        detail_tabs = QTabWidget()
        detail_card = Card("文档详情")
        self.doc_title = QLabel("请选择左侧文档")
        self.doc_title.setObjectName("SectionTitle")
        self.doc_title.setWordWrap(True)
        self.doc_tags = InfoRow("标签", "-")
        self.doc_source = InfoRow("来源", "-")
        self.doc_url = InfoRow("飞书", "-")
        self.doc_tag_edit = QLineEdit()
        self.doc_tag_edit.setPlaceholderText("用逗号、顿号或中文逗号分隔标签")
        save_tags = primary_button("保存标签")
        save_tags.clicked.connect(self.save_doc_tags)
        self.doc_summary = MarkdownPanel()
        self.doc_summary.setMinimumHeight(180)
        self.detail = JsonPanel()
        detail_card.layout.addWidget(self.doc_title)
        detail_card.layout.addWidget(self.doc_tags)
        detail_card.layout.addWidget(self.doc_source)
        detail_card.layout.addWidget(self.doc_url)
        doc_actions = QHBoxLayout()
        detail_dialog = QPushButton("详情弹窗")
        copy_summary = QPushButton("复制摘要")
        copy_link = QPushButton("复制链接")
        open_link = primary_button("打开飞书")
        detail_dialog.clicked.connect(self.open_doc_dialog)
        copy_summary.clicked.connect(self.copy_selected_doc_summary)
        copy_link.clicked.connect(self.copy_selected_doc_link)
        open_link.clicked.connect(self.open_selected_doc_link)
        doc_actions.addStretch(1)
        doc_actions.addWidget(detail_dialog)
        doc_actions.addWidget(copy_summary)
        doc_actions.addWidget(copy_link)
        doc_actions.addWidget(open_link)
        detail_card.layout.addLayout(doc_actions)
        detail_card.layout.addWidget(QLabel("编辑标签"))
        detail_card.layout.addWidget(self.doc_tag_edit)
        detail_card.layout.addWidget(save_tags)
        detail_card.layout.addWidget(QLabel("摘要 / 正文片段"))
        detail_card.layout.addWidget(self.doc_summary, 1)
        detail_tabs.addTab(detail_card, "文档")
        detail_tabs.addTab(self.detail, "原始数据")
        vector_tabs = QTabWidget()
        vector_scroll = QScrollArea()
        vector_scroll.setWidgetResizable(True)
        self.vector_card_host = QWidget()
        self.vector_card_layout = QVBoxLayout(self.vector_card_host)
        self.vector_card_layout.setContentsMargins(0, 0, 0, 0)
        self.vector_card_layout.setSpacing(12)
        self.vector_card_layout.addWidget(EmptyState("输入问题后点击向量检索"))
        vector_scroll.setWidget(self.vector_card_host)
        self.vector_output = JsonPanel()
        vector_tabs.addTab(vector_scroll, "结果卡片")
        vector_tabs.addTab(self.vector_output, "原始 JSON")
        detail_tabs.addTab(vector_tabs, "向量检索")
        sync_page = QWidget()
        sync_layout = QVBoxLayout(sync_page)
        sync_layout.setContentsMargins(0, 0, 0, 0)
        self.knowledge_sync_summary = QLabel("点击“同步飞书”后展示同步结果。")
        self.knowledge_sync_summary.setObjectName("Muted")
        self.knowledge_sync_output = JsonPanel()
        self.knowledge_sync_log_table = DataTable(["ID", "类型", "状态", "消息", "新增", "更新", "跳过", "错误", "时间"])
        sync_layout.addWidget(QLabel("同步状态"))
        sync_layout.addWidget(self.knowledge_sync_summary)
        sync_layout.addWidget(self.knowledge_sync_output, 1)
        sync_layout.addWidget(QLabel("同步日志"))
        sync_layout.addWidget(self.knowledge_sync_log_table, 1)
        detail_tabs.addTab(sync_page, "同步状态")
        splitter.addWidget(doc_tabs)
        splitter.addWidget(detail_tabs)
        splitter.setSizes([760, 420])
        root.addWidget(splitter, 1)

        self.search_button.clicked.connect(self.load)
        self.vector_button.clicked.connect(self.vector_search)
        self.table.itemSelectionChanged.connect(self.show_selected)

    def load(self) -> None:
        keyword = self.keyword.text().strip()
        self.run_api(lambda: self.client.knowledge_docs(keyword), self.render_docs)
        self.run_api(self.client.knowledge_sync_status, self.render_sync_status, on_fail=lambda _: None)
        self.run_api(self.client.knowledge_sync_logs, self.render_sync_logs, on_fail=lambda _: None)

    def render_docs(self, rows: list[dict[str, Any]]) -> None:
        self.docs = rows
        self.render_tag_filters()
        self.update_knowledge_stats()
        self.render_doc_table()
        self.render_doc_cards()

    def render_doc_table(self) -> None:
        normalized = []
        rows = self.filtered_docs()
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

    def render_doc_cards(self) -> None:
        clear_layout(self.doc_card_grid)
        rows = self.filtered_docs()
        if not rows:
            empty_text = "尚未同步飞书文档，点击上方“同步飞书”同步" if not self.docs else "没有匹配的文档，尝试清除筛选或更换关键词"
            self.doc_card_grid.addWidget(EmptyState(empty_text), 0, 0)
            return
        for index, doc in enumerate(rows):
            title = str(value(doc, "title", default="未命名文档"))
            summary = str(value(doc, "summary", default="点击同步飞书后会自动生成 AI 摘要。"))
            if len(summary) > 120:
                summary = f"{summary[:120]}..."
            updated = str(value(doc, "updatedAt", "createdAt", default=""))
            meta = f"{self.type_label(str(value(doc, 'feishuType', 'source', default='file')))} · {updated}"
            card = TextCard(title, summary, meta, doc)
            type_row = QHBoxLayout()
            doc_type = str(value(doc, "feishuType", "source", default="file"))
            type_row.addWidget(Badge(self.type_label(doc_type), self.type_kind(doc_type)))
            type_row.addStretch(1)
            card.layout.insertLayout(0, type_row)
            tags = self.doc_tags_list(doc)
            if tags:
                tag_row = QHBoxLayout()
                tag_row.setSpacing(6)
                for tag in tags[:5]:
                    tag_row.addWidget(Badge(tag, "info"))
                tag_row.addStretch(1)
                card.layout.addLayout(tag_row)
            card.clicked.connect(self.open_doc_card)
            self.doc_card_grid.addWidget(card, index // 3, index % 3)

    def filtered_docs(self) -> list[dict[str, Any]]:
        if not self.filter_tag:
            return self.docs
        return [row for row in self.docs if self.filter_tag in self.doc_tags_list(row)]

    def doc_tags_list(self, row: dict[str, Any]) -> list[str]:
        tags = value(row, "tags", default=[])
        if isinstance(tags, list):
            return [str(tag) for tag in tags if str(tag).strip()]
        if isinstance(tags, str):
            normalized = tags.replace("，", ",").replace("、", ",")
            return [item.strip() for item in normalized.split(",") if item.strip()]
        return []

    def render_tag_filters(self) -> None:
        clear_layout(self.tag_filter_layout)
        tags = sorted({tag for row in self.docs for tag in self.doc_tags_list(row)})
        if not tags:
            self.tag_filter_layout.addWidget(EmptyState("暂无标签"))
            return
        all_button = QPushButton("全部")
        all_button.setObjectName("PromptChip")
        all_button.clicked.connect(lambda: self.set_filter_tag(""))
        self.tag_filter_layout.addWidget(all_button)
        for tag in tags:
            button = QPushButton(tag)
            button.setObjectName("PromptChip")
            if self.filter_tag == tag:
                button.setStyleSheet("background:#eef2ff;color:#4c5cff;border:1px solid #5b6cff;border-radius:8px;padding:6px 10px;")
            button.clicked.connect(lambda checked=False, t=tag: self.set_filter_tag("" if self.filter_tag == t else t))
            self.tag_filter_layout.addWidget(button)
        self.tag_filter_layout.addStretch(1)

    def set_filter_tag(self, tag: str) -> None:
        self.filter_tag = tag
        self.render_tag_filters()
        self.render_doc_table()
        self.render_doc_cards()
        self.update_knowledge_stats()

    def type_label(self, doc_type: str) -> str:
        mapping = {
            "docx": "飞书文档",
            "doc": "文档",
            "sheet": "电子表格",
            "bitable": "多维表格",
            "pdf": "PDF",
            "file": "文件",
            "folder": "文件夹",
        }
        return mapping.get(doc_type, doc_type or "文件")

    def type_kind(self, doc_type: str) -> str:
        if doc_type in ("docx", "doc"):
            return "primary"
        if doc_type in ("sheet", "bitable"):
            return "success"
        if doc_type == "pdf":
            return "danger"
        if doc_type == "folder":
            return "warning"
        return "info"

    def update_knowledge_stats(self) -> None:
        tags = sorted({tag for row in self.docs for tag in self.doc_tags_list(row)})
        types = sorted({str(value(row, "feishuType", "source", default="")) for row in self.docs if value(row, "feishuType", "source", default="")})
        self.knowledge_doc_card.set_value(len(self.filtered_docs()), f"{len(types)} 种类型")
        self.knowledge_tag_card.set_value(len(tags), f"当前筛选：{self.filter_tag or '全部'}")
        self.knowledge_sync_card.set_value(len(self.docs), "最近同步状态见右侧页签")

    def show_selected(self) -> None:
        raw = self.selected_doc_raw()
        self.render_doc_detail(raw)

    def render_doc_detail(self, raw: dict[str, Any] | None) -> None:
        if raw:
            self.current_doc = raw
            self.current_doc_id = int(value(raw, "id", default=0) or 0)
            self.doc_title.setText(str(value(raw, "title", default="未命名文档")))
            tags = self.doc_tags_list(raw)
            self.doc_tags.set_value("、".join(tags))
            self.doc_tag_edit.setText("、".join(tags))
            self.doc_source.set_value(value(raw, "source", "feishuType", default="-"))
            self.doc_url.set_value(value(raw, "feishuUrl", default="-"))
            self.doc_summary.set_text(str(value(raw, "summary", "content", default="")))
            self.detail.set_json(raw)

    def open_doc_card(self, payload: object) -> None:
        if not isinstance(payload, dict):
            return
        self.render_doc_detail(payload)
        dialog = KnowledgeDocDialog(self, payload, self.doc_tags_list(payload))
        if dialog.exec() == QDialog.Accepted and self.current_doc_id:
            self.run_api(lambda: self.client.update_doc_tags(self.current_doc_id or 0, dialog.tags()), lambda _: self.load())

    def selected_doc_raw(self) -> dict[str, Any] | None:
        if self.current_doc:
            return self.current_doc
        row = self.table.selected_row_data()
        if row:
            raw = row.get("_raw") or row
            return raw if isinstance(raw, dict) else None
        if self.current_doc_id:
            current = next(
                (
                    doc
                    for doc in self.docs
                    if int(value(doc, "id", default=0) or 0) == self.current_doc_id
                ),
                None,
            )
            if current:
                return current
        return None

    def copy_selected_doc_summary(self) -> None:
        raw = self.selected_doc_raw()
        copy_text(self, str(value(raw or {}, "summary", "content", default="")), "文档摘要")

    def copy_selected_doc_link(self) -> None:
        raw = self.selected_doc_raw()
        copy_text(self, str(value(raw or {}, "feishuUrl", default="")), "飞书链接")

    def open_selected_doc_link(self) -> None:
        raw = self.selected_doc_raw()
        open_url(self, str(value(raw or {}, "feishuUrl", default="")))

    def open_doc_dialog(self) -> None:
        raw = self.selected_doc_raw()
        if not raw:
            show_error(self, "请先选择一篇知识库文档")
            return
        dialog = KnowledgeDocDialog(self, raw, self.doc_tags_list(raw))
        if dialog.exec() == QDialog.Accepted and self.current_doc_id:
            self.run_api(lambda: self.client.update_doc_tags(self.current_doc_id or 0, dialog.tags()), lambda _: self.load())

    def save_doc_tags(self) -> None:
        if not self.current_doc_id:
            show_error(self, "请先选择一篇知识库文档")
            return
        raw_text = self.doc_tag_edit.text().replace("，", ",").replace("、", ",")
        tags = [item.strip() for item in raw_text.split(",") if item.strip()]
        self.run_api(lambda: self.client.update_doc_tags(self.current_doc_id or 0, tags), lambda _: self.load())

    def vector_search(self) -> None:
        query = self.vector_query.text().strip()
        if not query:
            return
        self.run_api(lambda: self.client.vector_search(query, 5), self.render_vector_results)

    def render_vector_results(self, rows: list[dict[str, Any]]) -> None:
        self.vector_output.set_json(rows)
        clear_layout(self.vector_card_layout)
        if not rows:
            self.vector_card_layout.addWidget(EmptyState("暂无向量检索结果"))
            self.vector_card_layout.addStretch(1)
            return
        for row in rows:
            title = str(value(row, "title", default="未命名片段"))
            chunk = str(value(row, "chunkText", "content", "summary", default=""))
            meta = f"来源 {value(row, 'source', default='-')} / 距离 {value(row, 'distance', default='-')}"
            card = TextCard(title, chunk, meta, row)
            badge_row = QHBoxLayout()
            badge_row.addWidget(Badge(str(value(row, "source", default="vector")), "primary"))
            if value(row, "feishuType", default=""):
                badge_row.addWidget(Badge(str(value(row, "feishuType")), "info"))
            tags = value(row, "tags", default=[])
            if isinstance(tags, list):
                for tag in tags[:4]:
                    badge_row.addWidget(Badge(str(tag), "success"))
            badge_row.addStretch(1)
            card.layout.insertLayout(0, badge_row)
            url = str(value(row, "feishuUrl", default=""))
            if url:
                card.layout.addWidget(InfoRow("飞书链接", url))
            actions = QHBoxLayout()
            copy_chunk = QPushButton("复制片段")
            copy_chunk.clicked.connect(lambda checked=False, text=chunk: copy_text(self, text, "向量片段"))
            actions.addStretch(1)
            actions.addWidget(copy_chunk)
            if url:
                open_link = primary_button("打开飞书")
                open_link.clicked.connect(lambda checked=False, link=url: open_url(self, link))
                actions.addWidget(open_link)
            card.layout.addLayout(actions)
            self.vector_card_layout.addWidget(card)
        self.vector_card_layout.addStretch(1)

    def set_syncing(self, syncing: bool) -> None:
        self.sync_button.setEnabled(not syncing)
        self.sync_button.setText("同步中..." if syncing else "同步飞书")

    def sync(self) -> None:
        self.set_syncing(True)
        self.knowledge_sync_summary.setText("正在从飞书同步知识库文档，请稍候。")
        self.knowledge_sync_output.setPlainText("同步中...")
        self.run_api(self.client.sync_knowledge, self.after_sync, on_fail=self.after_sync_fail)

    def after_sync(self, result: Any) -> None:
        self.set_syncing(False)
        self.knowledge_sync_output.set_json(result)
        if isinstance(result, dict):
            added = value(result, "added", default=0)
            updated = value(result, "updated", default=0)
            skipped = value(result, "skipped", default=0)
            errors = value(result, "errors", default=0)
            message = value(result, "message", default="同步完成")
            self.knowledge_sync_summary.setText(f"{message}：新增 {added}，更新 {updated}，跳过 {skipped}，错误 {errors}。")
        else:
            self.knowledge_sync_summary.setText("同步完成，已刷新知识库列表。")
        self.load()

    def after_sync_fail(self, message: str) -> None:
        self.set_syncing(False)
        self.knowledge_sync_summary.setText(f"同步失败：{message}")
        show_error(self, f"同步失败：{message}")

    def render_sync_status(self, result: dict[str, Any]) -> None:
        self.knowledge_sync_output.set_json(result)
        docs = result.get("docs", {}) if isinstance(result, dict) else {}
        if isinstance(docs, dict):
            status = docs.get("status") or "待同步"
            count = docs.get("count") or len(self.docs)
            last_sync = docs.get("lastSync") or "待同步"
            self.knowledge_sync_card.set_value(count, f"{status} · {last_sync}")
            self.knowledge_sync_summary.setText(f"最近同步：{status} · {last_sync}，当前文档 {count} 篇。")

    def render_sync_logs(self, rows: list[dict[str, Any]]) -> None:
        normalized = [
            {
                "id": value(row, "id"),
                "syncType": value(row, "syncType", "type"),
                "status": value(row, "status"),
                "message": value(row, "message"),
                "added": value(row, "added", default=0),
                "updated": value(row, "updated", default=0),
                "skipped": value(row, "skipped", default=0),
                "errors": value(row, "errors", default=0),
                "createdAt": value(row, "createdAt"),
            }
            for row in rows
        ]
        self.knowledge_sync_log_table.set_rows(
            normalized,
            ["id", "syncType", "status", "message", "added", "updated", "skipped", "errors", "createdAt"],
        )


class KnowledgeDocDialog(QDialog):
    def __init__(self, parent: QWidget, doc: dict[str, Any], tags: list[str]):
        super().__init__(parent)
        self.doc = doc
        self.setWindowTitle(str(value(doc, "title", default="文档详情")))
        self.resize(860, 680)
        layout = QVBoxLayout(self)
        header = QHBoxLayout()
        title = QLabel(str(value(doc, "title", default="未命名文档")))
        title.setObjectName("PageTitle")
        title.setWordWrap(True)
        header.addWidget(title, 1)
        header.addWidget(Badge(str(value(doc, "feishuType", "source", default="file")), "primary"))
        layout.addLayout(header)

        meta = QHBoxLayout()
        meta.addWidget(Badge(str(value(doc, "updatedAt", "createdAt", default="未同步")), "info"))
        meta.addStretch(1)
        copy_summary = QPushButton("复制摘要")
        copy_link = QPushButton("复制链接")
        open_link = primary_button("打开飞书")
        copy_summary.clicked.connect(lambda: copy_text(self, str(value(self.doc, "summary", "content", default="")), "文档摘要"))
        copy_link.clicked.connect(lambda: copy_text(self, str(value(self.doc, "feishuUrl", default="")), "飞书链接"))
        open_link.clicked.connect(lambda: open_url(self, str(value(self.doc, "feishuUrl", default=""))))
        meta.addWidget(copy_summary)
        meta.addWidget(copy_link)
        meta.addWidget(open_link)
        layout.addLayout(meta)

        tabs = QTabWidget()
        content_page = QWidget()
        content_layout = QVBoxLayout(content_page)
        content_layout.addWidget(InfoRow("来源", str(value(doc, "source", "feishuType", default="-"))))
        content_layout.addWidget(InfoRow("飞书链接", str(value(doc, "feishuUrl", default="-"))))
        self.tag_list = QListWidget()
        self.tag_list.setMaximumHeight(96)
        for tag in tags:
            self.tag_list.addItem(QListWidgetItem(str(tag)))
        tag_tools = QHBoxLayout()
        self.new_tag = QLineEdit()
        self.new_tag.setPlaceholderText("输入新标签")
        add_tag = QPushButton("+ 添加")
        remove_tag = danger_button("删除选中")
        add_tag.clicked.connect(self.add_tag)
        remove_tag.clicked.connect(self.remove_selected_tag)
        self.new_tag.returnPressed.connect(self.add_tag)
        tag_tools.addWidget(self.new_tag, 1)
        tag_tools.addWidget(add_tag)
        tag_tools.addWidget(remove_tag)
        self.tag_edit = QLineEdit("、".join(tags))
        self.tag_edit.setPlaceholderText("用逗号、顿号或中文逗号分隔标签")
        content_layout.addWidget(QLabel("标签"))
        content_layout.addWidget(self.tag_list)
        content_layout.addLayout(tag_tools)
        content_layout.addWidget(QLabel("标签文本"))
        content_layout.addWidget(self.tag_edit)
        preview = MarkdownPanel()
        preview.setMinimumHeight(360)
        preview.set_text(str(value(doc, "summary", "content", default="")))
        content_layout.addWidget(QLabel("摘要 / 正文预览"))
        content_layout.addWidget(preview, 1)
        raw_page = JsonPanel()
        raw_page.set_json(doc)
        tabs.addTab(content_page, "文档")
        tabs.addTab(raw_page, "原始数据")
        layout.addWidget(tabs, 1)

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Close)
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def add_tag(self) -> None:
        tag = self.new_tag.text().strip()
        if not tag:
            return
        existing = {self.tag_list.item(index).text() for index in range(self.tag_list.count())}
        if tag not in existing:
            self.tag_list.addItem(QListWidgetItem(tag))
        self.new_tag.clear()
        self.sync_tag_edit()

    def remove_selected_tag(self) -> None:
        for item in self.tag_list.selectedItems():
            row = self.tag_list.row(item)
            self.tag_list.takeItem(row)
        self.sync_tag_edit()

    def sync_tag_edit(self) -> None:
        self.tag_edit.setText("、".join(self.tags_from_list()))

    def tags_from_list(self) -> list[str]:
        return [
            self.tag_list.item(index).text().strip()
            for index in range(self.tag_list.count())
            if self.tag_list.item(index).text().strip()
        ]

    def tags(self) -> list[str]:
        listed = self.tags_from_list()
        raw = self.tag_edit.text().replace("，", ",").replace("、", ",")
        typed = [item.strip() for item in raw.split(",") if item.strip()]
        result: list[str] = []
        for tag in [*listed, *typed]:
            if tag not in result:
                result.append(tag)
        return result


class ContentPage(BasePage):
    open_agent_requested = Signal()

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
        agent_hint = QPushButton("去 AI 工作台生成")
        agent_hint.clicked.connect(self.open_agent_requested.emit)
        self.agent_hint_button = agent_hint
        root.addWidget(PageHeader("内容运营", "主题库、文案库、内容日历和 SOP 生成", [agent_hint, refresh]))

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
        theme_action_row = QHBoxLayout()
        view_theme_detail = QPushButton("查看完整详情")
        copy_theme_summary = QPushButton("复制摘要")
        view_theme_detail.clicked.connect(self.open_theme_detail_dialog)
        copy_theme_summary.clicked.connect(self.copy_selected_theme_summary)
        theme_action_row.addStretch(1)
        theme_action_row.addWidget(copy_theme_summary)
        theme_action_row.addWidget(view_theme_detail)
        detail.layout.addWidget(self.theme_title_label)
        detail.layout.addWidget(self.theme_rating)
        detail.layout.addWidget(self.theme_summary_label)
        detail.layout.addWidget(self.theme_topic_row)
        detail.layout.addWidget(self.theme_heat_row)
        detail.layout.addWidget(self.theme_date_row)
        detail.layout.addWidget(self.theme_tags_row)
        detail.layout.addLayout(theme_action_row)
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
        self.theme_table.itemDoubleClicked.connect(lambda _: self.open_theme_detail_dialog())
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
        draft_action_row = QHBoxLayout()
        view_draft_detail = QPushButton("查看完整详情")
        copy_draft_content = QPushButton("复制正文")
        view_draft_detail.clicked.connect(self.open_draft_detail_dialog)
        copy_draft_content.clicked.connect(self.copy_selected_draft_content)
        draft_action_row.addStretch(1)
        draft_action_row.addWidget(copy_draft_content)
        draft_action_row.addWidget(view_draft_detail)
        editor_card.layout.addLayout(draft_action_row)
        image_reference_row = QHBoxLayout()
        self.draft_image_count = Badge("图片 0", "info")
        add_image = QPushButton("添加图片引用")
        copy_image = QPushButton("复制图片 URL")
        open_image = primary_button("打开图片")
        add_image.clicked.connect(self.add_draft_image)
        copy_image.clicked.connect(self.copy_selected_draft_image)
        open_image.clicked.connect(self.open_selected_draft_image)
        image_reference_row.addWidget(self.draft_image_count)
        image_reference_row.addStretch(1)
        image_reference_row.addWidget(copy_image)
        image_reference_row.addWidget(open_image)
        image_reference_row.addWidget(add_image)
        editor_card.layout.addLayout(image_reference_row)
        self.draft_image_table = DataTable(["ID", "名称", "URL", "存储"])
        self.draft_image_table.setMaximumHeight(120)
        editor_card.layout.addWidget(self.draft_image_table)
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
        self.draft_table.itemDoubleClicked.connect(lambda _: self.open_draft_detail_dialog())
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
        self.sop_tabs = QTabWidget()
        sop_structured_page = QScrollArea()
        sop_structured_page.setWidgetResizable(True)
        self.sop_card_host = QWidget()
        self.sop_card_layout = QVBoxLayout(self.sop_card_host)
        self.sop_card_layout.setContentsMargins(0, 0, 0, 0)
        self.sop_card_layout.setSpacing(12)
        self.sop_card_layout.addWidget(EmptyState("生成 SOP 后展示结构化结果"))
        sop_structured_page.setWidget(self.sop_card_host)
        self.sop_output = JsonPanel()
        self.sop_tabs.addTab(sop_structured_page, "结构化结果")
        self.sop_tabs.addTab(self.sop_output, "原始 JSON")
        layout.addWidget(self.sop_tabs, 1)
        generate.clicked.connect(self.generate_sop)
        architecture.clicked.connect(lambda: self.run_api(self.client.content_architecture, self.render_sop_result))
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
            delete_button = danger_button("删除")
            delete_button.clicked.connect(lambda checked=False, payload=theme: self.delete_theme_payload(payload))
            top.addWidget(delete_button)
            card.layout.insertLayout(0, top)
            card.clicked.connect(self.open_theme_card)
            self.theme_card_grid.addWidget(card, index // 2, index % 2)

    def selected_theme(self) -> dict[str, Any] | None:
        if self.current_theme_id:
            current = next(
                (
                    theme
                    for theme in self.themes
                    if int(value(theme, "id", default=0) or 0) == self.current_theme_id
                ),
                None,
            )
            if current:
                return current
        row = self.theme_table.selected_row_data()
        if row:
            return row.get("_raw") or row
        return None

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
        self.delete_theme_payload(row.get("_raw") or row)

    def delete_theme_payload(self, theme: dict[str, Any]) -> None:
        theme_id = int(value(theme, "id", default=0) or 0)
        if not theme_id:
            return
        title = str(value(theme, "title", default="当前主题"))
        answer = QMessageBox.question(
            self,
            "确认删除",
            f"确定删除主题“{title}”吗？该主题下的文案和日历排期也会一起删除。",
            QMessageBox.Yes | QMessageBox.No,
            QMessageBox.No,
        )
        if answer != QMessageBox.Yes:
            return
        self.run_api(lambda: self.client.delete_theme(theme_id), lambda _: self.after_delete_theme(theme_id))

    def after_delete_theme(self, theme_id: int) -> None:
        if self.current_theme_id == theme_id:
            self.current_theme_id = None
            self.current_draft_id = None
        self.load()

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

    def copy_selected_theme_summary(self) -> None:
        theme = self.selected_theme()
        if not theme:
            show_error(self, "请先选择一个主题")
            return
        copy_text(self, str(value(theme, "summary", default="")), "主题摘要")

    def open_theme_detail_dialog(self) -> None:
        theme = self.selected_theme()
        if not theme:
            show_error(self, "请先选择一个主题")
            return
        theme_id = int(value(theme, "id", default=0) or 0)
        drafts = [
            draft
            for draft in self.drafts
            if int(value(draft, "themeId", default=0) or 0) == theme_id
        ]
        ThemeDetailDialog(self, theme, drafts).exec()

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
        if self.current_draft_id:
            current = next((row for row in rows if int(value(row, "id", default=0) or 0) == self.current_draft_id), None)
            if current:
                self.update_draft_image_count(current)
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
            actions = QHBoxLayout()
            history = QPushButton("历史")
            edit = primary_button("编辑")
            detail = QPushButton("详情")
            copy_body = QPushButton("复制正文")
            delete = danger_button("删除")
            history.clicked.connect(lambda checked=False, payload=draft: self.open_draft_theme_history(payload))
            edit.clicked.connect(lambda checked=False, payload=draft: self.open_draft_card(payload))
            detail.clicked.connect(lambda checked=False, payload=draft: DraftDetailDialog(self, payload).exec())
            copy_body.clicked.connect(lambda checked=False, text=str(value(draft, "content", default="")): copy_text(self, text, "文案正文"))
            delete.clicked.connect(lambda checked=False, payload=draft: self.delete_draft_payload(payload))
            actions.addStretch(1)
            actions.addWidget(history)
            actions.addWidget(edit)
            actions.addWidget(detail)
            actions.addWidget(copy_body)
            actions.addWidget(delete)
            card.layout.addLayout(actions)
            card.clicked.connect(self.open_draft_card)
            self.draft_card_grid.addWidget(card, index // 2, index % 2)

    def theme_for_draft(self, draft: dict[str, Any]) -> dict[str, Any] | None:
        theme_id = int(value(draft, "themeId", default=0) or 0)
        return next(
            (
                theme
                for theme in self.themes
                if int(value(theme, "id", default=0) or 0) == theme_id
            ),
            None,
        )

    def open_draft_theme_history(self, draft: dict[str, Any]) -> None:
        theme = self.theme_for_draft(draft)
        if not theme:
            show_error(self, "未找到该文案所属主题")
            return
        theme_id = int(value(theme, "id", default=0) or 0)
        self.current_theme_id = theme_id
        self.render_theme_detail(theme)

        def show_history(rows: list[dict[str, Any]]) -> None:
            self.render_drafts(rows)
            ThemeDetailDialog(self, theme, rows).exec()

        self.run_api(lambda: self.client.theme_drafts(theme_id), show_history)

    def selected_draft(self) -> dict[str, Any] | None:
        if self.current_draft_id:
            current = next(
                (
                    draft
                    for draft in self.drafts
                    if int(value(draft, "id", default=0) or 0) == self.current_draft_id
                ),
                None,
            )
            if current:
                return current
        row = self.draft_table.selected_row_data()
        if row:
            return row.get("_raw") or row
        return None

    def open_draft_card(self, payload: object) -> None:
        if not isinstance(payload, dict):
            return
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
        draft = self.selected_draft() or {"id": self.current_draft_id, "title": "当前文案"}
        self.delete_draft_payload(draft)

    def delete_draft_payload(self, draft: dict[str, Any]) -> None:
        draft_id = int(value(draft, "id", default=0) or 0)
        if not draft_id:
            return
        title = str(value(draft, "title", default="当前文案"))
        answer = QMessageBox.question(
            self,
            "确认删除",
            f"确定删除文案“{title}”吗？",
            QMessageBox.Yes | QMessageBox.No,
            QMessageBox.No,
        )
        if answer != QMessageBox.Yes:
            return
        self.run_api(lambda: self.client.delete_draft(draft_id), lambda _: self.after_delete_draft(draft_id))

    def after_delete_draft(self, draft_id: int) -> None:
        if self.current_draft_id == draft_id:
            self.current_draft_id = None
            self.draft_title.clear()
            self.draft_meta_row.set_value("-")
            self.draft_rating.blockSignals(True)
            self.draft_rating.set_rating(0)
            self.draft_rating.blockSignals(False)
            self.draft_usage.setCurrentText("未使用")
            self.draft_used_date.clear()
            self.draft_feedback.clear()
            self.draft_image_suggestion.clear()
            self.draft_content.clear()
            self.update_draft_image_count({})
        self.load()

    def update_draft_image_count(self, draft: dict[str, Any]) -> None:
        images = draft.get("images") if isinstance(draft, dict) else []
        count = len(images) if isinstance(images, list) else 0
        self.draft_image_count.setText(f"图片 {count}")
        normalized = []
        if isinstance(images, list):
            for image in images:
                if not isinstance(image, dict):
                    continue
                normalized.append(
                    {
                        "id": value(image, "id"),
                        "name": value(image, "name"),
                        "url": value(image, "url"),
                        "storageProvider": value(image, "storageProvider", default=value(image, "provider", default="")),
                        "_raw": image,
                    }
                )
        self.draft_image_table.set_rows(normalized, ["id", "name", "url", "storageProvider"])

    def selected_draft_image_url(self) -> str:
        row = self.draft_image_table.selected_row_data()
        if not row:
            return ""
        raw = row.get("_raw") or row
        return str(value(raw, "url", default=""))

    def copy_selected_draft_image(self) -> None:
        copy_text(self, self.selected_draft_image_url(), "图片 URL")

    def open_selected_draft_image(self) -> None:
        open_url(self, self.selected_draft_image_url())

    def copy_selected_draft_content(self) -> None:
        draft = self.selected_draft()
        if not draft:
            show_error(self, "请先选择一条文案")
            return
        copy_text(self, str(value(draft, "content", default="")), "文案正文")

    def open_draft_detail_dialog(self) -> None:
        draft = self.selected_draft()
        if not draft:
            show_error(self, "请先选择一条文案")
            return
        DraftDetailDialog(self, draft).exec()

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
        self.run_api(lambda: self.client.content_generate(kind, payload), self.render_sop_result)

    def render_sop_result(self, result: Any) -> None:
        self.sop_output.set_json(result)
        clear_layout(self.sop_card_layout)
        if not isinstance(result, dict):
            self.sop_card_layout.addWidget(TextCard("生成结果", safe_json_dumps(result)))
            self.sop_card_layout.addStretch(1)
            return

        summary = Card("SOP 概览")
        badges = QHBoxLayout()
        badges.addWidget(Badge(str(value(result, "kind", default="architecture")), "primary"))
        badges.addWidget(Badge("ContentAgent", "purple"))
        badges.addStretch(1)
        summary.layout.addLayout(badges)
        input_payload = result.get("input") if isinstance(result.get("input"), dict) else {}
        if input_payload:
            for key in ["topic", "scene", "audience", "style", "agentType"]:
                if input_payload.get(key):
                    summary.layout.addWidget(InfoRow(key, str(input_payload.get(key))))
        else:
            summary.layout.addWidget(QLabel("内容生成、知识检索、飞书沉淀和本地入库的流程说明。"))
        self.sop_card_layout.addWidget(summary)

        steps = result.get("steps") or result.get("stages") or []
        if isinstance(steps, list) and steps:
            steps_card = Card("流程步骤")
            for index, step in enumerate(steps, start=1):
                steps_card.layout.addWidget(InfoRow(f"{index}.", str(step)))
            self.sop_card_layout.addWidget(steps_card)

        drafts = result.get("drafts") or result.get("copies") or result.get("items") or []
        if isinstance(drafts, list) and drafts:
            drafts_card = Card("生成草稿")
            for draft in drafts:
                if isinstance(draft, dict):
                    title = str(value(draft, "title", "name", default="未命名草稿"))
                    body = str(value(draft, "content", "body", "text", default=safe_json_dumps(draft)))
                    meta = str(value(draft, "style", "channel", "version", default=""))
                    drafts_card.layout.addWidget(TextCard(title, body, meta, draft))
                else:
                    drafts_card.layout.addWidget(TextCard("草稿", str(draft)))
            self.sop_card_layout.addWidget(drafts_card)

        assets = result.get("assets") or result.get("tags") or []
        if isinstance(assets, list) and assets:
            asset_card = Card("内容资产")
            asset_row = QHBoxLayout()
            for asset in assets:
                asset_row.addWidget(Badge(str(asset), "info"))
            asset_row.addStretch(1)
            asset_card.layout.addLayout(asset_row)
            self.sop_card_layout.addWidget(asset_card)

        self.sop_card_layout.addStretch(1)

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


class ThemeDetailDialog(QDialog):
    def __init__(self, parent: QWidget, theme: dict[str, Any], drafts: list[dict[str, Any]]):
        super().__init__(parent)
        self.theme = theme
        self.drafts = drafts
        self.setWindowTitle(str(value(theme, "title", default="主题详情")))
        self.resize(900, 700)

        layout = QVBoxLayout(self)
        header = QHBoxLayout()
        title = QLabel(str(value(theme, "title", default="未命名主题")))
        title.setObjectName("PageTitle")
        title.setWordWrap(True)
        header.addWidget(title, 1)
        header.addWidget(Badge(str(value(theme, "platform", default="-")), "primary"))
        header.addWidget(Badge(str(value(theme, "status", default="-")), "info"))
        layout.addLayout(header)

        action_row = QHBoxLayout()
        copy_summary = QPushButton("复制摘要")
        copy_tags = QPushButton("复制标签")
        copy_all = primary_button("复制主题卡片")
        copy_summary.clicked.connect(
            lambda: copy_text(self, str(value(self.theme, "summary", default="")), "主题摘要")
        )
        copy_tags.clicked.connect(lambda: copy_text(self, self.tag_text(), "主题标签"))
        copy_all.clicked.connect(lambda: copy_text(self, self.compact_card_text(), "主题卡片"))
        action_row.addWidget(Badge(f"热度 {value(theme, 'heat', default='-')}", "warning"))
        action_row.addWidget(Badge(f"评分 {value(theme, 'rating', default=0)}/5", "success"))
        action_row.addStretch(1)
        action_row.addWidget(copy_summary)
        action_row.addWidget(copy_tags)
        action_row.addWidget(copy_all)
        layout.addLayout(action_row)

        tabs = QTabWidget()
        tabs.addTab(self.build_info_tab(), "主题信息")
        tabs.addTab(self.build_drafts_tab(), f"历史文案 {len(drafts)}")
        raw = JsonPanel()
        raw.set_json({"theme": theme, "drafts": drafts})
        tabs.addTab(raw, "原始数据")
        layout.addWidget(tabs, 1)

        buttons = QDialogButtonBox(QDialogButtonBox.Close)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def tag_text(self) -> str:
        tags = value(self.theme, "tags", default=[])
        if isinstance(tags, list):
            return "、".join(map(str, tags))
        return str(tags)

    def compact_card_text(self) -> str:
        lines = [
            f"标题：{value(self.theme, 'title', default='')}",
            f"主题：{value(self.theme, 'topic', default='')}",
            f"平台：{value(self.theme, 'platform', default='')}",
            f"状态：{value(self.theme, 'status', default='')}",
            f"热度：{value(self.theme, 'heat', default='')}",
            f"评分：{value(self.theme, 'rating', default=0)}/5",
            f"计划日期：{value(self.theme, 'plannedDate', default='')}",
            f"标签：{self.tag_text()}",
            "",
            str(value(self.theme, "summary", default="")),
        ]
        return "\n".join(lines).strip()

    def build_info_tab(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        for label, text in [
            ("ID", value(self.theme, "id", default="-")),
            ("主题", value(self.theme, "topic", default="-")),
            ("平台", value(self.theme, "platform", default="-")),
            ("类型", value(self.theme, "type", default="-")),
            ("状态", value(self.theme, "status", default="-")),
            ("热度", value(self.theme, "heat", default="-")),
            ("评分", f"{value(self.theme, 'rating', default=0)}/5"),
            ("计划日期", value(self.theme, "plannedDate", default="-")),
            ("标签", self.tag_text() or "-"),
        ]:
            layout.addWidget(InfoRow(str(label), str(text)))

        summary = MarkdownPanel()
        summary.setMinimumHeight(280)
        summary.set_text(str(value(self.theme, "summary", default="暂无摘要")))
        layout.addWidget(QLabel("摘要"))
        layout.addWidget(summary, 1)
        return page

    def build_drafts_tab(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        if not self.drafts:
            layout.addWidget(EmptyState("当前主题下暂无已加载文案"))
            return page

        table = DataTable(["ID", "标题", "渠道", "版本", "使用状态", "评分"])
        rows = [
            {
                "id": value(draft, "id"),
                "title": value(draft, "title"),
                "channel": value(draft, "channel"),
                "version": value(draft, "version"),
                "usageStatus": value(draft, "usageStatus"),
                "rating": value(draft, "rating"),
                "_raw": draft,
            }
            for draft in self.drafts
        ]
        table.set_rows(rows, ["id", "title", "channel", "version", "usageStatus", "rating"])
        layout.addWidget(table, 1)

        preview = MarkdownPanel()
        preview.setMinimumHeight(260)
        preview.set_text(
            "\n\n---\n\n".join(
                f"### {value(draft, 'title', default='未命名文案')}\n\n"
                f"{value(draft, 'content', default='')}"
                for draft in self.drafts[:6]
            )
        )
        layout.addWidget(QLabel("文案预览"))
        layout.addWidget(preview, 1)
        return page


class DraftDetailDialog(QDialog):
    def __init__(self, parent: QWidget, draft: dict[str, Any]):
        super().__init__(parent)
        self.draft = draft
        self.setWindowTitle(str(value(draft, "title", default="文案详情")))
        self.resize(920, 720)

        layout = QVBoxLayout(self)
        header = QHBoxLayout()
        title = QLabel(str(value(draft, "title", default="未命名文案")))
        title.setObjectName("PageTitle")
        title.setWordWrap(True)
        header.addWidget(title, 1)
        header.addWidget(Badge(str(value(draft, "channel", default="-")), "primary"))
        header.addWidget(Badge(str(value(draft, "usageStatus", default="-")), "info"))
        layout.addLayout(header)

        action_row = QHBoxLayout()
        copy_title = QPushButton("复制标题")
        copy_content = primary_button("复制正文")
        copy_title.clicked.connect(lambda: copy_text(self, str(value(self.draft, "title", default="")), "文案标题"))
        copy_content.clicked.connect(lambda: copy_text(self, str(value(self.draft, "content", default="")), "文案正文"))
        action_row.addWidget(Badge(str(value(draft, "version", default="版本")), "warning"))
        action_row.addWidget(Badge(str(value(draft, "style", default="风格")), "purple"))
        action_row.addWidget(Badge(f"评分 {value(draft, 'rating', default=0)}/5", "success"))
        action_row.addStretch(1)
        action_row.addWidget(copy_title)
        action_row.addWidget(copy_content)
        layout.addLayout(action_row)

        tabs = QTabWidget()
        tabs.addTab(self.build_content_tab(), "文案正文")
        tabs.addTab(self.build_images_tab(), f"图片引用 {len(self.images())}")
        raw = JsonPanel()
        raw.set_json(draft)
        tabs.addTab(raw, "原始数据")
        layout.addWidget(tabs, 1)

        buttons = QDialogButtonBox(QDialogButtonBox.Close)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def images(self) -> list[dict[str, Any]]:
        images = self.draft.get("images") if isinstance(self.draft, dict) else []
        return images if isinstance(images, list) else []

    def build_content_tab(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        for label, text in [
            ("ID", value(self.draft, "id", default="-")),
            ("主题 ID", value(self.draft, "themeId", default="-")),
            ("渠道", value(self.draft, "channel", default="-")),
            ("版本", value(self.draft, "version", default="-")),
            ("风格", value(self.draft, "style", default="-")),
            ("负责人", value(self.draft, "owner", default="-")),
            ("使用状态", value(self.draft, "usageStatus", default="-")),
            ("使用日期", value(self.draft, "usedDate", default="-")),
            ("反馈", value(self.draft, "feedback", default="-")),
            ("配图建议", value(self.draft, "imageSuggestion", default="-")),
        ]:
            layout.addWidget(InfoRow(str(label), str(text)))

        content = MarkdownPanel()
        content.setMinimumHeight(360)
        content.set_text(str(value(self.draft, "content", default="暂无正文")))
        layout.addWidget(QLabel("正文预览"))
        layout.addWidget(content, 1)
        return page

    def build_images_tab(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        action_row = QHBoxLayout()
        copy_image = QPushButton("复制选中图片 URL")
        open_image = primary_button("打开选中图片")
        copy_image.clicked.connect(self.copy_selected_image)
        open_image.clicked.connect(self.open_selected_image)
        action_row.addStretch(1)
        action_row.addWidget(copy_image)
        action_row.addWidget(open_image)
        layout.addLayout(action_row)

        self.image_table = DataTable(["ID", "名称", "URL", "存储", "对象 Key"])
        rows = [
            {
                "id": value(image, "id"),
                "name": value(image, "name"),
                "url": value(image, "url"),
                "storageProvider": value(image, "storageProvider", "provider"),
                "objectKey": value(image, "objectKey"),
                "_raw": image,
            }
            for image in self.images()
            if isinstance(image, dict)
        ]
        self.image_table.set_rows(rows, ["id", "name", "url", "storageProvider", "objectKey"])
        layout.addWidget(self.image_table, 1)
        if not rows:
            layout.addWidget(EmptyState("暂无图片引用，编辑区可新增图片 URL"))
        return page

    def selected_image_url(self) -> str:
        row = self.image_table.selected_row_data()
        if not row:
            return ""
        raw = row.get("_raw") or row
        return str(value(raw, "url", default=""))

    def copy_selected_image(self) -> None:
        copy_text(self, self.selected_image_url(), "图片 URL")

    def open_selected_image(self) -> None:
        open_url(self, self.selected_image_url())


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
        self.students: list[dict[str, Any]] = []
        self.current_student_id: int | None = None
        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        refresh = primary_button("刷新")
        refresh.clicked.connect(self.load)
        add = QPushButton("新增学员")
        add.clicked.connect(self.add_student)
        edit = QPushButton("编辑学员")
        edit.clicked.connect(self.edit_student)
        analyze = QPushButton("AI 分析")
        analyze.clicked.connect(self.analyze_selected)
        delete = danger_button("删除")
        delete.clicked.connect(self.delete_selected)
        root.addWidget(PageHeader("学员管理", "学员画像、风险标签、申请阶段和进度条", [add, edit, analyze, delete, refresh]))

        stat_grid = QGridLayout()
        self.student_total_card = StatCard("学员总数", "0", "当前列表")
        self.student_low_risk_card = StatCard("低风险", "0", "节奏稳定")
        self.student_mid_risk_card = StatCard("中风险", "0", "需要跟进")
        self.student_high_risk_card = StatCard("高风险", "0", "优先处理")
        for index, card in enumerate([self.student_total_card, self.student_low_risk_card, self.student_mid_risk_card, self.student_high_risk_card]):
            stat_grid.addWidget(card, 0, index)
        root.addLayout(stat_grid)

        splitter = QSplitter()
        student_tabs = QTabWidget()
        student_cards_page = QWidget()
        student_cards_layout = QVBoxLayout(student_cards_page)
        student_cards_layout.setContentsMargins(0, 0, 0, 0)
        student_cards_layout.addWidget(SectionHeader("学员卡片", "阶段、风险和申请进度一屏扫读"))
        student_card_scroll = QScrollArea()
        student_card_scroll.setWidgetResizable(True)
        self.student_card_host = QWidget()
        self.student_card_grid = QGridLayout(self.student_card_host)
        self.student_card_grid.setSpacing(12)
        student_card_scroll.setWidget(self.student_card_host)
        student_cards_layout.addWidget(student_card_scroll, 1)
        self.table = DataTable(["ID", "姓名", "学校", "专业", "GPA", "排名", "英语", "目标", "阶段", "风险", "进度"])
        student_tabs.addTab(student_cards_page, "卡片")
        student_tabs.addTab(self.table, "表格")
        detail_tabs = QTabWidget()
        profile = Card("学员画像")
        self.student_name = QLabel("请选择学员")
        self.student_name.setObjectName("SectionTitle")
        self.student_school = InfoRow("学校/专业", "-")
        self.student_scores = InfoRow("成绩", "-")
        self.student_target = InfoRow("目标", "-")
        self.student_stage = InfoRow("阶段", "-")
        self.student_risk_badge = Badge("-", "info")
        self.student_progress_bar = QProgressBar()
        self.student_progress_bar.setRange(0, 100)
        self.student_progress_bar.setFormat("%p%")
        profile.layout.addWidget(self.student_name)
        profile.layout.addWidget(self.student_risk_badge)
        profile.layout.addWidget(self.student_school)
        profile.layout.addWidget(self.student_scores)
        profile.layout.addWidget(self.student_target)
        profile.layout.addWidget(self.student_stage)
        profile.layout.addWidget(QLabel("申请进度"))
        profile.layout.addWidget(self.student_progress_bar)
        analysis_card = Card("AI 分析结果")
        self.analysis_risk_badge = Badge("未分析", "info")
        self.analysis_summary = MarkdownPanel()
        self.analysis_summary.setMinimumHeight(160)
        self.analysis_actions = QPlainTextEdit()
        self.analysis_actions.setReadOnly(True)
        self.analysis_actions.setMaximumHeight(130)
        analysis_card.layout.addWidget(self.analysis_risk_badge)
        analysis_card.layout.addWidget(QLabel("分析摘要"))
        analysis_card.layout.addWidget(self.analysis_summary, 1)
        analysis_card.layout.addWidget(QLabel("下一步动作"))
        analysis_card.layout.addWidget(self.analysis_actions)
        self.output = JsonPanel()
        detail_tabs.addTab(profile, "画像")
        detail_tabs.addTab(analysis_card, "AI 分析")
        detail_tabs.addTab(self.output, "原始分析")
        splitter.addWidget(student_tabs)
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
        self.students = normalized
        self.table.set_rows(normalized, ["id", "name", "school", "major", "gpa", "rank", "english", "targetSchool", "stage", "risk", "progress"])
        self.decorate_student_table(normalized)
        self.render_student_cards(normalized)
        self.update_stats(normalized)

    def decorate_student_table(self, rows: list[dict[str, Any]]) -> None:
        for row_index, row in enumerate(rows):
            stage = str(value(row, "stage", default="-"))
            risk = str(value(row, "risk", default="-"))
            stage_badge = Badge(stage, "primary")
            risk_badge = Badge(risk, self.risk_kind(risk))
            progress = QProgressBar()
            progress.setRange(0, 100)
            progress.setValue(self.parse_progress(value(row, "progress", default=0)))
            progress.setFormat("%p%")
            self.table.setCellWidget(row_index, 8, stage_badge)
            self.table.setCellWidget(row_index, 9, risk_badge)
            self.table.setCellWidget(row_index, 10, progress)

    def render_student_cards(self, rows: list[dict[str, Any]]) -> None:
        clear_layout(self.student_card_grid)
        if not rows:
            self.student_card_grid.addWidget(EmptyState("暂无学员"), 0, 0)
            return
        for index, row in enumerate(rows):
            raw = row.get("_raw") or row
            name = str(value(raw, "name", default="未命名学员"))
            school = str(value(raw, "school", default="-"))
            major = str(value(raw, "major", default="-"))
            target = str(value(raw, "targetSchool", default="目标待定"))
            meta = f"{school} / {major} / GPA {value(raw, 'gpa', default='-')} / 排名 {value(raw, 'rank', 'ranking', default='-')}"
            card = TextCard(name, target, meta, raw)
            badge_row = QHBoxLayout()
            stage = str(value(raw, "stage", "applicationStage", default="-"))
            risk = str(value(raw, "risk", "riskLevel", default="-"))
            badge_row.addWidget(Badge(stage, "primary"))
            badge_row.addWidget(Badge(f"风险 {risk}", self.risk_kind(risk)))
            badge_row.addStretch(1)
            card.layout.insertLayout(0, badge_row)
            progress = QProgressBar()
            progress.setRange(0, 100)
            progress.setValue(self.parse_progress(value(raw, "progress", default=0)))
            progress.setFormat("申请进度 %p%")
            card.layout.addWidget(progress)
            actions = QHBoxLayout()
            analyze = QPushButton("AI 分析")
            analyze.clicked.connect(lambda checked=False, student=raw: self.analyze_student_payload(student))
            actions.addStretch(1)
            actions.addWidget(analyze)
            card.layout.addLayout(actions)
            card.clicked.connect(self.open_student_card)
            self.student_card_grid.addWidget(card, index // 3, index % 3)

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
        self.render_student_detail(raw)

    def render_student_detail(self, raw: dict[str, Any]) -> None:
        self.current_student_id = int(value(raw, "id", default=0) or 0)
        self.student_name.setText(str(value(raw, "name", default="未命名学员")))
        self.student_school.set_value(f"{value(raw, 'school')} / {value(raw, 'major')}")
        self.student_scores.set_value(f"GPA {value(raw, 'gpa')}，排名 {value(raw, 'rank', 'ranking')}，英语 {value(raw, 'english', 'englishScore')}")
        self.student_target.set_value(value(raw, "targetSchool", default="-"))
        self.student_stage.set_value(value(raw, "stage", "applicationStage", default="-"))
        risk = str(value(raw, "risk", "riskLevel", default="-"))
        self.student_risk_badge.setText(f"风险：{risk}")
        self.student_risk_badge.set_kind(self.risk_kind(risk))
        self.student_progress_bar.setValue(self.parse_progress(value(raw, "progress", default=0)))

    def open_student_card(self, payload: object) -> None:
        if isinstance(payload, dict):
            self.render_student_detail(payload)

    def selected_student_raw(self) -> dict[str, Any] | None:
        if self.current_student_id:
            current = next(
                (
                    (row.get("_raw") or row)
                    for row in self.students
                    if int(value(row, "id", default=0) or 0) == self.current_student_id
                ),
                None,
            )
            if isinstance(current, dict):
                return current
        row = self.table.selected_row_data()
        if row:
            raw = row.get("_raw") or row
            return raw if isinstance(raw, dict) else None
        return None

    def risk_kind(self, risk: str) -> str:
        if risk == "低":
            return "success"
        if risk == "中":
            return "warning"
        if risk == "高":
            return "danger"
        return "info"

    def parse_progress(self, raw: Any) -> int:
        try:
            return max(0, min(100, int(float(str(raw).replace("%", "")))))
        except (TypeError, ValueError):
            return 0

    def add_student(self) -> None:
        dialog = StudentDialog(self)
        if dialog.exec() == QDialog.Accepted:
            self.run_api(lambda: self.client.create_student(dialog.payload()), lambda _: self.load())

    def edit_student(self) -> None:
        raw = self.selected_student_raw()
        if not raw:
            show_error(self, "请先选择一名学员")
            return
        sid = int(value(raw, "id", default=0) or 0)
        if not sid:
            return
        dialog = StudentDialog(self, raw, "编辑学员")
        if dialog.exec() == QDialog.Accepted:
            self.run_api(lambda: self.client.update_student(sid, dialog.payload()), lambda _: self.load())

    def selected_id(self) -> int | None:
        raw = self.selected_student_raw()
        return int(value(raw or {}, "id", default=0) or 0) or None

    def analyze_selected(self) -> None:
        raw = self.selected_student_raw()
        if not raw:
            show_error(self, "请先选择一名学员")
            return
        self.analyze_student_payload(raw)

    def analyze_student_payload(self, raw: dict[str, Any]) -> None:
        self.render_student_detail(raw)
        sid = int(value(raw, "id", default=0) or 0)
        if not sid:
            return
        self.run_api(lambda: self.client.analyze_student(sid), self.render_student_analysis)

    def render_student_analysis(self, result: Any) -> None:
        self.output.set_json(result)
        if not isinstance(result, dict):
            self.analysis_risk_badge.setText("分析结果")
            self.analysis_risk_badge.set_kind("info")
            self.analysis_summary.set_text(safe_json_dumps(result))
            self.analysis_actions.setPlainText("")
            return
        risk = str(value(result, "risk", "riskLevel", default="未评级"))
        self.analysis_risk_badge.setText(risk)
        self.analysis_risk_badge.set_kind("danger" if "高" in risk else "warning" if "中" in risk else "success" if "低" in risk else "info")
        self.analysis_summary.set_text(str(value(result, "summary", "analysis", default="暂无摘要")))
        actions = result.get("actions") or result.get("suggestions") or result.get("nextSteps") or []
        if isinstance(actions, list):
            self.analysis_actions.setPlainText("\n".join(f"- {item}" for item in actions))
        else:
            self.analysis_actions.setPlainText(str(actions or "暂无下一步动作"))

    def delete_selected(self) -> None:
        sid = self.selected_id()
        if not sid:
            show_error(self, "请先选择一名学员")
            return
        self.run_api(lambda: self.client.delete_student(sid), lambda _: self.load())


class StudentDialog(QDialog):
    LABELS = {
        "name": "姓名",
        "school": "本科学校",
        "major": "专业",
        "gpa": "GPA",
        "rank": "排名",
        "english": "英语",
        "targetSchool": "目标院校",
        "stage": "申请阶段",
        "risk": "风险等级",
        "progress": "进度",
    }

    def __init__(self, parent: QWidget, initial: dict[str, Any] | None = None, title: str = "新增学员"):
        super().__init__(parent)
        self.setWindowTitle(title)
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
        if initial:
            defaults.update(
                {
                    "name": str(value(initial, "name", default=defaults["name"])),
                    "school": str(value(initial, "school", default=defaults["school"])),
                    "major": str(value(initial, "major", default=defaults["major"])),
                    "gpa": str(value(initial, "gpa", default=defaults["gpa"])),
                    "rank": str(value(initial, "rank", "ranking", default=defaults["rank"])),
                    "english": str(value(initial, "english", "englishScore", default=defaults["english"])),
                    "targetSchool": str(value(initial, "targetSchool", default=defaults["targetSchool"])),
                    "stage": str(value(initial, "stage", "applicationStage", default=defaults["stage"])),
                    "risk": str(value(initial, "risk", "riskLevel", default=defaults["risk"])),
                    "progress": str(value(initial, "progress", default=defaults["progress"])),
                }
            )
        for key, default in defaults.items():
            layout.addWidget(QLabel(self.LABELS.get(key, key)))
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
        self.current_project: dict[str, Any] | None = None
        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        refresh = primary_button("刷新")
        refresh.clicked.connect(self.load)
        add_school = QPushButton("新增学校")
        add_school.clicked.connect(self.add_school)
        add_project = QPushButton("新增项目")
        add_project.clicked.connect(self.add_project)
        recommend = QPushButton("院校推荐")
        recommend.clicked.connect(self.recommend)
        root.addWidget(PageHeader("院校情报", "院校项目、截止时间、条件、材料和 AI 匹配", [add_school, add_project, recommend, refresh]))
        stat_grid = QGridLayout()
        self.school_count_card = StatCard("学校数", "0", "已收录院校")
        self.project_count_card = StatCard("项目数", "0", "夏令营/预推免")
        self.match_avg_card = StatCard("平均匹配", "0", "项目匹配分")
        self.deadline_card = StatCard("最近截止", "-", "需要优先跟进")
        for index, card in enumerate([self.school_count_card, self.project_count_card, self.match_avg_card, self.deadline_card]):
            stat_grid.addWidget(card, 0, index)
        root.addLayout(stat_grid)
        self.school_tabs = QTabWidget()
        self.school_tabs.addTab(self.build_schools(), "学校")
        self.school_tabs.addTab(self.build_projects(), "项目")
        self.school_tabs.addTab(self.build_recommend(), "推荐")
        root.addWidget(self.school_tabs, 1)

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
        project_tabs = QTabWidget()
        project_cards_page = QWidget()
        project_cards_layout = QVBoxLayout(project_cards_page)
        project_cards_layout.setContentsMargins(0, 0, 0, 0)
        project_cards_layout.addWidget(SectionHeader("项目卡片", "对应 Web 端 SchoolProjectCard 的项目视图"))
        project_scroll = QScrollArea()
        project_scroll.setWidgetResizable(True)
        self.project_card_host = QWidget()
        self.project_card_grid = QGridLayout(self.project_card_host)
        self.project_card_grid.setSpacing(12)
        project_scroll.setWidget(self.project_card_host)
        project_cards_layout.addWidget(project_scroll, 1)
        self.project_table = DataTable(["ID", "学校", "项目", "类型", "截止", "要求", "材料", "匹配"])
        project_tabs.addTab(project_cards_page, "卡片")
        project_tabs.addTab(self.project_table, "表格")
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
        project_actions = QHBoxLayout()
        project_detail = QPushButton("详情弹窗")
        copy_requirements = QPushButton("复制条件")
        copy_materials = QPushButton("复制材料")
        project_recommend = primary_button("AI 匹配推荐")
        project_detail.clicked.connect(self.open_project_detail_dialog)
        copy_requirements.clicked.connect(self.copy_project_requirements)
        copy_materials.clicked.connect(self.copy_project_materials)
        project_recommend.clicked.connect(self.recommend_selected_project)
        project_actions.addStretch(1)
        project_actions.addWidget(project_detail)
        project_actions.addWidget(copy_requirements)
        project_actions.addWidget(copy_materials)
        project_actions.addWidget(project_recommend)
        detail.layout.addWidget(self.project_title)
        detail.layout.addWidget(self.project_match)
        detail.layout.addWidget(self.project_school)
        detail.layout.addWidget(self.project_deadline)
        detail.layout.addLayout(project_actions)
        detail.layout.addWidget(QLabel("报名条件"))
        detail.layout.addWidget(self.project_requirements, 1)
        detail.layout.addWidget(QLabel("材料要求"))
        detail.layout.addWidget(self.project_materials, 1)
        splitter.addWidget(project_tabs)
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
        rec_scroll = QScrollArea()
        rec_scroll.setWidgetResizable(True)
        self.rec_card_host = QWidget()
        self.rec_card_layout = QVBoxLayout(self.rec_card_host)
        self.rec_card_layout.setContentsMargins(0, 0, 0, 0)
        self.rec_card_layout.setSpacing(12)
        self.rec_card_layout.addWidget(EmptyState("点击院校推荐后展示匹配项目"))
        rec_scroll.setWidget(self.rec_card_host)
        tabs = QTabWidget()
        tabs.addTab(rec_scroll, "推荐卡片")
        tabs.addTab(self.rec_output, "原始结果")
        layout.addWidget(tabs, 1)
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
        self.decorate_project_table(normalized)
        self.render_project_cards(rows)
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

    def decorate_project_table(self, rows: list[dict[str, Any]]) -> None:
        for row_index, row in enumerate(rows):
            progress = QProgressBar()
            progress.setRange(0, 100)
            progress.setValue(self.parse_match_score(value(row, "matchScore", default=0)))
            progress.setFormat("匹配 %p%")
            self.project_table.setCellWidget(row_index, 7, progress)

    def parse_match_score(self, raw: Any) -> int:
        try:
            return max(0, min(100, int(float(str(raw).replace("%", "")))))
        except (TypeError, ValueError):
            return 0

    def render_project_cards(self, rows: list[dict[str, Any]]) -> None:
        clear_layout(self.project_card_grid)
        if not rows:
            self.project_card_grid.addWidget(EmptyState("暂无院校项目"), 0, 0)
            return
        for index, row in enumerate(rows):
            title = str(value(row, "projectName", default="未命名项目"))
            body = (
                f"截止：{value(row, 'deadline', default='-')}\n"
                f"条件：{value(row, 'requirements', default='-')}\n"
                f"材料：{value(row, 'materials', default='-')}"
            )
            meta = f"{value(row, 'schoolName', default='院校')} / {value(row, 'projectType', default='项目')}"
            card = TextCard(title, body, meta, row)
            top = QHBoxLayout()
            top.addWidget(Badge(str(value(row, "schoolName", default="-")), "primary"))
            score = value(row, "matchScore", default=0)
            try:
                numeric = float(score)
            except (TypeError, ValueError):
                numeric = 0
            top.addWidget(Badge(f"匹配 {score}", "success" if numeric >= 85 else "warning" if numeric >= 70 else "info"))
            top.addStretch(1)
            card.layout.insertLayout(0, top)
            action = primary_button("AI 匹配推荐")
            action.clicked.connect(lambda checked=False, payload=row: self.recommend_project_payload(payload))
            card.layout.addWidget(action)
            card.clicked.connect(self.render_selected_project_card)
            self.project_card_grid.addWidget(card, index // 3, index % 3)

    def render_selected_project_card(self, payload: object) -> None:
        if isinstance(payload, dict):
            self.render_project_detail(payload)

    def render_selected_project(self) -> None:
        row = self.project_table.selected_row_data()
        if not row:
            return
        self.render_project_detail(row)

    def render_project_detail(self, project: dict[str, Any]) -> None:
        self.current_project = project
        self.project_title.setText(str(value(project, "projectName", default="未命名项目")))
        self.project_school.set_value(value(project, "schoolName", default="-"))
        self.project_deadline.set_value(value(project, "deadline", default="-"))
        score = value(project, "matchScore", default="-")
        self.project_match.setText(f"匹配 {score}")
        try:
            numeric = float(score)
            self.project_match.set_kind("success" if numeric >= 85 else "warning" if numeric >= 70 else "info")
        except (TypeError, ValueError):
            self.project_match.set_kind("info")
        self.project_requirements.set_text(str(value(project, "requirements", default="")))
        self.project_materials.set_text(str(value(project, "materials", default="")))

    def selected_project_raw(self) -> dict[str, Any] | None:
        if self.current_project:
            return self.current_project
        row = self.project_table.selected_row_data()
        if row:
            return row if isinstance(row, dict) else None
        return None

    def open_project_detail_dialog(self) -> None:
        project = self.selected_project_raw()
        if not project:
            show_error(self, "请先选择一个院校项目")
            return
        SchoolProjectDetailDialog(self, project).exec()

    def copy_project_requirements(self) -> None:
        project = self.selected_project_raw()
        copy_text(self, str(value(project or {}, "requirements", default="")), "报名条件")

    def copy_project_materials(self) -> None:
        project = self.selected_project_raw()
        copy_text(self, str(value(project or {}, "materials", default="")), "材料要求")

    def recommend_selected_project(self) -> None:
        project = self.selected_project_raw()
        if not project:
            show_error(self, "请先选择一个院校项目")
            return
        self.recommend_project_payload(project)

    def recommend_project_payload(self, project: dict[str, Any]) -> None:
        self.render_project_detail(project)
        self.render_recommendations([project])
        if hasattr(self, "school_tabs"):
            self.school_tabs.setCurrentIndex(2)

    def add_school(self) -> None:
        dialog = SchoolDialog(self)
        if dialog.exec() == QDialog.Accepted:
            self.run_api(lambda: self.client.create_school(dialog.payload()), lambda _: self.load())

    def add_project(self) -> None:
        dialog = SchoolProjectDialog(self)
        if dialog.exec() == QDialog.Accepted:
            self.run_api(lambda: self.client.create_project(dialog.payload()), lambda _: self.load())

    def recommend(self) -> None:
        payload = {
            "gpa": self.rec_gpa.text(),
            "rank": self.rec_rank.text(),
            "english": self.rec_english.text(),
            "targetMajor": self.rec_major.text(),
            "riskPreference": self.rec_risk.currentText(),
        }
        self.run_api(lambda: self.client.recommend_schools(payload), self.render_recommendations)

    def render_recommendations(self, result: Any) -> None:
        self.rec_output.setPlainText(safe_json_dumps(result))
        rows = result if isinstance(result, list) else result.get("data", []) if isinstance(result, dict) else []
        clear_layout(self.rec_card_layout)
        if not rows:
            self.rec_card_layout.addWidget(EmptyState("暂无推荐结果"))
            self.rec_card_layout.addStretch(1)
            return
        for row in rows:
            title = f"{value(row, 'schoolName', default='院校')} · {value(row, 'projectName', default='项目')}"
            body = f"条件：{value(row, 'requirements', default='-')}\n材料：{value(row, 'materials', default='-')}"
            meta = f"截止 {value(row, 'deadline', default='-')} / 匹配 {value(row, 'matchScore', default='-')}"
            card = TextCard(title, body, meta, row)
            badge_row = QHBoxLayout()
            score = value(row, "matchScore", default=0)
            try:
                numeric = float(score)
            except (TypeError, ValueError):
                numeric = 0
            badge_row.addWidget(Badge(f"匹配 {score}", "success" if numeric >= 85 else "warning" if numeric >= 70 else "info"))
            badge_row.addWidget(Badge(str(value(row, "projectType", default="项目")), "primary"))
            badge_row.addStretch(1)
            card.layout.insertLayout(0, badge_row)
            self.rec_card_layout.addWidget(card)
        self.rec_card_layout.addStretch(1)


class SchoolProjectDetailDialog(QDialog):
    def __init__(self, parent: QWidget, project: dict[str, Any]):
        super().__init__(parent)
        self.project = project
        self.setWindowTitle(str(value(project, "projectName", default="院校项目详情")))
        self.resize(860, 660)

        layout = QVBoxLayout(self)
        header = QHBoxLayout()
        title = QLabel(str(value(project, "projectName", default="未命名项目")))
        title.setObjectName("PageTitle")
        title.setWordWrap(True)
        header.addWidget(title, 1)
        header.addWidget(Badge(str(value(project, "schoolName", default="院校")), "primary"))
        header.addWidget(Badge(str(value(project, "projectType", default="项目")), "info"))
        layout.addLayout(header)

        score = value(project, "matchScore", default="-")
        action_row = QHBoxLayout()
        action_row.addWidget(Badge(f"匹配 {score}", "success" if self.parse_score(score) >= 85 else "warning" if self.parse_score(score) >= 70 else "info"))
        action_row.addWidget(Badge(f"截止 {value(project, 'deadline', default='-')}", "warning"))
        action_row.addStretch(1)
        copy_requirements = QPushButton("复制条件")
        copy_materials = QPushButton("复制材料")
        copy_all = primary_button("复制项目卡片")
        copy_requirements.clicked.connect(lambda: copy_text(self, str(value(self.project, "requirements", default="")), "报名条件"))
        copy_materials.clicked.connect(lambda: copy_text(self, str(value(self.project, "materials", default="")), "材料要求"))
        copy_all.clicked.connect(lambda: copy_text(self, self.compact_text(), "项目卡片"))
        action_row.addWidget(copy_requirements)
        action_row.addWidget(copy_materials)
        action_row.addWidget(copy_all)
        layout.addLayout(action_row)

        tabs = QTabWidget()
        overview = QWidget()
        overview_layout = QVBoxLayout(overview)
        for label, text in [
            ("院校", value(project, "schoolName", default="-")),
            ("项目名称", value(project, "projectName", default="-")),
            ("项目类型", value(project, "projectType", default="-")),
            ("截止时间", value(project, "deadline", default="-")),
            ("匹配分", value(project, "matchScore", default="-")),
        ]:
            overview_layout.addWidget(InfoRow(str(label), str(text)))
        requirements = MarkdownPanel()
        requirements.setMinimumHeight(180)
        requirements.set_text(str(value(project, "requirements", default="暂无报名条件")))
        materials = MarkdownPanel()
        materials.setMinimumHeight(180)
        materials.set_text(str(value(project, "materials", default="暂无材料要求")))
        overview_layout.addWidget(QLabel("报名条件"))
        overview_layout.addWidget(requirements, 1)
        overview_layout.addWidget(QLabel("材料要求"))
        overview_layout.addWidget(materials, 1)
        raw = JsonPanel()
        raw.set_json(project)
        tabs.addTab(overview, "项目详情")
        tabs.addTab(raw, "原始数据")
        layout.addWidget(tabs, 1)

        buttons = QDialogButtonBox(QDialogButtonBox.Close)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def parse_score(self, raw: Any) -> int:
        try:
            return max(0, min(100, int(float(str(raw).replace("%", "")))))
        except (TypeError, ValueError):
            return 0

    def compact_text(self) -> str:
        lines = [
            f"院校：{value(self.project, 'schoolName', default='')}",
            f"项目：{value(self.project, 'projectName', default='')}",
            f"类型：{value(self.project, 'projectType', default='')}",
            f"截止：{value(self.project, 'deadline', default='')}",
            f"匹配：{value(self.project, 'matchScore', default='')}",
            "",
            f"条件：{value(self.project, 'requirements', default='')}",
            f"材料：{value(self.project, 'materials', default='')}",
        ]
        return "\n".join(lines).strip()


class SchoolDialog(QDialog):
    def __init__(self, parent: QWidget):
        super().__init__(parent)
        self.setWindowTitle("新增学校")
        layout = QVBoxLayout(self)
        self.name = QLineEdit("示例大学")
        self.region = QLineEdit("北京")
        self.level = QComboBox()
        self.level.addItems(["985", "211", "双一流", "双非", "海外"])
        self.tags = QLineEdit("经管、工科")
        for label, widget in [
            ("学校名称", self.name),
            ("地区", self.region),
            ("层次", self.level),
            ("学科标签", self.tags),
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
            "region": self.region.text().strip(),
            "level": self.level.currentText(),
            "disciplineTags": self.tags.text().strip(),
        }


class SchoolProjectDialog(QDialog):
    def __init__(self, parent: QWidget):
        super().__init__(parent)
        self.setWindowTitle("新增院校项目")
        layout = QVBoxLayout(self)
        self.school_name = QLineEdit("示例大学")
        self.project_name = QLineEdit("2026 夏令营项目")
        self.project_type = QComboBox()
        self.project_type.addItems(["夏令营", "预推免", "九推", "直博", "其他"])
        self.deadline = QLineEdit("2026-07-01")
        self.requirements = QTextEdit()
        self.requirements.setMinimumHeight(90)
        self.requirements.setPlainText("专业排名前 30%，英语六级 550+，具备科研或竞赛经历。")
        self.materials = QTextEdit()
        self.materials.setMinimumHeight(90)
        self.materials.setPlainText("简历、成绩单、个人陈述、推荐信、获奖证明。")
        self.match_score = QSpinBox()
        self.match_score.setRange(0, 100)
        self.match_score.setValue(85)
        for label, widget in [
            ("学校名称", self.school_name),
            ("项目名称", self.project_name),
            ("项目类型", self.project_type),
            ("截止日期", self.deadline),
            ("报名条件", self.requirements),
            ("材料要求", self.materials),
            ("匹配分", self.match_score),
        ]:
            layout.addWidget(QLabel(label))
            layout.addWidget(widget)
        buttons = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def payload(self) -> dict[str, Any]:
        return {
            "schoolName": self.school_name.text().strip(),
            "projectName": self.project_name.text().strip(),
            "projectType": self.project_type.currentText(),
            "deadline": self.deadline.text().strip(),
            "requirements": self.requirements.toPlainText(),
            "materials": self.materials.toPlainText(),
            "matchScore": self.match_score.value(),
        }


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
            StatCard("平均 GPA", "-", "样本 20"),
            StatCard("高风险学员", "-", "需跟进"),
            StatCard("本月截止", "-", "院校项目"),
            StatCard("内容热度", "-", "均值"),
        ]
        for i, card in enumerate(self.overview_cards):
            grid.addWidget(card, 0, i)
        self.student_chart = ChartCard("学员院校分布", "#5b6cff", "本科院校区域", self.load)
        self.content_chart = ChartCard("GPA 分布", "#19b37b", "成绩区间", self.load)
        self.funnel_chart = ChartCard("申请阶段漏斗", "#8b5cf6", "转化路径", self.load)
        self.deadline_chart = ChartCard("院校截止趋势", "#f59e0b", "项目时间趋势", self.load)
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
                "funnel": self.client.analytics_funnel(),
                "deadlines": self.client.analytics_deadlines(),
                "studentsRows": self.client.students(),
                "projects": self.client.projects(),
                "themes": self.client.content_themes(),
            }

        self.run_api(fetch, self.render)

    def render(self, data: dict[str, Any]) -> None:
        overview = data.get("overview") or {}
        student_rows = data.get("studentsRows") or []
        projects = data.get("projects") or []
        themes = data.get("themes") or []
        avg_gpa = self.average_gpa(student_rows)
        high_risk = sum(1 for row in student_rows if str(value(row, "risk", "riskLevel", default="")) == "高")
        month_deadlines = sum(1 for row in projects if str(value(row, "deadline", default="")).startswith("2026-06"))
        content_heat = self.average_heat(themes)
        self.overview_cards[0].set_value(f"{avg_gpa:.2f}" if avg_gpa else value(overview, "avgGpa", default="3.62"), f"样本 {len(student_rows) or value(overview, 'students', default='20')}")
        self.overview_cards[1].set_value(high_risk or value(overview, "highRiskStudents", default="6"), "需跟进")
        self.overview_cards[2].set_value(month_deadlines or value(overview, "monthlyDeadlines", default="9"), "院校项目")
        self.overview_cards[3].set_value(f"{content_heat:.0f}" if content_heat else value(overview, "contentHeat", default="86"), "均值")
        DashboardPage._set_series(self.student_chart, data.get("student"))
        gpa_labels, gpa_values = self.gpa_distribution(student_rows)
        self.content_chart.set_data(gpa_labels, gpa_values)
        funnel = data.get("funnel") or []
        self.funnel_chart.set_data([row.get("name") for row in funnel], [row.get("value") for row in funnel])
        DashboardPage._set_series(self.deadline_chart, data.get("deadlines"))

    def average_gpa(self, rows: list[dict[str, Any]]) -> float:
        values = []
        for row in rows:
            try:
                values.append(float(str(value(row, "gpa", default="0")).replace("GPA", "").strip()))
            except (TypeError, ValueError):
                pass
        return sum(values) / len(values) if values else 0

    def average_heat(self, rows: list[dict[str, Any]]) -> float:
        values = []
        for row in rows:
            try:
                values.append(float(value(row, "heat", default=0) or 0))
            except (TypeError, ValueError):
                pass
        return sum(values) / len(values) if values else 0

    def gpa_distribution(self, rows: list[dict[str, Any]]) -> tuple[list[str], list[int]]:
        labels = ["3.2", "3.4", "3.6", "3.8", "4.0"]
        buckets = [0, 0, 0, 0, 0]
        for row in rows:
            try:
                gpa = float(str(value(row, "gpa", default="0")).replace("GPA", "").strip())
            except (TypeError, ValueError):
                continue
            if gpa < 3.3:
                buckets[0] += 1
            elif gpa < 3.5:
                buckets[1] += 1
            elif gpa < 3.7:
                buckets[2] += 1
            elif gpa < 3.9:
                buckets[3] += 1
            else:
                buckets[4] += 1
        return labels, buckets


class FeishuPage(BasePage):
    def __init__(self, client: ApiClient):
        super().__init__(client)
        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        refresh = primary_button("刷新状态")
        refresh.clicked.connect(self.load)
        self.feishu_refresh_button = refresh
        root.addWidget(PageHeader("飞书同步", "飞书文档、多维表格、机器人和同步日志", [refresh]))
        status_grid = QGridLayout()
        self.feishu_badges: dict[str, Badge] = {}
        self.feishu_detail_labels: dict[str, QLabel] = {}
        self.feishu_time_labels: dict[str, QLabel] = {}
        for index, (name, title, subtitle) in enumerate(
            [
                ("docs", "文档同步", "飞书知识库文档"),
                ("bitable", "多维表格", "内容素材与业务数据"),
                ("tasks", "任务同步", "跟进任务与提醒"),
                ("bot", "群机器人", "群消息推送"),
                ("larkCli", "lark-cli", "本机授权状态"),
            ]
        ):
            box = Card()
            title_row = QHBoxLayout()
            title_label = QLabel(title)
            title_label.setObjectName("SectionTitle")
            badge = Badge("待同步", "info")
            title_row.addWidget(title_label)
            title_row.addStretch(1)
            title_row.addWidget(badge)
            detail = QLabel(subtitle)
            detail.setObjectName("Muted")
            detail.setWordWrap(True)
            time_label = QLabel("最近同步：--")
            time_label.setObjectName("Muted")
            self.feishu_badges[name] = badge
            self.feishu_detail_labels[name] = detail
            self.feishu_time_labels[name] = time_label
            box.layout.addLayout(title_row)
            box.layout.addWidget(detail)
            box.layout.addWidget(time_label)
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
        left = QTabWidget()
        status_page = QWidget()
        status_layout = QVBoxLayout(status_page)
        status_layout.setContentsMargins(0, 0, 0, 0)
        self.status_output = JsonPanel()
        status_layout.addWidget(self.status_output)
        left.addTab(status_page, "同步状态")

        doc_page = QWidget()
        doc_layout = QVBoxLayout(doc_page)
        doc_layout.setContentsMargins(8, 8, 8, 8)
        doc_layout.addWidget(SectionHeader("创建飞书文档", "通过后端调用 lark-cli 或飞书 API，不在桌面端保存密钥"))
        self.feishu_doc_title = QLineEdit("FlowMind 桌面端生成文档")
        self.feishu_doc_parent = QLineEdit()
        self.feishu_doc_parent.setPlaceholderText("目标文件夹 token，可为空")
        self.feishu_doc_content = QTextEdit()
        self.feishu_doc_content.setMinimumHeight(120)
        self.feishu_doc_content.setPlainText("# FlowMind 桌面端文档\n\n这里可以填写需要同步到飞书的 Markdown 内容。")
        create_doc = primary_button("创建飞书文档")
        create_doc.clicked.connect(self.create_feishu_doc)
        doc_layout.addWidget(QLabel("标题"))
        doc_layout.addWidget(self.feishu_doc_title)
        doc_layout.addWidget(QLabel("父文件夹 token"))
        doc_layout.addWidget(self.feishu_doc_parent)
        doc_layout.addWidget(QLabel("内容"))
        doc_layout.addWidget(self.feishu_doc_content)
        doc_layout.addWidget(create_doc)
        doc_layout.addWidget(SectionHeader("读取飞书文档", "输入 docToken 后由后端读取正文"))
        self.feishu_doc_token = QLineEdit()
        self.feishu_doc_token.setPlaceholderText("docToken / docxToken")
        fetch_doc = QPushButton("读取飞书文档")
        fetch_doc.clicked.connect(self.fetch_feishu_doc)
        doc_layout.addWidget(self.feishu_doc_token)
        doc_layout.addWidget(fetch_doc)
        doc_layout.addStretch(1)
        left.addTab(doc_page, "文档操作")

        self.action_output = JsonPanel()
        left.addTab(self.action_output, "操作结果")
        files_page = QWidget()
        files_layout = QVBoxLayout(files_page)
        files_layout.setContentsMargins(0, 0, 0, 0)
        self.feishu_files_summary = InfoRow("知识库文件夹", "点击“知识库文件”加载")
        self.feishu_files_table = DataTable(["ID", "标题", "类型", "标签", "飞书链接"])
        file_actions = QHBoxLayout()
        open_file = primary_button("打开选中文件")
        copy_file = QPushButton("复制选中链接")
        open_file.clicked.connect(self.open_selected_feishu_file)
        copy_file.clicked.connect(self.copy_selected_feishu_file)
        file_actions.addStretch(1)
        file_actions.addWidget(copy_file)
        file_actions.addWidget(open_file)
        files_layout.addWidget(self.feishu_files_summary)
        files_layout.addLayout(file_actions)
        files_layout.addWidget(self.feishu_files_table, 1)
        left.addTab(files_page, "知识库文件")
        right = Card("同步日志")
        self.log_table = DataTable(["ID", "类型", "目标", "状态", "消息", "新增", "更新", "跳过", "错误", "时间"])
        self.log_empty_state = EmptyState("暂无同步记录，请先执行同步")
        self.log_empty_state.setVisible(False)
        right.layout.addWidget(self.log_empty_state)
        right.layout.addWidget(self.log_table)
        splitter.addWidget(left)
        splitter.addWidget(right)
        splitter.setSizes([420, 780])
        root.addWidget(splitter, 1)

    def load(self) -> None:
        self.set_feishu_refreshing(True)

        def fetch_all() -> dict[str, Any]:
            return {
                "status": {
                    "sync": self.client.knowledge_sync_status(),
                    "feishu": self.client.feishu_status(),
                },
                "logs": self.client.feishu_logs(),
            }

        self.run_api(fetch_all, self.render_load_result, on_fail=self.render_load_error)

    def set_feishu_refreshing(self, refreshing: bool) -> None:
        self.feishu_refresh_button.setEnabled(not refreshing)
        self.feishu_refresh_button.setText("刷新中..." if refreshing else "刷新状态")

    def render_load_result(self, result: dict[str, Any]) -> None:
        self.set_feishu_refreshing(False)
        status = result.get("status") if isinstance(result, dict) else {}
        logs = result.get("logs") if isinstance(result, dict) else []
        self.render_status(status if isinstance(status, dict) else {})
        self.render_logs(logs if isinstance(logs, list) else [])

    def render_load_error(self, message: str) -> None:
        self.set_feishu_refreshing(False)
        self.status_output.setPlainText(f"刷新失败：{message}")
        show_error(self, f"刷新失败：{message}")

    def render_status(self, result: dict[str, Any]) -> None:
        self.status_output.set_json(result)
        sync_status = result.get("sync") if isinstance(result.get("sync"), dict) else result
        feishu_status = result.get("feishu") if isinstance(result.get("feishu"), dict) else result
        for key, badge in self.feishu_badges.items():
            if key in ("docs", "bitable", "tasks", "bot") and isinstance(sync_status.get(key), dict):
                info = sync_status.get(key) or {}
                current = str(info.get("status") or "--")
                badge.setText(self.sync_status_label(current))
                badge.set_kind(self.feishu_status_kind(current))
                detail = (
                    f"共 {info.get('count', 0)} 项 · 新增 {info.get('added', 0)} · "
                    f"更新 {info.get('updated', 0)} · 跳过 {info.get('skipped', 0)} · 错误 {info.get('errors', 0)}"
                )
                self.feishu_detail_labels[key].setText(detail)
                self.feishu_time_labels[key].setText(f"最近同步：{info.get('lastSync') or '--'}")
                continue
            current = str(feishu_status.get(key, "--"))
            badge.setText(self.sync_status_label(current))
            badge.set_kind(self.feishu_status_kind(current))
            self.feishu_detail_labels[key].setText(f"状态码：{current}")
            self.feishu_time_labels[key].setText("最近同步：--")

    def feishu_status_kind(self, status: str) -> str:
        status = status.upper()
        if status in ("CONNECTED", "READY", "AVAILABLE", "SUCCESS", "NORMAL"):
            return "success"
        if status in ("WAITING", "DEMO", "PENDING", "PARTIAL", "--"):
            return "warning"
        if status in ("FAILED", "ERROR", "UNAVAILABLE"):
            return "danger"
        return "info"

    def sync_status_label(self, status: str) -> str:
        mapping = {
            "SUCCESS": "正常",
            "PARTIAL": "部分成功",
            "FAILED": "失败",
            "CONNECTED": "已连接",
            "READY": "就绪",
            "AVAILABLE": "可用",
            "WAITING": "等待中",
            "PENDING": "处理中",
            "DEMO": "演示",
            "--": "待开发",
            "-": "待开发",
        }
        return mapping.get(status.upper(), status or "待开发")

    def run_action(self, action: str, payload: dict[str, Any] | None = None) -> None:
        self.run_api(lambda: self.client.feishu_action(action, payload), lambda result: (self.render_action_result(result), self.load()))

    def render_action_result(self, result: Any) -> None:
        self.action_output.set_json(result)
        self.status_output.set_json(result)
        self.render_feishu_files(result)

    def render_feishu_files(self, result: Any) -> None:
        if not isinstance(result, dict):
            return
        files = result.get("files")
        if files is None and isinstance(result.get("data"), dict):
            files = result["data"].get("files")
        if files is None and isinstance(result.get("result"), list):
            files = result.get("result")
        if not isinstance(files, list):
            return
        folder_name = str(value(result, "name", default="知识库"))
        folder_token = str(value(result, "folderToken", default="-"))
        self.feishu_files_summary.set_value(f"{folder_name} / {folder_token} / {len(files)} 个文件")
        normalized = []
        for row in files:
            if not isinstance(row, dict):
                continue
            normalized.append(
                {
                    "id": value(row, "id", "mysqlId"),
                    "title": value(row, "title", "name"),
                    "type": value(row, "feishuType", "type"),
                    "tags": value(row, "tags", default=[]),
                    "url": value(row, "feishuUrl", "url"),
                }
            )
        self.feishu_files_table.set_rows(normalized, ["id", "title", "type", "tags", "url"])

    def selected_feishu_file_url(self) -> str:
        row = self.feishu_files_table.selected_row_data()
        if not row:
            return ""
        return str(value(row, "url", default=""))

    def open_selected_feishu_file(self) -> None:
        open_url(self, self.selected_feishu_file_url())

    def copy_selected_feishu_file(self) -> None:
        copy_text(self, self.selected_feishu_file_url(), "飞书链接")

    def create_feishu_doc(self) -> None:
        payload = {
            "title": self.feishu_doc_title.text().strip() or "FlowMind 桌面端生成文档",
            "content": self.feishu_doc_content.toPlainText(),
            "parentToken": self.feishu_doc_parent.text().strip() or None,
            "as": "user",
        }
        self.run_action("create_doc", payload)

    def fetch_feishu_doc(self) -> None:
        token = self.feishu_doc_token.text().strip()
        if not token:
            show_error(self, "请先输入飞书文档 token")
            return
        self.run_action("fetch_doc", {"docToken": token, "as": "user"})

    def render_logs(self, rows: list[dict[str, Any]]) -> None:
        normalized = [
            {
                "id": value(row, "id"),
                "syncType": value(row, "syncType", "type"),
                "targetName": value(row, "targetName", "target"),
                "status": value(row, "status"),
                "message": value(row, "message"),
                "added": value(row, "added", default=0),
                "updated": value(row, "updated", default=0),
                "skipped": value(row, "skipped", default=0),
                "errors": value(row, "errors", default=0),
                "createdAt": value(row, "createdAt"),
            }
            for row in rows
        ]
        self.log_table.set_rows(normalized, ["id", "syncType", "targetName", "status", "message", "added", "updated", "skipped", "errors", "createdAt"])
        self.log_empty_state.setVisible(not normalized)
        self.log_table.setVisible(bool(normalized))
        self.decorate_feishu_logs(normalized)

    def decorate_feishu_logs(self, rows: list[dict[str, Any]]) -> None:
        for row_index, row in enumerate(rows):
            sync_type = str(value(row, "syncType", default=""))
            status = str(value(row, "status", default=""))
            type_badge = Badge(self.sync_type_label(sync_type), self.sync_type_kind(sync_type))
            status_badge = Badge(self.sync_status_label(status), self.feishu_status_kind(status))
            self.log_table.setCellWidget(row_index, 1, type_badge)
            self.log_table.setCellWidget(row_index, 3, status_badge)

    def sync_type_label(self, sync_type: str) -> str:
        mapping = {
            "docs": "文档",
            "bitable": "多维表格",
            "tasks": "任务",
            "bot": "机器人",
        }
        return mapping.get(sync_type, sync_type or "-")

    def sync_type_kind(self, sync_type: str) -> str:
        if sync_type == "docs":
            return "primary"
        if sync_type == "bitable":
            return "success"
        if sync_type == "tasks":
            return "warning"
        if sync_type == "bot":
            return "purple"
        return "info"


class PromptDetailDialog(QDialog):
    def __init__(self, parent: QWidget, prompt: dict[str, Any]):
        super().__init__(parent)
        self.prompt = prompt
        self.setWindowTitle(str(value(prompt, "name", default="Prompt 模板详情")))
        self.resize(760, 560)

        layout = QVBoxLayout(self)
        header = QHBoxLayout()
        title = QLabel(str(value(prompt, "name", default="未命名模板")))
        title.setObjectName("PageTitle")
        title.setWordWrap(True)
        agent_type = str(value(prompt, "agentType", "agent", default="auto"))
        header.addWidget(title, 1)
        header.addWidget(Badge(agent_type, "primary"))
        layout.addLayout(header)

        actions = QHBoxLayout()
        copy_name = QPushButton("复制名称")
        copy_template = primary_button("复制模板")
        copy_name.clicked.connect(lambda: copy_text(self, str(value(self.prompt, "name", default="")), "Prompt 名称"))
        copy_template.clicked.connect(lambda: copy_text(self, str(value(self.prompt, "template", "text", default="")), "Prompt 模板"))
        actions.addWidget(Badge(f"ID {value(prompt, 'id', default='-')}", "info"))
        actions.addStretch(1)
        actions.addWidget(copy_name)
        actions.addWidget(copy_template)
        layout.addLayout(actions)

        tabs = QTabWidget()
        overview = QWidget()
        overview_layout = QVBoxLayout(overview)
        overview_layout.addWidget(InfoRow("Agent", agent_type))
        overview_layout.addWidget(InfoRow("模板名", str(value(prompt, "name", default="-"))))
        template = MarkdownPanel()
        template.setMinimumHeight(300)
        template.set_text(str(value(prompt, "template", "text", default="")))
        overview_layout.addWidget(QLabel("模板正文"))
        overview_layout.addWidget(template, 1)
        raw = JsonPanel()
        raw.set_json(prompt)
        tabs.addTab(overview, "模板")
        tabs.addTab(raw, "原始数据")
        layout.addWidget(tabs, 1)

        buttons = QDialogButtonBox(QDialogButtonBox.Close)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)


class SettingsPage(BasePage):
    def __init__(self, client: ApiClient, user: dict[str, Any] | None = None):
        super().__init__(client)
        self.user = normalize_user(user or client.current_user or {})
        self.is_admin = is_admin_user(self.user)
        root = QVBoxLayout(self)
        root.setContentsMargins(22, 22, 22, 22)
        role = Badge(role_label(str(self.user.get("role") or "")), "primary" if self.is_admin else "info")
        root.addWidget(PageHeader("系统设置", "模型、Prompt、接口、后端地址与权限接口", [role]))
        tabs = QTabWidget()
        tabs.addTab(self.build_model(), "AI 模型")
        tabs.addTab(self.build_connection(), "连接")
        tabs.addTab(self.build_prompts(), "Prompt")
        tabs.addTab(self.build_feishu_app(), "飞书应用")
        tabs.addTab(self.build_permissions(), "权限")
        tabs.addTab(self.build_routes(), "路由")
        tabs.addTab(self.build_logs(), "系统日志")
        root.addWidget(tabs, 1)

    def build_model(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        provider = QComboBox()
        provider.addItems(["mock", "deepseek", "openai-compatible"])
        api_key = QLineEdit()
        api_key.setPlaceholderText("请放在 application-local.yml 或环境变量中")
        api_key.setEnabled(False)
        tip = QLabel("防呆提示：不要把真实 API Key 写入 Git。桌面端只访问后端 API，不直接持有 LLM 密钥。")
        tip.setObjectName("Muted")
        tip.setWordWrap(True)
        layout.addWidget(QLabel("Provider"))
        layout.addWidget(provider)
        layout.addWidget(QLabel("API Key"))
        layout.addWidget(api_key)
        layout.addWidget(tip)
        layout.addStretch(1)
        return page

    def build_connection(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        self.base_url = QLineEdit(self.client.base_url)
        self.token = QLineEdit(self.client.token)
        save = primary_button("保存到当前进程")
        save.clicked.connect(self.save_connection)
        test = QPushButton("测试连接")
        test.clicked.connect(self.test_connection)
        self.connection_output = JsonPanel()
        layout.addWidget(QLabel("Base URL"))
        layout.addWidget(self.base_url)
        layout.addWidget(QLabel("Token"))
        layout.addWidget(self.token)
        layout.addWidget(save)
        layout.addWidget(test)
        layout.addWidget(QLabel("连接测试结果"))
        layout.addWidget(self.connection_output, 1)
        return page

    def build_prompts(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        form = Card("新增 Prompt 模板")
        self.prompt_agent = QComboBox()
        self.prompt_agent.addItems(["content", "knowledge", "student", "school", "feishu", "auto"])
        self.prompt_name = QLineEdit()
        self.prompt_name.setPlaceholderText("模板名称")
        self.prompt_template = QTextEdit()
        self.prompt_template.setMinimumHeight(90)
        self.prompt_template.setPlaceholderText("请输入 Prompt 模板正文，可包含 {topic}、{student} 等变量")
        add_prompt = primary_button("新增 Prompt")
        add_prompt.clicked.connect(self.create_prompt)
        form.layout.addWidget(QLabel("Agent"))
        form.layout.addWidget(self.prompt_agent)
        form.layout.addWidget(QLabel("名称"))
        form.layout.addWidget(self.prompt_name)
        form.layout.addWidget(QLabel("模板"))
        form.layout.addWidget(self.prompt_template)
        form.layout.addWidget(add_prompt)
        load = QPushButton("加载 Prompt")
        load.clicked.connect(self.load_prompts)
        prompt_tabs = QTabWidget()
        prompt_cards_page = QWidget()
        prompt_cards_layout = QVBoxLayout(prompt_cards_page)
        prompt_cards_layout.setContentsMargins(0, 0, 0, 0)
        prompt_cards_layout.addWidget(SectionHeader("Prompt 卡片", "展示 Agent、模板名和模板正文预览"))
        prompt_scroll = QScrollArea()
        prompt_scroll.setWidgetResizable(True)
        self.prompt_card_host = QWidget()
        self.prompt_card_grid = QGridLayout(self.prompt_card_host)
        self.prompt_card_grid.setSpacing(12)
        prompt_scroll.setWidget(self.prompt_card_host)
        prompt_cards_layout.addWidget(prompt_scroll, 1)
        self.prompt_table = DataTable(["ID", "Agent", "名称", "模板"])
        prompt_tabs.addTab(prompt_cards_page, "卡片")
        prompt_tabs.addTab(self.prompt_table, "表格")
        layout.addWidget(form)
        layout.addWidget(load)
        layout.addWidget(prompt_tabs, 1)
        return page

    def build_feishu_app(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        self.feishu_app_id = QLineEdit()
        self.feishu_app_id.setPlaceholderText("cli_xxx，仅作为桌面端显示占位")
        self.feishu_app_secret = QLineEdit()
        self.feishu_app_secret.setEchoMode(QLineEdit.Password)
        self.feishu_app_secret.setPlaceholderText("实际密钥应由后端本地配置或环境变量读取")
        tip = QLabel("桌面端不直接保存飞书 App Secret。创建文档、读取文档、同步知识库都应通过后端接口完成。")
        tip.setObjectName("Muted")
        tip.setWordWrap(True)
        layout.addWidget(QLabel("App ID"))
        layout.addWidget(self.feishu_app_id)
        layout.addWidget(QLabel("App Secret"))
        layout.addWidget(self.feishu_app_secret)
        layout.addWidget(tip)
        layout.addStretch(1)
        return page

    def build_permissions(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        row = QHBoxLayout()
        roles = primary_button("刷新角色权限")
        permissions = QPushButton("查看全部权限")
        role_perm = QPushButton("查询角色权限")
        self.role_code = QLineEdit("ADMIN")
        self.permission_filter = QLineEdit()
        self.permission_filter.setPlaceholderText("搜索权限名称、路径或代码")
        self.rbac_notice = QLabel(
            "团队管理员可以修改角色权限。当前账号不是团队管理员时，只能查看权限配置，不能保存修改。"
        )
        self.rbac_notice.setObjectName("Muted")
        self.rbac_notice.setWordWrap(True)
        self.rbac_refresh_button = roles
        self.rbac_permissions_button = permissions
        self.rbac_role_perm_button = role_perm
        roles.setEnabled(self.is_admin)
        role_perm.setEnabled(self.is_admin)
        row.addWidget(roles)
        row.addWidget(permissions)
        row.addWidget(self.role_code)
        row.addWidget(role_perm)
        row.addWidget(self.permission_filter, 1)
        layout.addLayout(row)
        layout.addWidget(self.rbac_notice)
        self.last_rbac_data: dict[str, Any] = {}
        self.role_permission_checks: dict[tuple[str, str], QCheckBox] = {}
        self.role_cards_host = QWidget()
        self.role_cards_layout = QVBoxLayout(self.role_cards_host)
        self.role_cards_layout.setContentsMargins(0, 0, 0, 0)
        self.role_cards_layout.setSpacing(12)
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setWidget(self.role_cards_host)
        self.permissions_output = JsonPanel()
        tabs = QTabWidget()
        tabs.addTab(scroll, "角色权限配置")
        tabs.addTab(self.permissions_output, "原始响应")
        layout.addWidget(tabs, 1)
        roles.clicked.connect(self.load_rbac)
        self.permission_filter.textChanged.connect(self.refresh_rbac_filter)
        permissions.clicked.connect(lambda: self.run_api(self.client.permissions, lambda result: self.permissions_output.set_json(result)))
        role_perm.clicked.connect(
            lambda: self.run_api(
                lambda: self.client.role_permissions(self.role_code.text().strip()),
                lambda result: self.permissions_output.set_json(result),
            )
        )
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

    def build_logs(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        logs = [
            ("2026-06-14", "ContentAgent mock response generated", "内容智能体完成 Mock 响应验证。"),
            ("2026-06-14", "Feishu mock sync completed", "飞书同步 Mock 流程完成。"),
            ("2026-06-19", "RBAC roles initialized in MySQL", "角色权限初始化完成。"),
            ("2026-06-19", "Desktop client migration", "Python 桌面端持续复刻 Vue Web 页面和后端 API。"),
        ]
        for timestamp, title, detail in logs:
            card = Card()
            top = QHBoxLayout()
            top.addWidget(Badge(timestamp, "primary"))
            title_label = QLabel(title)
            title_label.setObjectName("SectionTitle")
            top.addWidget(title_label, 1)
            card.layout.addLayout(top)
            desc = QLabel(detail)
            desc.setObjectName("Muted")
            desc.setWordWrap(True)
            card.layout.addWidget(desc)
            layout.addWidget(card)
        layout.addStretch(1)
        return page

    def load(self) -> None:
        self.load_routes()
        self.load_prompts()
        self.load_rbac()

    def save_connection(self) -> None:
        self.client.set_base_url(self.base_url.text().strip())
        self.client.set_token(self.token.text().strip())
        QMessageBox.information(self, "FlowMind", "已更新当前进程连接信息。登录页保存的信息将在下次登录后写入。")

    def test_connection(self) -> None:
        self.client.set_base_url(self.base_url.text().strip())
        self.client.set_token(self.token.text().strip())
        self.connection_output.setPlainText("测试中...")
        self.run_api(self.client.test_connection, lambda result: self.connection_output.set_json(result))

    def load_prompts(self) -> None:
        self.run_api(self.client.prompts, self.render_prompts)

    def render_prompts(self, rows: list[dict[str, Any]]) -> None:
        normalized = [
            {
                "id": value(row, "id"),
                "agentType": value(row, "agentType"),
                "name": value(row, "name"),
                "template": value(row, "template"),
                "_raw": row,
            }
            for row in rows
        ]
        self.prompt_table.set_rows(normalized, ["id", "agentType", "name", "template"])
        self.render_prompt_cards(rows)

    def render_prompt_cards(self, rows: list[dict[str, Any]]) -> None:
        clear_layout(self.prompt_card_grid)
        if not rows:
            self.prompt_card_grid.addWidget(EmptyState("暂无 Prompt 模板"), 0, 0)
            return
        for index, row in enumerate(rows):
            title = str(value(row, "name", default="未命名模板"))
            body = str(value(row, "template", "text", default=""))
            if len(body) > 150:
                body = f"{body[:150]}..."
            agent_type = str(value(row, "agentType", "agent", default="auto"))
            card = TextCard(title, body, f"Agent: {agent_type}", row)
            top = QHBoxLayout()
            top.addWidget(Badge(agent_type, self.prompt_agent_kind(agent_type)))
            top.addStretch(1)
            card.layout.insertLayout(0, top)
            actions = QHBoxLayout()
            detail = QPushButton("详情")
            copy_template = QPushButton("复制模板")
            detail.clicked.connect(lambda checked=False, payload=row: self.open_prompt_dialog(payload))
            copy_template.clicked.connect(lambda checked=False, text=str(value(row, "template", "text", default="")): copy_text(self, text, "Prompt 模板"))
            actions.addStretch(1)
            actions.addWidget(copy_template)
            actions.addWidget(detail)
            card.layout.addLayout(actions)
            card.clicked.connect(self.open_prompt_dialog)
            self.prompt_card_grid.addWidget(card, index // 2, index % 2)

    def prompt_agent_kind(self, agent_type: str) -> str:
        if agent_type == "content":
            return "purple"
        if agent_type == "knowledge":
            return "success"
        if agent_type == "student":
            return "warning"
        if agent_type == "school":
            return "danger"
        if agent_type == "feishu":
            return "primary"
        return "info"

    def open_prompt_dialog(self, payload: object) -> None:
        if isinstance(payload, dict):
            PromptDetailDialog(self, payload).exec()

    def create_prompt(self) -> None:
        name = self.prompt_name.text().strip()
        template = self.prompt_template.toPlainText().strip()
        if not name or not template:
            show_error(self, "请填写 Prompt 名称和模板正文")
            return
        payload = {
            "agentType": self.prompt_agent.currentText(),
            "name": name,
            "template": template,
        }
        self.run_api(lambda: self.client.create_prompt(payload), lambda _: self.load_prompts())

    def load_rbac(self) -> None:
        if not self.is_admin:
            self.permissions_output.setPlainText("当前账号不是团队管理员，只能查看基础设置，不能修改角色权限。")
            clear_layout(self.role_cards_layout)
            self.role_cards_layout.addWidget(EmptyState("当前账号不是团队管理员，角色权限配置为只读。"))
            return

        def fetch() -> dict[str, Any]:
            permissions = self.client.permissions()
            roles = self.client.roles()
            hydrated_roles = []
            for role in roles:
                role_copy = dict(role)
                if not role_copy.get("permissions") and role_copy.get("roleCode"):
                    detail = self.client.role_permissions(str(role_copy["roleCode"]))
                    if isinstance(detail, dict):
                        role_copy["permissionCodes"] = detail.get("permissionCodes") or []
                        role_copy["permissions"] = detail.get("permissions") or []
                    elif isinstance(detail, list):
                        role_copy["permissions"] = detail
                hydrated_roles.append(role_copy)
            return {"roles": hydrated_roles, "permissions": permissions}

        self.run_api(fetch, self.render_rbac, on_fail=lambda error: self.permissions_output.setPlainText(error))

    def render_rbac(self, data: dict[str, Any]) -> None:
        self.last_rbac_data = data
        roles = data.get("roles") or []
        permissions = self.filtered_permissions(data.get("permissions") or [])
        self.permissions_output.set_json(data)
        self.role_permission_checks = {}
        clear_layout(self.role_cards_layout)
        if not roles:
            self.role_cards_layout.addWidget(EmptyState("暂无角色数据"))
            return
        for role in roles:
            role_code = str(value(role, "roleCode", default=""))
            role_name = str(value(role, "roleName", default=role_code or "未命名角色"))
            selected = self.selected_permission_codes(role)
            card = Card()
            header = QHBoxLayout()
            title = QLabel(f"{role_name}  {role_code}")
            title.setObjectName("SectionTitle")
            header.addWidget(title, 1)
            header.addWidget(Badge(f"{len(selected)} 项权限", "success" if selected else "warning"))
            card.layout.addLayout(header)
            for group_name, group_permissions in self.group_permissions(permissions).items():
                group_box = Card(group_name)
                group_box.layout.setContentsMargins(12, 10, 12, 10)
                for permission in group_permissions:
                    code = self.permission_code(permission)
                    if not code:
                        continue
                    name = self.permission_name(permission)
                    path = value(permission, "pathPattern", "frontendRoute", default="")
                    check = QCheckBox(f"{name}    {path}")
                    check.setChecked(code in selected)
                    check.setEnabled(self.is_admin and role_code != "TEAM_ADMIN")
                    group_box.layout.addWidget(check)
                    self.role_permission_checks[(role_code, code)] = check
                card.layout.addWidget(group_box)
            actions = QHBoxLayout()
            select_all = QPushButton("全选")
            clear_all = QPushButton("清空")
            save = primary_button("保存该角色")
            editable = self.is_admin and role_code != "TEAM_ADMIN"
            select_all.setEnabled(editable)
            clear_all.setEnabled(editable)
            save.setEnabled(editable)
            select_all.clicked.connect(lambda checked=False, rc=role_code: self.set_role_checks(rc, True))
            clear_all.clicked.connect(lambda checked=False, rc=role_code: self.set_role_checks(rc, False))
            save.clicked.connect(lambda checked=False, rc=role_code: self.save_role_permissions(rc))
            actions.addStretch(1)
            actions.addWidget(select_all)
            actions.addWidget(clear_all)
            actions.addWidget(save)
            if role_code == "TEAM_ADMIN":
                actions.addWidget(Badge("受保护", "warning"))
            elif not self.is_admin:
                actions.addWidget(Badge("只读", "info"))
            card.layout.addLayout(actions)
            self.role_cards_layout.addWidget(card)
        self.role_cards_layout.addStretch(1)

    def refresh_rbac_filter(self) -> None:
        if self.last_rbac_data:
            self.render_rbac(self.last_rbac_data)

    def filtered_permissions(self, permissions: list[dict[str, Any]]) -> list[dict[str, Any]]:
        keyword = self.permission_filter.text().strip().lower() if hasattr(self, "permission_filter") else ""
        if not keyword:
            return permissions
        result = []
        for permission in permissions:
            haystack = " ".join(
                [
                    self.permission_code(permission),
                    self.permission_name(permission),
                    str(value(permission, "pathPattern", "frontendRoute", default="")),
                ]
            ).lower()
            if keyword in haystack:
                result.append(permission)
        return result

    def group_permissions(self, permissions: list[dict[str, Any]]) -> dict[str, list[dict[str, Any]]]:
        groups: dict[str, list[dict[str, Any]]] = {}
        for permission in permissions:
            route = str(value(permission, "frontendRoute", default=""))
            code = self.permission_code(permission)
            if route:
                key = route.strip("/") or "system"
            elif ":" in code:
                key = code.split(":", 1)[0]
            else:
                key = "system"
            label = {
                "dashboard": "Dashboard",
                "agent": "AI 工作台",
                "content": "内容运营",
                "knowledge": "知识库",
                "students": "学员管理",
                "student": "学员管理",
                "schools": "院校情报",
                "school": "院校情报",
                "analytics": "数据分析",
                "feishu": "飞书同步",
                "settings": "系统设置",
                "system": "系统",
            }.get(key, key)
            groups.setdefault(label, []).append(permission)
        return groups

    def set_role_checks(self, role_code: str, checked: bool) -> None:
        for (current_role, _code), check in self.role_permission_checks.items():
            if current_role == role_code and check.isEnabled():
                check.setChecked(checked)

    def permission_code(self, permission: dict[str, Any]) -> str:
        return str(value(permission, "permissionCode", "code", default=""))

    def permission_name(self, permission: dict[str, Any]) -> str:
        return str(value(permission, "permissionName", "name", default=self.permission_code(permission)))

    def selected_permission_codes(self, role: dict[str, Any]) -> set[str]:
        explicit = role.get("permissionCodes")
        if isinstance(explicit, list):
            return {str(code) for code in explicit}
        permissions = role.get("permissions")
        if isinstance(permissions, list):
            codes = set()
            for permission in permissions:
                if isinstance(permission, dict):
                    code = self.permission_code(permission)
                    if code:
                        codes.add(code)
                elif permission:
                    codes.add(str(permission))
            return codes
        return set()

    def save_role_permissions(self, role_code: str) -> None:
        if not self.is_admin:
            show_error(self, "只有团队管理员可以修改权限")
            return
        if role_code == "TEAM_ADMIN":
            show_error(self, "团队管理员权限受保护，不能在前端清空或覆盖")
            return
        selected = [
            code
            for (current_role, code), check in self.role_permission_checks.items()
            if current_role == role_code and check.isChecked()
        ]
        if not selected:
            show_error(self, "至少保留一个权限，避免该角色完全无法使用系统")
            return
        self.run_api(
            lambda: self.client.update_role_permissions(role_code, selected),
            lambda result: (self.permissions_output.set_json(result), self.load_rbac()),
        )

    def load_routes(self) -> None:
        self.run_api(self.client.routes, self.render_routes, on_fail=lambda _: None)

    def render_routes(self, rows: list[dict[str, Any]]) -> None:
        normalized = [{"service": value(row, "service"), "path": value(row, "path")} for row in rows]
        self.routes_table.set_rows(normalized, ["service", "path"])
