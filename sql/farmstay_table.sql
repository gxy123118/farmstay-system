-- farmstay basic data table (operators manage their own stays, visitors may only read)
CREATE TABLE IF NOT EXISTS farmstay (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'farmstay primary key',
  owner_id BIGINT NOT NULL COMMENT 'owner user_account.id reference',
  name VARCHAR(128) NOT NULL COMMENT 'farmstay name',
  city VARCHAR(64) NOT NULL COMMENT 'city name',
  address VARCHAR(255) DEFAULT NULL COMMENT 'detailed address',
  description TEXT COMMENT 'description text',
  price_range VARCHAR(64) DEFAULT NULL COMMENT 'display price range',
  price_level VARCHAR(32) DEFAULT NULL COMMENT 'price level (standard/premium)',
  average_rating DECIMAL(3,2) DEFAULT NULL COMMENT 'average rating score',
  cover_image VARCHAR(512) DEFAULT NULL COMMENT 'cover image url',
  contact_phone VARCHAR(32) DEFAULT NULL COMMENT 'contact phone',
  tags VARCHAR(256) DEFAULT NULL COMMENT 'comma separated feature tags',
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'status: PUBLISHED / DRAFT / ARCHIVED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_owner (owner_id),
  CONSTRAINT fk_farmstay_owner FOREIGN KEY (owner_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='farmstay table';
