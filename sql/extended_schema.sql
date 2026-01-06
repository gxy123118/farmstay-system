USE farmstay_db;

-- 房型表
CREATE TABLE IF NOT EXISTS room_type (
  id BIGINT NOT NULL AUTO_INCREMENT,
  farm_stay_id BIGINT NOT NULL COMMENT '所属农家乐',
  name VARCHAR(128) NOT NULL COMMENT '房型名称',
  description TEXT,
  bed_type VARCHAR(64),
  max_guests INT DEFAULT 2,
  price DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  tags VARCHAR(256),
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_room_farmstay (farm_stay_id),
  CONSTRAINT fk_room_farm FOREIGN KEY (farm_stay_id) REFERENCES farmstay (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='房型与库存';

-- 预订订单表
CREATE TABLE IF NOT EXISTS booking_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  visitor_id BIGINT NOT NULL COMMENT '游客ID',
  farm_stay_id BIGINT NOT NULL,
  room_type_id BIGINT NOT NULL,
  check_in_date DATE NOT NULL,
  check_out_date DATE NOT NULL,
  guests INT NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  payment_channel VARCHAR(32) DEFAULT 'UNPAID',
  contact_name VARCHAR(64),
  contact_phone VARCHAR(32),
  coupon_code VARCHAR(64),
  remarks VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_order_visitor (visitor_id),
  KEY idx_order_farmstay (farm_stay_id),
  CONSTRAINT fk_order_visitor FOREIGN KEY (visitor_id) REFERENCES user_account (id),
  CONSTRAINT fk_order_room FOREIGN KEY (room_type_id) REFERENCES room_type (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预订订单';

-- 评论表
CREATE TABLE IF NOT EXISTS review (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  farm_stay_id BIGINT NOT NULL,
  visitor_id BIGINT NOT NULL,
  rating INT NOT NULL,
  content TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_review_farm (farm_stay_id),
  UNIQUE KEY uk_review_order (order_id),
  CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES booking_order (id),
  CONSTRAINT fk_review_visitor FOREIGN KEY (visitor_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='游客评价';

-- 优惠券表
CREATE TABLE IF NOT EXISTS coupon (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  title VARCHAR(128) NOT NULL,
  description VARCHAR(255),
  discount_amount DECIMAL(10,2) NOT NULL,
  minimum_spend DECIMAL(10,2) NOT NULL,
  valid_from DATETIME NOT NULL,
  valid_to DATETIME NOT NULL,
  farm_stay_id BIGINT NULL COMMENT '为空表示全平台可用',
  total_count INT NOT NULL,
  used_count INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_coupon_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优惠券';
