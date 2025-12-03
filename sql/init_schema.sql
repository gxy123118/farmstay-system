-- 初始化数据库和登录表（对应后端 login 模块）
CREATE DATABASE IF NOT EXISTS farmstay_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE farmstay_db;

DROP TABLE IF EXISTS user_account;
CREATE TABLE user_account (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键',
  username VARCHAR(64) NOT NULL COMMENT '登录账号',
  password CHAR(64) NOT NULL COMMENT 'sha256(明文+salt)',
  salt VARCHAR(32) DEFAULT '' COMMENT '加密盐',
  display_name VARCHAR(64) DEFAULT NULL COMMENT '显示名',
  user_type VARCHAR(16) NOT NULL COMMENT '角色，visitor 或 operator',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '账户状态',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_type_username (user_type, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='游客/经营者账户表';

-- 示例数据（密码采用 PasswordUtil.sha256Hex(raw+salt)）
INSERT INTO user_account (username, password, salt, display_name, user_type, status)
VALUES
  ('visitor01', '292bcbc41bb078cf5bd258db60b63a4b337c8c954409442cfad7148bc6428fee', 'abc', '刘游客', 'visitor', 'ACTIVE'),
  ('operator01', '83fcf9dba46dabf4d281d3e181fd9218839e46d362075efdfa844b52bafdfbb6', 'def', '张经营者', 'operator', 'ACTIVE');
