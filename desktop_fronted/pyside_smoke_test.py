from __future__ import annotations

import os

os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")

from PySide6.QtWidgets import QApplication

from flowmind_desktop.api import ApiClient
from flowmind_desktop.views import MainWindow


def main() -> None:
    app = QApplication([])
    client = ApiClient("offline://demo", "mock-jwt.demo")
    window = MainWindow(client, {"token": "mock-jwt.demo", "base_url": "offline://demo"}, lambda config: None)
    shell = window.centralWidget()

    for index in range(shell.stack.count()):
        shell.switch_page(index)
        app.processEvents()

    print(f"pyside smoke test passed: {shell.stack.count()} pages")
    window.close()
    app.quit()


if __name__ == "__main__":
    main()

