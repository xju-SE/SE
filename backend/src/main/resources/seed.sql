-- XJourney 疆行 · 联调种子数据
-- 用法: mysql -u root -p sem < seed.sql  (先执行 schema.sql)
-- 注意: tag 的 id 与前端演示数据对齐(1-4 专业 / 11-16 问题类型), 请勿改动 id。

-- ===== 标签: 专业(MAJOR) =====
INSERT INTO tag (id, tag_type, tag_name, sort_order) VALUES
  (1, 'MAJOR', '计算机科学与技术', 1),
  (2, 'MAJOR', '软件工程', 2),
  (3, 'MAJOR', '电子信息工程', 3),
  (4, 'MAJOR', '金融学', 4)
ON DUPLICATE KEY UPDATE tag_name = VALUES(tag_name);

-- ===== 标签: 问题类型(QUESTION_TYPE) =====
INSERT INTO tag (id, tag_type, tag_name, sort_order) VALUES
  (11, 'QUESTION_TYPE', '考研升学', 1),
  (12, 'QUESTION_TYPE', '求职实习', 2),
  (13, 'QUESTION_TYPE', '转专业', 3),
  (14, 'QUESTION_TYPE', '竞赛项目', 4),
  (15, 'QUESTION_TYPE', '课程学习', 5),
  (16, 'QUESTION_TYPE', '校园生活', 6)
ON DUPLICATE KEY UPDATE tag_name = VALUES(tag_name);

-- ===== 标签: 成长/兴趣(供画像) =====
INSERT INTO tag (id, tag_type, tag_name, sort_order) VALUES
  (21, 'GROWTH', '数据结构', 1),
  (22, 'GROWTH', '算法', 2),
  (23, 'GROWTH', '后端开发', 3),
  (24, 'INTEREST', '摄影', 1),
  (25, 'INTEREST', '徒步', 2)
ON DUPLICATE KEY UPDATE tag_name = VALUES(tag_name);

-- ===== 模拟学籍库(学生认证核验数据源) =====
INSERT INTO mock_student_roster (student_no, real_name, college, major_name, enroll_year) VALUES
  ('20220001', '林一航', '计算机科学与技术学院', '计算机科学与技术', 2022),
  ('20220002', '李思远', '软件学院', '软件工程', 2022),
  ('20210003', '陈昊天', '计算机科学与技术学院', '计算机科学与技术', 2021),
  ('20230004', '赵梦琪', '软件学院', '软件工程', 2023)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name);

-- ===== 成长时间线: 考研路线模板(通用) =====
INSERT INTO timeline_template (id, major_tag_id, route_type, name, status) VALUES
  (1, NULL, 'POSTGRAD', '考研通用成长路线', 'PUBLISHED'),
  (2, NULL, 'EMPLOY', '就业通用成长路线', 'PUBLISHED'),
  (3, NULL, 'COMPETITION', '竞赛通用成长路线', 'PUBLISHED'),
  (4, NULL, 'CIVIL', '考公通用成长路线', 'PUBLISHED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO timeline_node (id, template_id, title, stage, suggested_time, suggested_month, importance, order_no, description) VALUES
  (1, 1, '夯实数学与专业基础课', 'GRADE2_2', '大二下学期', 6, 3, 1, '高数/线代/数据结构成绩保持良好，为考研打底'),
  (2, 1, '确定目标院校与专业方向', 'GRADE3_1', '大三上第8周', 11, 3, 2, '结合兴趣与实力圈定 2-3 所目标院校'),
  (3, 1, '第一轮全面复习', 'GRADE3_2', '大三下学期', 4, 3, 3, '数学+专业课过一遍教材与真题'),
  (4, 1, '暑期强化与真题训练', 'GRADE4_1', '大四上暑期', 8, 3, 4, '每日真题+错题本，政治英语启动'),
  (5, 1, '报名与冲刺模考', 'GRADE4_1', '大四上10月', 10, 3, 5, '研招网报名，全真模考查漏补缺'),
  (6, 1, '初试与复试准备', 'GRADE4_2', '大四下', 3, 3, 6, '初试后立即启动复试英语口语与专业面'),
  (11, 2, '完成简历与项目梳理', 'GRADE3_1', '大三上第4周', 10, 3, 1, '把课程项目按 STAR 法写进简历'),
  (12, 2, '投递暑期实习', 'GRADE3_2', '大三下3-4月', 3, 3, 2, '大厂暑期实习集中在春招前投递'),
  (13, 2, '实习转正或秋招准备', 'GRADE4_1', '大四上8-9月', 9, 3, 3, '秋招网申高峰期，笔试面试并行'),
  (14, 2, '签约与毕业设计', 'GRADE4_2', '大四下', 4, 2, 4, '完成三方签约,聚焦毕设'),
  (21, 3, '加入实验室或竞赛团队', 'GRADE1_2', '大一下', 5, 2, 1, 'ACM/数模/大创任选方向组队'),
  (22, 3, '完成一次校级比赛', 'GRADE2_1', '大二上', 11, 2, 2, '以校赛练手,熟悉赛制'),
  (23, 3, '冲击省级/国家级奖项', 'GRADE3_1', '大三上', 9, 3, 3, '数模国赛/ACM区域赛/大创结题'),
  (31, 4, '了解考公方向与岗位表', 'GRADE3_1', '大三上', 10, 2, 1, '国考/省考/选调差异与报考条件'),
  (32, 4, '行测申论系统学习', 'GRADE3_2', '大三下', 4, 3, 2, '每日刷题+申论批改'),
  (33, 4, '国考报名与笔试', 'GRADE4_1', '大四上10-11月', 11, 3, 3, '国考10月报名,11月底笔试')
ON DUPLICATE KEY UPDATE title = VALUES(title);
