USE farmstay_db;
SET NAMES utf8mb4;

START TRANSACTION;

DELETE FROM booking_order_dining
WHERE order_id IN (
  SELECT id
  FROM (
    SELECT id
    FROM booking_order
    WHERE order_no IN (
      'FS202603110001',
      'FS202603110002',
      'FS202603110003',
      'FS202603110004',
      'FS202603110005',
      'FS202603110006'
    )
  ) AS t
);

DELETE FROM booking_order_activity
WHERE order_id IN (
  SELECT id
  FROM (
    SELECT id
    FROM booking_order
    WHERE order_no IN (
      'FS202603110001',
      'FS202603110002',
      'FS202603110003',
      'FS202603110004',
      'FS202603110005',
      'FS202603110006'
    )
  ) AS t
);

DELETE FROM review
WHERE order_id IN (
  SELECT id
  FROM (
    SELECT id
    FROM booking_order
    WHERE order_no IN (
      'FS202603110001',
      'FS202603110002',
      'FS202603110003',
      'FS202603110004',
      'FS202603110005',
      'FS202603110006'
    )
  ) AS t
);

DELETE FROM booking_order
WHERE order_no IN (
  'FS202603110001',
  'FS202603110002',
  'FS202603110003',
  'FS202603110004',
  'FS202603110005',
  'FS202603110006'
);

DELETE FROM coupon
WHERE code IN ('SPRING88', 'BAMBOO120', 'STAR60', 'FAMILY150');

DELETE FROM farmstay_activity
WHERE name IN (
  '竹林采茶体验',
  '山野篝火晚会',
  '星空露营课堂',
  '稻田捉鱼比赛',
  '果园采摘半日游',
  '亲子手作草木染',
  '湖畔桨板体验',
  '农家年糕制作课'
);

DELETE FROM farmstay_dining
WHERE name IN (
  '柴火土鸡汤',
  '竹笋腊肉煲',
  '山泉水蒸鱼',
  '农家红烧肉',
  '时蔬菌菇锅',
  '桂花糖藕',
  '湖鲜双拼套餐',
  '石磨豆花宴'
);

DELETE FROM room_type
WHERE name IN (
  '竹景大床房',
  '亲子庭院房',
  '星空复式套房',
  '田园双床房',
  '湖景阳台房',
  '榻榻米家庭房',
  '果园木屋',
  '稻香套房'
);

DELETE FROM farmstay
WHERE name IN (
  '云栖竹舍',
  '稻香小院',
  '星湖慢居',
  '桃源果墅'
);

DELETE FROM user_account
WHERE username IN (
  '经营者周山',
  '经营者林秋',
  '游客张敏',
  '游客李娜',
  '游客王磊',
  '游客陈晨'
);

INSERT INTO user_account (username, password, salt, display_name, user_type, status)
VALUES
  ('经营者周山', '83fcf9dba46dabf4d281d3e181fd9218839e46d362075efdfa844b52bafdfbb6', 'def', '周山', 'operator', 'ACTIVE'),
  ('经营者林秋', '83fcf9dba46dabf4d281d3e181fd9218839e46d362075efdfa844b52bafdfbb6', 'ghi', '林秋', 'operator', 'ACTIVE'),
  ('游客张敏', '292bcbc41bb078cf5bd258db60b63a4b337c8c954409442cfad7148bc6428fee', 'abc', '张敏', 'visitor', 'ACTIVE'),
  ('游客李娜', '292bcbc41bb078cf5bd258db60b63a4b337c8c954409442cfad7148bc6428fee', 'abd', '李娜', 'visitor', 'ACTIVE'),
  ('游客王磊', '292bcbc41bb078cf5bd258db60b63a4b337c8c954409442cfad7148bc6428fee', 'abe', '王磊', 'visitor', 'ACTIVE'),
  ('游客陈晨', '292bcbc41bb078cf5bd258db60b63a4b337c8c954409442cfad7148bc6428fee', 'abf', '陈晨', 'visitor', 'ACTIVE');

SET @operator_zhoushan := (SELECT id FROM user_account WHERE username = '经营者周山' AND user_type = 'operator' LIMIT 1);
SET @operator_linqiu := (SELECT id FROM user_account WHERE username = '经营者林秋' AND user_type = 'operator' LIMIT 1);
SET @visitor_zhangmin := (SELECT id FROM user_account WHERE username = '游客张敏' AND user_type = 'visitor' LIMIT 1);
SET @visitor_lina := (SELECT id FROM user_account WHERE username = '游客李娜' AND user_type = 'visitor' LIMIT 1);
SET @visitor_wanglei := (SELECT id FROM user_account WHERE username = '游客王磊' AND user_type = 'visitor' LIMIT 1);
SET @visitor_chenchen := (SELECT id FROM user_account WHERE username = '游客陈晨' AND user_type = 'visitor' LIMIT 1);

INSERT INTO farmstay (
  owner_id, name, city, address, description, price_range, price_level,
  average_rating, cover_image, contact_phone, tags, status
)
VALUES
  (@operator_zhoushan, '云栖竹舍', '湖州安吉', '安吉县灵峰街道竹海路18号', '主打竹海观景与静养度假，适合情侣出游、亲子周末和企业小团建。', '￥428-￥988', 'premium', 4.50, 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', '0572-5011888', '竹海,下午茶,亲子,观景', 'PUBLISHED'),
  (@operator_zhoushan, '稻香小院', '杭州桐庐', '桐庐县富春江镇稻香村6号', '围绕稻田生活打造的乡村民宿，提供农事体验、土灶晚餐和亲子活动。', '￥368-￥768', 'standard', 4.50, 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80', '0571-6466889', '稻田,土灶,亲子,农事', 'PUBLISHED'),
  (@operator_linqiu, '星湖慢居', '绍兴嵊州', '嵊州市星湖景区东岸9号', '湖边慢生活主题民宿，清晨可看湖雾，夜晚适合围炉、赏星和轻运动。', '￥458-￥1088', 'premium', 5.00, 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', '0575-8333991', '湖景,露台,围炉,赏星', 'PUBLISHED'),
  (@operator_linqiu, '桃源果墅', '宁波宁海', '宁海县桑洲镇桃源果园88号', '果园里的木屋与庭院组合，适合家庭度假、采摘和乡村手作课程。', '￥398-￥898', 'standard', 4.00, 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=1200&q=80', '0574-6555882', '果园,木屋,采摘,家庭', 'PUBLISHED');

SET @farm_yunqi := (SELECT id FROM farmstay WHERE name = '云栖竹舍' LIMIT 1);
SET @farm_daoxiang := (SELECT id FROM farmstay WHERE name = '稻香小院' LIMIT 1);
SET @farm_xinghu := (SELECT id FROM farmstay WHERE name = '星湖慢居' LIMIT 1);
SET @farm_taoyuan := (SELECT id FROM farmstay WHERE name = '桃源果墅' LIMIT 1);

INSERT INTO room_type (farm_stay_id, name, description, bed_type, max_guests, price, stock, tags, status)
VALUES
  (@farm_yunqi, '竹景大床房', '落地窗面向竹海，含双人早餐与欢迎水果。', '1.8米大床', 2, 488.00, 6, '竹海景观,双早,安静', 'ACTIVE'),
  (@farm_yunqi, '亲子庭院房', '带独立小院和儿童绘本角，适合一家三口或四口。', '1.5米双床', 4, 688.00, 4, '亲子,庭院,儿童用品', 'ACTIVE'),
  (@farm_daoxiang, '稻香套房', '面朝稻田的套房，带茶桌和休闲榻。', '2.0米大床', 2, 568.00, 5, '稻田景观,茶桌,舒适', 'ACTIVE'),
  (@farm_daoxiang, '田园双床房', '适合好友同行，窗外就是菜园和田埂。', '1.2米双床', 2, 428.00, 7, '双床,田园,通风', 'ACTIVE'),
  (@farm_xinghu, '湖景阳台房', '带观景阳台和躺椅，清晨可看湖面薄雾。', '1.8米大床', 2, 598.00, 5, '湖景,阳台,度假', 'ACTIVE'),
  (@farm_xinghu, '星空复式套房', '上下两层设计，顶层天窗可观星。', '1.8米大床+榻榻米', 4, 988.00, 3, '复式,观星,家庭', 'ACTIVE'),
  (@farm_taoyuan, '果园木屋', '独立木屋带露台，步行即可进入果园。', '1.8米大床', 2, 468.00, 4, '木屋,果园,露台', 'ACTIVE'),
  (@farm_taoyuan, '榻榻米家庭房', '宽敞通铺设计，适合亲子家庭长住。', '榻榻米', 4, 628.00, 4, '家庭,榻榻米,宽敞', 'ACTIVE');

SET @room_yunqi_bed := (SELECT id FROM room_type WHERE farm_stay_id = @farm_yunqi AND name = '竹景大床房' LIMIT 1);
SET @room_yunqi_family := (SELECT id FROM room_type WHERE farm_stay_id = @farm_yunqi AND name = '亲子庭院房' LIMIT 1);
SET @room_daoxiang_suite := (SELECT id FROM room_type WHERE farm_stay_id = @farm_daoxiang AND name = '稻香套房' LIMIT 1);
SET @room_daoxiang_twin := (SELECT id FROM room_type WHERE farm_stay_id = @farm_daoxiang AND name = '田园双床房' LIMIT 1);
SET @room_xinghu_lake := (SELECT id FROM room_type WHERE farm_stay_id = @farm_xinghu AND name = '湖景阳台房' LIMIT 1);
SET @room_xinghu_star := (SELECT id FROM room_type WHERE farm_stay_id = @farm_xinghu AND name = '星空复式套房' LIMIT 1);
SET @room_taoyuan_wood := (SELECT id FROM room_type WHERE farm_stay_id = @farm_taoyuan AND name = '果园木屋' LIMIT 1);
SET @room_taoyuan_family := (SELECT id FROM room_type WHERE farm_stay_id = @farm_taoyuan AND name = '榻榻米家庭房' LIMIT 1);

INSERT INTO farmstay_dining (farm_stay_id, name, description, price, cover_image, tags, status)
VALUES
  (@farm_yunqi, '柴火土鸡汤', '选用散养土鸡，小火慢炖四小时，适合晚餐共享。', 128.00, 'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1200&q=80', '招牌,热汤,晚餐', 'ACTIVE'),
  (@farm_yunqi, '竹笋腊肉煲', '本地春笋搭配腊肉，口味鲜香，下饭很稳。', 88.00, 'https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=1200&q=80', '竹笋,本地菜,家常', 'ACTIVE'),
  (@farm_daoxiang, '农家红烧肉', '土灶炖煮，肥瘦相间，配米饭很受欢迎。', 78.00, 'https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=1200&q=80', '土灶,招牌,下饭', 'ACTIVE'),
  (@farm_daoxiang, '石磨豆花宴', '包含咸豆花、时蔬和小菜，适合多人分享。', 68.00, 'https://images.unsplash.com/photo-1547592166-23ac45744acd?auto=format&fit=crop&w=1200&q=80', '豆花,轻食,特色', 'ACTIVE'),
  (@farm_xinghu, '湖鲜双拼套餐', '清蒸白鱼配椒盐小河虾，主打湖鲜新鲜。', 158.00, 'https://images.unsplash.com/photo-1559847844-5315695dadae?auto=format&fit=crop&w=1200&q=80', '湖鲜,套餐,招牌', 'ACTIVE'),
  (@farm_xinghu, '桂花糖藕', '本地糯米藕，甜度适中，适合下午茶。', 36.00, 'https://images.unsplash.com/photo-1516684732162-798a0062be99?auto=format&fit=crop&w=1200&q=80', '甜品,下午茶,本地味', 'ACTIVE'),
  (@farm_taoyuan, '时蔬菌菇锅', '果园自种时蔬搭配菌菇，清爽暖胃。', 96.00, 'https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?auto=format&fit=crop&w=1200&q=80', '菌菇,时蔬,清淡', 'ACTIVE'),
  (@farm_taoyuan, '山泉水蒸鱼', '用山泉水和本地河鱼清蒸，突出鲜味。', 118.00, 'https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?auto=format&fit=crop&w=1200&q=80', '蒸鱼,鲜味,本地食材', 'ACTIVE');

INSERT INTO farmstay_activity (farm_stay_id, name, description, schedule, capacity, price, cover_image, tags, status)
VALUES
  (@farm_yunqi, '竹林采茶体验', '由本地茶农带领进竹林茶地，体验采摘与手工杀青。', '每日 09:30-11:30', 12, 68.00, 'https://images.unsplash.com/photo-1464226184884-fa280b87c399?auto=format&fit=crop&w=1200&q=80', '采茶,体验,亲近自然', 'ACTIVE'),
  (@farm_yunqi, '山野篝火晚会', '晚上围炉聊天、烤棉花糖和民谣互动。', '每周五六 19:30-21:00', 20, 48.00, 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80', '篝火,夜间活动,轻社交', 'ACTIVE'),
  (@farm_daoxiang, '稻田捉鱼比赛', '适合亲子参加，活动结束后可现场加工。', '周末 15:00-16:30', 16, 58.00, 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1200&q=80', '亲子,农趣,互动', 'ACTIVE'),
  (@farm_daoxiang, '农家年糕制作课', '体验打年糕与包馅，适合家庭游客。', '每日 14:00-15:30', 10, 42.00, 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1200&q=80', '手作,年糕,家庭', 'ACTIVE'),
  (@farm_xinghu, '湖畔桨板体验', '专业教练带领的低强度水上活动，新手也可参加。', '每日 16:00-17:30', 8, 128.00, 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80', '水上运动,湖景,教练带领', 'ACTIVE'),
  (@farm_xinghu, '星空露营课堂', '讲解观星基础知识，附望远镜体验。', '晴天 20:00-21:30', 15, 88.00, 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80', '观星,夜间活动,露营', 'ACTIVE'),
  (@farm_taoyuan, '果园采摘半日游', '按照季节开放草莓、枇杷或桃子采摘。', '每日 10:00-12:00', 18, 66.00, 'https://images.unsplash.com/photo-1464226184884-fa280b87c399?auto=format&fit=crop&w=1200&q=80', '采摘,果园,亲子', 'ACTIVE'),
  (@farm_taoyuan, '亲子手作草木染', '使用天然植物染料完成围巾或布袋染色。', '周末 13:30-15:00', 12, 52.00, 'https://images.unsplash.com/photo-1495546968767-f0573cca821e?auto=format&fit=crop&w=1200&q=80', '手作,草木染,亲子', 'ACTIVE');

INSERT INTO coupon (
  code, title, description, discount_amount, minimum_spend,
  valid_from, valid_to, farm_stay_id, total_count, used_count, status
)
VALUES
  ('SPRING88', '春游满减券', '全平台通用，订单满 500 元立减 88 元。', 88.00, 500.00, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), NULL, 300, 2, 'ACTIVE'),
  ('BAMBOO120', '竹舍双晚专享券', '云栖竹舍专用，双晚及以上订单满 800 元立减 120 元。', 120.00, 800.00, NOW(), DATE_ADD(NOW(), INTERVAL 45 DAY), @farm_yunqi, 80, 1, 'ACTIVE'),
  ('STAR60', '星湖下午茶券', '星湖慢居专用，订单满 600 元立减 60 元。', 60.00, 600.00, NOW(), DATE_ADD(NOW(), INTERVAL 45 DAY), @farm_xinghu, 100, 1, 'ACTIVE'),
  ('FAMILY150', '家庭出游券', '桃源果墅家庭房专享，订单满 1000 元立减 150 元。', 150.00, 1000.00, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), @farm_taoyuan, 60, 0, 'ACTIVE');

INSERT INTO booking_order (
  order_no, visitor_id, farm_stay_id, room_type_id, check_in_date, check_out_date,
  guests, dining_amount, activity_amount, total_amount, status, payment_channel, contact_name, contact_phone, coupon_code, remarks
)
VALUES
  ('FS202603110001', @visitor_zhangmin, @farm_yunqi, @room_yunqi_bed, DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 18 DAY), 2, 128.00, 48.00, 856.00, 'COMPLETED', 'wechat', '张敏', '13800010001', 'SPRING88', '希望安排安静朝竹海一侧房间'),
  ('FS202603110002', @visitor_lina, @farm_xinghu, @room_xinghu_lake, DATE_SUB(CURDATE(), INTERVAL 12 DAY), DATE_SUB(CURDATE(), INTERVAL 10 DAY), 2, 158.00, 128.00, 1136.00, 'COMPLETED', 'alipay', '李娜', '13800010002', 'STAR60', '想体验湖畔桨板活动'),
  ('FS202603110003', @visitor_wanglei, @farm_daoxiang, @room_daoxiang_suite, DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 7 DAY), 2, 78.00, 58.00, 1048.00, 'PAID', 'wechat', '王磊', '13800010003', 'SPRING88', '需要两份早餐，预计下午三点后到店'),
  ('FS202603110004', @visitor_chenchen, @farm_taoyuan, @room_taoyuan_family, DATE_ADD(CURDATE(), INTERVAL 8 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY), 4, 96.00, 52.00, 1106.00, 'CREATED', 'UNPAID', '陈晨', '13800010004', 'FAMILY150', '带两个孩子入住，希望靠近活动区'),
  ('FS202603110005', @visitor_zhangmin, @farm_daoxiang, @room_daoxiang_twin, DATE_SUB(CURDATE(), INTERVAL 35 DAY), DATE_SUB(CURDATE(), INTERVAL 33 DAY), 2, 0.00, 0.00, 856.00, 'CANCELLED', 'UNPAID', '张敏', '13800010001', NULL, '行程调整，后续可能改期再来'),
  ('FS202603110006', @visitor_lina, @farm_yunqi, @room_yunqi_family, DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_SUB(CURDATE(), INTERVAL 4 DAY), 3, 88.00, 0.00, 1256.00, 'COMPLETED', 'wechat', '李娜', '13800010002', 'BAMBOO120', '亲子出行，需要加一套儿童洗漱用品');

SET @order_1 := (SELECT id FROM booking_order WHERE order_no = 'FS202603110001' LIMIT 1);
SET @order_2 := (SELECT id FROM booking_order WHERE order_no = 'FS202603110002' LIMIT 1);
SET @order_3 := (SELECT id FROM booking_order WHERE order_no = 'FS202603110003' LIMIT 1);
SET @order_4 := (SELECT id FROM booking_order WHERE order_no = 'FS202603110004' LIMIT 1);
SET @order_5 := (SELECT id FROM booking_order WHERE order_no = 'FS202603110005' LIMIT 1);
SET @order_6 := (SELECT id FROM booking_order WHERE order_no = 'FS202603110006' LIMIT 1);

SET @dining_yunqi_chicken := (SELECT id FROM farmstay_dining WHERE farm_stay_id = @farm_yunqi AND name = '柴火土鸡汤' LIMIT 1);
SET @dining_yunqi_bamboo := (SELECT id FROM farmstay_dining WHERE farm_stay_id = @farm_yunqi AND name = '竹笋腊肉煲' LIMIT 1);
SET @dining_xinghu_combo := (SELECT id FROM farmstay_dining WHERE farm_stay_id = @farm_xinghu AND name = '湖鲜双拼套餐' LIMIT 1);
SET @dining_daoxiang_meat := (SELECT id FROM farmstay_dining WHERE farm_stay_id = @farm_daoxiang AND name = '农家红烧肉' LIMIT 1);
SET @dining_taoyuan_hotpot := (SELECT id FROM farmstay_dining WHERE farm_stay_id = @farm_taoyuan AND name = '时蔬菌菇锅' LIMIT 1);

SET @activity_yunqi_fire := (SELECT id FROM farmstay_activity WHERE farm_stay_id = @farm_yunqi AND name = '山野篝火晚会' LIMIT 1);
SET @activity_xinghu_board := (SELECT id FROM farmstay_activity WHERE farm_stay_id = @farm_xinghu AND name = '湖畔桨板体验' LIMIT 1);
SET @activity_daoxiang_fish := (SELECT id FROM farmstay_activity WHERE farm_stay_id = @farm_daoxiang AND name = '稻田捉鱼比赛' LIMIT 1);
SET @activity_taoyuan_dye := (SELECT id FROM farmstay_activity WHERE farm_stay_id = @farm_taoyuan AND name = '亲子手作草木染' LIMIT 1);

INSERT INTO booking_order_dining (order_id, dining_item_id, item_name, price, quantity, created_at)
VALUES
  (@order_1, @dining_yunqi_chicken, '柴火土鸡汤', 128.00, 1, DATE_SUB(NOW(), INTERVAL 20 DAY)),
  (@order_2, @dining_xinghu_combo, '湖鲜双拼套餐', 158.00, 1, DATE_SUB(NOW(), INTERVAL 12 DAY)),
  (@order_3, @dining_daoxiang_meat, '农家红烧肉', 78.00, 1, NOW()),
  (@order_4, @dining_taoyuan_hotpot, '时蔬菌菇锅', 96.00, 1, NOW()),
  (@order_6, @dining_yunqi_bamboo, '竹笋腊肉煲', 88.00, 1, DATE_SUB(NOW(), INTERVAL 6 DAY));

INSERT INTO booking_order_activity (order_id, activity_item_id, item_name, price, quantity, created_at)
VALUES
  (@order_1, @activity_yunqi_fire, '山野篝火晚会', 48.00, 1, DATE_SUB(NOW(), INTERVAL 20 DAY)),
  (@order_2, @activity_xinghu_board, '湖畔桨板体验', 128.00, 1, DATE_SUB(NOW(), INTERVAL 12 DAY)),
  (@order_3, @activity_daoxiang_fish, '稻田捉鱼比赛', 58.00, 1, NOW()),
  (@order_4, @activity_taoyuan_dye, '亲子手作草木染', 52.00, 1, NOW());

INSERT INTO review (order_id, farm_stay_id, visitor_id, rating, content, created_at)
VALUES
  (@order_1, @farm_yunqi, @visitor_zhangmin, 5, '竹林景色很舒服，房间干净安静，晚上篝火氛围也很好，适合周末放松。', DATE_SUB(NOW(), INTERVAL 17 DAY)),
  (@order_2, @farm_xinghu, @visitor_lina, 5, '湖景阳台房视野很棒，早餐新鲜，工作人员响应很快，整体体验超出预期。', DATE_SUB(NOW(), INTERVAL 9 DAY)),
  (@order_6, @farm_yunqi, @visitor_lina, 4, '亲子庭院房空间够大，孩子很喜欢小院，不过周末用餐高峰上菜稍慢，其他都不错。', DATE_SUB(NOW(), INTERVAL 3 DAY)),
  (@order_5, @farm_daoxiang, @visitor_zhangmin, 4, '虽然最后取消了行程，但客服沟通及时，改期说明清楚，后面还会考虑再来入住。', DATE_SUB(NOW(), INTERVAL 32 DAY));

COMMIT;
