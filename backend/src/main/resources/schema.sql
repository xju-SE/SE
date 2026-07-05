-- =====================================================================
-- 新疆大学校友圈与双圈成长导航平台  数据库 DDL
-- 版本 v3.1（已按 08 集成报告 / 09 修订说明 reconcile）
-- 引擎 InnoDB，字符集 utf8mb4；枚举以 VARCHAR 存枚举名，取值见列注释
-- 通用约定：所有业务表含 deleted/created_at/updated_at；
--          可并发编辑表(knowledge_entry 等)含 version 乐观锁
-- =====================================================================
CREATE DATABASE IF NOT EXISTS sem DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE sem;

-- ---------- M1 用户与认证 ----------
CREATE TABLE user (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  username      VARCHAR(50)  NOT NULL COMMENT '登录名/学号/邮箱',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码散列',
  role          VARCHAR(16)  NOT NULL COMMENT 'STUDENT/ALUMNI/ADMIN（注册身份类型，无持久化GUEST）',
  auth_status   VARCHAR(16)  NOT NULL DEFAULT 'UNVERIFIED' COMMENT 'UNVERIFIED/PENDING/VERIFIED/REJECTED（与role正交，控制写权限）',
  status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  contact_visibility VARCHAR(16) NOT NULL DEFAULT 'SELF' COMMENT 'SELF/SAME_MAJOR/PUBLIC 联系方式可见性',
  profile_visibility VARCHAR(16) NOT NULL DEFAULT 'SAME_MAJOR' COMMENT 'SELF/SAME_MAJOR/PUBLIC 画像可见性',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='用户账号';

CREATE TABLE student_profile (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       BIGINT       NOT NULL,
  real_name     VARCHAR(50)  NOT NULL COMMENT '真实姓名（认证材料，不进入PUBLIC可见）',
  student_no    VARCHAR(30)  NOT NULL COMMENT '学号（认证写入后只读）',
  college       VARCHAR(100) NOT NULL COMMENT '学院',
  major_tag_id  BIGINT       NOT NULL COMMENT '专业标签 FK→tag.id (tag_type=MAJOR)',
  enroll_year   SMALLINT     NOT NULL COMMENT '入学年份，如2023',
  grade_level   TINYINT      NOT NULL COMMENT '年级档 1..10（1大一..4大四,5研一..，系统按enroll_year每年重算）',
  gpa           DECIMAL(3,2) NULL COMMENT '当前GPA，范围[0,gpa_scale]',
  gpa_scale     TINYINT      NOT NULL DEFAULT 4 COMMENT 'GPA满分制 4/5',
  target_city   VARCHAR(50)  NULL COMMENT '目标城市',
  target_industry_tag_id BIGINT NULL COMMENT '目标行业标签 FK→tag.id (tag_type=INDUSTRY)',
  bio           VARCHAR(500) NULL,
  avatar_url    VARCHAR(255) NULL,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user (user_id),
  UNIQUE KEY uk_student_no (student_no),
  KEY idx_major (major_tag_id)
) ENGINE=InnoDB COMMENT='在校生档案';

CREATE TABLE alumni_profile (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       BIGINT       NOT NULL,
  real_name     VARCHAR(50)  NOT NULL COMMENT '真实姓名（认证材料，不进入PUBLIC可见）',
  college       VARCHAR(100) NOT NULL,
  major_tag_id  BIGINT       NOT NULL COMMENT '最高学历专业 FK→tag.id',
  grad_year     SMALLINT     NOT NULL COMMENT '最高学历毕业年份',
  degree_type   VARCHAR(16)  NOT NULL COMMENT 'BACHELOR/MASTER/PHD',
  is_contributor_badge TINYINT NOT NULL DEFAULT 0 COMMENT '贡献者认证标识 0无1有',
  helped_count  INT          NOT NULL DEFAULT 0 COMMENT '已帮助学弟学妹计数(缓存,由M4采纳事件累加)',
  adopted_count INT          NOT NULL DEFAULT 0 COMMENT '被采纳次数(缓存)',
  honor_cert_url VARCHAR(255) NULL COMMENT '荣誉证明附件',
  bio           VARCHAR(500) NULL,
  avatar_url    VARCHAR(255) NULL,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user (user_id),
  KEY idx_major (major_tag_id)
) ENGINE=InnoDB COMMENT='毕业生档案';

CREATE TABLE auth_application (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       BIGINT       NULL COMMENT '申请人(邀请码预生成时为NULL,认领后回填)',
  apply_role    VARCHAR(16)  NOT NULL COMMENT 'STUDENT/ALUMNI',
  verify_method VARCHAR(24)  NOT NULL COMMENT 'STUDENT_SSO/STUDENT_MANUAL/ALUMNI_INVITE_CODE/ALUMNI_MANUAL_GUARANTEE',
  real_name     VARCHAR(50)  NULL,
  student_no    VARCHAR(30)  NULL,
  major_text    VARCHAR(100) NULL COMMENT '申请填写的专业文本，终审时解析为major_tag_id',
  college       VARCHAR(100) NULL,
  evidence_url  VARCHAR(255) NULL COMMENT '证件/证明材料',
  invite_code   VARCHAR(32)  NULL COMMENT '毕业生邀请码(机构侧签发)',
  guarantor1_id BIGINT       NULL COMMENT '担保人1',
  guarantor2_id BIGINT       NULL COMMENT '担保人2',
  guarantor1_status VARCHAR(12) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/REJECTED 担保人1确认态(S3双人担保)',
  guarantor2_status VARCHAR(12) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/REJECTED 担保人2确认态；两人均CONFIRMED才转UNDER_REVIEW',
  status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'INVITE_ISSUED/AWAITING_GUARANTEE/PENDING/UNDER_REVIEW/APPROVED/REJECTED/RETURNED',
  auto_approved TINYINT      NOT NULL DEFAULT 0 COMMENT '是否SSO自动通过',
  reject_reason VARCHAR(255) NULL,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  KEY idx_invite (invite_code),
  KEY idx_status (status)
) ENGINE=InnoDB COMMENT='认证申请';

-- 模拟学号库（认证数据源，演示用；真实对接教务在未来展望）
CREATE TABLE mock_student_roster (
  student_no VARCHAR(30) NOT NULL,
  real_name  VARCHAR(50) NOT NULL,
  college    VARCHAR(100) NOT NULL,
  major_name VARCHAR(100) NOT NULL,
  enroll_year SMALLINT NOT NULL,
  PRIMARY KEY (student_no)
) ENGINE=InnoDB COMMENT='模拟学籍库(认证核验数据源)';

-- ---------- 全局：标签 / 通知 ----------
CREATE TABLE tag (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  tag_type   VARCHAR(16) NOT NULL COMMENT 'MAJOR/GRADE/INDUSTRY/INTEREST/GROWTH/QUESTION_TYPE',
  tag_name   VARCHAR(50) NOT NULL,
  parent_id  BIGINT      NULL,
  sort_order INT         NOT NULL DEFAULT 0,
  deleted    TINYINT     NOT NULL DEFAULT 0,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_type_name_parent (tag_type, tag_name, parent_id),
  KEY idx_type (tag_type)
) ENGINE=InnoDB COMMENT='标签(专业/年级/行业/兴趣/成长/问题类型)';

CREATE TABLE user_tag (
  id        BIGINT      NOT NULL AUTO_INCREMENT,
  user_id   BIGINT      NOT NULL,
  tag_id    BIGINT      NOT NULL,
  tag_source VARCHAR(8) NOT NULL DEFAULT 'SELF' COMMENT 'SELF/SYSTEM',
  created_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_tag (user_id, tag_id),
  KEY idx_tag (tag_id)
) ENGINE=InnoDB COMMENT='用户-标签';

CREATE TABLE notification (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  user_id    BIGINT      NOT NULL COMMENT '接收人',
  type       VARCHAR(16) NOT NULL COMMENT 'HELP_MATCH/ADOPT/AUDIT_RESULT/SYSTEM',
  title      VARCHAR(100) NOT NULL,
  content    VARCHAR(500) NOT NULL,
  ref_type   VARCHAR(24) NULL COMMENT '关联对象类型',
  ref_id     BIGINT      NULL COMMENT '关联对象ID',
  is_read    TINYINT     NOT NULL DEFAULT 0,
  channel    VARCHAR(8)  NOT NULL DEFAULT 'INAPP' COMMENT 'INAPP/PUSH(PUSH本期预留不投递)',
  deleted    TINYINT     NOT NULL DEFAULT 0,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_read (user_id, is_read)
) ENGINE=InnoDB COMMENT='站内通知';

-- ---------- M2 画像与校友路径 ----------
CREATE TABLE alumni_path_card (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       BIGINT       NOT NULL COMMENT '所属校友',
  grad_stage    VARCHAR(16)  NOT NULL COMMENT '毕业阶段/学历 BACHELOR/MASTER/PHD',
  major_tag_id  BIGINT       NOT NULL,
  grad_year     SMALLINT     NOT NULL,
  grad_gpa      DECIMAL(3,2) NULL,
  gpa_scale     TINYINT      NOT NULL DEFAULT 4 COMMENT 'GPA满分制 4/5(C11:按此校验grad_gpa,勿硬编码)',
  destination_type VARCHAR(16) NOT NULL COMMENT 'EMPLOY/POSTGRAD/CIVIL_SERVICE/ABROAD/OTHER',
  -- 就业分支
  city          VARCHAR(50)  NULL,
  industry_tag_id BIGINT     NULL,
  company       VARCHAR(100) NULL,
  position      VARCHAR(100) NULL,
  -- 深造分支
  postgrad_admission_type VARCHAR(12) NULL COMMENT 'S7:RECOMMEND(保研)/EXAM(考研);仅POSTGRAD必填,EXAM时exam_score必填',
  target_school VARCHAR(100) NULL,
  target_major  VARCHAR(100) NULL,
  exam_score    VARCHAR(100) NULL COMMENT '初试成绩构成(EXAM时必填)',
  interview_exp VARCHAR(1000) NULL COMMENT '复试经历',
  prep_months   TINYINT      NULL COMMENT '备考时长(月)',
  prep_materials VARCHAR(1000) NULL COMMENT 'C10:备考资料/参考书目',
  -- 通用
  advice        VARCHAR(2000) NULL COMMENT '学长学姐建议/经验总结',
  status        VARCHAR(12)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/HIDDEN',
  version       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁(本人编辑与管理员下架并发)',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_stage (user_id, grad_stage) COMMENT 'S6:同一校友每个毕业阶段至多一张路径卡',
  KEY idx_major_dest (major_tag_id, destination_type),
  KEY idx_user (user_id)
) ENGINE=InnoDB COMMENT='校友路径卡';

CREATE TABLE path_visibility (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  path_card_id BIGINT      NOT NULL,
  field_group  VARCHAR(24) NOT NULL COMMENT '字段组:COMPANY/POSITION/SCHOOL/SCORE/...',
  visibility   VARCHAR(12) NOT NULL DEFAULT 'SAME_MAJOR' COMMENT 'SELF/SAME_MAJOR/PUBLIC',
  PRIMARY KEY (id),
  UNIQUE KEY uk_card_group (path_card_id, field_group)
) ENGINE=InnoDB COMMENT='路径卡字段级可见性';

-- ---------- M3 经验知识库 ----------
CREATE TABLE knowledge_entry (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  title          VARCHAR(150) NOT NULL,
  content        TEXT         NOT NULL,
  category       VARCHAR(20)  NOT NULL COMMENT 'LIFE/COURSE/COMPETITION/POSTGRAD_EMPLOY/NAV',
  author_id      BIGINT       NOT NULL,
  applicable_scope VARCHAR(200) NULL COMMENT '适用范围(专业/年级)',
  valid_until    DATE         NULL COMMENT '时效截止；高时效信息不自存改外链',
  external_url   VARCHAR(255) NULL COMMENT '外链官方渠道(高时效信息)',
  status         VARCHAR(12)  NOT NULL DEFAULT 'CANDIDATE' COMMENT 'CANDIDATE/REVIEWING/PUBLISHED/EXPIRED/OFFLINE',
  source_type    VARCHAR(12)  NOT NULL DEFAULT 'ORIGINAL' COMMENT 'ORIGINAL/FROM_HELP',
  source_help_id BIGINT       NULL COMMENT '来源求助单id',
  claimer_id     BIGINT       NULL COMMENT '认领更新的当届学生',
  view_count     INT          NOT NULL DEFAULT 0,
  version        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁(可并发编辑)',
  deleted        TINYINT      NOT NULL DEFAULT 0,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_status_cat (status, category),
  FULLTEXT KEY ft_title_content (title, content) WITH PARSER ngram
) ENGINE=InnoDB COMMENT='知识条目';

CREATE TABLE knowledge_feedback (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  entry_id   BIGINT      NOT NULL,
  user_id    BIGINT      NOT NULL,
  feedback_type VARCHAR(12) NOT NULL COMMENT 'USEFUL/OUTDATED/NEED_UPDATE',
  comment    VARCHAR(300) NULL,
  deleted    TINYINT     NOT NULL DEFAULT 0,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_entry_user (entry_id, user_id),
  KEY idx_entry (entry_id)
) ENGINE=InnoDB COMMENT='三态评价/纠错反馈';

-- ---------- M4 结构化求助（系统灵魂） ----------
CREATE TABLE help_ticket (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  asker_id      BIGINT       NOT NULL,
  title         VARCHAR(150) NOT NULL,
  content       TEXT         NOT NULL,
  major_tag_id  BIGINT       NULL COMMENT '相关专业标签',
  grade_level   TINYINT      NULL COMMENT '提问时年级',
  question_type_tag_id BIGINT NULL COMMENT '问题类型标签',
  target_direction VARCHAR(50) NULL COMMENT '目标方向(考研/就业/竞赛...)',
  status        VARCHAR(12)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/MATCHED/ANSWERED/ADOPTED/CLOSED',
  followup_count TINYINT     NOT NULL DEFAULT 0,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_status_major (status, major_tag_id)
) ENGINE=InnoDB COMMENT='求助单';

CREATE TABLE help_answer (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  ticket_id     BIGINT       NOT NULL,
  responder_id  BIGINT       NOT NULL,
  precondition  VARCHAR(500) NULL COMMENT '适用前提',
  steps         VARCHAR(2000) NOT NULL COMMENT '具体步骤',
  cautions      VARCHAR(500) NULL COMMENT '注意事项',
  is_adopted    TINYINT      NOT NULL DEFAULT 0,
  knowledge_entry_id BIGINT  NULL COMMENT '采纳后生成的知识候选id(回写,链1补列)',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ticket (ticket_id)
) ENGINE=InnoDB COMMENT='回答';

CREATE TABLE help_followup (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  ticket_id  BIGINT      NOT NULL,
  from_user_id BIGINT    NOT NULL,
  content    VARCHAR(500) NOT NULL,
  deleted    TINYINT     NOT NULL DEFAULT 0,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ticket (ticket_id)
) ENGINE=InnoDB COMMENT='追问';

CREATE TABLE help_route (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  ticket_id     BIGINT      NOT NULL,
  matched_user_id BIGINT    NOT NULL COMMENT '匹配到的校友',
  match_score   INT         NOT NULL DEFAULT 0 COMMENT '匹配得分',
  status        VARCHAR(12) NOT NULL DEFAULT 'NOTIFIED' COMMENT 'NOTIFIED/VIEWED/ANSWERED/EXPIRED',
  notified_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ticket_user (ticket_id, matched_user_id),
  KEY idx_matched (matched_user_id)
) ENGINE=InnoDB COMMENT='求助路由通知';

-- ---------- M5 机会与组队 ----------
CREATE TABLE opportunity (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  type          VARCHAR(16)  NOT NULL COMMENT 'COMPETITION/INNOVATION/INTERNSHIP/LECTURE',
  title         VARCHAR(150) NOT NULL,
  description   TEXT         NULL,
  deadline      DATETIME     NULL,
  status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT 'PENDING_REVIEW/ONGOING/CLOSING_SOON/CLOSED/ENDED',
  publisher_id  BIGINT       NOT NULL,
  is_referral   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否内推类(需审核)',
  team_required TINYINT      NOT NULL DEFAULT 0 COMMENT 'S19:是否允许围绕本机会发起组队(为0则不可组队)',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_type_status (type, status),
  KEY idx_deadline (deadline)
) ENGINE=InnoDB COMMENT='机会(竞赛/大创/实习/讲座)';

CREATE TABLE team (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  opportunity_id BIGINT      NULL COMMENT '关联机会(可空,自由组队)',
  leader_id     BIGINT       NOT NULL,
  title         VARCHAR(150) NOT NULL,
  description   VARCHAR(1000) NULL,
  need_desc     VARCHAR(500) NULL COMMENT '招募需求',
  capacity      TINYINT      NOT NULL DEFAULT 5,
  current_size  TINYINT      NOT NULL DEFAULT 1,
  status        VARCHAR(12)  NOT NULL DEFAULT 'RECRUITING' COMMENT 'RECRUITING/FULL/ONGOING/ENDED',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_status (status),
  KEY idx_opp (opportunity_id)
) ENGINE=InnoDB COMMENT='队伍';

CREATE TABLE team_member (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  team_id    BIGINT      NOT NULL,
  user_id    BIGINT      NOT NULL,
  member_role VARCHAR(12) NOT NULL DEFAULT 'MEMBER' COMMENT 'LEADER/MEMBER',
  join_status VARCHAR(12) NOT NULL DEFAULT 'APPLYING' COMMENT 'APPLYING/JOINED/REJECTED/LEFT',
  deleted    TINYINT     NOT NULL DEFAULT 0,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_team_user (team_id, user_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB COMMENT='队伍成员';

CREATE TABLE referral_ticket (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  referrer_id   BIGINT       NOT NULL COMMENT '内推人(校友)',
  applicant_id  BIGINT       NOT NULL COMMENT '申请人(学生)',
  opportunity_id BIGINT      NULL,
  resume_url    VARCHAR(255) NULL,
  status        VARCHAR(12)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/REFERRED/IN_PROCESS/ENDED',
  note          VARCHAR(500) NULL,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_applicant (applicant_id),
  KEY idx_referrer (referrer_id)
) ENGINE=InnoDB COMMENT='内推申请单(Could,本期不实现业务)';

-- ---------- M6 成长时间线 ----------
CREATE TABLE timeline_template (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  major_tag_id BIGINT      NULL COMMENT '专业(NULL=通用)',
  route_type   VARCHAR(16) NOT NULL COMMENT 'UNDECIDED/POSTGRAD/EMPLOY/COMPETITION/CIVIL',
  name         VARCHAR(100) NOT NULL,
  status       VARCHAR(12) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/OFFLINE',
  created_by   BIGINT      NULL,
  deleted      TINYINT     NOT NULL DEFAULT 0,
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_major_route (major_tag_id, route_type)
) ENGINE=InnoDB COMMENT='时间线模板(专业×路线)';

CREATE TABLE timeline_node (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  template_id  BIGINT      NOT NULL,
  title        VARCHAR(150) NOT NULL,
  stage        VARCHAR(16) NOT NULL COMMENT '阶段 GRADE1_1/GRADE1_2/.../GRADE4_2',
  suggested_time VARCHAR(50) NULL COMMENT '建议完成时间(相对,如"大一上第8周")',
  suggested_month TINYINT   NULL COMMENT '建议月份1-12(用于逾期比对)',
  importance   TINYINT     NOT NULL DEFAULT 1 COMMENT '重要度1-3',
  order_no     INT         NOT NULL DEFAULT 0,
  description  VARCHAR(500) NULL,
  deleted      TINYINT     NOT NULL DEFAULT 0,
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_template (template_id, order_no)
) ENGINE=InnoDB COMMENT='时间线节点';

CREATE TABLE timeline_node_ref (
  id        BIGINT      NOT NULL AUTO_INCREMENT,
  node_id   BIGINT      NOT NULL,
  ref_type  VARCHAR(20) NOT NULL COMMENT 'ALUMNI_PATH_CARD/KNOWLEDGE_ENTRY/OPPORTUNITY',
  ref_id    BIGINT      NOT NULL COMMENT '只存ID,绝不复制内容',
  PRIMARY KEY (id),
  UNIQUE KEY uk_node_ref (node_id, ref_type, ref_id),
  KEY idx_ref (ref_type, ref_id)
) ENGINE=InnoDB COMMENT='节点关联(只存ID)';

CREATE TABLE user_progress (
  id        BIGINT      NOT NULL AUTO_INCREMENT,
  user_id   BIGINT      NOT NULL,
  node_id   BIGINT      NOT NULL,
  status    VARCHAR(12) NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'NOT_STARTED/DONE',
  marked_at DATETIME    NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_node (user_id, node_id)
) ENGINE=InnoDB COMMENT='个人节点进度';

-- ---------- M7 管理与治理 ----------
CREATE TABLE audit_task (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  target_type  VARCHAR(24) NOT NULL COMMENT 'AUTH_APPLICATION/KNOWLEDGE_ENTRY/OPPORTUNITY/CONTRIBUTOR_CERT',
  target_id    BIGINT      NOT NULL,
  submitter_id BIGINT      NULL,
  review_kind  VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/REVISION/AUTO/NEW_FROM_HELP',
  status       VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/RETURNED/AUTO_APPROVED',
  auto_precheck VARCHAR(500) NULL COMMENT '自动预检结果(隐私/完整性)',
  reviewer_id  BIGINT      NULL,
  decision_note VARCHAR(300) NULL,
  decided_at   DATETIME    NULL,
  deleted      TINYINT     NOT NULL DEFAULT 0,
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_type_status (target_type, status),
  KEY idx_status_created (status, created_at)
) ENGINE=InnoDB COMMENT='审核任务(统一队列)';

CREATE TABLE report (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  target_type VARCHAR(24) NOT NULL COMMENT 'HELP_TICKET/KNOWLEDGE_ENTRY/ALUMNI_PATH_CARD/OPPORTUNITY/USER',
  target_id   BIGINT      NOT NULL,
  reporter_id BIGINT      NOT NULL,
  reason      VARCHAR(300) NOT NULL,
  status      VARCHAR(12) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/HANDLED/DISMISSED',
  handler_id  BIGINT      NULL,
  handle_note VARCHAR(300) NULL,
  deleted     TINYINT     NOT NULL DEFAULT 0,
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_status (status)
) ENGINE=InnoDB COMMENT='举报';
