from __future__ import annotations

import os

os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")

from PySide6.QtWidgets import QApplication, QFrame, QProgressBar, QPushButton

from flowmind_desktop.api import ApiClient
from flowmind_desktop.views import KnowledgeDocDialog, MainWindow, PromptDetailDialog, SchoolProjectDetailDialog, SettingsPage


def main() -> None:
    app = QApplication([])
    client = ApiClient("offline://demo", "mock-jwt.demo")
    window = MainWindow(client, {"token": "mock-jwt.demo", "base_url": "offline://demo"}, lambda config: None)
    shell = window.centralWidget()

    for index in range(shell.stack.count()):
        shell.switch_page(index)
        app.processEvents()

    dashboard_page = shell.pages[0]
    dashboard_page.render(
        {
            "overview": client.analytics_overview(),
            "distribution": client.analytics_distribution(),
            "content": client.analytics_content_stats(),
            "funnel": client.analytics_funnel(),
            "deadlines": client.analytics_deadlines(),
        }
    )
    assert dashboard_page.content_stats.canvas.labels

    agent_page = shell.pages[1]
    session_id = client.sessions()[0]["id"]
    assert agent_page.agent_overview.objectName() == "AgentOverview"
    assert len(agent_page.agent_overview_chips) == 5
    agent_page.render_sessions(client.sessions())
    assert agent_page.session_list.itemWidget(agent_page.session_list.item(0)).objectName() == "SessionItem"
    assert agent_page.session_list.itemWidget(agent_page.session_list.item(0)).findChild(QPushButton) is not None
    assert agent_page.session_time_label("2026-06-19T00:00:00")
    agent_page.current_session_id = session_id
    agent_page.render_history(client.history("auto", session_id))
    assert agent_page.context_tabs.count() == 2
    assert agent_page.agent_selector.currentData() in {"auto", "content", "knowledge", "student", "school", "feishu"}
    agent_page.agent_capability_buttons["content"].click()
    assert agent_page.current_agent_type == "content"
    assert agent_page.agent_selector.currentData() == "content"
    assert agent_page.agent_capability_buttons["content"].text() == "当前"
    agent_page.set_agent_selector("auto")
    agent_page.current_agent_type = "auto"
    agent_page.update_agent_capability_buttons()
    assert agent_page.agent_selector.currentData() == "auto"
    assert agent_page.history_preview_list.count() >= 1
    assert agent_page.chat_inner.findChildren(QFrame, "InlinePanel")

    knowledge_page = shell.pages[2]
    docs = client.knowledge_docs()
    knowledge_page.render_docs(docs)
    knowledge_page.render_doc_detail(docs[0])
    assert knowledge_page.doc_card_grid.count() >= 1
    knowledge_page.set_syncing(True)
    assert knowledge_page.sync_button.text() == "同步中..."
    assert not knowledge_page.sync_button.isEnabled()
    knowledge_page.set_syncing(False)
    assert knowledge_page.sync_button.text() == "同步飞书"
    assert knowledge_page.sync_button.isEnabled()
    dialog = KnowledgeDocDialog(knowledge_page, docs[0], docs[0]["tags"])
    dialog.new_tag.setText("Smoke 标签")
    dialog.add_tag()
    assert "Smoke 标签" in dialog.tags()
    dialog.close()

    content_page = shell.pages[3]
    themes = client.content_themes()
    drafts = client.drafts()
    content_page.render_themes(themes)
    content_page.render_drafts(drafts)
    assert content_page.theme_card_grid.count() >= 1
    assert content_page.draft_card_grid.count() >= 1
    assert content_page.theme_for_draft(drafts[0]) is not None
    content_page.open_draft_card(drafts[0])
    assert content_page.current_draft_id == drafts[0]["id"]
    assert content_page.draft_title.text() == drafts[0]["title"]
    content_page.agent_hint_button.click()
    assert shell.stack.currentIndex() == 1
    shell.switch_page(3)

    students_page = shell.pages[4]
    students = client.students()
    students_page.render(students)
    students_page.open_student_card(students[0])
    students_page.render_student_analysis(client.analyze_student(students[0]["id"]))
    assert students_page.student_card_grid.count() >= len(students)
    assert students_page.student_name.text() == students[0]["name"]
    assert isinstance(students_page.table.cellWidget(0, 10), QProgressBar)

    schools_page = shell.pages[5]
    projects = client.projects()
    schools_page.render_projects(projects)
    schools_page.render_project_detail(projects[0])
    schools_page.recommend_project_payload(projects[0])
    project_dialog = SchoolProjectDetailDialog(schools_page, projects[0])
    assert schools_page.project_card_grid.count() >= len(projects)
    assert schools_page.project_title.text() == projects[0]["projectName"]
    assert isinstance(schools_page.project_table.cellWidget(0, 7), QProgressBar)
    assert project_dialog.windowTitle() == projects[0]["projectName"]
    project_dialog.close()

    feishu_page = shell.pages[7]
    feishu_page.render_status({"sync": client.knowledge_sync_status(), "feishu": client.feishu_status()})
    feishu_page.render_logs(client.feishu_logs())
    feishu_page.render_feishu_files(client.feishu_action("knowledge_files"))
    feishu_page.set_feishu_refreshing(True)
    assert feishu_page.feishu_refresh_button.text() == "刷新中..."
    assert not feishu_page.feishu_refresh_button.isEnabled()
    feishu_page.set_feishu_refreshing(False)
    assert feishu_page.feishu_refresh_button.text() == "刷新状态"
    assert feishu_page.feishu_refresh_button.isEnabled()
    assert feishu_page.feishu_badges["docs"].text()
    assert "新增" in feishu_page.feishu_detail_labels["docs"].text()
    assert feishu_page.log_table.cellWidget(0, 1) is not None
    feishu_page.render_logs([])
    assert not feishu_page.log_empty_state.isHidden()
    assert feishu_page.log_table.isHidden()
    feishu_page.render_logs(client.feishu_logs())
    assert feishu_page.feishu_files_table.rowCount() >= 1

    analytics_page = shell.pages[6]
    analytics_page.render(
        {
            "overview": client.analytics_overview(),
            "student": client.analytics_distribution(),
            "funnel": client.analytics_funnel(),
            "deadlines": client.analytics_deadlines(),
            "studentsRows": client.students(),
            "projects": client.projects(),
            "themes": client.content_themes(),
        }
    )
    assert analytics_page.overview_cards[0].value_label.text()
    assert analytics_page.content_chart.canvas.labels == ["3.2", "3.4", "3.6", "3.8", "4.0"]

    settings_page = shell.pages[8]
    prompts = client.prompts()
    settings_page.render_prompts(prompts)
    prompt_dialog = PromptDetailDialog(settings_page, prompts[0])
    assert settings_page.prompt_card_grid.count() >= len(prompts)
    assert prompt_dialog.windowTitle() == prompts[0]["name"]
    assert settings_page.prompt_table.rowCount() >= len(prompts)
    prompt_dialog.close()

    readonly_settings = SettingsPage(client, client.offline_user("content"))
    assert not readonly_settings.is_admin
    assert not readonly_settings.rbac_refresh_button.isEnabled()
    readonly_settings.load_rbac()
    assert "不是团队管理员" in readonly_settings.permissions_output.toPlainText()
    readonly_settings.close()

    print(f"pyside smoke test passed: {shell.stack.count()} pages")
    window.close()
    app.quit()


if __name__ == "__main__":
    main()
