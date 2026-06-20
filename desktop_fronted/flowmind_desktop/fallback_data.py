from __future__ import annotations

from copy import deepcopy
from datetime import date
from typing import Any


OVERVIEW = {
    "students": 20,
    "activeProjects": 15,
    "contentTopics": 48,
    "taskRate": "86%",
}

STUDENT_DISTRIBUTION = {
    "labels": ["985", "211", "双非", "其他"],
    "values": [8, 5, 4, 3],
}

CONTENT_STATS = {
    "labels": ["简历", "套磁", "规划", "面试", "科研"],
    "values": [18, 12, 9, 7, 6],
}

APPLICATION_FUNNEL = [
    {"name": "初筛", "value": 20},
    {"name": "材料准备", "value": 17},
    {"name": "夏令营", "value": 12},
    {"name": "面试", "value": 9},
    {"name": "录取", "value": 6},
]

SCHOOL_DEADLINES = {
    "labels": ["6月", "7月", "8月", "9月", "10月"],
    "values": [3, 7, 12, 9, 4],
}

AGENTS = [
    {"name": "FlowMindAgent", "type": "auto", "description": "总智能体入口，自动判断应该调用哪个专业 Agent。"},
    {"name": "ContentAgent", "type": "content", "description": "内容运营、小红书、朋友圈和 SOP 生成。"},
    {"name": "KnowledgeAgent", "type": "knowledge", "description": "知识库问答、资料总结和语义检索。"},
    {"name": "StudentAgent", "type": "student", "description": "学员画像、申请风险和材料建议。"},
    {"name": "SchoolAgent", "type": "school", "description": "院校项目检索、匹配和推荐。"},
    {"name": "FeishuAgent", "type": "feishu", "description": "飞书文档、同步和共享知识库能力。"},
]

SESSIONS = [
    {
        "id": "demo-session-001",
        "title": "根据保研知识库总结课程论文写法",
        "agentType": "auto",
        "turnCount": 2,
        "createdAt": "2026-06-18T10:00:00",
        "updatedAt": "2026-06-18T10:05:00",
    },
    {
        "id": "demo-session-002",
        "title": "生成保研小红书选题",
        "agentType": "content",
        "turnCount": 1,
        "createdAt": "2026-06-18T11:00:00",
        "updatedAt": "2026-06-18T11:03:00",
    },
]

CONVERSATION_HISTORY = [
    {
        "id": 1,
        "agentType": "auto",
        "sessionId": "demo-session-001",
        "turnIndex": 0,
        "role": "user",
        "content": "根据保研知识库，期末如何速成课程论文？",
        "metadata": None,
        "createdAt": "2026-06-18T10:00:00",
    },
    {
        "id": 2,
        "agentType": "knowledge",
        "sessionId": "demo-session-001",
        "turnIndex": 0,
        "role": "assistant",
        "content": "可以按“选题缩小、资料检索、结构搭建、案例填充、格式检查”五步推进。先把题目压到一个可论证的问题，再用知识库资料补足论据。",
        "metadata": '{"traceItems":[{"name":"SemanticVectorSearch","status":"used","summary":"命中课程论文写作片段"}]}',
        "createdAt": "2026-06-18T10:00:20",
    },
]

CONTENT_THEMES = [
    {
        "id": 1,
        "title": "保研简历怎么写才像有科研潜力",
        "topic": "保研简历",
        "platform": "小红书",
        "type": "爆款仿写",
        "status": "已生成",
        "heat": 96,
        "rating": 4,
        "tags": ["保研简历", "科研潜力", "材料优化"],
        "plannedDate": "2026-06-18",
        "summary": "围绕简历结构、科研表达和材料证据生成多版本笔记。",
    },
    {
        "id": 2,
        "title": "导师套磁邮件怎么写不尴尬",
        "topic": "导师套磁",
        "platform": "小红书",
        "type": "经验干货",
        "status": "待发布",
        "heat": 91,
        "rating": 5,
        "tags": ["导师套磁", "邮件模板", "面试准备"],
        "plannedDate": "2026-06-20",
        "summary": "拆解套磁邮件结构，强调研究兴趣、匹配理由和克制表达。",
    },
    {
        "id": 3,
        "title": "低年级保研规划清单",
        "topic": "保研规划",
        "platform": "朋友圈",
        "type": "人设表达",
        "status": "待创作",
        "heat": 88,
        "rating": 3,
        "tags": ["低年级", "规划", "时间线"],
        "plannedDate": "2026-06-22",
        "summary": "强调早规划不是焦虑，而是把不确定拆成可执行任务。",
    },
    {
        "id": 4,
        "title": "夏令营面试自我介绍模板",
        "topic": "夏令营面试",
        "platform": "小红书",
        "type": "模板",
        "status": "已发布",
        "heat": 93,
        "rating": 5,
        "tags": ["面试", "自我介绍", "模板"],
        "plannedDate": "2026-06-08",
        "summary": "围绕背景、科研、目标方向和结尾承接设计面试表达。",
    },
    {
        "id": 5,
        "title": "收到 offer 后如何发朋友圈不浮夸",
        "topic": "成功案例",
        "platform": "朋友圈",
        "type": "成果型人设",
        "status": "已生成",
        "heat": 90,
        "rating": 4,
        "tags": ["offer", "成功案例", "朋友圈"],
        "plannedDate": "2026-06-24",
        "summary": "用克制语气表达结果、过程和方法，避免强营销感。",
    },
    {
        "id": 6,
        "title": "科研小白如何找到第一个项目",
        "topic": "科研入门",
        "platform": "小红书",
        "type": "爆款干货",
        "status": "待发布",
        "heat": 89,
        "rating": 0,
        "tags": ["科研入门", "项目经历", "保研"],
        "plannedDate": "2026-06-25",
        "summary": "把找项目拆成课程项目、导师课题、竞赛延展和论文复现四条路径。",
    },
]

COPY_DRAFTS = [
    {
        "id": 101,
        "themeId": 1,
        "title": "保研er别再这样写简历了",
        "channel": "小红书",
        "version": "干货版",
        "style": "干货",
        "content": "保研简历不是经历堆砌，而是让老师快速看到你的科研潜力。建议每段经历都写清楚：你做了什么、用了什么方法、最后产出了什么证据。",
        "usageStatus": "已使用",
        "usedDate": "2026-06-12",
        "generatedAt": "2026-06-10 10:30",
        "owner": "内容运营",
        "feedback": "收藏率高，适合继续拆成简历模板系列。",
        "rating": 4,
        "imageSuggestion": "三栏式简历改前改后对比图。",
    },
    {
        "id": 102,
        "themeId": 1,
        "title": "简历没亮点？先改这 4 个位置",
        "channel": "小红书",
        "version": "情绪增强版",
        "style": "学姐风",
        "content": "很多同学不是经历不够，而是不知道怎么把经历讲成优势。尤其是科研、竞赛、课程项目这三块，写法不同，老师看到的信息完全不同。",
        "usageStatus": "未使用",
        "generatedAt": "2026-06-10 10:34",
        "owner": "内容运营",
        "feedback": "适合配案例图发布。",
        "rating": 3,
        "imageSuggestion": "配一张“简历亮点提取清单”长图。",
    },
    {
        "id": 201,
        "themeId": 2,
        "title": "套磁邮件别一上来就求机会",
        "channel": "小红书",
        "version": "干货版",
        "style": "干货",
        "content": "一封好的套磁邮件，核心不是表达“我很想来”，而是说明你为什么匹配这位老师：研究方向、已有经历、能继续推进的问题。",
        "usageStatus": "未使用",
        "generatedAt": "2026-06-13 09:12",
        "owner": "内容运营",
        "feedback": "待配套邮件模板图。",
        "rating": 0,
        "imageSuggestion": "配“邮件结构拆解图”。",
    },
    {
        "id": 301,
        "themeId": 3,
        "title": "大一大二做保研规划，重点不是焦虑",
        "channel": "朋友圈",
        "version": "专业理性版",
        "style": "克制",
        "content": "低年级做规划，不是为了提前焦虑，而是知道 GPA、英语、科研、竞赛分别在什么时候该补到什么程度。",
        "usageStatus": "未使用",
        "generatedAt": "2026-06-14 21:10",
        "owner": "主理人",
        "feedback": "可作为招生转化铺垫。",
        "rating": 0,
        "imageSuggestion": "配一张低年级三阶段时间轴。",
    },
    {
        "id": 401,
        "themeId": 4,
        "title": "夏令营自我介绍照这个顺序讲",
        "channel": "小红书",
        "version": "转化引导版",
        "style": "干货",
        "content": "自我介绍建议按“基础背景-核心经历-研究兴趣-项目匹配”来讲。不要背简历，要让老师听到一条清楚的成长线。",
        "usageStatus": "已使用",
        "usedDate": "2026-06-08",
        "generatedAt": "2026-06-06 13:00",
        "owner": "内容运营",
        "feedback": "互动率 12%，评论区追问较多。",
        "rating": 5,
        "imageSuggestion": "配一张自我介绍流程卡。",
    },
]

CONTENT_CALENDAR = [
    {"id": 1, "draftId": 401, "themeId": 4, "date": "2026-06-08", "title": "夏令营自我介绍照这个顺序讲", "channel": "小红书", "status": "已发布", "usageStatus": "已使用"},
    {"id": 2, "draftId": 101, "themeId": 1, "date": "2026-06-12", "title": "保研er别再这样写简历了", "channel": "小红书", "status": "已发布", "usageStatus": "已使用"},
    {"id": 3, "draftId": 201, "themeId": 2, "date": "2026-06-20", "title": "套磁邮件别一上来就求机会", "channel": "小红书", "status": "待发布", "usageStatus": "未使用"},
    {"id": 4, "draftId": 301, "themeId": 3, "date": "2026-06-22", "title": "大一大二做保研规划，重点不是焦虑", "channel": "朋友圈", "status": "待创作", "usageStatus": "未使用"},
]

KNOWLEDGE_DOCS = [
    {
        "id": 1,
        "feishuToken": "demo_doc_001",
        "title": "期末如何速成课程论文",
        "content": "课程论文可以先确定一个小问题，再围绕问题建立论点、资料和案例。",
        "summary": "课程论文写作流程与速成结构。",
        "tags": ["论文", "期末", "写作模板"],
        "feishuUrl": "https://example.com/doc/demo_doc_001",
        "feishuType": "docx",
        "createdAt": "2026-06-18 10:00:00",
        "updatedAt": "2026-06-18 10:00:00",
    },
    {
        "id": 2,
        "feishuToken": "demo_doc_002",
        "title": "保研简历科研经历表达模板",
        "content": "科研经历要写清楚问题、方法、结果和证据，避免只写参与。",
        "summary": "简历科研经历的结构化表达。",
        "tags": ["保研", "简历", "科研"],
        "feishuUrl": "https://example.com/doc/demo_doc_002",
        "feishuType": "docx",
        "createdAt": "2026-06-18 11:00:00",
        "updatedAt": "2026-06-18 11:00:00",
    },
]

STUDENTS = [
    {
        "id": i,
        "name": f"学员{i:02d}",
        "school": "示例大学",
        "major": ["金融学", "计算机", "食品科学", "法学"][i % 4],
        "gpa": f"{3.45 + (i % 5) * 0.08:.2f}",
        "rank": f"{i + 2}/120",
        "english": f"六级 {530 + i * 3}",
        "targetSchool": "985/211 相关项目",
        "stage": ["材料准备", "夏令营", "预推免", "面试准备"][i % 4],
        "risk": ["低", "中", "高"][i % 3],
        "progress": 45 + (i * 5) % 50,
    }
    for i in range(1, 21)
]

SCHOOLS = [
    {"id": 1, "name": "清华大学", "region": "北京", "level": "985", "disciplineTags": "工科、管理"},
    {"id": 2, "name": "北京大学", "region": "北京", "level": "985", "disciplineTags": "综合、理科"},
    {"id": 3, "name": "复旦大学", "region": "上海", "level": "985", "disciplineTags": "经管、新闻"},
    {"id": 4, "name": "浙江大学", "region": "浙江", "level": "985", "disciplineTags": "工科、农学"},
]

PROJECTS = [
    {
        "id": 1,
        "schoolName": "清华大学",
        "projectName": "2026 夏令营项目",
        "projectType": "夏令营",
        "deadline": "2026-07-01",
        "requirements": "专业排名前 10%，英语六级优秀。",
        "materials": "简历、成绩单、个人陈述、推荐信。",
        "matchScore": 88,
    },
    {
        "id": 2,
        "schoolName": "复旦大学",
        "projectName": "管理学院预推免",
        "projectType": "预推免",
        "deadline": "2026-09-15",
        "requirements": "经管相关专业，具备科研或竞赛经历。",
        "materials": "申请表、简历、成绩单、获奖证明。",
        "matchScore": 84,
    },
]

FEISHU_STATUS = {
    "docs": "CONNECTED",
    "bitable": "WAITING",
    "tasks": "WAITING",
    "bot": "READY",
    "larkCli": "DEMO",
}

FEISHU_LOGS = [
    {
        "id": 1,
        "syncType": "docs",
        "targetName": "保研知识库",
        "status": "SUCCESS",
        "message": "同步 3 个文档",
        "added": 0,
        "updated": 3,
        "skipped": 0,
        "errors": 0,
        "createdAt": "2026-06-18 10:00:00",
    },
    {
        "id": 2,
        "syncType": "bitable",
        "targetName": "内容素材库",
        "status": "WAITING",
        "message": "等待飞书授权",
        "added": 0,
        "updated": 0,
        "skipped": 0,
        "errors": 0,
        "createdAt": "2026-06-18 10:05:00",
    },
]

PROMPTS = [
    {"id": 1, "agentType": "content", "name": "小红书爆款结构仿写", "template": "围绕 {theme} 生成标题、结构和三版正文。"},
    {"id": 2, "agentType": "knowledge", "name": "知识库检索回答", "template": "先检索资料，再基于资料回答用户问题。"},
]

ROUTES = [
    {"service": "ai-agent-service", "path": "/api/agents/**,/api/prompts/**"},
    {"service": "content-service", "path": "/api/content/**"},
    {"service": "knowledge-service", "path": "/api/knowledge/**"},
    {"service": "feishu-service", "path": "/api/feishu/**"},
]

PERMISSIONS = [
    {"code": "dashboard:view", "permissionCode": "dashboard:view", "name": "查看 Dashboard", "permissionName": "查看 Dashboard", "pathPattern": "/api/analytics/**", "frontendRoute": "/dashboard"},
    {"code": "agent:chat", "permissionCode": "agent:chat", "name": "使用 AI 工作台", "permissionName": "使用 AI 工作台", "pathPattern": "/api/agents/**", "frontendRoute": "/agent"},
    {"code": "content:manage", "permissionCode": "content:manage", "name": "管理内容运营", "permissionName": "管理内容运营", "pathPattern": "/api/content/**", "frontendRoute": "/content"},
    {"code": "knowledge:manage", "permissionCode": "knowledge:manage", "name": "管理知识库", "permissionName": "管理知识库", "pathPattern": "/api/knowledge/**", "frontendRoute": "/knowledge"},
    {"code": "student:manage", "permissionCode": "student:manage", "name": "管理学员", "permissionName": "管理学员", "pathPattern": "/api/students/**", "frontendRoute": "/students"},
    {"code": "school:manage", "permissionCode": "school:manage", "name": "管理院校", "permissionName": "管理院校", "pathPattern": "/api/schools/**", "frontendRoute": "/schools"},
]

ROLES = [
    {"roleCode": "ADMIN", "roleName": "管理员", "permissions": deepcopy(PERMISSIONS)},
    {"roleCode": "OPERATOR", "roleName": "运营人员", "permissions": deepcopy(PERMISSIONS[:4])},
    {"roleCode": "CONSULTANT", "roleName": "保研顾问", "permissions": deepcopy(PERMISSIONS[:2] + PERMISSIONS[4:])},
]


def copy(value: Any) -> Any:
    return deepcopy(value)


def filter_contains(rows: list[dict[str, Any]], keyword: str) -> list[dict[str, Any]]:
    if not keyword:
        return copy(rows)
    keyword = keyword.lower()
    result = []
    for row in rows:
        if keyword in str(row).lower():
            result.append(row)
    return copy(result)


def next_id(rows: list[dict[str, Any]]) -> int:
    return max([int(row.get("id") or 0) for row in rows] or [0]) + 1
