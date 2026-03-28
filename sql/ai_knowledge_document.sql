USE farmstay_db;

CREATE TABLE IF NOT EXISTS ai_knowledge_document (
  id BIGINT NOT NULL AUTO_INCREMENT,
  knowledge_code VARCHAR(64) NOT NULL COMMENT '知识编码',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  content TEXT NOT NULL COMMENT '知识正文',
  summary VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  keywords VARCHAR(500) DEFAULT NULL COMMENT '关键词，逗号分隔',
  scope VARCHAR(32) NOT NULL DEFAULT 'public' COMMENT 'public/operator_only',
  farm_stay_id BIGINT DEFAULT NULL COMMENT '为空表示平台级知识',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  created_by BIGINT DEFAULT NULL COMMENT '创建人',
  updated_by BIGINT DEFAULT NULL COMMENT '更新人',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_knowledge_code (knowledge_code),
  KEY idx_ai_knowledge_scope_status (scope, status),
  KEY idx_ai_knowledge_farm_status (farm_stay_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI知识片段表';

INSERT INTO ai_knowledge_document
  (knowledge_code, title, content, summary, keywords, scope, farm_stay_id, status)
VALUES
  (
    'refund_rule_v1',
    '退款规则',
    '平台通用规则：未支付订单可直接取消，已支付订单可在订单详情页申请退款，结果以订单状态更新为准。',
    '退款与取消的基础规则',
    '退款,退订,取消,订单',
    'public',
    NULL,
    'ACTIVE'
  ),
  (
    'payment_rule_v1',
    '支付规则',
    '平台通用规则：订单支付成功后状态会更新为PAID，若支付失败可重新发起支付。',
    '支付状态与重试规则',
    '支付,付款,订单,PAID',
    'public',
    NULL,
    'ACTIVE'
  );
