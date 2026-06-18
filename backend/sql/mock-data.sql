USE FlowMind;
INSERT INTO sys_user(username,password,nickname) VALUES
('admin','123456','团队管理员 Admin'),
('content','123456','内容运营人员'),
('teacher','123456','教育咨询老师'),
('ip','123456','个人IP运营者'),
('student','123456','学员用户'),
('demo','123456','兼容演示账号');
INSERT INTO sys_role(role_code,role_name) VALUES
('CONTENT_OPERATOR','内容运营人员'),
('EDU_CONSULTANT','教育咨询老师'),
('IP_OPERATOR','个人IP运营者'),
('TEAM_ADMIN','团队管理员'),
('STUDENT_USER','学员用户');
INSERT INTO sys_permission(permission_code,permission_name,path_pattern,frontend_route,description) VALUES
('PAGE_DASHBOARD','Dashboard','/api/analytics/**','/dashboard','查看工作台总览和统计数据'),
('PAGE_AGENT','AI 工作台','/api/agents/**','/agent','访问总智能体、会话和流式对话'),
('PAGE_PROMPT','Prompt 模板','/api/prompts/**','/agent','查看和维护 Prompt 模板'),
('PAGE_KNOWLEDGE','知识库','/api/knowledge/**','/knowledge','查看知识库文档、标签和向量检索'),
('PAGE_CONTENT','内容运营','/api/content/**','/content','查看和维护主题库、文案库和内容日历'),
('PAGE_STUDENTS','学员管理','/api/students/**','/students','查看和维护学员画像、进度和风险等级'),
('PAGE_SCHOOLS','院校情报','/api/schools/**','/schools','查看学校信息和执行院校推荐'),
('PAGE_SCHOOL_PROJECTS','院校项目','/api/school-projects/**','/schools','查看和维护夏令营、预推免项目'),
('PAGE_FEISHU','飞书同步','/api/feishu/**','/feishu','查看飞书同步状态、知识库文件和机器人日志'),
('PAGE_SETTINGS','系统设置','/api/users/**','/settings','查看当前用户和基础配置'),
('RBAC_ROLE_MANAGE','角色权限配置','/api/roles/**','/settings','团队管理员维护角色权限'),
('RBAC_PERMISSION_VIEW','权限清单查看','/api/permissions/**','/settings','团队管理员查看权限点清单'),
('GATEWAY_ROUTE_VIEW','网关路由','/api/gateway/**','/settings','查看服务路由与网关信息');
INSERT INTO sys_user_role(user_id,role_id)
SELECT u.id,r.id FROM sys_user u JOIN sys_role r ON r.role_code='TEAM_ADMIN' WHERE u.username='admin';
INSERT INTO sys_user_role(user_id,role_id)
SELECT u.id,r.id FROM sys_user u JOIN sys_role r ON r.role_code='CONTENT_OPERATOR' WHERE u.username='content';
INSERT INTO sys_user_role(user_id,role_id)
SELECT u.id,r.id FROM sys_user u JOIN sys_role r ON r.role_code='EDU_CONSULTANT' WHERE u.username='teacher';
INSERT INTO sys_user_role(user_id,role_id)
SELECT u.id,r.id FROM sys_user u JOIN sys_role r ON r.role_code='IP_OPERATOR' WHERE u.username='ip';
INSERT INTO sys_user_role(user_id,role_id)
SELECT u.id,r.id FROM sys_user u JOIN sys_role r ON r.role_code='STUDENT_USER' WHERE u.username='student';
INSERT INTO sys_user_role(user_id,role_id)
SELECT u.id,r.id FROM sys_user u JOIN sys_role r ON r.role_code='CONTENT_OPERATOR' WHERE u.username='demo';
INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code IN ('TEAM_ADMIN','CONTENT_OPERATOR','EDU_CONSULTANT','IP_OPERATOR')
  AND p.permission_code IN ('PAGE_DASHBOARD','PAGE_AGENT','PAGE_PROMPT','PAGE_KNOWLEDGE','PAGE_CONTENT','PAGE_STUDENTS','PAGE_SCHOOLS','PAGE_SCHOOL_PROJECTS','PAGE_FEISHU','PAGE_SETTINGS','GATEWAY_ROUTE_VIEW');
INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code='TEAM_ADMIN'
  AND p.permission_code IN ('RBAC_ROLE_MANAGE','RBAC_PERMISSION_VIEW');
INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code='STUDENT_USER'
  AND p.permission_code IN ('PAGE_AGENT','PAGE_KNOWLEDGE','PAGE_SCHOOLS','PAGE_SCHOOL_PROJECTS','PAGE_SETTINGS');
INSERT INTO prompt_template(agent_type,name,template) VALUES ('content','小红书选题','围绕{theme}生成10个适合教育服务的选题'),('student','学员画像','根据{profile}分析风险与建议'),('school','院校推荐','根据学生条件匹配院校项目');
INSERT INTO knowledge_tag(name,color) VALUES ('夏令营','#5B6CFF'),('预推免','#19B37B'),('材料','#F59E0B'),('面试','#EF4444'),('内容运营','#8B5CF6');
INSERT INTO knowledge_doc(title,category,summary,source) VALUES
('2026 保研夏令营时间线','政策资料','按月份拆解报名、材料、面试与确认节点','mock'),
('院校项目材料清单模板','申请材料','覆盖简历、成绩单、排名证明、科研材料','mock'),
('朋友圈转化文案案例库','内容运营','沉淀8类教育服务朋友圈文案','mock'),
('经管类面试高频问题','面试资料','整理专业课、英语和科研追问','mock'),
('保研咨询 SOP','运营手册','从线索到签约的标准流程','mock'),
('低 GPA 补强策略','学员分析','科研竞赛与项目经历补强路径','mock'),
('院校情报采集模板','院校项目','字段、来源、更新时间与负责人','mock'),
('飞书多维表格字段设计','系统配置','学员、项目、内容三张表结构','mock'),
('公众号标题库','内容运营','50条教育行业标题模板','mock'),
('学员周报模板','交付材料','进度、风险、下周动作清单','mock');
INSERT INTO content_topic(title,platform,topic_type,style,status,heat_score) VALUES
('保研边缘人如何逆袭夏令营','小红书','经验干货','学姐风','待创作',92),
('三分钟讲清预推免和夏令营区别','公众号','科普','干货','已生成',88),
('低年级保研规划清单','朋友圈','转化文案','温柔','待发布',84),
('英语成绩不够怎么补救','小红书','痛点','正式',79),
('科研小白第一段项目怎么找','小红书','方法论','干货',91),
('排名证明材料避坑','公众号','材料','正式',77),
('夏令营面试自我介绍模板','小红书','模板','学姐风',89),
('目标院校梯度怎么定','朋友圈','咨询转化','营销',85),
('普通双非的保研路线','小红书','案例','温柔',93),
('保研复盘周报怎么写','公众号','工具','干货',73);
INSERT INTO student_profile(name,school,major,gpa,ranking,english_score,target_school,application_stage,risk_level) VALUES
('学员01','示例大学1','金融学',3.61,'8/120','六级560','985经管','材料准备','中'),
('学员02','示例大学2','会计学',3.82,'3/100','雅思6.5','顶尖财经','夏令营报名','低'),
('学员03','示例大学3','经济学',3.35,'22/130','六级510','211经管','初筛','高');
INSERT INTO school_info(name,region,level,discipline_tags) VALUES ('复旦大学','上海','985','经管,新闻'),('华东师范大学','上海','985','教育,心理'),('中南财经政法大学','武汉','211','金融,法学');
INSERT INTO school_project(school_id,project_name,project_type,deadline,requirements,materials) VALUES
(1,'管理学院夏令营','夏令营','2026-07-10','排名前20%，英语优秀','简历、成绩单、排名证明、个人陈述'),
(2,'教育学部预推免','预推免','2026-09-15','有科研经历优先','成绩单、科研证明、推荐信'),
(3,'金融学院夏令营','夏令营','2026-07-25','六级500+，专业基础扎实','简历、成绩单、英语证明');
INSERT INTO feishu_sync_record(sync_type,target_name,status,message) VALUES ('docs','保研资料库','SUCCESS','同步10篇文档'),('bitable','学员管理表','SUCCESS','同步20条学员记录'),('tasks','申请进度任务','SUCCESS','创建8条任务'),('bot','运营通知群','SUCCESS','推送周报摘要');
INSERT INTO system_log(module,level,message,trace_id) VALUES ('agent','INFO','ContentAgent mock response generated','trace-demo-001'),('feishu','INFO','Mock sync completed','trace-demo-002');
