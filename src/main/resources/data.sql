USE booking_system;

-- 1. Disable foreign key checks and safe updates first
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

TRUNCATE TABLE complaint;
TRUNCATE TABLE specialist_profile_edit_request;
TRUNCATE TABLE booking;
TRUNCATE TABLE time_slot;
TRUNCATE TABLE specialist_profile;
TRUNCATE TABLE user;
TRUNCATE TABLE expertise_category;

-- Step 1: Categories (Added 9-Film and Television Acting, 10-Esports)
INSERT INTO expertise_category (id, name, description) VALUES
                                                           (1, 'Music/Singing Guidance', 'Top 10 national singers guide you to start your music journey, focusing on vocal skills and stage performance'),
                                                           (2, 'IELTS Foreign Language', 'Focusing on all-around improvement guidance for IELTS listening, speaking, reading, and writing'),
                                                           (3, 'Backend Development', 'Java, Spring Boot, and System Architecture Design'),
                                                           (4, 'Career Consulting', 'Resume optimization, interview skills, and career planning'),
                                                           (5, 'Legal Services', 'Contract review, corporate legal affairs, and personal legal consulting'),
                                                           (6, 'Mental Health/Emotional Wellness', 'Stress relief, emotional counseling, and psychological growth'),

                                                           (7, 'Algorithm Competitions', 'In-depth guidance for ACM/ICPC, Lanqiao Cup, and major algorithm competitions'),
                                                           (8, 'Art and Design', 'UI/UX design, visual communication, and brand modeling'),
                                                           (9, 'Film and Television Acting', 'One-on-one guidance from senior actors, comprehensive improvement of line delivery skills and camera presence'),
                                                           (10, 'Esports', 'Professional-level esports strategy analysis, teaching top-tier mechanics and macro-awareness');

-- Step 2: Users (Added Qlin, zhangjuyi, Faker)
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

-- Step 3: Profiles with Resumes
INSERT INTO specialist_profile (id, hourly_fee, level, status, expertise_id, user_id, proposed_expertise_name, real_name, resume) VALUES
                                                                                                                                      (1, 10.00, 'SENIOR', 'ACTIVE', 3, 1, NULL, 'Wanfeng', 'Senior backend development expert, highly proficient in Spring Boot.'),
                                                                                                                                      (100, 8.88, 'EXPERT', 'ACTIVE', 6, 100, NULL, 'Shaohui Shen', 'Senior emotional blogger. If you talk like a bot and have no idea how to flirt, then you should consult me! Specializing in curing all kinds of "straight male talks", using ultimate logical breakdown to deliver a multidimensional strike to your emotional intelligence.'),
                                                                                                                                      (101, 60.00, 'EXPERT', 'ACTIVE', 5, 101, NULL, 'Xingjian Wu', 'Deeply engaged in corporate legal affairs and criminal defense for many years, known for objectivity and precision.'),
                                                                                                                                      (102, 698.00, 'EXPERT', 'ACTIVE', 1, 102, NULL, 'Xinling Du', 'Hello everyone, I am Xinling Du, one of the top 10 national singers, the number one sister in the singing field of Deyang, Sichuan, and also a computer science student. If you have any questions about music, just call me.'),
                                                                                                                                      (103, 35.00, 'SENIOR', 'ACTIVE', 2, 103, NULL, 'Sarah', 'Former IELTS examiner, 10 years of teaching experience.'),
                                                                                                                                      (104, 40.00, 'SENIOR', 'ACTIVE', 8, 104, NULL, 'Jessica', 'Renowned design award winner, specializing in B2B interaction design.'),
                                                                                                                                      (105, 535.00, 'EXPERT', 'ACTIVE', 9, 105, NULL, 'zhangjuyi', 'Well-known veteran film and television actor, working for many years, shaping countless classic screen images. Not only can I teach you voice, lines, form, and acting, but also lead you to understand the soul behind the script.'),
                                                                                                                                      (106, 9999.00, 'SENIOR', 'ACTIVE', 10, 106, NULL, 'Faker', 'The absolute core of T1, six-time League of Legends World Champion, the undisputed God of Esports (The Unkillable Demon King). Want to learn screen switching and prediction? Come find me.');

INSERT INTO time_slot (specialist_id, slot_date, start_time, end_time, status) VALUES
                                                                                   (106, '2026-05-06', '14:00:00', '16:00:00', 'AVAILABLE'),
                                                                                   (106, '2026-05-15', '20:00:00', '22:00:00', 'AVAILABLE'),
                                                                                   (105, '2026-05-19', '10:00:00', '12:00:00', 'AVAILABLE'),
                                                                                   (103, '2026-05-11', '09:00:00', '10:00:00', 'AVAILABLE'),
                                                                                   (103, '2026-05-11', '10:00:00', '11:00:00', 'AVAILABLE'),
                                                                                   (103, '2026-05-11', '11:00:00', '12:00:00', 'AVAILABLE'),
                                                                                   (104, '2026-05-07', '09:15:00', '10:00:00', 'AVAILABLE'),
                                                                                   (104, '2026-05-13', '14:30:00', '15:00:00', 'AVAILABLE'),
                                                                                   (104, '2026-05-18', '16:45:00', '18:15:00', 'AVAILABLE'),
                                                                                   (101, '2026-05-12', '06:30:00', '08:00:00', 'AVAILABLE'),
                                                                                   (100, '2026-05-08', '23:00:00', '23:59:59', 'AVAILABLE'),
                                                                                   (1, '2026-05-16', '13:00:00', '18:00:00', 'AVAILABLE'),
                                                                                   (102, '2026-05-17', '14:00:00', '17:00:00', 'AVAILABLE'),
                                                                                   (106, '2026-05-09', '14:00:00', '16:00:00', 'AVAILABLE'),
                                                                                   (101, '2026-05-09', '14:00:00', '16:00:00', 'AVAILABLE'),
                                                                                   (105, '2026-05-09', '14:00:00', '16:00:00', 'AVAILABLE'),
                                                                                   (106, '2026-05-10', '14:00:00', '15:00:00', 'BOOKED'),
                                                                                   (103, '2026-05-12', '09:00:00', '11:00:00', 'BOOKED'),
                                                                                   (102, '2026-05-01', '09:00:00', '10:00:00', 'AVAILABLE'),
                                                                                   (106, '2026-05-04', '14:00:00', '16:00:00', 'BOOKED');
SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;