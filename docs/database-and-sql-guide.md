# 数据库与 SQL 说明

## 1. 主要 SQL 文件
- [admin_init.sql](E:\code\farmstay-system\sql\admin_init.sql)
- [ai_chat_session.sql](E:\code\farmstay-system\sql\ai_chat_session.sql)
- [ai_knowledge_document.sql](E:\code\farmstay-system\sql\ai_knowledge_document.sql)
- [ai_knowledge_document_drop_unused_columns.sql](E:\code\farmstay-system\sql\ai_knowledge_document_drop_unused_columns.sql)
- [balance_payment.sql](E:\code\farmstay-system\sql\balance_payment.sql)
- [operator_insight_report.sql](E:\code\farmstay-system\sql\operator_insight_report.sql)

## 2. 用途说明

### 2.1 管理员初始化
`admin_init.sql`
- 初始化内置管理员账号

### 2.2 AI 聊天
`ai_chat_session.sql`
- 聊天会话
- 聊天消息

### 2.3 AI 知识库
`ai_knowledge_document.sql`
- AI 知识片段主表
- 当前已移除 `doc_type` 和 `priority`

`ai_knowledge_document_drop_unused_columns.sql`
- 用于把旧库迁移为当前字段结构

### 2.4 余额支付
`balance_payment.sql`
- `user_account.balance`
- `user_balance_flow`
- `recharge_order`
- `refund_order`

### 2.5 经营建议
`operator_insight_report.sql`
- AI 经营建议报告持久化表

## 3. 执行建议
新环境优先执行：
1. 基础表结构
2. `ai_chat_session.sql`
3. `ai_knowledge_document.sql`
4. `balance_payment.sql`
5. `operator_insight_report.sql`
6. `admin_init.sql`

旧环境迁移时：
- 额外执行 [ai_knowledge_document_drop_unused_columns.sql](E:\code\farmstay-system\sql\ai_knowledge_document_drop_unused_columns.sql)
