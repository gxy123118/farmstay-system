USE farmstay_db;

CREATE TABLE IF NOT EXISTS operator_insight_report (
  id BIGINT NOT NULL AUTO_INCREMENT,
  report_id BIGINT NOT NULL COMMENT '业务报告ID',
  farm_stay_id BIGINT NOT NULL COMMENT '民宿ID',
  owner_id BIGINT NOT NULL COMMENT '经营者ID',
  period_days INT NOT NULL COMMENT '分析周期天数',
  generation_mode VARCHAR(32) NOT NULL COMMENT 'llm/fallback',
  model VARCHAR(64) DEFAULT NULL COMMENT '模型标识',
  review_count INT NOT NULL DEFAULT 0 COMMENT '评论数',
  average_rating DECIMAL(4,2) NOT NULL DEFAULT 0.00 COMMENT '平均评分',
  summary VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  report_json JSON NOT NULL COMMENT '完整报告JSON',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  generated_at DATETIME NOT NULL COMMENT '报告生成时间',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_operator_insight_report_id (report_id),
  KEY idx_operator_insight_farm_generated (farm_stay_id, generated_at DESC),
  KEY idx_operator_insight_owner_farm_deleted (owner_id, farm_stay_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='运营洞察报告';
