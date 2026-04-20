-- ==========================================
-- SAS 系统全量数据整合版 (V5.0)
-- 完美兼容新版 SpecialistProfile 字段 (level, real_name, proposed_expertise_name)
-- ==========================================
USE booking_system;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- 1. 字段扩容与清理
ALTER TABLE user MODIFY COLUMN role VARCHAR(50);

DELETE FROM time_slot;
DELETE FROM specialist_profile;
DELETE FROM user;
DELETE FROM expertise_category;

-- ==========================================
-- 第一步：初始化专家分类
-- ==========================================
INSERT INTO expertise_category (id, name, description) VALUES
                                                           (1, '金融理财', '涵盖投资理财、风险管理及个人财务规划'),
                                                           (2, '雅思外语', '专注IELTS听说读写全方位提分指导'),
                                                           (3, '后端开发', 'Java、Spring Boot 及系统架构设计'),
                                                           (4, '职场咨询', '简历优化、面试技巧及职业规划'),
                                                           (5, '法律服务', '合同审核、企业法务及个人法律咨询'),
                                                           (6, '心理健康/情感', '压力疏导、情感咨询及心理成长'),
                                                           (7, '网络安全', '渗透测试、安全审计及防御策略'),
                                                           (8, '艺术设计', 'UI/UX设计、视觉传达及品牌建模'),
                                                           (9, '算法竞赛', 'ACM、大厂面试算法专项突破'),
                                                           (10, '留学规划', '常青藤申请、背景提升及文书润色');

-- ==========================================
-- 第二步：初始化用户账号 (密码: 123456)
-- ==========================================
-- 核心账号
INSERT INTO user (id, username, email, password, role, created_at) VALUES
                                                                       (1, 'Wanfeng', 'expert@qq.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (2, 'Carrot', 'carrot@qq.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'CUSTOMER', NOW()),
                                                                       (3, 'God', 'admin@qq.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'ADMIN', NOW());

-- 小徐的专家团
INSERT INTO user (id, username, email, password, role, created_at) VALUES
                                                                       (100, 'ShenShaohui', 'ssh@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (101, 'Aris_Finance', 'aris@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (102, 'Sarah_IELTS', 'sarah@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (103, 'Kevin_Java', 'kevin@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (105, 'David_Law', 'david@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (108, 'Jessica_Design', 'jessica@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW());

-- ==========================================
-- 第三步：初始化专家档案 (严格对应 9 个字段)
-- 字段: id, hourly_fee, level, status, expertise_id, user_id, proposed_expertise_name, real_name, resume
-- ==========================================
INSERT INTO specialist_profile
(id, hourly_fee, level, status, expertise_id, user_id, proposed_expertise_name, real_name, resume)
VALUES
    (1, 100.00, 'SENIOR', 'ACTIVE', 3, 1, NULL, '万峰', '资深后端开发专家。'),
    (100, 888.00, 'EXPERT', 'ACTIVE', 6, 100, NULL, '申少辉', '资深情感咨询师。说话理智像人机，逻辑分析能力恐怖，能精准剖析任何复杂的情感纠纷。'),
    (101, 500.00, 'EXPERT', 'ACTIVE', 1, 101, NULL, 'Aris', '深港双地投资专家，擅长二级市场趋势分析与家庭长期资产配置。'),
    (102, 350.00, 'SENIOR', 'ACTIVE', 2, 102, NULL, 'Sarah', '前雅思口语考官，10年教龄，累计帮助上千学生突破口语瓶颈。'),
    (103, 450.00, 'EXPERT', 'ACTIVE', 3, 103, NULL, 'Kevin', '前知名大厂技术架构师，精通微服务治理及高并发性能调优。'),
    (105, 600.00, 'EXPERT', 'ACTIVE', 5, 105, NULL, 'David', '顶级律所合伙人，在商业合同、企业合规及投融资领域有深厚造诣。'),
    (108, 400.00, 'SENIOR', 'ACTIVE', 8, 108, NULL, 'Jessica', '知名设计奖得主，擅长B端复杂系统交互逻辑与视觉规范建立。');

-- ==========================================
-- 第四步：初始化预约时间槽
-- ==========================================
INSERT INTO time_slot (specialist_id, slot_date, start_time, end_time, is_booked, status) VALUES
                                                                                              (100, '2026-04-20', '2026-04-20 09:00:00', '2026-04-20 10:00:00', 0, 'AVAILABLE'),
                                                                                              (100, '2026-04-20', '2026-04-20 10:30:00', '2026-04-20 11:30:00', 0, 'AVAILABLE'),
                                                                                              (101, '2026-04-20', '2026-04-20 09:00:00', '2026-04-20 10:00:00', 0, 'AVAILABLE'),
                                                                                              (1, '2026-04-22', '2026-04-22 10:00:00', '2026-04-22 11:00:00', 0, 'AVAILABLE');

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;
--     1