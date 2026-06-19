# FlowMind Agent Python 桌面客户端

这是 FlowMind Agent 的 Python/PySide6 桌面端复刻工程，目标是尽可能按现有 Vue Web 前端复刻页面结构和功能，并复用当前 Java 后端 REST/SSE API。

## 技术栈

- Python 3.10+
- PySide6
- httpx

## 安装依赖

```powershell
cd D:\Desktop\agent\FlowMind-Agent\desktop_fronted
python -m pip install -r requirements.txt
```

## 运行

先启动 Java 后端，默认接口地址为：

```text
http://localhost:8080
```

然后运行桌面端：

```powershell
python run.py
```

如果当前机器 PySide6/Qt DLL 导入失败，可以先运行 Tkinter 兜底版。它不需要额外 GUI 依赖，仍然复用同一套 API 客户端和页面结构：

```powershell
python run_tk.py
```

## Smoke Test

不启动 GUI，只验证 API 客户端和离线 fallback 数据：

```powershell
python smoke_test.py
```

验证 PySide6 页面可以创建和切换：

```powershell
python pyside_smoke_test.py
```

如果后端在 ngrok 或其他地址，登录页可以修改 Base URL，例如：

```text
https://gracious-justifier-espresso.ngrok-free.dev
```

## 当前页面覆盖

- 登录
- Dashboard
- AI 工作台
- 知识库
- 内容运营
- 学员管理
- 院校情报
- 数据分析
- 飞书同步
- 系统设置/权限接口

## 设计约束

桌面端只访问 HTTP/SSE 接口，不直接读取 MySQL、Weaviate、飞书 CLI 或后端本地配置。
