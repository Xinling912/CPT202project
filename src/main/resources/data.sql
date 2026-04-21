USE booking_system;

-- 1. 先关闭外键检查和安全更新
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- 2. 🌟 核心修复：按顺序用 TRUNCATE 彻底清空数据并重置自增 ID
TRUNCATE TABLE booking;
TRUNCATE TABLE time_slot;
TRUNCATE TABLE specialist_profile;
TRUNCATE TABLE user;
TRUNCATE TABLE expertise_category;

-- 第一步：分类
INSERT INTO expertise_category (id, name, description) VALUES
                                                           (1, '金融理财', '涵盖投资理财、风险管理及个人财务规划'),
                                                           (2, '雅思外语', '专注IELTS听说读写全方位提分指导'),
                                                           (3, '后端开发', 'Java、Spring Boot 及系统架构设计'),
                                                           (4, '职场咨询', '简历优化、面试技巧及职业规划'),
                                                           (5, '法律服务', '合同审核、企业法务及个人法律咨询'),
                                                           (6, '心理健康/情感', '压力疏导、情感咨询及心理成长'),
                                                           (8, '艺术设计', 'UI/UX设计、视觉传达及品牌建模');

-- 第二步：用户
INSERT INTO user (id, username, email, password, role, created_at) VALUES
                                                                       (1, 'Wanfeng', 'expert@qq.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (2, 'Carrot', 'carrot@qq.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'CUSTOMER', NOW()),
                                                                       (3, 'God', 'admin@qq.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'ADMIN', NOW()),
                                                                       (100, 'ShenShaohui', 'ssh@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (101, 'XingjianWu', 'wu@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (102, 'XinlingDu', 'du@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (103, 'Sarah_IELTS', 'sarah@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (104, 'Jessica_Design', 'jessica@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW());

-- 第三步：档案
INSERT INTO specialist_profile (id, hourly_fee, level, status, expertise_id, user_id, proposed_expertise_name, real_name, resume) VALUES
                                                                                                                                      (1, 100.00, 'SENIOR', 'ACTIVE', 3, 1, NULL, '万峰', '资深后端开发专家，精通 Spring Boot。'),
                                                                                                                                      (100, 888.00, 'EXPERT', 'ACTIVE', 6, 100, NULL, 'Shaohui Shen', '资深情感博主。如果你说话像人机、根本不会撩妹，那你就该来咨询我了！专治各种“直男发言”，通过极致的逻辑拆解，让你的情商实现降维打击。'),
                                                                                                                                      (101, 600.00, 'EXPERT', 'ACTIVE', 5, 101, NULL, 'Xingjian Wu', '深耕企业法务与刑事辩护多年，以客观精准著称。'),
                                                                                                                                      (102, 500.00, 'EXPERT', 'ACTIVE', 1, 102, NULL, 'Xinling Du', '资深理财师，擅长二级市场趋势分析与资产配置。'),
                                                                                                                                      (103, 350.00, 'SENIOR', 'ACTIVE', 2, 103, NULL, 'Sarah', '前雅思考官，10年教龄。'),
                                                                                                                                      (104, 400.00, 'SENIOR', 'ACTIVE', 8, 104, NULL, 'Jessica', '知名设计奖得主，擅长B端交互设计。');

-- 第四步：排班
INSERT INTO time_slot (specialist_id, slot_date, start_time, end_time, is_booked, status) VALUES
                                                                                              (101, '2026-04-22', '2026-04-22 09:00:00', '2026-04-22 10:00:00', 0, 'AVAILABLE'),
                                                                                              (101, '2026-04-22', '2026-04-22 10:00:00', '2026-04-22 11:00:00', 0, 'AVAILABLE'),
                                                                                              (102, '2026-04-22', '2026-04-22 14:00:00', '2026-04-22 15:00:00', 0, 'AVAILABLE');

-- 3. 重新开启检查
SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;