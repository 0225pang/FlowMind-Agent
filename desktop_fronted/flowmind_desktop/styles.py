APP_QSS = """
* {
    font-family: "Microsoft YaHei", "PingFang SC", "Segoe UI";
    font-size: 13px;
    color: #152033;
}
QMainWindow, QWidget#Root, QWidget#ContentArea {
    background: #f6f8fc;
}
QFrame#Sidebar {
    background: #ffffff;
    border-right: 1px solid #e7ebf3;
}
QFrame#Topbar {
    background: rgba(255, 255, 255, 230);
    border-bottom: 1px solid #e7ebf3;
}
QFrame#Card {
    background: #ffffff;
    border: 1px solid #e7ebf3;
    border-radius: 8px;
}
QFrame#DashboardHero {
    background: qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #ffffff, stop:1 #f4f6ff);
    border: 1px solid #e7ebf3;
    border-radius: 8px;
}
QFrame#AgentOverview {
    background: qlineargradient(x1:0, y1:0, x2:1, y2:0, stop:0 #f4f6ff, stop:1 #f3fbf8);
    border: 1px solid rgba(91, 108, 255, 36);
    border-radius: 8px;
}
QLabel#AgentMasterDot {
    background: qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #5b6cff, stop:1 #19b37b);
    border-radius: 7px;
}
QFrame#LoginPanel {
    background: rgba(255, 255, 255, 230);
    border: 1px solid #ffffff;
    border-radius: 8px;
}
QFrame#LoginBrand {
    background: qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #5b6cff, stop:1 #8b5cf6);
    border-top-left-radius: 8px;
    border-bottom-left-radius: 8px;
}
QLabel#LoginMark {
    background: rgba(255,255,255,54);
    color: #ffffff;
    border-radius: 8px;
    font-size: 28px;
    font-weight: 850;
}
QLabel#LoginTitle {
    color: #ffffff;
    font-size: 34px;
    font-weight: 850;
}
QLabel#LoginSubtitle {
    color: rgba(255,255,255,230);
    font-size: 15px;
    line-height: 1.7;
}
QLabel#LogoMark {
    background: qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #5b6cff, stop:1 #19b37b);
    color: #ffffff;
    border-radius: 8px;
    font-weight: 800;
    font-size: 20px;
}
QLabel#PageTitle {
    font-size: 22px;
    font-weight: 800;
}
QLabel#HeroTitle {
    font-size: 28px;
    font-weight: 850;
    color: #101828;
}
QLabel#SectionTitle {
    font-size: 16px;
    font-weight: 750;
}
QLabel#Muted {
    color: #667085;
    font-size: 12px;
}
QLabel#StatValue {
    font-size: 26px;
    font-weight: 850;
    color: #152033;
}
QLabel#Tag {
    background: #eef2ff;
    color: #4c5cff;
    border-radius: 6px;
    padding: 3px 8px;
}
QLabel#Avatar {
    background: #152033;
    color: #ffffff;
    border-radius: 18px;
    font-weight: 800;
}
QLabel#Badge {
    border-radius: 6px;
    padding: 3px 8px;
    font-size: 12px;
    font-weight: 700;
}
QPushButton {
    border: 1px solid #d7deea;
    border-radius: 8px;
    background: #ffffff;
    padding: 8px 12px;
}
QPushButton:hover {
    border-color: #5b6cff;
    background: #f7f8ff;
}
QPushButton:pressed {
    background: #eef2ff;
}
QPushButton#PrimaryButton {
    background: #5b6cff;
    color: #ffffff;
    border-color: #5b6cff;
    font-weight: 700;
}
QPushButton#PrimaryButton:hover {
    background: #4c5cff;
}
QPushButton#DangerButton {
    color: #ef4444;
    border-color: #ffd4d4;
    background: #fff7f7;
}
QPushButton#PromptChip {
    min-height: 32px;
    border: 1px solid #dfe5f2;
    border-radius: 16px;
    padding: 4px 12px;
    background: #ffffff;
    color: #475467;
    font-size: 12px;
}
QPushButton#PromptChip:hover {
    color: #5b6cff;
    border-color: #5b6cff;
    background: #f8faff;
}
QPushButton#StarButton {
    border: 0;
    background: transparent;
}
QPushButton#NavButton {
    border: 0;
    border-radius: 8px;
    text-align: left;
    padding: 10px 12px;
    background: transparent;
    color: #394150;
}
QPushButton#NavButton:checked {
    background: qlineargradient(x1:0, y1:0, x2:1, y2:0, stop:0 rgba(91,108,255,36), stop:1 rgba(25,179,123,24));
    color: #4c5cff;
    font-weight: 750;
}
QPushButton#NavButton:disabled {
    color: #98a2b3;
    background: transparent;
}
QPushButton#DemoAccountButton {
    border: 1px solid rgba(255,255,255,66);
    background: rgba(255,255,255,28);
    color: #ffffff;
    border-radius: 8px;
    padding: 9px 12px;
    text-align: left;
    font-weight: 700;
}
QPushButton#DemoAccountButton:hover,
QPushButton#DemoAccountButton:checked {
    background: rgba(255,255,255,58);
    border-color: rgba(255,255,255,120);
}
QLineEdit, QTextEdit, QPlainTextEdit, QComboBox, QDateEdit, QSpinBox {
    background: #ffffff;
    border: 1px solid #d7deea;
    border-radius: 8px;
    padding: 8px;
    selection-background-color: #5b6cff;
}
QLineEdit:focus, QTextEdit:focus, QPlainTextEdit:focus, QComboBox:focus {
    border-color: #5b6cff;
}
QTableWidget {
    background: #ffffff;
    border: 1px solid #e7ebf3;
    border-radius: 8px;
    gridline-color: #eef2f7;
    selection-background-color: #eef2ff;
    selection-color: #152033;
}
QTextBrowser#MarkdownPanel,
QPlainTextEdit#JsonPanel {
    background: #ffffff;
    border: 1px solid #e7ebf3;
    border-radius: 8px;
    padding: 10px;
}
QHeaderView::section {
    background: #f8fafc;
    border: 0;
    border-bottom: 1px solid #e7ebf3;
    padding: 8px;
    font-weight: 750;
}
QTabWidget::pane {
    border: 0;
}
QTabBar::tab {
    background: #ffffff;
    border: 1px solid #e7ebf3;
    padding: 8px 14px;
    margin-right: 6px;
    border-radius: 8px;
}
QTabBar::tab:selected {
    background: #eef2ff;
    color: #4c5cff;
    font-weight: 750;
}
QListWidget {
    background: #ffffff;
    border: 1px solid #e7ebf3;
    border-radius: 8px;
}
QListWidget::item {
    padding: 10px;
    border-radius: 6px;
    margin: 2px;
}
QListWidget::item:selected {
    background: #eef2ff;
    color: #4c5cff;
}
QFrame#SessionItem {
    background: transparent;
    border: 0;
    border-radius: 6px;
}
QFrame#InlinePanel {
    background: #f8faff;
    border: 1px solid #dfe5f2;
    border-radius: 8px;
}
QLabel#InlinePanelTitle {
    color: #344054;
    font-size: 12px;
    font-weight: 750;
}
QListWidget::item:hover {
    background: #f4f6ff;
}
QScrollArea {
    border: 0;
    background: transparent;
}
"""

PRIMARY = "#5b6cff"
SUCCESS = "#19b37b"
WARNING = "#f59e0b"
DANGER = "#ef4444"
MUTED = "#667085"
LINE = "#e7ebf3"
BG = "#f6f8fc"
CARD = "#ffffff"
