-- 社交三件套 + 徽章 新增表（关注/私信/徽章）。风格对齐现有 schema.sql（BaseEntity: id/deleted/created_at/updated_at）。
-- 应用：mysql -usem -psem123 -h127.0.0.1 sem < migration_social.sql

-- 关注关系
CREATE TABLE IF NOT EXISTS user_follow (
  id          BIGINT   NOT NULL AUTO_INCREMENT,
  follower_id BIGINT   NOT NULL COMMENT '关注者',
  followee_id BIGINT   NOT NULL COMMENT '被关注者',
  deleted     TINYINT  NOT NULL DEFAULT 0,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_follow (follower_id, followee_id),
  KEY idx_followee (followee_id)
) ENGINE=InnoDB COMMENT='用户关注关系';

-- 私信消息
CREATE TABLE IF NOT EXISTS message (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  sender_id   BIGINT       NOT NULL COMMENT '发送者',
  receiver_id BIGINT       NOT NULL COMMENT '接收者',
  content     VARCHAR(2000) NOT NULL,
  is_read     TINYINT      NOT NULL DEFAULT 0 COMMENT '接收者是否已读',
  deleted     TINYINT      NOT NULL DEFAULT 0,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_pair (sender_id, receiver_id),
  KEY idx_receiver_unread (receiver_id, is_read)
) ENGINE=InnoDB COMMENT='站内私信';

-- 用户徽章/成就
CREATE TABLE IF NOT EXISTS user_badge (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT      NOT NULL,
  badge_code  VARCHAR(40) NOT NULL COMMENT '徽章代码',
  badge_name  VARCHAR(60) NOT NULL,
  icon        VARCHAR(16) NULL COMMENT '展示用emoji/代号',
  pinned      TINYINT     NOT NULL DEFAULT 0 COMMENT '置顶展示',
  hidden      TINYINT     NOT NULL DEFAULT 0 COMMENT '隐藏',
  awarded_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT     NOT NULL DEFAULT 0,
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_badge (user_id, badge_code),
  KEY idx_user (user_id)
) ENGINE=InnoDB COMMENT='用户徽章';

-- 演示种子：给 user id=1(林一航/lian18336)、2(demo26307)、3(admintest) 造一些关注/消息/徽章，
-- 使真实模式也能看到内容（幂等 INSERT IGNORE）
INSERT IGNORE INTO user_follow (follower_id, followee_id) VALUES (1,2),(2,1),(3,1),(1,3);
INSERT IGNORE INTO message (sender_id, receiver_id, content, is_read) VALUES
  (2,1,'学长你好！请问数据结构期末怎么复习效率最高？',1),
  (1,2,'先过一遍思维导图梳理框架，再对着代码模板刷典型例题，最后做两套真题。',1),
  (2,1,'谢谢学长！那我先从思维导图开始～',0);
INSERT IGNORE INTO user_badge (user_id, badge_code, badge_name, icon, pinned) VALUES
  (1,'CONTRIBUTOR','知识贡献者','🏅',1),
  (1,'HELPER','热心答主','🤝',1),
  (1,'STREAK_30','连续活跃30天','🔥',0),
  (1,'FIRST_POST','初次分享','🌱',0),
  (3,'ADMIN','平台管理员','🛡️',1);
