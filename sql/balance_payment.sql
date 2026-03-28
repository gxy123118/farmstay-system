ALTER TABLE user_account
ADD COLUMN balance DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '平台余额';

CREATE TABLE user_balance_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_no VARCHAR(64) NOT NULL COMMENT '流水号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    change_type VARCHAR(32) NOT NULL COMMENT 'RECHARGE/PAY_ORDER/REFUND',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务单号，如充值单号/订单号/退款单号',
    amount DECIMAL(10,2) NOT NULL COMMENT '变动金额，正数为入账，负数为扣减',
    balance_before DECIMAL(10,2) NOT NULL,
    balance_after DECIMAL(10,2) NOT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_flow_no (flow_no),
    KEY idx_user_created (user_id, created_at DESC),
    KEY idx_biz_no (biz_no)
) COMMENT='用户余额流水';

CREATE TABLE recharge_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recharge_no VARCHAR(64) NOT NULL COMMENT '充值单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '充值金额',
    pay_method VARCHAR(32) NOT NULL COMMENT '固定为 ALIPAY',
    status VARCHAR(32) NOT NULL COMMENT 'PENDING/SUCCESS/FAILED/CLOSED',
    third_trade_no VARCHAR(128) DEFAULT NULL COMMENT '支付宝交易号',
    subject VARCHAR(128) DEFAULT NULL COMMENT '订单标题',
    notify_content TEXT DEFAULT NULL COMMENT '回调报文',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_recharge_no (recharge_no),
    KEY idx_user_status (user_id, status),
    KEY idx_third_trade_no (third_trade_no)
) COMMENT='充值单';

CREATE TABLE refund_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    refund_no VARCHAR(64) NOT NULL COMMENT '退款单号',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    refund_channel VARCHAR(32) NOT NULL COMMENT '固定为 BALANCE',
    status VARCHAR(32) NOT NULL COMMENT 'PENDING/SUCCESS/FAILED',
    reason VARCHAR(255) DEFAULT NULL COMMENT '退款原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refunded_at DATETIME DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_refund_no (refund_no),
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_order_id (order_id),
    KEY idx_user_status (user_id, status)
) COMMENT='退款单';
