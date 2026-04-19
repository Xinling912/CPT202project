-- ==========================================
-- 终极初始化脚本：专家预约系统全量数据 (V2.0)
-- ==========================================
USE booking_system;
SET SQL_SAFE_UPDATES = 0;

-- 1. 扩容 role 字段，防止字符串长度溢出
ALTER TABLE user MODIFY COLUMN role VARCHAR(50);

-- 2. 清理旧数据（仅清理 ID >= 100 的系统预设专家数据，不触碰你的账号）
DELETE FROM time_slot WHERE specialist_id >= 100;
DELETE FROM specialist_profile WHERE user_id >= 100;
DELETE FROM user WHERE id >= 100;
DELETE FROM expertise_category;

-- ==========================================
-- 第一步：初始化专家分类 (Expertise Categories)
-- ==========================================
INSERT INTO expertise_category (id, name, description) VALUES
(1, '金融理财', '涵盖投资理财、风险管理及个人财务规划'),
(2, '雅思外语', '专注IELTS听说读写全方位提分指导'),
(3, '后端开发', 'Java、Spring Boot 及系统架构设计'),
(4, '职场咨询', '简历优化、面试技巧及职业规划'),
(5, '法律服务', '合同审核、企业法务及个人法律咨询'),
(6, '心理健康', '压力疏导、情绪管理及心理成长'),
(7, '网络安全', '渗透测试、安全审计及防御策略'),
(8, '艺术设计', 'UI/UX设计、视觉传达及品牌建模'),
(9, '算法竞赛', 'ACM、大厂面试算法专项突破'),
(10, '留学规划', '常青藤申请、背景提升及文书润色'),
(11, '风险管理', '金融风控、项目审计与合规性评估'),
(12, '数据科学', '大数据分析、机器学习及模型部署'),
(13, '创业指导', '项目孵化、投融资对接及商业计划'),
(14, '文案策划', '创意写作、新媒体运营及品牌故事');

-- ==========================================
-- 第二步：初始化专家账号 (Users)
-- 密码明文：123456
-- ==========================================
INSERT INTO user (id, username, email, password, role, created_at) VALUES
(100, 'ShenShaohui', 'ssh@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
(101, 'Aris_Finance', 'aris@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
(102, 'Sarah_IELTS', 'sarah@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
(103, 'Kevin_Java', 'kevin@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
(104, 'Elena_HR', 'elena@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
(105, 'David_Law', 'david@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
(106, 'Rachel_Soul', 'rachel@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
(107, 'Tom_Cyber', 'tom@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
(108, 'Jessica_Design', 'jessica@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
(109, 'Mark_Algo', 'mark@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
(110, 'Linda_Ivy', 'linda@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
(111, 'Bruce_Risk', 'bruce@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
(112, 'Nancy_Data', 'nancy@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
(113, 'Jack_Startup', 'jack@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW()),
(114, 'Luna_Write', 'luna@expert.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW());

-- ==========================================
-- 第三步：初始化专家档案 (Profiles)
-- ==========================================
INSERT INTO specialist_profile (user_id, expertise_id, hourly_fee, resume, status) VALUES
(100, 1, 888.00, '申少辉，资深金融理财师。性格极其温柔，擅长通过深度共情为客户量身定制资产管理方案。从业15年，零差评，被誉为“最懂客户心的理财管家”。', 'ACTIVE'),
(101, 1, 500.00, 'Aris，深港双地投资专家，擅长二级市场趋势分析与家庭长期资产配置。', 'ACTIVE'),
(102, 2, 350.00, 'Sarah，前雅思口语考官，10年教龄，累计帮助上千学生突破口语瓶颈。', 'ACTIVE'),
(103, 3, 450.00, 'Kevin，前知名大厂技术架构师，精通微服务治理及高并发性能调优。', 'ACTIVE'),
(104, 4, 300.00, 'Elena，知名外企HRBP，擅长职业规划、模拟面试及精准简历重塑。', 'ACTIVE'),
(105, 5, 600.00, 'David，顶级律所合伙人，在商业合同、企业合规及投融资领域有深厚造诣。', 'ACTIVE'),
(106, 6, 400.00, 'Rachel，注册心理咨询师，专注于职场压力疏导与原生家庭情感疗愈。', 'ACTIVE'),
(107, 7, 550.00, 'Tom，安全实验室核心成员，擅长黑盒测试、系统加固及防御策略部署。', 'ACTIVE'),
(108, 8, 400.00, 'Jessica，知名设计奖得主，擅长B端复杂系统交互逻辑与视觉规范建立。', 'ACTIVE'),
(109, 9, 500.00, 'Mark，ACM全球总决赛选手，擅长动态规划、图论及大厂算法专项突破。', 'ACTIVE'),
(110, 10, 450.00, 'Linda，常青藤名校申请导师，擅长挖掘学生核心竞争力及文书深度润色。', 'ACTIVE'),
(111, 11, 500.00, 'Bruce，资深内控审计师，擅长供应链风控模型构建及企业反舞弊审计。', 'ACTIVE'),
(112, 12, 550.00, 'Nancy，统计学博士，深耕自然语言处理（NLP）及推荐算法工业化落地。', 'ACTIVE'),
(113, 13, 700.00, 'Jack，成功连续创业者，拥有多次千万级美元融资经验，擅长商业闭环构建。', 'ACTIVE'),
(114, 14, 300.00, 'Luna，前新媒体主编，擅长品牌故事讲述、爆款文案策划及全平台流量分发。', 'ACTIVE');

-- ==========================================
-- 第四步：初始化预约时间槽 (Time Slots)
-- ==========================================
INSERT INTO time_slot (specialist_id, slot_date, start_time, end_time, is_booked, status) VALUES
(100, '2026-04-20', '2026-04-20 09:00:00', '2026-04-20 10:00:00', 0, 'AVAILABLE'),
(100, '2026-04-20', '2026-04-20 10:30:00', '2026-04-20 11:30:00', 0, 'AVAILABLE'),
(100, '2026-04-21', '2026-04-21 14:00:00', '2026-04-21 15:00:00', 0, 'AVAILABLE'),
(100, '2026-04-22', '2026-04-22 10:00:00', '2026-04-22 11:00:00', 0, 'AVAILABLE'),
(101, '2026-04-20', '2026-04-20 09:00:00', '2026-04-20 10:00:00', 0, 'AVAILABLE'),
(102, '2026-04-20', '2026-04-20 09:00:00', '2026-04-20 10:00:00', 0, 'AVAILABLE'),
(103, '2026-04-21', '2026-04-21 10:00:00', '2026-04-21 11:00:00', 0, 'AVAILABLE');

SET SQL_SAFE_UPDATES = 1;

-- 验证数据是否完整
SELECT u.username, c.name AS category, p.hourly_fee, p.status
FROM user u
JOIN specialist_profile p ON u.id = p.user_id
JOIN expertise_category c ON p.expertise_id = c.id;