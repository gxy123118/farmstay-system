USE farmstay_db;

CREATE TABLE IF NOT EXISTS ai_chat_session (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  farm_stay_id BIGINT DEFAULT NULL COMMENT '会话关联民宿ID',
  scene VARCHAR(64) DEFAULT NULL COMMENT '会话场景',
  title VARCHAR(200) DEFAULT NULL COMMENT '会话标题，默认取首轮问题摘要',
  last_message_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后消息时间',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ai_chat_session_user (user_id, last_message_at),
  KEY idx_ai_chat_session_farm (farm_stay_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI聊天会话表';

CREATE TABLE IF NOT EXISTS ai_chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL COMMENT '会话ID',
  role VARCHAR(16) NOT NULL COMMENT 'user/assistant',
  content TEXT NOT NULL COMMENT '消息内容',
  citations_json TEXT DEFAULT NULL COMMENT '引用JSON',
  confidence DECIMAL(5,2) DEFAULT NULL COMMENT '置信度',
  refuse_reason VARCHAR(255) DEFAULT NULL COMMENT '拒答原因',
  fallback TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否兜底',
  useful TINYINT(1) DEFAULT NULL COMMENT '用户反馈是否有帮助',
  feedback_comment VARCHAR(500) DEFAULT NULL COMMENT '反馈备注',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ai_chat_message_session (session_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI聊天消息表';
