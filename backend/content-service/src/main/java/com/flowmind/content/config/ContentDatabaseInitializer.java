package com.flowmind.content.config;

import com.flowmind.content.mapper.ContentMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ContentDatabaseInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final ContentMapper contentMapper;

    public ContentDatabaseInitializer(JdbcTemplate jdbcTemplate, ContentMapper contentMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.contentMapper = contentMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        createTables();
        migrateColumns();
        if (contentMapper.countThemes() == 0) {
            seedMockData();
        }
    }

    private void createTables() {
        jdbcTemplate.execute("""
                create table if not exists content_theme (
                  id bigint primary key auto_increment,
                  title varchar(200) not null comment '主题标题',
                  topic varchar(100) not null comment '主题关键词',
                  platform varchar(30) not null comment '默认平台',
                  type varchar(50) not null comment '主题类型',
                  status varchar(30) not null comment '待创作/已生成/待发布/已发布',
                  heat int not null default 0 comment '选题热度',
                  rating int null comment '星级评分 1-5，null=未评分',
                  planned_date date null comment '计划日期',
                  summary varchar(500) null comment '主题摘要',
                  source_type varchar(30) not null default 'mock' comment 'mock/agent/feishu/manual',
                  external_ref varchar(120) null comment '外部系统引用',
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint(1) not null default 0,
                  key idx_content_theme_status(status),
                  key idx_content_theme_platform(platform),
                  key idx_content_theme_planned_date(planned_date)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='内容主题库'
                """);

        jdbcTemplate.execute("""
                create table if not exists content_copy (
                  id bigint primary key auto_increment,
                  theme_id bigint not null comment '关联主题ID',
                  title varchar(220) not null comment '文案标题',
                  channel varchar(30) not null comment '小红书/朋友圈/公众号',
                  version varchar(50) not null comment '文案版本',
                  style varchar(50) not null comment '文案风格',
                  content text not null comment '文案正文',
                  usage_status varchar(30) not null default '未使用' comment '未使用/已使用/已归档',
                  used_date date null comment '实际使用日期',
                  generated_at datetime not null comment '生成时间',
                  owner varchar(80) not null default '内容运营',
                  feedback varchar(500) null comment '发布反馈或备注',
                  rating int null comment '星级评分 1-5，null=未评分',
                  image_suggestion varchar(500) null comment '无配图时的配图建议',
                  generation_source varchar(30) not null default 'mock' comment 'mock/agent/manual/import',
                  prompt_snapshot json null comment '生成时的Prompt参数快照',
                  llm_trace_id varchar(120) null comment '未来接入LLM调用链路ID',
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint(1) not null default 0,
                  key idx_content_copy_theme(theme_id),
                  key idx_content_copy_channel(channel),
                  key idx_content_copy_usage(usage_status),
                  key idx_content_copy_generated(generated_at),
                  constraint fk_content_copy_theme foreign key(theme_id) references content_theme(id)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='内容文案库'
                """);

        jdbcTemplate.execute("""
                create table if not exists content_copy_image (
                  id bigint primary key auto_increment,
                  copy_id bigint not null comment '关联文案ID',
                  file_name varchar(160) not null,
                  url varchar(1000) not null comment '图片访问地址',
                  storage_provider varchar(30) not null default 'minio' comment 'minio/feishu/local/mock',
                  object_key varchar(300) null comment '对象存储key',
                  sort_order int not null default 1,
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint(1) not null default 0,
                  key idx_content_image_copy(copy_id),
                  constraint fk_content_image_copy foreign key(copy_id) references content_copy(id)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='文案配图'
                """);

        jdbcTemplate.execute("""
                create table if not exists content_calendar (
                  id bigint primary key auto_increment,
                  theme_id bigint not null,
                  copy_id bigint not null,
                  publish_date date not null comment '发布或计划发布日期',
                  channel varchar(30) not null,
                  publish_status varchar(30) not null comment '待创作/已生成/待发布/已发布',
                  usage_status varchar(30) not null default '未使用',
                  feishu_task_id varchar(120) null comment '未来飞书任务ID',
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint(1) not null default 0,
                  key idx_content_calendar_date(publish_date),
                  key idx_content_calendar_copy(copy_id),
                  constraint fk_content_calendar_theme foreign key(theme_id) references content_theme(id),
                  constraint fk_content_calendar_copy foreign key(copy_id) references content_copy(id)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='内容日历'
                """);

        jdbcTemplate.execute("""
                create table if not exists content_tag (
                  id bigint primary key auto_increment,
                  name varchar(60) not null,
                  category varchar(40) not null default 'topic' comment 'topic/purpose/style/conversion',
                  color varchar(20) null,
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint(1) not null default 0,
                  unique key uk_content_tag_name_category(name, category)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='内容标签'
                """);

        jdbcTemplate.execute("""
                create table if not exists content_theme_tag (
                  id bigint primary key auto_increment,
                  theme_id bigint not null,
                  tag_id bigint not null,
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint(1) not null default 0,
                  unique key uk_content_theme_tag(theme_id, tag_id),
                  constraint fk_content_theme_tag_theme foreign key(theme_id) references content_theme(id),
                  constraint fk_content_theme_tag_tag foreign key(tag_id) references content_tag(id)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='主题标签关联'
                """);

        jdbcTemplate.execute("""
                create table if not exists content_generation_record (
                  id bigint primary key auto_increment,
                  agent_type varchar(50) not null comment 'xiaohongshu/moments/asset',
                  input_json json null,
                  output_json json null,
                  status varchar(30) not null default 'success',
                  feishu_doc_token varchar(120) null,
                  feishu_bitable_record_id varchar(120) null,
                  vector_record_id varchar(120) null,
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint(1) not null default 0,
                  key idx_content_generation_agent(agent_type),
                  key idx_content_generation_created(created_at)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='内容生成流水'
                """);

        jdbcTemplate.execute("""
                create table if not exists content_publish_metric (
                  id bigint primary key auto_increment,
                  copy_id bigint not null,
                  publish_date date not null,
                  view_count int not null default 0,
                  like_count int not null default 0,
                  comment_count int not null default 0,
                  collect_count int not null default 0,
                  conversion_count int not null default 0,
                  remark varchar(300) null,
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint(1) not null default 0,
                  key idx_content_metric_copy(copy_id),
                  key idx_content_metric_date(publish_date),
                  constraint fk_content_metric_copy foreign key(copy_id) references content_copy(id)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='内容发布表现'
                """);
    }

    private void migrateColumns() {
        addColumnIfMissing("content_theme", "rating", "alter table content_theme add column rating int null comment '星级评分 1-5，null=未评分' after heat");
        addColumnIfMissing("content_copy", "rating", "alter table content_copy add column rating int null comment '星级评分 1-5，null=未评分' after feedback");
    }

    private void addColumnIfMissing(String table, String column, String sql) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(1)
                from information_schema.columns
                where table_schema = database() and table_name = ? and column_name = ?
                """, Integer.class, table, column);
        if (count == null || count == 0) {
            jdbcTemplate.execute(sql);
        }
    }

    private void seedMockData() {
        seedThemes();
        seedTags();
        seedThemeTags();
        seedCopies();
        seedImages();
        seedCalendar();
        seedMetrics();
    }

    private void seedThemes() {
        jdbcTemplate.batchUpdate("""
                insert into content_theme(id, title, topic, platform, type, status, heat, rating, planned_date, summary, source_type)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'mock')
                """, java.util.List.of(
                new Object[]{1L, "保研简历怎么写才像有科研潜力", "保研简历", "小红书", "爆款仿写", "已生成", 96, 4, "2026-06-18", "围绕简历结构、科研表达和材料证据生成多版本笔记，适合做保研材料系列。"},
                new Object[]{2L, "导师套磁邮件怎么写不尴尬", "导师套磁", "小红书", "经验干货", "待发布", 91, 5, "2026-06-20", "拆解套磁邮件结构，强调研究兴趣、匹配理由和克制表达。"},
                new Object[]{3L, "低年级保研规划清单", "保研规划", "朋友圈", "人设表达", "待创作", 88, 3, "2026-06-22", "强调早规划不是焦虑，而是把不确定拆成可执行任务。"},
                new Object[]{4L, "夏令营面试自我介绍模板", "夏令营面试", "小红书", "模板", "已发布", 93, 5, "2026-06-08", "围绕背景、科研、目标方向和结尾承接设计面试表达。"},
                new Object[]{5L, "收到 offer 后如何发朋友圈不浮夸", "成功案例", "朋友圈", "成果型人设", "已生成", 90, 4, "2026-06-24", "用克制语气表达结果、过程和方法，避免强营销感。"},
                new Object[]{6L, "科研小白如何找到第一个项目", "科研入门", "小红书", "爆款干货", "待发布", 89, null, "2026-06-25", "把找项目拆成课程项目、导师课题、竞赛延展和论文复现四条路径。"},
                new Object[]{7L, "预推免报名材料避坑清单", "预推免材料", "小红书", "清单型", "已生成", 87, 3, "2026-06-27", "沉淀预推免报名材料检查表，适合引导私信领取模板。"},
                new Object[]{8L, "公众号长文标题：保研全年规划", "公众号标题", "公众号", "标题库", "待创作", 82, null, "2026-06-29", "为公众号长文准备标题和导语，后续承接知识库沉淀。"}
        ));
    }

    private void seedTags() {
        jdbcTemplate.batchUpdate("""
                insert into content_tag(id, name, category, color)
                values (?, ?, ?, ?)
                """, java.util.List.of(
                new Object[]{1L, "保研简历", "topic", "#2563eb"},
                new Object[]{2L, "科研潜力", "topic", "#7c3aed"},
                new Object[]{3L, "材料优化", "purpose", "#0f766e"},
                new Object[]{4L, "导师套磁", "topic", "#9333ea"},
                new Object[]{5L, "邮件模板", "purpose", "#0891b2"},
                new Object[]{6L, "面试准备", "purpose", "#ea580c"},
                new Object[]{7L, "低年级", "topic", "#16a34a"},
                new Object[]{8L, "规划", "purpose", "#4f46e5"},
                new Object[]{9L, "时间线", "style", "#0284c7"},
                new Object[]{10L, "面试", "topic", "#c2410c"},
                new Object[]{11L, "自我介绍", "purpose", "#be123c"},
                new Object[]{12L, "offer", "topic", "#15803d"},
                new Object[]{13L, "成功案例", "purpose", "#65a30d"},
                new Object[]{14L, "朋友圈", "style", "#db2777"},
                new Object[]{15L, "科研入门", "topic", "#0d9488"},
                new Object[]{16L, "项目经历", "purpose", "#0369a1"},
                new Object[]{17L, "预推免", "topic", "#b45309"},
                new Object[]{18L, "材料清单", "purpose", "#ca8a04"},
                new Object[]{19L, "公众号", "topic", "#334155"},
                new Object[]{20L, "全年规划", "purpose", "#475569"}
        ));
    }

    private void seedThemeTags() {
        jdbcTemplate.batchUpdate("insert into content_theme_tag(theme_id, tag_id) values (?, ?)", java.util.List.of(
                new Object[]{1L, 1L}, new Object[]{1L, 2L}, new Object[]{1L, 3L},
                new Object[]{2L, 4L}, new Object[]{2L, 5L}, new Object[]{2L, 6L},
                new Object[]{3L, 7L}, new Object[]{3L, 8L}, new Object[]{3L, 9L},
                new Object[]{4L, 10L}, new Object[]{4L, 11L}, new Object[]{4L, 6L},
                new Object[]{5L, 12L}, new Object[]{5L, 13L}, new Object[]{5L, 14L},
                new Object[]{6L, 15L}, new Object[]{6L, 16L}, new Object[]{6L, 1L},
                new Object[]{7L, 17L}, new Object[]{7L, 18L}, new Object[]{7L, 3L},
                new Object[]{8L, 19L}, new Object[]{8L, 20L}, new Object[]{8L, 8L}
        ));
    }

    private void seedCopies() {
        jdbcTemplate.batchUpdate("""
                insert into content_copy(id, theme_id, title, channel, version, style, content, usage_status, used_date,
                  generated_at, owner, feedback, rating, image_suggestion, generation_source, prompt_snapshot, llm_trace_id)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'mock', ?, ?)
                """, java.util.List.of(
                new Object[]{101L, 1L, "保研er别再这样写简历了", "小红书", "干货版", "干货", "保研简历不是经历堆砌，而是让老师快速看到你的科研潜力。建议每段经历都写清楚：你做了什么、用了什么方法、最后产出了什么证据。", "已使用", "2026-06-12", "2026-06-10 10:30:00", "内容运营", "收藏率高，适合继续拆成简历模板系列。", 4, "三栏式简历改前改后对比图，突出科研经历、成果证据、匹配方向。", "{\"agent\":\"ContentAgent\",\"sop\":\"xiaohongshu\"}", "mock-101"},
                new Object[]{102L, 1L, "简历没亮点？先改这 4 个位置", "小红书", "情绪增强版", "学姐风", "很多同学不是经历不够，而是不知道怎么把经历讲成优势。尤其是科研、竞赛、课程项目这三块，写法不同，老师看到的信息完全不同。", "未使用", null, "2026-06-10 10:34:00", "内容运营", "适合配案例图发布。", 3, "配一张“简历亮点提取清单”长图，包含问题意识、方法、结果、证据四列。", "{\"agent\":\"ContentAgent\",\"sop\":\"xiaohongshu\"}", "mock-102"},
                new Object[]{103L, 1L, "我会这样帮学员改保研简历", "朋友圈", "专业理性版", "专业", "今天复盘一份简历，重点不是把语言修得更漂亮，而是把科研经历里的问题意识、方法动作和结果证据补清楚。材料的可信度，往往来自这些细节。", "已使用", "2026-06-14", "2026-06-11 16:20:00", "主理人", "私信咨询 3 条，适合继续扩写成长文。", 5, "配会议白板或简历批注局部图，弱化营销感，强化真实服务场景。", "{\"agent\":\"ContentAgent\",\"sop\":\"moments\"}", "mock-103"},
                new Object[]{201L, 2L, "套磁邮件别一上来就求机会", "小红书", "干货版", "干货", "一封好的套磁邮件，核心不是表达“我很想来”，而是说明你为什么匹配这位老师：研究方向、已有经历、能继续推进的问题。", "未使用", null, "2026-06-13 09:12:00", "内容运营", "待配套邮件模板图。", null, "配“邮件结构拆解图”：称呼、研究匹配、经历证据、请教问题、克制结尾。", "{\"agent\":\"ContentAgent\",\"sop\":\"xiaohongshu\"}", "mock-201"},
                new Object[]{202L, 2L, "给学生讲套磁，我最常提醒这一点", "朋友圈", "学姐温和版", "温柔", "套磁不是打扰老师，而是一次简洁的自我说明。把自己的研究兴趣、已有准备和想请教的问题讲清楚，就已经比空泛表达好很多。", "已使用", "2026-06-15", "2026-06-13 09:20:00", "主理人", "适合继续扩写成长文。", 4, "配一张简洁邮件模板截图，重点标出“匹配理由”段落。", "{\"agent\":\"ContentAgent\",\"sop\":\"moments\"}", "mock-202"},
                new Object[]{301L, 3L, "大一大二做保研规划，重点不是焦虑", "朋友圈", "专业理性版", "克制", "低年级做规划，不是为了提前焦虑，而是知道 GPA、英语、科研、竞赛分别在什么时候该补到什么程度。节奏清楚，反而会轻松很多。", "未使用", null, "2026-06-14 21:10:00", "主理人", "可作为招生转化铺垫。", null, "配一张低年级三阶段时间轴：基础分、项目经历、材料准备。", "{\"agent\":\"ContentAgent\",\"sop\":\"moments\"}", "mock-301"},
                new Object[]{302L, 3L, "低年级保研准备，不要只盯着绩点", "小红书", "转化引导版", "干货", "绩点是门槛，但不是全部。低年级更应该同步搭建英语、科研、竞赛和表达能力，这些会在夏令营材料里一起被看见。", "未使用", null, "2026-06-15 11:30:00", "内容运营", "标题可继续 A/B 测试。", 2, "配“四象限能力地图”：绩点、英语、科研、表达。", "{\"agent\":\"ContentAgent\",\"sop\":\"xiaohongshu\"}", "mock-302"},
                new Object[]{401L, 4L, "夏令营自我介绍照这个顺序讲", "小红书", "转化引导版", "干货", "自我介绍建议按“基础背景-核心经历-研究兴趣-项目匹配”来讲。不要背简历，要让老师听到一条清楚的成长线。", "已使用", "2026-06-08", "2026-06-06 13:00:00", "内容运营", "互动率 12%，评论区追问较多。", 5, "配一张自我介绍流程卡，四段结构用不同色块区分。", "{\"agent\":\"ContentAgent\",\"sop\":\"xiaohongshu\"}", "mock-401"},
                new Object[]{501L, 5L, "今天收到一个很踏实的好消息", "朋友圈", "学姐温和版", "温柔", "今天收到学员的录取反馈，开心之外更多是替她松一口气。前期把材料反复打磨，面试前又做了三轮模拟，结果其实是一步步攒出来的。", "未使用", null, "2026-06-16 09:45:00", "主理人", "适合等下一条真实反馈后发布。", 4, "配模糊处理后的 offer 截图或聊天反馈局部，注意隐私遮挡。", "{\"agent\":\"ContentAgent\",\"sop\":\"moments\"}", "mock-501"},
                new Object[]{502L, 5L, "一个 offer 背后，最值得复盘的是过程", "朋友圈", "专业理性版", "专业", "比起单纯晒结果，我更想记录这次申请里做对的几件事：材料定位更聚焦，项目表达更具体，面试回答有证据链。这些才是真正可复用的经验。", "已使用", "2026-06-17", "2026-06-16 10:05:00", "主理人", "获得 5 条咨询，语气克制有效。", 5, "配复盘清单图，弱化炫耀，突出方法论。", "{\"agent\":\"ContentAgent\",\"sop\":\"moments\"}", "mock-502"},
                new Object[]{601L, 6L, "科研小白别只问老师有没有项目", "小红书", "干货版", "干货", "第一个科研项目不一定从正式课题开始。你可以从课程论文、文献复现、竞赛选题和实验室小任务切入，先证明自己能稳定推进。", "未使用", null, "2026-06-16 15:20:00", "内容运营", "需要做成可收藏长图。", 3, "配“科研入门四路径”信息图：课程、复现、竞赛、实验室任务。", "{\"agent\":\"ContentAgent\",\"sop\":\"xiaohongshu\"}", "mock-601"},
                new Object[]{602L, 6L, "科研经历不是等来的，是一点点搭出来的", "朋友圈", "稍带传播版", "轻传播", "很多同学卡在“我没有科研经历，所以不敢联系老师”。但第一段经历往往不是等来的，而是从能完成的小任务开始搭出来的。", "已归档", null, "2026-06-16 15:32:00", "主理人", "语气可保留，暂不发布。", null, "配书桌、文献、任务清单类真实工作场景图。", "{\"agent\":\"ContentAgent\",\"sop\":\"moments\"}", "mock-602"},
                new Object[]{701L, 7L, "预推免材料提交前，一定检查这 6 件事", "小红书", "转化引导版", "营销", "材料提交前最怕小错误影响印象。建议检查命名、盖章、证明顺序、成绩单版本、科研附件和个人陈述是否一致。", "未使用", null, "2026-06-18 08:40:00", "内容运营", "适合搭配资料包领取。", 4, "配报名材料检查清单长图。", "{\"agent\":\"ContentAgent\",\"sop\":\"xiaohongshu\"}", "mock-701"},
                new Object[]{801L, 8L, "从大一到预推免：一份可执行的保研全年规划", "公众号", "专业理性版", "正式", "这篇文章会按年级拆解保研准备重点：低年级打基础，中期补经历，高年级做材料和面试冲刺。核心不是制造焦虑，而是给出清晰节奏。", "未使用", null, "2026-06-18 20:10:00", "内容运营", "可作为公众号选题池。", 3, "配全年规划路线图，按月份标出 GPA、英语、科研、材料节点。", "{\"agent\":\"ContentAgent\",\"sop\":\"asset\"}", "mock-801"}
        ));
    }

    private void seedImages() {
        jdbcTemplate.batchUpdate("""
                insert into content_copy_image(id, copy_id, file_name, url, storage_provider, object_key, sort_order)
                values (?, ?, ?, ?, 'mock', ?, ?)
                """, java.util.List.of(
                new Object[]{1001L, 101L, "resume-cover.svg", "/mock-assets/content/resume-cover.svg", "content/resume-cover.svg", 1},
                new Object[]{2002L, 202L, "email-template.svg", "/mock-assets/content/email-template.svg", "content/email-template.svg", 1},
                new Object[]{4001L, 401L, "interview-flow.svg", "/mock-assets/content/interview-flow.svg", "content/interview-flow.svg", 1},
                new Object[]{5002L, 502L, "offer-story.svg", "/mock-assets/content/offer-story.svg", "content/offer-story.svg", 1},
                new Object[]{7001L, 701L, "checklist.svg", "/mock-assets/content/checklist.svg", "content/checklist.svg", 1}
        ));
    }

    private void seedCalendar() {
        jdbcTemplate.batchUpdate("""
                insert into content_calendar(id, theme_id, copy_id, publish_date, channel, publish_status, usage_status)
                values (?, ?, ?, ?, ?, ?, ?)
                """, java.util.List.of(
                new Object[]{1L, 4L, 401L, "2026-06-08", "小红书", "已发布", "已使用"},
                new Object[]{2L, 1L, 101L, "2026-06-12", "小红书", "已发布", "已使用"},
                new Object[]{3L, 1L, 103L, "2026-06-14", "朋友圈", "已发布", "已使用"},
                new Object[]{4L, 2L, 202L, "2026-06-15", "朋友圈", "已发布", "已使用"},
                new Object[]{5L, 5L, 502L, "2026-06-17", "朋友圈", "已发布", "已使用"},
                new Object[]{6L, 2L, 201L, "2026-06-20", "小红书", "待发布", "未使用"},
                new Object[]{7L, 3L, 301L, "2026-06-22", "朋友圈", "待创作", "未使用"},
                new Object[]{8L, 5L, 501L, "2026-06-24", "朋友圈", "已生成", "未使用"},
                new Object[]{9L, 6L, 601L, "2026-06-25", "小红书", "待发布", "未使用"},
                new Object[]{10L, 7L, 701L, "2026-06-27", "小红书", "已生成", "未使用"},
                new Object[]{11L, 8L, 801L, "2026-06-29", "公众号", "待创作", "未使用"}
        ));
    }

    private void seedMetrics() {
        jdbcTemplate.batchUpdate("""
                insert into content_publish_metric(copy_id, publish_date, view_count, like_count, comment_count, collect_count, conversion_count, remark)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, java.util.List.of(
                new Object[]{401L, "2026-06-08", 8600, 720, 96, 430, 18, "模板类内容收藏表现好"},
                new Object[]{101L, "2026-06-12", 12500, 980, 141, 760, 24, "简历主题持续有私信"},
                new Object[]{103L, "2026-06-14", 1200, 68, 12, 0, 3, "朋友圈带来咨询"},
                new Object[]{202L, "2026-06-15", 980, 52, 7, 0, 2, "表达克制，适合人设沉淀"},
                new Object[]{502L, "2026-06-17", 1700, 96, 16, 0, 5, "成功案例不浮夸，转化较自然"}
        ));
    }
}
