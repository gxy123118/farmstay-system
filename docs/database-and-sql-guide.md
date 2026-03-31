# 数据库与 SQL 说明

## 1. 主要 SQL 文件

- [admin_init.sql](E:\code\farmstay-system\sql\admin_init.sql)
- [ai_chat_session.sql](E:\code\farmstay-system\sql\ai_chat_session.sql)
- [ai_knowledge_document.sql](E:\code\farmstay-system\sql\ai_knowledge_document.sql)
- [balance_payment.sql](E:\code\farmstay-system\sql\balance_payment.sql)
- [withdraw_order.sql](E:\code\farmstay-system\sql\withdraw_order.sql)
- [operator_insight_report.sql](E:\code\farmstay-system\sql\operator_insight_report.sql)

## 2. 文件用途

### 2.1 管理员初始化

`admin_init.sql`

用途：

- 初始化内置管理员账号

### 2.2 AI 聊天

`ai_chat_session.sql`

用途：

- 聊天会话表
- 聊天消息表

### 2.3 AI 知识库

`ai_knowledge_document.sql`

用途：

- AI 知识片段主表

说明：

- 当前结构已移除 `docType` 和 `priority`

### 2.4 余额支付与充值退款

`balance_payment.sql`

用途：

- `user_account.balance`
- `user_balance_flow`
- `recharge_order`
- `refund_order`

### 2.5 经营者提现

`withdraw_order.sql`

用途：

- 经营者提现申请单
- 记录审核、拒绝、人工打款确认过程

核心字段：

- `withdraw_no`
- `user_id`
- `amount`
- `channel`
- `account_name`
- `account_no`
- `status`
- `review_remark`
- `transfer_no`
- `reviewed_at`
- `paid_at`

状态说明：

- `PENDING`
- `APPROVED`
- `SUCCESS`
- `REJECTED`

### 2.6 经营建议报告

`operator_insight_report.sql`

用途：

- 经营者运营建议报告持久化

## 3. 新环境执行建议

建议按以下顺序执行：

1. 基础表结构
2. `ai_chat_session.sql`
3. `ai_knowledge_document.sql`
4. `balance_payment.sql`
5. `withdraw_order.sql`
6. `operator_insight_report.sql`
7. `admin_init.sql`

## 4. 与交易闭环直接相关的表

如果只关注交易闭环，至少需要保证以下表已存在：

- `booking_order`
- `user_account`
- `user_balance_flow`
- `recharge_order`
- `refund_order`
- `withdraw_order`

## 5. 与提现业务直接相关的 SQL

本轮提现功能上线前，必须执行：

- [withdraw_order.sql](E:\code\farmstay-system\sql\withdraw_order.sql)

如果没有执行该文件，经营者提现申请和管理员审核接口将无法正常工作。
