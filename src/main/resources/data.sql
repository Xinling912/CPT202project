-- ==========================================
-- 第一步：按顺序清理旧数据（防外键报错）  $2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq
-- ==========================================
DELETE FROM time_slot WHERE specialist_id = 1;
DELETE FROM specialist_profile WHERE user_id = 1;
DELETE FROM user WHERE id IN (1, 2);

-- ==========================================
-- 第二步：初始化基础数据
-- ==========================================
INSERT IGNORE INTO expertise_category (id, name, description) VALUES (1, 'General', 'General Specialist');

-- ==========================================
-- 第三步：塞入测试账号 (加入了必填的 created_at 字段)
-- 密码我重置成了标准加密串，两个账号的明文密码现在都是：123456
-- ==========================================

INSERT INTO user (id, username, email, password, role, created_at)
VALUES (1, 'Wanfeng', 'expert@qq.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'SPECIALIST', NOW());

INSERT INTO user (id, username, email, password, role, created_at)
VALUES (2, 'Carrot', 'carrot@qq.com', '$2a$10$PUKIponqAJqaZCWN/kBoIOJT3pf/aRBs5N1Eg9M4F/aGRQw/3bacq', 'CUSTOMER', NOW());

-- ==========================================
-- 第四步：给专家发执照并排班
-- ==========================================
INSERT INTO specialist_profile (id, user_id, expertise_id, hourly_fee, status)
VALUES (1, 1, 1, 100.00, 'ACTIVE');

INSERT INTO time_slot (specialist_id, slot_date, start_time, end_time, is_booked, status) VALUES
                                                                                              (1, '2026-04-12', '2026-04-12 09:00:00', '2026-04-12 10:00:00', 0, 'AVAILABLE'),
                                                                                              (1, '2026-04-18', '2026-04-18 14:00:00', '2026-04-18 15:00:00', 0, 'AVAILABLE'),
                                                                                              (1, '2026-04-18', '2026-04-18 16:30:00', '2026-04-18 17:30:00', 0, 'AVAILABLE'),
                                                                                              (1, '2026-04-22', '2026-04-22 10:00:00', '2026-04-22 11:00:00', 0, 'AVAILABLE');