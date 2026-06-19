from __future__ import annotations

import json
import sys
from pathlib import Path

from PySide6.QtWidgets import QApplication

from .api import ApiClient
from .styles import APP_QSS
from .views import MainWindow


CONFIG_DIR = Path.home() / ".flowmind_desktop"
CONFIG_FILE = CONFIG_DIR / "config.json"


def load_config() -> dict[str, str]:
    if not CONFIG_FILE.exists():
        return {}
    try:
        return json.loads(CONFIG_FILE.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {}


def save_config(config: dict[str, str]) -> None:
    CONFIG_DIR.mkdir(parents=True, exist_ok=True)
    CONFIG_FILE.write_text(json.dumps(config, ensure_ascii=False, indent=2), encoding="utf-8")


def main() -> int:
    app = QApplication(sys.argv)
    app.setApplicationName("FlowMind Agent Desktop")
    app.setStyleSheet(APP_QSS)

    config = load_config()
    client = ApiClient(
        base_url=config.get("base_url", "http://localhost:8080"),
        token=config.get("token", "mock-jwt.demo"),
    )
    window = MainWindow(client, config, save_config)
    window.resize(1380, 860)
    window.show()
    return app.exec()


if __name__ == "__main__":
    raise SystemExit(main())

