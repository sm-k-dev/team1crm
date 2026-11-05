-- Development-only dummy data for CRM. Run AFTER starting the app once to create JPA tables.
-- Run manually in MySQL Workbench / IntelliJ SQL console; never on production.
-- Fixed 900xxx IDs + INSERT IGNORE make repeated runs safe for unchanged schema.
-- Passwords are BCrypt hashes of test password "password"; use only for local dummy accounts.
USE crm;
START TRANSACTION;

-- member: 4 rows
INSERT IGNORE INTO `member` (`id`, `email`, `password`, `nickname`, `role`, `created_at`) VALUES (900001, 'admin.demo@crm.local', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi8m8n2nLxZ3B7oKzJjKnS9Jc4F1gTe', '관리자테스트', 'ADMIN', '2026-10-01 09:00:00');
INSERT IGNORE INTO `member` (`id`, `email`, `password`, `nickname`, `role`, `created_at`) VALUES (900002, 'owner1.demo@crm.local', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi8m8n2nLxZ3B7oKzJjKnS9Jc4F1gTe', '카페사장', 'USER', '2026-10-01 09:00:00');
INSERT IGNORE INTO `member` (`id`, `email`, `password`, `nickname`, `role`, `created_at`) VALUES (900003, 'owner2.demo@crm.local', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi8m8n2nLxZ3B7oKzJjKnS9Jc4F1gTe', '식당사장', 'USER', '2026-10-01 09:00:00');
INSERT IGNORE INTO `member` (`id`, `email`, `password`, `nickname`, `role`, `created_at`) VALUES (900004, 'owner3.demo@crm.local', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi8m8n2nLxZ3B7oKzJjKnS9Jc4F1gTe', '베이커리사장', 'USER', '2026-10-01 09:00:00');

-- store: 5 rows
INSERT IGNORE INTO `store` (`id`, `member_id`, `name`, `category`, `address`, `point_rate`, `created_at`) VALUES (900101, 900002, '모닝커피 강남점', '카페', '서울 강남구 테헤란로 10', 5, '2026-10-02 10:00:00');
INSERT IGNORE INTO `store` (`id`, `member_id`, `name`, `category`, `address`, `point_rate`, `created_at`) VALUES (900102, 900002, '모닝커피 홍대점', '카페', '서울 마포구 양화로 20', 3, '2026-10-02 10:00:00');
INSERT IGNORE INTO `store` (`id`, `member_id`, `name`, `category`, `address`, `point_rate`, `created_at`) VALUES (900103, 900003, '한상식당 본점', '한식', '서울 종로구 종로 30', 5, '2026-10-02 10:00:00');
INSERT IGNORE INTO `store` (`id`, `member_id`, `name`, `category`, `address`, `point_rate`, `created_at`) VALUES (900104, 900003, '한상식당 2호점', '한식', '서울 송파구 올림픽로 40', 4, '2026-10-02 10:00:00');
INSERT IGNORE INTO `store` (`id`, `member_id`, `name`, `category`, `address`, `point_rate`, `created_at`) VALUES (900105, 900004, '달콤베이커리', '베이커리', '서울 서초구 서초대로 50', 7, '2026-10-02 10:00:00');

-- customer: 30 rows
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900201, 900101, '김하늘', '010-2000-4000', 1, '단골 고객', 0, '2026-10-03 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900202, 900101, '이서준', '010-2001-4001', 2, '첫 방문 고객', 1, '2026-10-04 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900203, 900101, '박지우', '010-2002-4002', 3, '포인트 관심', 1, '2026-10-05 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900204, 900101, '최민서', '010-2003-4003', 4, '생일 쿠폰 안내', 0, '2026-10-06 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900205, 900101, '정도윤', '010-2004-4004', 5, NULL, 1, '2026-10-07 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900206, 900101, '강수빈', '010-2005-4005', 6, '단골 고객', 1, '2026-10-08 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900207, 900102, '조예린', '010-2006-4006', 7, '첫 방문 고객', 0, '2026-10-09 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900208, 900102, '윤지호', '010-2007-4007', 8, '포인트 관심', 1, '2026-10-10 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900209, 900102, '장유진', '010-2008-4008', 9, '생일 쿠폰 안내', 1, '2026-10-11 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900210, 900102, '임태양', '010-2009-4009', 10, NULL, 0, '2026-10-12 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900211, 900102, '한서아', '010-2010-4010', 11, '단골 고객', 1, '2026-10-13 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900212, 900102, '오지훈', '010-2011-4011', 12, '첫 방문 고객', 1, '2026-10-14 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900213, 900103, '신가은', '010-2012-4012', 1, '포인트 관심', 0, '2026-10-15 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900214, 900103, '서민준', '010-2013-4013', 2, '생일 쿠폰 안내', 1, '2026-10-16 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900215, 900103, '권지민', '010-2014-4014', 3, NULL, 1, '2026-10-17 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900216, 900103, '황다은', '010-2015-4015', 4, '단골 고객', 0, '2026-10-18 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900217, 900103, '안유나', '010-2016-4016', 5, '첫 방문 고객', 1, '2026-10-19 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900218, 900103, '송재현', '010-2017-4017', 6, '포인트 관심', 1, '2026-10-20 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900219, 900104, '전소율', '010-2018-4018', 7, '생일 쿠폰 안내', 0, '2026-10-21 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900220, 900104, '홍예준', '010-2019-4019', 8, NULL, 1, '2026-10-22 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900221, 900104, '문시우', '010-2020-4020', 9, '단골 고객', 1, '2026-10-03 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900222, 900104, '백다현', '010-2021-4021', 10, '첫 방문 고객', 0, '2026-10-04 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900223, 900104, '유지안', '010-2022-4022', 11, '포인트 관심', 1, '2026-10-05 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900224, 900104, '남도현', '010-2023-4023', 12, '생일 쿠폰 안내', 1, '2026-10-06 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900225, 900105, '노하린', '010-2024-4024', 1, NULL, 0, '2026-10-07 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900226, 900105, '차은우', '010-2025-4025', 2, '단골 고객', 1, '2026-10-08 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900227, 900105, '배지수', '010-2026-4026', 3, '첫 방문 고객', 1, '2026-10-09 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900228, 900105, '류현우', '010-2027-4027', 4, '포인트 관심', 0, '2026-10-10 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900229, 900105, '성유빈', '010-2028-4028', 5, '생일 쿠폰 안내', 1, '2026-10-11 11:00:00');
INSERT IGNORE INTO `customer` (`id`, `store_id`, `name`, `phone`, `birth_month`, `memo`, `agree_marketing`, `created_at`) VALUES (900230, 900105, '표서연', '010-2029-4029', 6, NULL, 1, '2026-10-12 11:00:00');

-- visit: 60 rows
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900301, 900201, '2026-10-05 10:00:00', 6500, 500, '아메리카노');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900302, 900202, '2026-10-06 11:07:00', 12000, 0, '점심 세트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900303, 900203, '2026-10-07 12:14:00', 18000, 0, '디저트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900304, 900204, '2026-10-08 13:21:00', 25000, 0, '테이크아웃');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900305, 900205, '2026-10-09 14:28:00', 34000, 0, '저녁 식사');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900306, 900206, '2026-10-10 15:35:00', 45000, 500, '아메리카노');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900307, 900207, '2026-10-11 16:42:00', 6500, 0, '점심 세트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900308, 900208, '2026-10-12 17:49:00', 12000, 0, '디저트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900309, 900209, '2026-10-13 18:56:00', 18000, 0, '테이크아웃');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900310, 900210, '2026-10-14 19:03:00', 25000, 0, '저녁 식사');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900311, 900211, '2026-10-15 10:10:00', 34000, 500, '아메리카노');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900312, 900212, '2026-10-16 11:17:00', 45000, 0, '점심 세트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900313, 900213, '2026-10-17 12:24:00', 6500, 0, '디저트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900314, 900214, '2026-10-18 13:31:00', 12000, 0, '테이크아웃');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900315, 900215, '2026-10-19 14:38:00', 18000, 0, '저녁 식사');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900316, 900216, '2026-10-20 15:45:00', 25000, 500, '아메리카노');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900317, 900217, '2026-10-21 16:52:00', 34000, 0, '점심 세트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900318, 900218, '2026-10-22 17:59:00', 45000, 0, '디저트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900319, 900219, '2026-10-23 18:06:00', 6500, 0, '테이크아웃');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900320, 900220, '2026-10-24 19:13:00', 12000, 0, '저녁 식사');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900321, 900221, '2026-10-25 10:20:00', 18000, 500, '아메리카노');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900322, 900222, '2026-10-26 11:27:00', 25000, 0, '점심 세트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900323, 900223, '2026-10-27 12:34:00', 34000, 0, '디저트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900324, 900224, '2026-10-05 13:41:00', 45000, 0, '테이크아웃');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900325, 900225, '2026-10-06 14:48:00', 6500, 0, '저녁 식사');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900326, 900226, '2026-10-07 15:55:00', 12000, 500, '아메리카노');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900327, 900227, '2026-10-08 16:02:00', 18000, 0, '점심 세트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900328, 900228, '2026-10-09 17:09:00', 25000, 0, '디저트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900329, 900229, '2026-10-10 18:16:00', 34000, 0, '테이크아웃');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900330, 900230, '2026-10-11 19:23:00', 45000, 0, '저녁 식사');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900331, 900201, '2026-10-12 10:30:00', 6500, 500, '아메리카노');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900332, 900202, '2026-10-13 11:37:00', 12000, 0, '점심 세트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900333, 900203, '2026-10-14 12:44:00', 18000, 0, '디저트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900334, 900204, '2026-10-15 13:51:00', 25000, 0, '테이크아웃');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900335, 900205, '2026-10-16 14:58:00', 34000, 0, '저녁 식사');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900336, 900206, '2026-10-17 15:05:00', 45000, 500, '아메리카노');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900337, 900207, '2026-10-18 16:12:00', 6500, 0, '점심 세트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900338, 900208, '2026-10-19 17:19:00', 12000, 0, '디저트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900339, 900209, '2026-10-20 18:26:00', 18000, 0, '테이크아웃');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900340, 900210, '2026-10-21 19:33:00', 25000, 0, '저녁 식사');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900341, 900211, '2026-10-22 10:40:00', 34000, 500, '아메리카노');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900342, 900212, '2026-10-23 11:47:00', 45000, 0, '점심 세트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900343, 900213, '2026-10-24 12:54:00', 6500, 0, '디저트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900344, 900214, '2026-10-25 13:01:00', 12000, 0, '테이크아웃');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900345, 900215, '2026-10-26 14:08:00', 18000, 0, '저녁 식사');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900346, 900216, '2026-10-27 15:15:00', 25000, 500, '아메리카노');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900347, 900217, '2026-10-05 16:22:00', 34000, 0, '점심 세트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900348, 900218, '2026-10-06 17:29:00', 45000, 0, '디저트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900349, 900219, '2026-10-07 18:36:00', 6500, 0, '테이크아웃');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900350, 900220, '2026-10-08 19:43:00', 12000, 0, '저녁 식사');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900351, 900221, '2026-10-09 10:50:00', 18000, 500, '아메리카노');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900352, 900222, '2026-10-10 11:57:00', 25000, 0, '점심 세트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900353, 900223, '2026-10-11 12:04:00', 34000, 0, '디저트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900354, 900224, '2026-10-12 13:11:00', 45000, 0, '테이크아웃');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900355, 900225, '2026-10-13 14:18:00', 6500, 0, '저녁 식사');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900356, 900226, '2026-10-14 15:25:00', 12000, 500, '아메리카노');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900357, 900227, '2026-10-15 16:32:00', 18000, 0, '점심 세트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900358, 900228, '2026-10-16 17:39:00', 25000, 0, '디저트');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900359, 900229, '2026-10-17 18:46:00', 34000, 0, '테이크아웃');
INSERT IGNORE INTO `visit` (`id`, `customer_id`, `visited_at`, `amount`, `used_point`, `menu_memo`) VALUES (900360, 900230, '2026-10-18 19:53:00', 45000, 0, '저녁 식사');

-- point_history: 60 rows
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900401, 900201, 900301, 'USE', 500, '2026-10-05 10:00:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900402, 900202, 900302, 'EARN', 600, '2026-10-06 11:07:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900403, 900203, 900303, 'EARN', 900, '2026-10-07 12:14:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900404, 900204, 900304, 'EARN', 1250, '2026-10-08 13:21:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900405, 900205, 900305, 'EARN', 1700, '2026-10-09 14:28:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900406, 900206, 900306, 'USE', 500, '2026-10-10 15:35:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900407, 900207, 900307, 'EARN', 325, '2026-10-11 16:42:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900408, 900208, 900308, 'EARN', 600, '2026-10-12 17:49:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900409, 900209, 900309, 'EARN', 900, '2026-10-13 18:56:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900410, 900210, 900310, 'EARN', 1250, '2026-10-14 19:03:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900411, 900211, 900311, 'USE', 500, '2026-10-15 10:10:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900412, 900212, 900312, 'EARN', 2250, '2026-10-16 11:17:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900413, 900213, 900313, 'EARN', 325, '2026-10-17 12:24:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900414, 900214, 900314, 'EARN', 600, '2026-10-18 13:31:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900415, 900215, 900315, 'EARN', 900, '2026-10-19 14:38:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900416, 900216, 900316, 'USE', 500, '2026-10-20 15:45:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900417, 900217, 900317, 'EARN', 1700, '2026-10-21 16:52:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900418, 900218, 900318, 'EARN', 2250, '2026-10-22 17:59:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900419, 900219, 900319, 'EARN', 325, '2026-10-23 18:06:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900420, 900220, 900320, 'EARN', 600, '2026-10-24 19:13:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900421, 900221, 900321, 'USE', 500, '2026-10-25 10:20:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900422, 900222, 900322, 'EARN', 1250, '2026-10-26 11:27:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900423, 900223, 900323, 'EARN', 1700, '2026-10-27 12:34:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900424, 900224, 900324, 'EARN', 2250, '2026-10-05 13:41:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900425, 900225, 900325, 'EARN', 325, '2026-10-06 14:48:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900426, 900226, 900326, 'USE', 500, '2026-10-07 15:55:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900427, 900227, 900327, 'EARN', 900, '2026-10-08 16:02:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900428, 900228, 900328, 'EARN', 1250, '2026-10-09 17:09:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900429, 900229, 900329, 'EARN', 1700, '2026-10-10 18:16:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900430, 900230, 900330, 'EARN', 2250, '2026-10-11 19:23:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900431, 900201, 900331, 'USE', 500, '2026-10-12 10:30:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900432, 900202, 900332, 'EARN', 600, '2026-10-13 11:37:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900433, 900203, 900333, 'EARN', 900, '2026-10-14 12:44:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900434, 900204, 900334, 'EARN', 1250, '2026-10-15 13:51:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900435, 900205, 900335, 'EARN', 1700, '2026-10-16 14:58:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900436, 900206, 900336, 'USE', 500, '2026-10-17 15:05:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900437, 900207, 900337, 'EARN', 325, '2026-10-18 16:12:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900438, 900208, 900338, 'EARN', 600, '2026-10-19 17:19:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900439, 900209, 900339, 'EARN', 900, '2026-10-20 18:26:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900440, 900210, 900340, 'EARN', 1250, '2026-10-21 19:33:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900441, 900211, 900341, 'USE', 500, '2026-10-22 10:40:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900442, 900212, 900342, 'EARN', 2250, '2026-10-23 11:47:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900443, 900213, 900343, 'EARN', 325, '2026-10-24 12:54:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900444, 900214, 900344, 'EARN', 600, '2026-10-25 13:01:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900445, 900215, 900345, 'EARN', 900, '2026-10-26 14:08:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900446, 900216, 900346, 'USE', 500, '2026-10-27 15:15:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900447, 900217, 900347, 'EARN', 1700, '2026-10-05 16:22:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900448, 900218, 900348, 'EARN', 2250, '2026-10-06 17:29:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900449, 900219, 900349, 'EARN', 325, '2026-10-07 18:36:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900450, 900220, 900350, 'EARN', 600, '2026-10-08 19:43:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900451, 900221, 900351, 'USE', 500, '2026-10-09 10:50:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900452, 900222, 900352, 'EARN', 1250, '2026-10-10 11:57:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900453, 900223, 900353, 'EARN', 1700, '2026-10-11 12:04:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900454, 900224, 900354, 'EARN', 2250, '2026-10-12 13:11:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900455, 900225, 900355, 'EARN', 325, '2026-10-13 14:18:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900456, 900226, 900356, 'USE', 500, '2026-10-14 15:25:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900457, 900227, 900357, 'EARN', 900, '2026-10-15 16:32:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900458, 900228, 900358, 'EARN', 1250, '2026-10-16 17:39:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900459, 900229, 900359, 'EARN', 1700, '2026-10-17 18:46:00');
INSERT IGNORE INTO `point_history` (`id`, `customer_id`, `visit_id`, `type`, `point`, `created_at`) VALUES (900460, 900230, 900360, 'EARN', 2250, '2026-10-18 19:53:00');

-- coupon: 20 rows
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900501, 900201, '웰컴 할인', 2000, '2027-03-31', '2026-10-25 12:00:00');
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900502, 900202, '단골 감사 쿠폰', 3000, '2027-03-31', NULL);
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900503, 900203, '웰컴 할인', 2000, '2027-03-31', NULL);
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900504, 900204, '단골 감사 쿠폰', 3000, '2027-03-31', NULL);
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900505, 900205, '웰컴 할인', 2000, '2027-03-31', '2026-10-25 12:00:00');
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900506, 900206, '단골 감사 쿠폰', 3000, '2027-03-31', NULL);
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900507, 900207, '웰컴 할인', 2000, '2027-03-31', NULL);
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900508, 900208, '단골 감사 쿠폰', 3000, '2027-03-31', NULL);
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900509, 900209, '웰컴 할인', 2000, '2027-03-31', '2026-10-25 12:00:00');
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900510, 900210, '단골 감사 쿠폰', 3000, '2027-03-31', NULL);
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900511, 900211, '웰컴 할인', 2000, '2027-03-31', NULL);
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900512, 900212, '단골 감사 쿠폰', 3000, '2027-03-31', NULL);
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900513, 900213, '웰컴 할인', 2000, '2027-03-31', '2026-10-25 12:00:00');
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900514, 900214, '단골 감사 쿠폰', 3000, '2027-03-31', NULL);
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900515, 900215, '웰컴 할인', 2000, '2027-03-31', NULL);
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900516, 900216, '단골 감사 쿠폰', 3000, '2027-03-31', NULL);
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900517, 900217, '웰컴 할인', 2000, '2027-03-31', '2026-10-25 12:00:00');
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900518, 900218, '단골 감사 쿠폰', 3000, '2027-03-31', NULL);
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900519, 900219, '웰컴 할인', 2000, '2027-03-31', NULL);
INSERT IGNORE INTO `coupon` (`id`, `customer_id`, `title`, `discount`, `expires_at`, `used_at`) VALUES (900520, 900220, '단골 감사 쿠폰', 3000, '2027-03-31', NULL);

-- image: 5 rows
INSERT IGNORE INTO `image` (`id`, `original_file_name`, `stored_file_name`, `file_path`, `file_size`, `content_type`, `created_at`) VALUES (900601, 'demo_1.jpg', 'demo_1_placeholder.jpg', '/uploads/demo_1_placeholder.jpg', 1024, 'image/jpeg', '2026-10-20 10:00:00');
INSERT IGNORE INTO `image` (`id`, `original_file_name`, `stored_file_name`, `file_path`, `file_size`, `content_type`, `created_at`) VALUES (900602, 'demo_2.jpg', 'demo_2_placeholder.jpg', '/uploads/demo_2_placeholder.jpg', 2048, 'image/jpeg', '2026-10-20 10:00:00');
INSERT IGNORE INTO `image` (`id`, `original_file_name`, `stored_file_name`, `file_path`, `file_size`, `content_type`, `created_at`) VALUES (900603, 'demo_3.jpg', 'demo_3_placeholder.jpg', '/uploads/demo_3_placeholder.jpg', 3072, 'image/jpeg', '2026-10-20 10:00:00');
INSERT IGNORE INTO `image` (`id`, `original_file_name`, `stored_file_name`, `file_path`, `file_size`, `content_type`, `created_at`) VALUES (900604, 'demo_4.jpg', 'demo_4_placeholder.jpg', '/uploads/demo_4_placeholder.jpg', 4096, 'image/jpeg', '2026-10-20 10:00:00');
INSERT IGNORE INTO `image` (`id`, `original_file_name`, `stored_file_name`, `file_path`, `file_size`, `content_type`, `created_at`) VALUES (900605, 'demo_5.jpg', 'demo_5_placeholder.jpg', '/uploads/demo_5_placeholder.jpg', 5120, 'image/jpeg', '2026-10-20 10:00:00');

COMMIT;
-- NOTE: Image rows contain metadata only; no actual image files are created.
