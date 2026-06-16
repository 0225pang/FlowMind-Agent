# FlowMind Agent Frontend

Vue3 + TypeScript + Vite + Element Plus + ECharts 的网页端 Demo。

## 启动

```bash
cd frontend
npm install
npm run dev
```

访问：http://localhost:5173

Demo 账号：`admin / 123456`。

当前前端默认使用 `src/api/mock.ts` 中的本地 Mock 数据，Vite 已配置 `/api -> http://localhost:8080`，后续可以将页面数据切换到后端 REST API。

## 构建

```bash
npm run build    # 类型检查 + 生产构建
npm run preview  # 预览构建产物
```

## 页面路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/login` | 登录 | Demo 账号 admin/123456 |
| `/dashboard` | 工作台 | 整体数据看板 |
| `/workspace` | AI 工作台 | Agent 对话与内容 SOP 生成 |
| `/content` | 内容运营 | 主题库、文案库、评分、日历 |
| `/knowledge` | 知识库 | 内容资产与知识沉淀 |
| `/students` | 学员管理 | 学员信息与进度 |
| `/schools` | 院校情报 | 院校数据库 |
| `/feishu` | 飞书集成 | 飞书审批与通知 |

## 内容运营功能

- **主题库**：查看/新增/删除主题，5 星评分
- **文案库**：查看/新增/删除/编辑文案，5 星评分
- **内容日历**：按日期查看发布排期
- **AI 工作台**：小红书/朋友圈/内容资产 SOP 生成
