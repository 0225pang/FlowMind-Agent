from __future__ import annotations

from flowmind_desktop.api import ApiClient


def assert_non_empty(value, name: str) -> None:
    if not value:
        raise AssertionError(f"{name} should not be empty")


def assert_true(condition: bool, name: str) -> None:
    if not condition:
        raise AssertionError(name)


def main() -> None:
    client = ApiClient(base_url="offline://demo", token="mock-jwt.demo")

    admin_login = client.login("admin", "123456")
    assert_true(admin_login["token"] == "mock-jwt.admin", "admin login should return account token")
    assert_true(admin_login["user"]["role"] == "ADMIN", "admin login should include role")
    content_login = client.login("content", "123456")
    assert_true(content_login["user"]["role"] == "CONTENT_OPERATOR", "content login should include role")
    assert_true("/api/content/**" in content_login["user"].get("permissions", []), "content role should include content permission")
    assert_true(client.test_connection()["ok"], "offline connection should be ok")
    assert_non_empty(client.analytics_overview(), "overview")
    assert_non_empty(client.analytics_distribution(), "student distribution")
    assert_non_empty(client.content_themes(), "content themes")
    drafts = client.drafts()
    assert_non_empty(drafts, "drafts")
    assert_non_empty(client.calendar(), "calendar")
    docs = client.knowledge_docs()
    assert_non_empty(docs, "knowledge docs")
    assert_non_empty(client.vector_search("课程论文"), "vector search")
    assert_non_empty(client.students(), "students")
    assert_non_empty(client.schools(), "schools")
    assert_non_empty(client.projects(), "projects")
    assert_non_empty(client.feishu_status(), "feishu status")
    assert_non_empty(client.feishu_logs(), "feishu logs")
    assert_non_empty(client.roles(), "roles")
    assert_non_empty(client.permissions(), "permissions")

    session_id = client.new_session()
    events = list(client.stream_chat({"sessionId": session_id, "message": "帮我生成小红书文案"}))
    assert_true([event.event for event in events][0] == "session", "stream should start with session")
    assert_true(any(event.event == "delta" for event in events), "stream should include delta")
    assert_true(len(client.history("auto", session_id)) >= 2, "stream should write history")
    assert_true(client.clear_history("auto", session_id)["ok"], "clear history should return ok")
    assert_true(client.history("auto", session_id) == [], "history should be empty after clear")

    sop = client.content_generate("xiaohongshu", {"topic": "保研简历", "agentType": "content"})
    assert_true(len(sop.get("drafts", [])) >= 3, "content sop should return draft variants")
    image = client.add_draft_image(
        int(drafts[0]["id"]),
        {"name": "demo.png", "url": "https://example.com/demo.png", "storageProvider": "local", "objectKey": "demo.png"},
    )
    assert_true(image["url"].startswith("https://"), "draft image should include url")

    updated_doc = client.update_doc_tags(int(docs[0]["id"]), ["测试标签", "离线"])
    assert_true("测试标签" in updated_doc.get("tags", []), "knowledge tags should update")
    assert_true(client.knowledge_sync_status()["docs"]["count"] >= 1, "knowledge sync status should include docs count")
    assert_non_empty(client.knowledge_sync_logs(), "knowledge sync logs")

    student = client.create_student({"name": "Smoke 学员", "school": "示例大学", "major": "计算机", "progress": 70})
    student = client.update_student(int(student["id"]), {"name": "Smoke 学员更新", "progress": 80})
    assert_true(student["name"] == "Smoke 学员更新", "student update should work")
    analysis = client.analyze_student(int(student["id"]))
    assert_non_empty(analysis.get("actions"), "student analysis actions")
    assert_true(client.delete_student(int(student["id"]))["ok"], "student delete should work")

    school = client.create_school({"name": "Smoke 大学", "region": "北京", "level": "985"})
    assert_true(school["name"] == "Smoke 大学", "school create should work")
    project = client.create_project({"schoolName": "Smoke 大学", "projectName": "Smoke 项目", "deadline": "2026-07-01"})
    assert_true(project["projectName"] == "Smoke 项目", "project create should work")
    assert_non_empty(client.recommend_schools({"gpa": "3.80"}), "school recommendation")

    files = client.feishu_action("knowledge_files")
    assert_non_empty(files.get("files"), "feishu knowledge files")
    created_doc = client.feishu_action("create_doc", {"title": "Smoke 飞书文档", "content": "正文", "as": "user"})
    assert_true(created_doc.get("token") or created_doc.get("url"), "feishu create doc should return reference")
    fetched_doc = client.feishu_action("fetch_doc", {"docToken": "offline_demo_doc_token", "as": "user"})
    assert_true(fetched_doc.get("content"), "feishu fetch doc should return content")

    prompt = client.create_prompt({"agentType": "content", "name": "Smoke Prompt", "template": "围绕 {topic} 生成内容"})
    assert_true(prompt["name"] == "Smoke Prompt", "prompt create should work")
    permission_codes = [permission["permissionCode"] for permission in client.permissions()[:2]]
    role_result = client.update_role_permissions("OPERATOR", permission_codes)
    assert_true(role_result.get("roleCode") == "OPERATOR", "role permission update should return role")

    print("desktop_fronted smoke test passed")


if __name__ == "__main__":
    main()
