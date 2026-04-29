USE booking_system;

-- 1. 先关闭外键检查和安全更新
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

TRUNCATE TABLE complaint;
TRUNCATE TABLE specialist_profile_edit_request;
TRUNCATE TABLE booking;
TRUNCATE TABLE time_slot;
TRUNCATE TABLE specialist_profile;
TRUNCATE TABLE user;

-- 第一步：分类 (新增 9-影视表演, 10-电子竞技)
INSERT INTO expertise_category (id, name, description) VALUES
                                                           (1, '音乐/歌唱指导', '全国十佳歌手带你开启音乐之旅，专注声乐技巧与舞台表现'),
                                                           (2, '雅思外语', '专注IELTS听说读写全方位提分指导'),
                                                           (3, '后端开发', 'Java、Spring Boot 及系统架构设计'),
                                                           (4, '职场咨询', '简历优化、面试技巧及职业规划'),
                                                           (5, '法律服务', '合同审核、企业法务及个人法律咨询'),
                                                           (6, '心理健康/情感', '压力疏导、情感咨询及心理成长'),

                                                           (7, '算法竞赛', 'ACM/ICPC、蓝桥杯及各大算法竞赛深度指导'),
                                                           (8, '艺术设计', 'UI/UX设计、视觉传达及品牌建模'),
                                                           (9, '影视表演', '资深演员一对一指导，台词功底与镜头感全方位提升'),
                                                           (10, '电子竞技', '职业级电竞思路解析，顶尖操作与大局观教学');

-- 第二步：用户 (新增 Qlin, zhangjuyi, Faker)
INSERT INTO user (id, username, email, password, role, created_at) VALUES
                                                                       (1, 'Wanfeng', 'expert@qq.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (2, 'Carrot', 'carrot@qq.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'CUSTOMER', NOW()),
                                                                       (3, 'God', 'admin@qq.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'ADMIN', NOW()),
                                                                       (4, 'Qlin', 'Qlin@qq.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'CUSTOMER', NOW()),
                                                                       (5, 'Tudou', 'Tudou@qq.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'CUSTOMER', NOW()),
                                                                       (100, 'ShenShaohui', 'ssh@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (101, 'XingjianWu', 'wu@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (102, 'XinlingDu', 'du@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (103, 'Sarah_IELTS', 'sarah@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (104, 'Jessica_Design', 'jessica@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (105, 'zhangjuyi', 'zjy@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
                                                                       (106, 'Faker', 'faker@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW());

-- 第三步：档案 配上简历
INSERT INTO specialist_profile (id, hourly_fee, level, status, expertise_id, user_id, proposed_expertise_name, real_name, resume) VALUES
                                                                                                                                      (1, 10.00, 'SENIOR', 'ACTIVE', 3, 1, NULL, '万峰', '资深后端开发专家，精通 Spring Boot。'),
                                                                                                                                      (100, 8.88, 'EXPERT', 'ACTIVE', 6, 100, NULL, 'Shaohui Shen', '资深情感博主。如果你说话像人机、根本不会撩妹，那你就该来咨询我了！专治各种“直男发言”，通过极致的逻辑拆解，让你的情商实现降维打击。'),
                                                                                                                                      (101, 60.00, 'EXPERT', 'ACTIVE', 5, 101, NULL, 'Xingjian Wu', '深耕企业法务与刑事辩护多年，以客观精准著称。'),
                                                                                                                                      (102, 698.00, 'EXPERT', 'ACTIVE', 1, 102, NULL, 'Xinling Du', 'Hello,大家好,我是杜馨玲,全国十佳歌手之一,四川德阳歌坛领域的一姐,同时也是一名在读计算机学生.如果你有任何关于music的问题,call me就对了.'),
                                                                                                                                      (103, 35.00, 'SENIOR', 'ACTIVE', 2, 103, NULL, 'Sarah', '前雅思考官，10年教龄。'),
                                                                                                                                      (104, 40.00, 'SENIOR', 'ACTIVE', 8, 104, NULL, 'Jessica', '知名设计奖得主，擅长B端交互设计。'),
                                                                                                                                      (105, 535.00, 'EXPERT', 'ACTIVE', 9, 105, NULL, 'zhangjuyi', '知名实力派影视演员，从业多年，塑造无数经典银幕形象。不仅能教你声台形表，更能带你理解剧本背后的灵魂。'),
                                                                                                                                      (106, 9999.00, 'SENIOR', 'ACTIVE', 10, 106, NULL, 'Faker', 'T1 绝对核心，英雄联盟六冠王，无可争议的电竞之神 (The Unkillable Demon King)。想学切屏和预判？来找我吧');

INSERT INTO time_slot (specialist_id, slot_date, start_time, end_time, status) VALUES
                                                                                   (106, '2026-05-05', '09:00:00', '11:00:00', 'AVAILABLE'),

                                                                                   (106, '2026-05-05', '14:00:00', '15:00:00', 'AVAILABLE'),
                                                                                   (106, '2026-05-06', '14:00:00', '17:00:00', 'AVAILABLE'),

                                                                                   (101, '2026-05-05', '09:00:00', '11:00:00', 'AVAILABLE'),

                                                                                   (102, '2026-05-05', '14:00:00', '16:00:00', 'AVAILABLE');
SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;