from __future__ import annotations

from typing import Any, Callable

from PySide6.QtCore import QThread, Signal, Qt
from PySide6.QtGui import QColor, QFont, QPainter, QPen
from PySide6.QtWidgets import (
    QButtonGroup,
    QFrame,
    QGridLayout,
    QHBoxLayout,
    QLabel,
    QPlainTextEdit,
    QProgressBar,
    QPushButton,
    QTextBrowser,
    QTableWidget,
    QTableWidgetItem,
    QVBoxLayout,
    QWidget,
)


class ApiWorker(QThread):
    ok = Signal(object)
    fail = Signal(str)

    def __init__(self, fn: Callable[..., Any], *args: Any, **kwargs: Any):
        super().__init__()
        self.fn = fn
        self.args = args
        self.kwargs = kwargs

    def run(self) -> None:
        try:
            self.ok.emit(self.fn(*self.args, **self.kwargs))
        except Exception as exc:  # noqa: BLE001 - show user-facing API errors.
            self.fail.emit(str(exc))


class StreamWorker(QThread):
    session = Signal(str)
    thinking = Signal(str)
    trace = Signal(object)
    reasoning = Signal(str)
    delta = Signal(str)
    done = Signal(str)
    fail = Signal(str)

    def __init__(self, stream_fn: Callable[[], Any]):
        super().__init__()
        self.stream_fn = stream_fn
        self._stopped = False

    def stop(self) -> None:
        self._stopped = True

    def run(self) -> None:
        try:
            for event in self.stream_fn():
                if self._stopped:
                    return
                data = event.data
                if event.event == "session" and data.get("sessionId"):
                    self.session.emit(str(data["sessionId"]))
                elif event.event == "thinking":
                    self.thinking.emit(str(data.get("content") or ""))
                elif event.event == "trace":
                    self.trace.emit(data.get("items") or [])
                elif event.event == "reasoning":
                    self.reasoning.emit(str(data.get("content") or ""))
                elif event.event == "delta":
                    content = str(data.get("content") or "")
                    if content and content != "null":
                        self.delta.emit(content)
                elif event.event == "done":
                    self.done.emit(str(data.get("sessionId") or ""))
                    return
                elif event.event == "error":
                    self.fail.emit(str(data.get("message") or "流式对话失败"))
                    return
        except Exception as exc:  # noqa: BLE001
            self.fail.emit(str(exc))


class Card(QFrame):
    def __init__(self, title: str | None = None, parent: QWidget | None = None):
        super().__init__(parent)
        self.setObjectName("Card")
        self.layout = QVBoxLayout(self)
        self.layout.setContentsMargins(16, 14, 16, 14)
        self.layout.setSpacing(10)
        if title:
            label = QLabel(title)
            label.setObjectName("SectionTitle")
            self.layout.addWidget(label)


class Badge(QLabel):
    COLORS = {
        "primary": ("#eef2ff", "#4c5cff"),
        "success": ("#ecfdf5", "#087443"),
        "warning": ("#fffbeb", "#b54708"),
        "danger": ("#fff1f3", "#c01048"),
        "info": ("#f2f4f7", "#475467"),
        "purple": ("#f4f3ff", "#6941c6"),
    }

    def __init__(self, text: str = "", kind: str = "info"):
        super().__init__(text)
        self.setObjectName("Badge")
        self.setAlignment(Qt.AlignCenter)
        self.setMinimumHeight(24)
        self.set_kind(kind)

    def set_kind(self, kind: str) -> None:
        bg, fg = self.COLORS.get(kind, self.COLORS["info"])
        self.setStyleSheet(
            "QLabel#Badge {"
            f"background: {bg};"
            f"color: {fg};"
            "border-radius: 6px;"
            "padding: 3px 8px;"
            "font-size: 12px;"
            "font-weight: 700;"
            "}"
        )


class StarRating(QWidget):
    changed = Signal(int)

    def __init__(self, rating: int = 0, maximum: int = 5, show_label: bool = True):
        super().__init__()
        self.rating = int(rating or 0)
        self.maximum = maximum
        self.show_label = show_label
        self.buttons: list[QPushButton] = []
        self.group = QButtonGroup(self)
        self.group.setExclusive(False)
        layout = QHBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(2)
        for index in range(1, maximum + 1):
            button = QPushButton("★")
            button.setObjectName("StarButton")
            button.setFixedSize(24, 24)
            button.clicked.connect(lambda checked=False, value=index: self.set_rating(value))
            self.group.addButton(button, index)
            self.buttons.append(button)
            layout.addWidget(button)
        self.label = QLabel()
        self.label.setObjectName("Muted")
        if show_label:
            layout.addWidget(self.label)
        self.refresh()

    def set_rating(self, rating: int) -> None:
        self.rating = max(0, min(self.maximum, int(rating)))
        self.refresh()
        self.changed.emit(self.rating)

    def refresh(self) -> None:
        for index, button in enumerate(self.buttons, start=1):
            active = index <= self.rating
            button.setStyleSheet(
                "QPushButton#StarButton {"
                "border: 0;"
                "background: transparent;"
                f"color: {'#f59e0b' if active else '#d0d5dd'};"
                "font-size: 18px;"
                "padding: 0;"
                "}"
            )
        self.label.setText(f"{self.rating}/{self.maximum}")


class InfoRow(QWidget):
    def __init__(self, label: str, value: str = ""):
        super().__init__()
        layout = QHBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(8)
        self.label = QLabel(label)
        self.label.setObjectName("Muted")
        self.value = QLabel(value)
        self.value.setWordWrap(True)
        layout.addWidget(self.label)
        layout.addWidget(self.value, 1)

    def set_value(self, value: object) -> None:
        self.value.setText("" if value is None else str(value))


class MarkdownPanel(QTextBrowser):
    def __init__(self):
        super().__init__()
        self.setOpenExternalLinks(True)
        self.setObjectName("MarkdownPanel")

    def set_text(self, text: str) -> None:
        self.setMarkdown(text or "")


class JsonPanel(QPlainTextEdit):
    def __init__(self):
        super().__init__()
        self.setReadOnly(True)
        self.setObjectName("JsonPanel")

    def set_json(self, value: object) -> None:
        import json

        self.setPlainText(json.dumps(value, ensure_ascii=False, indent=2))


class PromptChip(QPushButton):
    def __init__(self, text: str):
        super().__init__(text)
        self.setObjectName("PromptChip")
        self.setMinimumHeight(32)
        self.setCursor(Qt.PointingHandCursor)


class SectionHeader(QWidget):
    def __init__(self, title: str, subtitle: str = "", actions: list[QPushButton] | None = None):
        super().__init__()
        layout = QHBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(10)
        text_col = QVBoxLayout()
        text_col.setSpacing(2)
        title_label = QLabel(title)
        title_label.setObjectName("SectionTitle")
        subtitle_label = QLabel(subtitle)
        subtitle_label.setObjectName("Muted")
        text_col.addWidget(title_label)
        if subtitle:
            text_col.addWidget(subtitle_label)
        layout.addLayout(text_col, 1)
        for action in actions or []:
            layout.addWidget(action)


class StatCard(Card):
    def __init__(self, title: str, value: str = "-", subtitle: str = ""):
        super().__init__()
        title_label = QLabel(title)
        title_label.setObjectName("Muted")
        value_label = QLabel(value)
        value_label.setObjectName("StatValue")
        sub_label = QLabel(subtitle)
        sub_label.setObjectName("Muted")
        self.value_label = value_label
        self.sub_label = sub_label
        self.layout.addWidget(title_label)
        self.layout.addWidget(value_label)
        self.layout.addWidget(sub_label)

    def set_value(self, value: Any, subtitle: str | None = None) -> None:
        self.value_label.setText(str(value))
        if subtitle is not None:
            self.sub_label.setText(subtitle)


class PageHeader(QWidget):
    def __init__(self, title: str, subtitle: str = "", actions: list[QPushButton] | None = None):
        super().__init__()
        layout = QHBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        text = QVBoxLayout()
        title_label = QLabel(title)
        title_label.setObjectName("PageTitle")
        sub_label = QLabel(subtitle)
        sub_label.setObjectName("Muted")
        text.addWidget(title_label)
        text.addWidget(sub_label)
        layout.addLayout(text, 1)
        for action in actions or []:
            layout.addWidget(action)


class EmptyState(QLabel):
    def __init__(self, text: str = "暂无数据"):
        super().__init__(text)
        self.setAlignment(Qt.AlignCenter)
        self.setObjectName("Muted")


class BarList(Card):
    def __init__(self, title: str):
        super().__init__(title)
        self.rows = QVBoxLayout()
        self.rows.setSpacing(8)
        self.layout.addLayout(self.rows)

    def set_data(self, labels: list[Any], values: list[Any]) -> None:
        clear_layout(self.rows)
        numeric = [float(v or 0) for v in values]
        max_value = max(numeric) if numeric else 1
        for label, value in zip(labels, numeric):
            row = QHBoxLayout()
            name = QLabel(str(label))
            name.setMinimumWidth(86)
            bar = QProgressBar()
            bar.setRange(0, 100)
            bar.setTextVisible(False)
            bar.setValue(int((value / max_value) * 100) if max_value else 0)
            count = QLabel(str(int(value) if value.is_integer() else value))
            count.setMinimumWidth(38)
            row.addWidget(name)
            row.addWidget(bar, 1)
            row.addWidget(count)
            self.rows.addLayout(row)


class ChartCanvas(QWidget):
    def __init__(self):
        super().__init__()
        self.labels: list[str] = []
        self.values: list[float] = []
        self.color = QColor("#5b6cff")
        self.setMinimumHeight(220)

    def set_data(self, labels: list[Any], values: list[Any], color: str = "#5b6cff") -> None:
        self.labels = [str(label) for label in labels]
        parsed: list[float] = []
        for value in values:
            try:
                parsed.append(float(value or 0))
            except (TypeError, ValueError):
                parsed.append(0)
        self.values = parsed
        self.color = QColor(color)
        self.update()

    def paintEvent(self, event) -> None:  # noqa: N802 - Qt override.
        painter = QPainter(self)
        painter.setRenderHint(QPainter.Antialiasing)
        rect = self.rect().adjusted(12, 12, -12, -12)
        painter.fillRect(rect, QColor("#ffffff"))
        if not self.labels or not self.values:
            painter.setPen(QColor("#98a2b3"))
            painter.drawText(rect, Qt.AlignCenter, "暂无数据")
            return

        max_value = max(self.values) or 1
        left = rect.left() + 72
        right = rect.right() - 38
        top = rect.top() + 8
        row_height = max(28, min(42, int(rect.height() / max(1, len(self.labels)))))
        painter.setFont(QFont("Microsoft YaHei", 9))

        for index, (label, value) in enumerate(zip(self.labels, self.values)):
            y = top + index * row_height
            if y + row_height > rect.bottom() + 12:
                break
            painter.setPen(QColor("#667085"))
            painter.drawText(rect.left(), y, 66, row_height, Qt.AlignVCenter | Qt.AlignRight, label)
            track_y = y + int(row_height * 0.28)
            track_h = max(8, int(row_height * 0.34))
            painter.setPen(Qt.NoPen)
            painter.setBrush(QColor("#eef2ff"))
            painter.drawRoundedRect(left, track_y, right - left, track_h, 5, 5)
            width = int((right - left) * (value / max_value))
            painter.setBrush(self.color)
            painter.drawRoundedRect(left, track_y, max(4, width), track_h, 5, 5)
            painter.setPen(QColor("#344054"))
            display = str(int(value)) if float(value).is_integer() else f"{value:.1f}"
            painter.drawText(right + 8, y, 32, row_height, Qt.AlignVCenter | Qt.AlignLeft, display)


class ChartCard(Card):
    def __init__(self, title: str, color: str = "#5b6cff"):
        super().__init__(title)
        self.canvas = ChartCanvas()
        self.color = color
        self.layout.addWidget(self.canvas, 1)

    def set_data(self, labels: list[Any], values: list[Any]) -> None:
        self.canvas.set_data(labels, values, self.color)


class TraceListPanel(QWidget):
    def __init__(self):
        super().__init__()
        self.layout = QVBoxLayout(self)
        self.layout.setContentsMargins(8, 8, 8, 8)
        self.layout.setSpacing(8)
        self.layout.addStretch(1)

    def set_items(self, items: list[dict[str, Any]]) -> None:
        clear_layout(self.layout)
        visible = [item for item in items if item.get("status") != "skipped"]
        if not visible:
            empty = EmptyState("暂无工具调用")
            self.layout.addWidget(empty)
            self.layout.addStretch(1)
            return
        for item in visible:
            card = Card()
            card.layout.setContentsMargins(12, 10, 12, 10)
            header = QHBoxLayout()
            status = str(item.get("status") or "used")
            kind = "success" if status == "used" else "danger" if status == "failed" else "info"
            header.addWidget(Badge(status, kind))
            name = QLabel(str(item.get("name") or "Tool"))
            name.setObjectName("SectionTitle")
            header.addWidget(name, 1)
            header.addWidget(QLabel(f"{item.get('durationMs') or 0}ms"))
            card.layout.addLayout(header)
            summary = QLabel(str(item.get("summary") or "已调用并返回上下文。"))
            summary.setWordWrap(True)
            summary.setObjectName("Muted")
            card.layout.addWidget(summary)
            if item.get("detail"):
                detail = QPlainTextEdit()
                detail.setReadOnly(True)
                detail.setMaximumHeight(140)
                detail.setPlainText(str(item.get("detail")))
                card.layout.addWidget(detail)
            self.layout.addWidget(card)
        self.layout.addStretch(1)


class CalendarGrid(QWidget):
    selected = Signal(str)

    def __init__(self):
        super().__init__()
        self.month = ""
        self.items_by_day: dict[str, list[dict[str, Any]]] = {}
        self.layout = QGridLayout(self)
        self.layout.setContentsMargins(0, 0, 0, 0)
        self.layout.setSpacing(8)
        self.set_month_items("", [])

    def set_month_items(self, month: str, rows: list[dict[str, Any]]) -> None:
        self.month = month or "2026-06"
        self.items_by_day = {}
        for row in rows:
            day = str(row.get("date") or row.get("publishDate") or "")
            if day:
                self.items_by_day.setdefault(day, []).append(row)
        self.render()

    def render(self) -> None:
        clear_layout(self.layout)
        headers = ["一", "二", "三", "四", "五", "六", "日"]
        for col, header in enumerate(headers):
            label = QLabel(header)
            label.setObjectName("Muted")
            label.setAlignment(Qt.AlignCenter)
            self.layout.addWidget(label, 0, col)
        day = 1
        for row in range(1, 7):
            for col in range(7):
                date_text = f"{self.month}-{day:02d}"
                count = len(self.items_by_day.get(date_text, []))
                button = QPushButton(f"{day}\n{count} 条" if count else str(day))
                button.setObjectName("CalendarCell")
                button.setMinimumHeight(58)
                if count:
                    button.setStyleSheet(
                        "QPushButton#CalendarCell {"
                        "background: #eef2ff;"
                        "border: 1px solid #5b6cff;"
                        "border-radius: 8px;"
                        "color: #4c5cff;"
                        "font-weight: 700;"
                        "}"
                    )
                button.clicked.connect(lambda checked=False, d=date_text: self.selected.emit(d))
                self.layout.addWidget(button, row, col)
                day += 1
                if day > 31:
                    return


class TextCard(Card):
    clicked = Signal(object)

    def __init__(self, title: str, body: str = "", meta: str = "", payload: object | None = None):
        super().__init__()
        self.payload = payload
        self.title_label = QLabel(title)
        self.title_label.setObjectName("SectionTitle")
        self.title_label.setWordWrap(True)
        self.body_label = QLabel(body)
        self.body_label.setWordWrap(True)
        self.body_label.setObjectName("Muted")
        self.meta_label = QLabel(meta)
        self.meta_label.setObjectName("Muted")
        self.layout.addWidget(self.title_label)
        if body:
            self.layout.addWidget(self.body_label)
        if meta:
            self.layout.addWidget(self.meta_label)
        self.setCursor(Qt.PointingHandCursor)

    def mousePressEvent(self, event) -> None:  # noqa: N802 - Qt override.
        self.clicked.emit(self.payload)
        super().mousePressEvent(event)


class DataTable(QTableWidget):
    def __init__(self, columns: list[str]):
        super().__init__(0, len(columns))
        self.columns = columns
        self.setHorizontalHeaderLabels(columns)
        self.verticalHeader().setVisible(False)
        self.setAlternatingRowColors(True)
        self.setSelectionBehavior(QTableWidget.SelectRows)
        self.setEditTriggers(QTableWidget.NoEditTriggers)

    def set_rows(self, rows: list[dict[str, Any]], keys: list[str]) -> None:
        self.setRowCount(len(rows))
        for row_i, row in enumerate(rows):
            self.setVerticalHeaderItem(row_i, QTableWidgetItem(str(row_i + 1)))
            for col_i, key in enumerate(keys):
                value = row.get(key, "")
                if isinstance(value, list):
                    value = "、".join(map(str, value))
                elif isinstance(value, dict):
                    value = str(value)
                item = QTableWidgetItem("" if value is None else str(value))
                item.setData(Qt.UserRole, row)
                self.setItem(row_i, col_i, item)
        self.resizeColumnsToContents()
        self.horizontalHeader().setStretchLastSection(True)

    def selected_row_data(self) -> dict[str, Any] | None:
        indexes = self.selectionModel().selectedRows()
        if not indexes:
            return None
        item = self.item(indexes[0].row(), 0)
        return item.data(Qt.UserRole) if item else None


def primary_button(text: str) -> QPushButton:
    button = QPushButton(text)
    button.setObjectName("PrimaryButton")
    return button


def danger_button(text: str) -> QPushButton:
    button = QPushButton(text)
    button.setObjectName("DangerButton")
    return button


def clear_layout(layout: QVBoxLayout | QHBoxLayout) -> None:
    while layout.count():
        item = layout.takeAt(0)
        widget = item.widget()
        if widget:
            widget.deleteLater()
        child_layout = item.layout()
        if child_layout:
            clear_layout(child_layout)
