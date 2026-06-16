CREATE DATABASE IF NOT EXISTS `FlowMind`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `FlowMind`;

CREATE TABLE IF NOT EXISTS content_theme (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL COMMENT '主题标题',
  topic VARCHAR(100) NOT NULL COMMENT '主题关键词',
  platform VARCHAR(30) NOT NULL COMMENT '默认平台：小红书/朋友圈/公众号',
  type VARCHAR(50) NOT NULL COMMENT '主题类型',
  status VARCHAR(30) NOT NULL COMMENT '待创作/已生成/待发布/已发布',
  heat INT NOT NULL DEFAULT 0 COMMENT '选题热度',
  planned_date DATE NULL COMMENT '计划日期',
  summary VARCHAR(500) NULL COMMENT '主题摘要',
  source_type VARCHAR(30) NOT NULL DEFAULT 'mock' COMMENT 'mock/agent/feishu/manual',
  external_ref VARCHAR(120) NULL COMMENT '飞书或外部系统引用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_content_theme_status(status),
  KEY idx_content_theme_platform(platform),
  KEY idx_content_theme_planned_date(planned_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容主题库';

CREATE TABLE IF NOT EXISTS content_copy (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  theme_id BIGINT NOT NULL COMMENT '关联主题ID',
  title VARCHAR(220) NOT NULL COMMENT '文案标题',
  channel VARCHAR(30) NOT NULL COMMENT '小红书/朋友圈/公众号',
  version VARCHAR(50) NOT NULL COMMENT '干货版/情绪增强版/转化引导版等',
  style VARCHAR(50) NOT NULL COMMENT '文案风格',
  content TEXT NOT NULL COMMENT '文案正文',
  usage_status VARCHAR(30) NOT NULL DEFAULT '未使用' COMMENT '未使用/已使用/已归档',
  used_date DATE NULL COMMENT '实际使用日期',
  generated_at DATETIME NOT NULL COMMENT '生成时间',
  owner VARCHAR(80) NOT NULL DEFAULT '内容运营',
  feedback VARCHAR(500) NULL COMMENT '发布反馈或运营备注',
  image_suggestion VARCHAR(500) NULL COMMENT '无配图时的配图建议',
  generation_source VARCHAR(30) NOT NULL DEFAULT 'mock' COMMENT 'mock/agent/manual/import',
  prompt_snapshot JSON NULL COMMENT '生成时的Prompt参数快照',
  llm_trace_id VARCHAR(120) NULL COMMENT '未来接入LLM调用链路ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_content_copy_theme(theme_id),
  KEY idx_content_copy_channel(channel),
  KEY idx_content_copy_usage(usage_status),
  KEY idx_content_copy_generated(generated_at),
  CONSTRAINT fk_content_copy_theme FOREIGN KEY(theme_id) REFERENCES content_theme(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容文案库';

CREATE TABLE IF NOT EXISTS content_copy_image (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  copy_id BIGINT NOT NULL COMMENT '关联文案ID',
  file_name VARCHAR(160) NOT NULL,
  url VARCHAR(1000) NOT NULL COMMENT '图片访问地址',
  storage_provider VARCHAR(30) NOT NULL DEFAULT 'minio' COMMENT 'minio/feishu/local/mock',
  object_key VARCHAR(300) NULL COMMENT '对象存储key',
  sort_order INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_content_image_copy(copy_id),
  CONSTRAINT fk_content_image_copy FOREIGN KEY(copy_id) REFERENCES content_copy(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文案配图';

CREATE TABLE IF NOT EXISTS content_calendar (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  theme_id BIGINT NOT NULL,
  copy_id BIGINT NOT NULL,
  publish_date DATE NOT NULL COMMENT '发布或计划发布日期',
  channel VARCHAR(30) NOT NULL,
  publish_status VARCHAR(30) NOT NULL COMMENT '待创作/已生成/待发布/已发布',
  usage_status VARCHAR(30) NOT NULL DEFAULT '未使用',
  feishu_task_id VARCHAR(120) NULL COMMENT '未来飞书任务ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_content_calendar_date(publish_date),
  KEY idx_content_calendar_copy(copy_id),
  CONSTRAINT fk_content_calendar_theme FOREIGN KEY(theme_id) REFERENCES content_theme(id),
  CONSTRAINT fk_content_calendar_copy FOREIGN KEY(copy_id) REFERENCES content_copy(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容日历';

CREATE TABLE IF NOT EXISTS content_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(60) NOT NULL,
  category VARCHAR(40) NOT NULL DEFAULT 'topic' COMMENT 'topic/purpose/style/conversion',
  color VARCHAR(20) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_content_tag_name_category(name, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容标签';

CREATE TABLE IF NOT EXISTS content_theme_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  theme_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_content_theme_tag(theme_id, tag_id),
  CONSTRAINT fk_content_theme_tag_theme FOREIGN KEY(theme_id) REFERENCES content_theme(id),
  CONSTRAINT fk_content_theme_tag_tag FOREIGN KEY(tag_id) REFERENCES content_tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='主题标签关联';

CREATE TABLE IF NOT EXISTS content_generation_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  agent_type VARCHAR(50) NOT NULL COMMENT 'xiaohongshu/moments/asset',
  input_json JSON NULL,
  output_json JSON NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'success',
  feishu_doc_token VARCHAR(120) NULL,
  feishu_bitable_record_id VARCHAR(120) NULL,
  vector_record_id VARCHAR(120) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_content_generation_agent(agent_type),
  KEY idx_content_generation_created(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容生成流水';

CREATE TABLE IF NOT EXISTS content_publish_metric (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  copy_id BIGINT NOT NULL,
  publish_date DATE NOT NULL,
  view_count INT NOT NULL DEFAULT 0,
  like_count INT NOT NULL DEFAULT 0,
  comment_count INT NOT NULL DEFAULT 0,
  collect_count INT NOT NULL DEFAULT 0,
  conversion_count INT NOT NULL DEFAULT 0,
  remark VARCHAR(300) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_content_metric_copy(copy_id),
  KEY idx_content_metric_date(publish_date),
  CONSTRAINT fk_content_metric_copy FOREIGN KEY(copy_id) REFERENCES content_copy(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容发布表现';

INSERT IGNORE INTO content_theme(id, title, topic, platform, type, status, heat, planned_date, summary, source_type) VALUES
(1, '保研简历怎么写才像有科研潜力', '保研简历', '小红书', '爆款仿写', '已生成', 96, '2026-06-18', '围绕简历结构、科研表达和材料证据生成多版本笔记，适合做保研材料系列。', 'mock'),
(2, '导师套磁邮件怎么写不尴尬', '导师套磁', '小红书', '经验干货', '待发布', 91, '2026-06-20', '拆解套磁邮件结构，强调研究兴趣、匹配理由和克制表达。', 'mock'),
(3, '低年级保研规划清单', '保研规划', '朋友圈', '人设表达', '待创作', 88, '2026-06-22', '强调早规划不是焦虑，而是把不确定拆成可执行任务。', 'mock'),
(4, '夏令营面试自我介绍模板', '夏令营面试', '小红书', '模板', '已发布', 93, '2026-06-08', '围绕背景、科研、目标方向和结尾承接设计面试表达。', 'mock'),
(5, '收到 offer 后如何发朋友圈不浮夸', '成功案例', '朋友圈', '成果型人设', '已生成', 90, '2026-06-24', '用克制语气表达结果、过程和方法，避免强营销感。', 'mock'),
(6, '科研小白如何找到第一个项目', '科研入门', '小红书', '爆款干货', '待发布', 89, '2026-06-25', '把“找项目”拆成课程项目、导师课题、竞赛延展和论文复现四条路径。', 'mock'),
(7, '预推免报名材料避坑清单', '预推免材料', '小红书', '清单型', '已生成', 87, '2026-06-27', '沉淀预推免报名材料检查表，适合引导私信领取模板。', 'mock'),
(8, '公众号长文标题：保研全年规划', '公众号标题', '公众号', '标题库', '待创作', 82, '2026-06-29', '为公众号长文准备标题和导语，后续承接知识库沉淀。', 'mock');

INSERT IGNORE INTO content_tag(id, name, category, color) VALUES
(1, '保研简历', 'topic', '#2563eb'), (2, '科研潜力', 'topic', '#7c3aed'), (3, '材料优化', 'purpose', '#0f766e'),
(4, '导师套磁', 'topic', '#9333ea'), (5, '邮件模板', 'purpose', '#0891b2'), (6, '面试准备', 'purpose', '#ea580c'),
(7, '低年级', 'topic', '#16a34a'), (8, '规划', 'purpose', '#4f46e5'), (9, '时间线', 'style', '#0284c7'),
(10, '面试', 'topic', '#c2410c'), (11, '自我介绍', 'purpose', '#be123c'), (12, 'offer', 'topic', '#15803d'),
(13, '成功案例', 'purpose', '#65a30d'), (14, '朋友圈', 'style', '#db2777'), (15, '科研入门', 'topic', '#0d9488'),
(16, '项目经历', 'purpose', '#0369a1'), (17, '预推免', 'topic', '#b45309'), (18, '材料清单', 'purpose', '#ca8a04'),
(19, '公众号', 'topic', '#334155'), (20, '全年规划', 'purpose', '#475569');

INSERT IGNORE INTO content_theme_tag(theme_id, tag_id) VALUES
(1,1),(1,2),(1,3),(2,4),(2,5),(2,6),(3,7),(3,8),(3,9),(4,10),(4,11),(4,6),
(5,12),(5,13),(5,14),(6,15),(6,16),(6,1),(7,17),(7,18),(7,3),(8,19),(8,20),(8,8);

INSERT IGNORE INTO content_copy(id, theme_id, title, channel, version, style, content, usage_status, used_date, generated_at, owner, feedback, image_suggestion, generation_source, prompt_snapshot, llm_trace_id) VALUES
(101, 1, '保研er别再这样写简历了', '小红书', '干货版', '干货', '保研简历不是经历堆砌，而是让老师快速看到你的科研潜力。建议每段经历都写清楚：你做了什么、用了什么方法、最后产出了什么证据。', '已使用', '2026-06-12', '2026-06-10 10:30:00', '内容运营', '收藏率高，适合继续拆成简历模板系列。', '三栏式简历改前改后对比图，突出科研经历、成果证据、匹配方向。', 'mock', JSON_OBJECT('agent','ContentAgent','sop','xiaohongshu'), 'mock-101'),
(102, 1, '简历没亮点？先改这 4 个位置', '小红书', '情绪增强版', '学姐风', '很多同学不是经历不够，而是不知道怎么把经历讲成优势。尤其是科研、竞赛、课程项目这三块，写法不同，老师看到的信息完全不同。', '未使用', NULL, '2026-06-10 10:34:00', '内容运营', '适合配案例图发布。', '配一张“简历亮点提取清单”长图，包含问题意识、方法、结果、证据四列。', 'mock', JSON_OBJECT('agent','ContentAgent','sop','xiaohongshu'), 'mock-102'),
(103, 1, '我会这样帮学员改保研简历', '朋友圈', '专业理性版', '专业', '今天复盘一份简历，重点不是把语言修得更漂亮，而是把科研经历里的问题意识、方法动作和结果证据补清楚。材料的可信度，往往来自这些细节。', '已使用', '2026-06-14', '2026-06-11 16:20:00', '主理人', '私信咨询 3 条，适合继续扩写成长文。', '配会议白板或简历批注局部图，弱化营销感，强化真实服务场景。', 'mock', JSON_OBJECT('agent','ContentAgent','sop','moments'), 'mock-103'),
(201, 2, '套磁邮件别一上来就求机会', '小红书', '干货版', '干货', '一封好的套磁邮件，核心不是表达“我很想来”，而是说明你为什么匹配这位老师：研究方向、已有经历、能继续推进的问题。', '未使用', NULL, '2026-06-13 09:12:00', '内容运营', '待配套邮件模板图。', '配“邮件结构拆解图”：称呼、研究匹配、经历证据、请教问题、克制结尾。', 'mock', JSON_OBJECT('agent','ContentAgent','sop','xiaohongshu'), 'mock-201'),
(202, 2, '给学生讲套磁，我最常提醒这一点', '朋友圈', '学姐温和版', '温柔', '套磁不是“打扰老师”，而是一次简洁的自我说明。把自己的研究兴趣、已有准备和想请教的问题讲清楚，就已经比空泛表达好很多。', '已使用', '2026-06-15', '2026-06-13 09:20:00', '主理人', '适合继续扩写成长文。', '配一张简洁邮件模板截图，重点标出“匹配理由”段落。', 'mock', JSON_OBJECT('agent','ContentAgent','sop','moments'), 'mock-202'),
(301, 3, '大一大二做保研规划，重点不是焦虑', '朋友圈', '专业理性版', '克制', '低年级做规划，不是为了提前焦虑，而是知道 GPA、英语、科研、竞赛分别在什么时候该补到什么程度。节奏清楚，反而会轻松很多。', '未使用', NULL, '2026-06-14 21:10:00', '主理人', '可作为招生转化铺垫。', '配一张低年级三阶段时间轴：基础分、项目经历、材料准备。', 'mock', JSON_OBJECT('agent','ContentAgent','sop','moments'), 'mock-301'),
(302, 3, '低年级保研准备，不要只盯着绩点', '小红书', '转化引导版', '干货', '绩点是门槛，但不是全部。低年级更该同步搭建英语、科研、竞赛和表达能力，这些会在夏令营材料里一起被看见。', '未使用', NULL, '2026-06-15 11:30:00', '内容运营', '标题可继续 A/B 测试。', '配“四象限能力地图”：绩点、英语、科研、表达。', 'mock', JSON_OBJECT('agent','ContentAgent','sop','xiaohongshu'), 'mock-302'),
(401, 4, '夏令营自我介绍照这个顺序讲', '小红书', '转化引导版', '干货', '自我介绍建议按“基础背景-核心经历-研究兴趣-项目匹配”来讲。不要背简历，要让老师听到一条清楚的成长线。', '已使用', '2026-06-08', '2026-06-06 13:00:00', '内容运营', '互动率 12%，评论区追问较多。', '配一张自我介绍流程卡，四段结构用不同色块区分。', 'mock', JSON_OBJECT('agent','ContentAgent','sop','xiaohongshu'), 'mock-401'),
(501, 5, '今天收到一个很踏实的好消息', '朋友圈', '学姐温和版', '温柔', '今天收到学员的录取反馈，开心之外更多是替她松一口气。前期把材料反复打磨，面试前又做了三轮模拟，结果其实是一步步攒出来的。', '未使用', NULL, '2026-06-16 09:45:00', '主理人', '适合等下一条真实反馈后发布。', '配模糊处理后的 offer 截图或聊天反馈局部，注意隐私遮挡。', 'mock', JSON_OBJECT('agent','ContentAgent','sop','moments'), 'mock-501'),
(502, 5, '一个 offer 背后，最值得复盘的是过程', '朋友圈', '专业理性版', '专业', '比起单纯晒结果，我更想记录这次申请里做对的几件事：材料定位更聚焦，项目表达更具体，面试回答有证据链。这些才是真正可复用的经验。', '已使用', '2026-06-17', '2026-06-16 10:05:00', '主理人', '获得 5 条咨询，语气克制有效。', '配复盘清单图，弱化炫耀，突出方法论。', 'mock', JSON_OBJECT('agent','ContentAgent','sop','moments'), 'mock-502'),
(601, 6, '科研小白别只问老师有没有项目', '小红书', '干货版', '干货', '第一个科研项目不一定从正式课题开始。你可以从课程论文、文献复现、竞赛选题和实验室小任务切入，先证明自己能稳定推进。', '未使用', NULL, '2026-06-16 15:20:00', '内容运营', '需要做成可收藏长图。', '配“科研入门四路径”信息图：课程、复现、竞赛、实验室任务。', 'mock', JSON_OBJECT('agent','ContentAgent','sop','xiaohongshu'), 'mock-601'),
(602, 6, '科研经历不是等来的，是一点点搭出来的', '朋友圈', '稍带传播版', '轻传播', '很多同学卡在“我没有科研经历，所以不敢联系老师”。但第一段经历往往不是等来的，而是从能完成的小任务开始搭出来的。', '已归档', NULL, '2026-06-16 15:32:00', '主理人', '语气可保留，暂不发布。', '配书桌、文献、任务清单类真实工作场景图。', 'mock', JSON_OBJECT('agent','ContentAgent','sop','moments'), 'mock-602'),
(701, 7, '预推免材料提交前，一定检查这 6 件事', '小红书', '转化引导版', '营销', '材料提交前最怕小错误影响印象。建议检查命名、盖章、证明顺序、成绩单版本、科研附件和个人陈述是否一致。', '未使用', NULL, '2026-06-18 08:40:00', '内容运营', '适合搭配资料包领取。', '配报名材料检查清单长图。', 'mock', JSON_OBJECT('agent','ContentAgent','sop','xiaohongshu'), 'mock-701'),
(801, 8, '从大一到预推免：一份可执行的保研全年规划', '公众号', '专业理性版', '正式', '这篇文章会按年级拆解保研准备重点：低年级打基础，中期补经历，高年级做材料和面试冲刺。核心不是制造焦虑，而是给出清晰节奏。', '未使用', NULL, '2026-06-18 20:10:00', '内容运营', '可作为公众号选题池。', '配全年规划路线图，按月份标出 GPA、英语、科研、材料节点。', 'mock', JSON_OBJECT('agent','ContentAgent','sop','asset'), 'mock-801');

INSERT IGNORE INTO content_copy_image(id, copy_id, file_name, url, storage_provider, object_key, sort_order) VALUES
(1001, 101, 'resume-cover.svg', '/mock-assets/content/resume-cover.svg', 'mock', 'content/resume-cover.svg', 1),
(2002, 202, 'email-template.svg', '/mock-assets/content/email-template.svg', 'mock', 'content/email-template.svg', 1),
(4001, 401, 'interview-flow.svg', '/mock-assets/content/interview-flow.svg', 'mock', 'content/interview-flow.svg', 1),
(5002, 502, 'offer-story.svg', '/mock-assets/content/offer-story.svg', 'mock', 'content/offer-story.svg', 1),
(7001, 701, 'checklist.svg', '/mock-assets/content/checklist.svg', 'mock', 'content/checklist.svg', 1);

INSERT IGNORE INTO content_calendar(id, theme_id, copy_id, publish_date, channel, publish_status, usage_status) VALUES
(1, 4, 401, '2026-06-08', '小红书', '已发布', '已使用'),
(2, 1, 101, '2026-06-12', '小红书', '已发布', '已使用'),
(3, 1, 103, '2026-06-14', '朋友圈', '已发布', '已使用'),
(4, 2, 202, '2026-06-15', '朋友圈', '已发布', '已使用'),
(5, 5, 502, '2026-06-17', '朋友圈', '已发布', '已使用'),
(6, 2, 201, '2026-06-20', '小红书', '待发布', '未使用'),
(7, 3, 301, '2026-06-22', '朋友圈', '待创作', '未使用'),
(8, 5, 501, '2026-06-24', '朋友圈', '已生成', '未使用'),
(9, 6, 601, '2026-06-25', '小红书', '待发布', '未使用'),
(10, 7, 701, '2026-06-27', '小红书', '已生成', '未使用'),
(11, 8, 801, '2026-06-29', '公众号', '待创作', '未使用');

INSERT IGNORE INTO content_publish_metric(copy_id, publish_date, view_count, like_count, comment_count, collect_count, conversion_count, remark) VALUES
(401, '2026-06-08', 8600, 720, 96, 430, 18, '模板类内容收藏表现好'),
(101, '2026-06-12', 12500, 980, 141, 760, 24, '简历主题持续有私信'),
(103, '2026-06-14', 1200, 68, 12, 0, 3, '朋友圈带来咨询'),
(202, '2026-06-15', 980, 52, 7, 0, 2, '表达克制，适合人设沉淀'),
(502, '2026-06-17', 1700, 96, 16, 0, 5, '成功案例不浮夸，转化较自然');
