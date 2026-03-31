# 后端接口对接指南

本文档面向前端与联调人员，说明当前项目已开放的核心后端接口、状态口径和主要对接注意事项。统一响应格式为 `ApiResponse<T>`。

## 1. 认证与用户

### 1.1 登录

`POST /api/auth/login`

请求示例：

```json
{
  "username": "gxy666",
  "password": "123456",
  "userType": "visitor"
}
```

说明：

- `userType` 支持 `visitor`、`operator`、`admin`
- `admin` 可以登录，但不能通过前台注册创建

### 1.2 注册

`POST /api/auth/register`

说明：

- 前台注册仅开放 `visitor` 和 `operator`

### 1.3 当前用户信息

`GET /api/auth/me`

重点字段：

- `id`
- `username`
- `displayName`
- `userType`
- `status`
- `balance`

## 2. 游客订单、支付、退款与履约

### 2.1 创建订单

`POST /api/bookings`

说明：

- 创建成功后订单状态为 `CREATED`

### 2.2 余额支付

`POST /api/bookings/pay`

请求示例：

```json
{
  "orderId": 30,
  "channel": "BALANCE"
}
```

成功后：

- 订单状态 `CREATED -> PAID`
- 游客余额扣减
- 写入游客余额流水

余额不足时：

- 返回明确失败提示
- 前端应引导用户先去充值

### 2.3 游客退款

`POST /api/bookings/{orderId}/refund`

说明：

- 仅 `PAID` 且未履约完成的订单允许退款
- 退款成功后订单状态变为 `REFUNDED`
- 退款金额退回游客余额
- 一旦订单进入 `COMPLETED`，游客不能再退款

### 2.4 经营者核销完成

`POST /api/bookings/{orderId}/complete`

说明：

- 仅经营者可调用
- 仅允许核销本人店铺的 `PAID` 订单
- 核销成功后订单状态变为 `COMPLETED`
- 同时将订单金额结算到经营者余额
- 写入经营者余额流水，类型为 `ORDER_SETTLEMENT`

### 2.5 游客查看自己的订单

`GET /api/bookings/mine`

返回说明：

- 返回游客自己的订单详情列表
- 包含订单、民宿、房型、评价状态、餐饮和活动明细

## 3. 经营者订单与交易统计

### 3.1 单店订单列表

`GET /api/bookings?farmStayId={farmStayId}`

说明：

- 仅允许查看当前经营者本人名下店铺
- 适合单店详情页

### 3.2 经营者全部订单

`GET /api/bookings/operator/orders`

可选查询参数：

- `farmStayId`
- `status`，支持 `CREATED`、`PAID`、`COMPLETED`、`CANCELLED`、`REFUNDED`

返回说明：

- 返回当前经营者名下全部店铺订单
- 每条记录包含：
  - 订单号
  - 游客昵称与账号
  - 农家乐信息
  - 房型信息
  - 入住和离店日期
  - 订单金额
  - 支付渠道
  - 订单状态
  - 是否已评价
  - 联系人和联系电话
  - 餐饮与活动明细

### 3.3 经营者交易汇总

`GET /api/bookings/operator/summary`

可选查询参数：

- `farmStayId`

返回字段：

- `farmStayCount`
- `orderCount`
- `paidOrderCount`
- `refundedOrderCount`
- `grossTransactionAmount`
- `refundAmount`
- `netTransactionAmount`
- `refundRate`

退款率口径：

- 后端返回比例值
- 例如 `0.5` 表示 `50%`
- 前端展示时应乘以 `100` 再追加 `%`

## 4. 账户余额、充值与提现

### 4.1 查询余额

`GET /api/account/balance`

### 4.2 查询余额流水

`GET /api/account/balance/flows`

### 4.3 创建充值单

`POST /api/account/recharges`

请求示例：

```json
{
  "amount": 100
}
```

返回重点字段：

- `rechargeNo`
- `amount`
- `status`
- `payInfo`
- `qrCode`

说明：

- `qrCode` 是支付宝付款码内容字符串，不是图片
- 前端负责将 `qrCode` 渲染为二维码

### 4.4 查询充值单状态

`GET /api/account/recharges/{rechargeNo}`

说明：

- 如果本地状态仍为 `PENDING`，后端会主动调用支付宝查询作为补偿

### 4.5 支付宝异步回调

`POST /api/account/recharges/alipay/notify`

### 4.6 本地模拟充值

`POST /api/account/recharges/{rechargeNo}/mock-pay`

### 4.7 经营者发起提现

`POST /api/account/withdraws`

请求示例：

```json
{
  "amount": 300,
  "channel": "ALIPAY",
  "accountName": "张三",
  "accountNo": "13800138000",
  "remark": "本月结算提现"
}
```

说明：

- 仅经营者可调用
- 当前仅支持 `ALIPAY`
- `accountNo` 当前按支付宝收款手机号填写，必须是 11 位大陆手机号
- 发起申请时会立即扣减经营者余额
- 提现单初始状态为 `PENDING`

### 4.8 经营者查看自己的提现单

`GET /api/account/withdraws`

返回字段：

- `id`
- `withdrawNo`
- `amount`
- `channel`
- `accountName`
- `accountNo`
- `status`
- `remark`
- `reviewRemark`
- `transferNo`
- `createdAt`
- `reviewedAt`
- `paidAt`

## 5. 管理员提现审核接口

### 5.1 提现申请列表

`GET /api/admin/withdraws`

可选查询参数：

- `status`
- `page`
- `pageSize`

### 5.2 审核通过

`POST /api/admin/withdraws/{withdrawId}/approve`

请求体：

```json
{
  "reviewRemark": "信息核对通过，等待人工打款"
}
```

说明：

- 状态 `PENDING -> APPROVED`
- 审核通过不代表已到账
- 此时进入人工打款阶段

### 5.3 审核拒绝

`POST /api/admin/withdraws/{withdrawId}/reject`

请求体：

```json
{
  "reviewRemark": "账户信息不完整，已退回余额"
}
```

说明：

- 状态 `PENDING -> REJECTED`
- 后端会将申请金额退回经营者余额
- 写入余额流水 `WITHDRAW_REJECT_RETURN`

### 5.4 确认人工打款完成

`POST /api/admin/withdraws/{withdrawId}/complete-transfer`

请求体：

```json
{
  "transferNo": "ALIPAY-MANUAL-20260331-001",
  "reviewRemark": "已线下人工打款"
}
```

说明：

- 仅 `APPROVED` 状态可确认打款完成
- 状态 `APPROVED -> SUCCESS`
- `transferNo` 用于记录线下打款凭证号

## 6. AI 助手接口

### 6.1 会话接口

- `GET /api/ai/chat/sessions`
- `POST /api/ai/chat/sessions`
- `GET /api/ai/chat/sessions/{sessionId}`
- `PUT /api/ai/chat/sessions/{sessionId}`
- `DELETE /api/ai/chat/sessions/{sessionId}`
- `DELETE /api/ai/chat/sessions`

### 6.2 聊天接口

- 非流式：`POST /api/ai/chat/sessions/{sessionId}/messages`
- 流式：`POST /api/ai/chat/sessions/{sessionId}/stream`
- 消息历史：`GET /api/ai/chat/sessions/{sessionId}/messages`
- 消息反馈：`POST /api/ai/chat/messages/feedback`

非流式返回重点字段：

- `content`
- `confidence`
- `refuseReason`
- `fallback`

说明：

- 默认不对前端返回 `citations`

### 6.3 知识库管理接口

- `GET /api/ai/knowledge`
- `GET /api/ai/knowledge/{id}`
- `POST /api/ai/knowledge`
- `PUT /api/ai/knowledge/{id}`
- `PUT /api/ai/knowledge/{id}/status`
- `DELETE /api/ai/knowledge/{id}`
- `POST /api/ai/knowledge/batch-upsert`
- `POST /api/ai/knowledge/retrieve-preview`

说明：

- `PUT .../status`：启用或停用
- `DELETE .../{id}`：物理删除
- 知识库字段已移除 `docType` 和 `priority`

## 7. 经营者运营建议接口

统一前缀：

`/api/operator/insights`

主要接口：

- `POST /api/operator/insights/reviews/{farmStayId}/generate`
- `GET /api/operator/insights/reviews/{farmStayId}`
- `GET /api/operator/insights/reports`
- `GET /api/operator/insights/reports/{reportId}`
- `DELETE /api/operator/insights/reports/{reportId}`

## 8. 管理员后台接口

### 8.1 用户管理

- `GET /api/admin/users`
- `PUT /api/admin/users/{userId}/status`

### 8.2 评论治理

- `GET /api/admin/reviews`
- `DELETE /api/admin/reviews/{reviewId}`

### 8.3 知识库管理

- `GET /api/admin/knowledge`
- `GET /api/admin/knowledge/{id}`
- `POST /api/admin/knowledge`
- `POST /api/admin/knowledge/batch-upsert`
- `PUT /api/admin/knowledge/{id}`
- `PUT /api/admin/knowledge/{id}/status`
- `DELETE /api/admin/knowledge/{id}`
- `POST /api/admin/knowledge/retrieve-preview`

### 8.4 平台统计

`GET /api/admin/dashboard/overview`

返回重点字段：

- `orderCount`
- `turnover`
- `refundRate`
- `farmStayCount`
- `activeOperatorCount`
