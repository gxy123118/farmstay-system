USE farmstay_db;

SET @has_dining_amount := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'farmstay_db'
    AND TABLE_NAME = 'booking_order'
    AND COLUMN_NAME = 'dining_amount'
);
SET @sql := IF(
  @has_dining_amount = 0,
  'ALTER TABLE booking_order ADD COLUMN dining_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER guests',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_activity_amount := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'farmstay_db'
    AND TABLE_NAME = 'booking_order'
    AND COLUMN_NAME = 'activity_amount'
);
SET @sql := IF(
  @has_activity_amount = 0,
  'ALTER TABLE booking_order ADD COLUMN activity_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER dining_amount',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS booking_order_dining (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  dining_item_id BIGINT NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_order_dining_order (order_id),
  KEY idx_order_dining_item (dining_item_id),
  CONSTRAINT fk_order_dining_order FOREIGN KEY (order_id) REFERENCES booking_order (id),
  CONSTRAINT fk_order_dining_item FOREIGN KEY (dining_item_id) REFERENCES farmstay_dining (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单餐饮明细';

CREATE TABLE IF NOT EXISTS booking_order_activity (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  activity_item_id BIGINT NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_order_activity_order (order_id),
  KEY idx_order_activity_item (activity_item_id),
  CONSTRAINT fk_order_activity_order FOREIGN KEY (order_id) REFERENCES booking_order (id),
  CONSTRAINT fk_order_activity_item FOREIGN KEY (activity_item_id) REFERENCES farmstay_activity (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单活动明细';
