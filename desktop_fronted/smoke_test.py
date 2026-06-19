from __future__ import annotations

from flowmind_desktop.api import ApiClient


def assert_non_empty(value, name: str) -> None:
    if not value:
        raise AssertionError(f"{name} should not be empty")


def main() -> None:
    client = ApiClient(base_url="offline://demo", token="mock-jwt.demo")

    assert client.login("admin", "123456")["token"] == "mock-jwt.demo"
    assert_non_empty(client.analytics_overview(), "overview")
    assert_non_empty(client.analytics_distribution(), "student distribution")
    assert_non_empty(client.content_themes(), "content themes")
    assert_non_empty(client.drafts(), "drafts")
    assert_non_empty(client.calendar(), "calendar")
    assert_non_empty(client.knowledge_docs(), "knowledge docs")
    assert_non_empty(client.vector_search("课程论文"), "vector search")
    assert_non_empty(client.students(), "students")
    assert_non_empty(client.schools(), "schools")
    assert_non_empty(client.projects(), "projects")
    assert_non_empty(client.feishu_status(), "feishu status")
    assert_non_empty(client.feishu_logs(), "feishu logs")
    assert_non_empty(client.roles(), "roles")
    assert_non_empty(client.permissions(), "permissions")
    print("desktop_fronted smoke test passed")


if __name__ == "__main__":
    main()
