USE farmstay_db;

-- 房型示例数据（关联示例农家乐，假设 farmstay 表已有 ID 1 和 2）
INSERT INTO room_type (farm_stay_id, name, description, bed_type, max_guests, price, stock, tags, status)
VALUES
  (1, '山景大床房', '带阳台可观山景，含早餐', '大床1.8m', 2, 680.00, 6, '山景,早餐,阳台', 'ACTIVE'),
  (1, '亲子双床房', '适合亲子出行，赠儿童玩具包', '双床1.2m', 3, 520.00, 5, '亲子,玩具,阳光', 'ACTIVE'),
  (2, '庭院套房', '独立小院，含下午茶', '大床2.0m', 2, 980.00, 4, '庭院,下午茶,私密', 'ACTIVE'),
  (2, '河景Loft', '上下双层，落地窗看河景', '大床1.8m', 2, 860.00, 3, '河景,loft,观景', 'ACTIVE');

-- 优惠券示例（可按农家乐过滤，数量有限）
INSERT INTO coupon (code, title, description, discount_amount, minimum_spend, valid_from, valid_to, farm_stay_id, total_count, used_count, status)
VALUES
  ('WELCOME50', '新人立减50', '新人订单满300减50', 50.00, 300.00, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NULL, 200, 0, 'ACTIVE'),
  ('YARD80', '庭院专享80', '仅限庭院套房满600减80', 80.00, 600.00, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 2, 100, 0, 'ACTIVE');

-- 预订订单示例（假设游客用户 ID 为 1，经营者 ID 为 2，房型 ID 对应上方插入）
INSERT INTO booking_order (order_no, visitor_id, farm_stay_id, room_type_id, check_in_date, check_out_date, guests, total_amount, status, payment_channel, contact_name, contact_phone, coupon_code, remarks)
VALUES
  ('ORD20240101001', 1, 1, 1, DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 2, 1360.00, 'PAID', 'mock', '刘游客', '13800000001', 'WELCOME50', '需要高楼层'),
  ('ORD20240101002', 1, 2, 3, DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 9 DAY), 2, 1880.00, 'CREATED', 'UNPAID', '王体验', '13800000002', NULL, '晚到请保留');

