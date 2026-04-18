-- 1. 无脑塞入一个专业分类 (ID=1)，防止缺失
INSERT IGNORE INTO expertise_category (id, name, description) VALUES (1, 'General', 'General Specialist');

-- 2. 🌟 【新增】给组员预留的管理员/专家账号！

DELETE FROM user WHERE id = 1;

-- 2. 再把全新的 Wanfeng 塞进去！
INSERT INTO user (id, username, email, password, role)
VALUES (1, 'Wanfeng', 'expert@expert.com', '$2a$10$0dD3K0i', 'SPECIALIST');
-- 3. 无脑发一张专家执照给这个账号 (ID=1, 绑定上面的用户和分类)
INSERT IGNORE INTO specialist_profile (id, user_id, expertise_id, hourly_fee, status)
VALUES (1, 1, 1, 100.00, 'ACTIVE');

-- ==========================================
-- 4. 下面是排班代码，每次执行先清空旧数据，防止重复
-- ==========================================
DELETE FROM time_slot WHERE specialist_id = 1;

INSERT INTO time_slot (specialist_id, slot_date, start_time, end_time, is_booked, status) VALUES
                                                                                              (1, '2026-04-12', '2026-04-12 09:00:00', '2026-04-12 10:00:00', 0, 'AVAILABLE'),
                                                                                              (1, '2026-04-18', '2026-04-18 14:00:00', '2026-04-18 15:00:00', 0, 'AVAILABLE'),
                                                                                              (1, '2026-04-18', '2026-04-18 16:30:00', '2026-04-18 17:30:00', 0, 'AVAILABLE'),
                                                                                              (1, '2026-04-22', '2026-04-22 10:00:00', '2026-04-22 11:00:00', 0, 'AVAILABLE');