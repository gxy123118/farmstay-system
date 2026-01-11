-- Additional service tables for dining and activities
CREATE TABLE IF NOT EXISTS farmstay_dining (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'dining item id',
  farm_stay_id BIGINT NOT NULL COMMENT 'farmstay id',
  name VARCHAR(128) NOT NULL COMMENT 'dining item name',
  description TEXT COMMENT 'description',
  price DECIMAL(10,2) NOT NULL COMMENT 'price',
  cover_image VARCHAR(512) DEFAULT NULL COMMENT 'cover image',
  tags VARCHAR(256) DEFAULT NULL COMMENT 'comma separated tags',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'status',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_farm_stay (farm_stay_id),
  CONSTRAINT fk_dining_farm_stay FOREIGN KEY (farm_stay_id) REFERENCES farmstay (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='farmstay dining items';

CREATE TABLE IF NOT EXISTS farmstay_activity (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'activity id',
  farm_stay_id BIGINT NOT NULL COMMENT 'farmstay id',
  name VARCHAR(128) NOT NULL COMMENT 'activity name',
  description TEXT COMMENT 'description',
  schedule VARCHAR(128) DEFAULT NULL COMMENT 'schedule',
  capacity INT DEFAULT NULL COMMENT 'capacity',
  price DECIMAL(10,2) NOT NULL COMMENT 'price',
  cover_image VARCHAR(512) DEFAULT NULL COMMENT 'cover image',
  tags VARCHAR(256) DEFAULT NULL COMMENT 'comma separated tags',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'status',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_farm_stay (farm_stay_id),
  CONSTRAINT fk_activity_farm_stay FOREIGN KEY (farm_stay_id) REFERENCES farmstay (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='farmstay activities';
