# 后端接口对接指南

本文档面向前端和联调人员，汇总当前项目已开放的核心后端接口。接口统一返回 `ApiResponse<T>`。

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
- `admin` 允许登录，但不允许前台注册

### 1.2 注册

`POST /api/auth/register`

说明：

- 注册页仅开放 `visitor`、`operator`

### 1.3 当前用户信息

`GET /api/auth/me`

返回重点字段：

- `id`
- `username`
- `displayName`
- `userType`
- `status`
- `balance`

## 2. 游客订单、支付与退款

### 2.1 创建订单

`POST /api/bookings`

说明：

- 创建成功后订单状态通常为 `CREATED`

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

- 订单状态更新为 `PAID`
- 用户余额扣减
- 写入余额流水

余额不足时：

- 返回明确的失败提示，前端应引导用户先充值

### 2.3 游客退款

`POST /api/bookings/{orderId}/refund`

说明：

- 仅已支付订单允许退款
- 退款成功后退回平台余额

### 2.4 游客查看自己的订单

`GET /api/bookings/mine`

返回说明：

- 返回游客自己的订单详情列表
- 包含订单、民宿、房型、评价状态、餐饮和活动明细

## 3. 经营者订单与交易统计

### 3.1 经营者查看单店订单

`GET /api/bookings?farmStayId={farmStayId}`

说明：

- 仅允许查看当前经营者本人名下店铺
- 保留该接口用于旧页面或单店视图

### 3.2 经营者查看名下全部订单

`GET /api/bookings/operator/orders`

查询参数：

- `farmStayId`：可选，按单个农家乐筛选
- `status`：可选，按订单状态筛选，如 `CREATED`、`PAID`、`REFUNDED`

返回说明：

- 返回当前经营者名下全部农家乐订单
- 每条记录包含：
  - 订单号
  - 游客账号/昵称
  - 农家乐信息
  - 房型信息
  - 入住和离店日期
  - 金额
  - 支付渠道
  - 订单状态
  - 是否已评价

### 3.3 经营者查看交易统计

`GET /api/bookings/operator/summary`

查询参数：

- `farmStayId`：可选，按单个农家乐统计；不传则统计当前经营者名下全部农家乐

返回字段：

- `farmStayCount`：纳入统计的农家乐数量
- `orderCount`：订单总数
- `paidOrderCount`：已支付类订单数
- `refundedOrderCount`：已退款订单数
- `grossTransactionAmount`：历史成交总额，按 `PAID` 和 `REFUNDED` 订单累计
- `refundAmount`：退款总额
- `netTransactionAmount`：净成交额，等于成交总额减退款总额
- `refundRate`：退款率，按订单数口径计算

## 4. 账户余额与充值

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

- `qrCode` 为支付宝付款码内容，前端负责渲染二维码

### 4.4 查询充值单状态

`GET /api/account/recharges/{rechargeNo}`

说明：

- 本地状态为 `PENDING` 时，后端会主动调用支付宝查询作为补偿

### 4.5 支付宝回调

`POST /api/account/recharges/alipay/notify`

### 4.6 本地模拟充值

`POST /api/account/recharges/{rechargeNo}/mock-pay`

## 5. AI 助手接口

### 5.1 会话接口

- `GET /api/ai/chat/sessions`
- `POST /api/ai/chat/sessions`
- `GET /api/ai/chat/sessions/{sessionId}`
- `PUT /api/ai/chat/sessions/{sessionId}`
- `DELETE /api/ai/chat/sessions/{sessionId}`
- `DELETE /api/ai/chat/sessions`

### 5.2 聊天接口

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

### 5.3 知识库管理接口

- `GET /api/ai/knowledge`
- `GET /api/ai/knowledge/{id}`
- `POST /api/ai/knowledge`
- `PUT /api/ai/knowledge/{id}`
- `PUT /api/ai/knowledge/{id}/status`
- `DELETE /api/ai/knowledge/{id}`
- `POST /api/ai/knowledge/batch-upsert`
- `POST /api/ai/knowledge/retrieve-preview`

说明：

- `PUT .../status`：启用/停用
- `DELETE .../{id}`：物理删除
- 知识库字段已移除 `docType` 和 `priority`

## 6. 经营者运营建议接口

统一前缀：

`/api/operator/insights`

主要接口：

- `POST /api/operator/insights/reviews/{farmStayId}/generate`
- `GET /api/operator/insights/reviews/{farmStayId}`
- `GET /api/operator/insights/reports`
- `GET /api/operator/insights/reports/{reportId}`
- `DELETE /api/operator/insights/reports/{reportId}`

## 7. 管理员后台接口

### 7.1 登录

管理员使用统一登录接口：

`POST /api/auth/login`

### 7.2 用户管理

- `GET /api/admin/users`
- `PUT /api/admin/users/{userId}/status`

### 7.3 评论治理

- `GET /api/admin/reviews`
- `DELETE /api/admin/reviews/{reviewId}`

### 7.4 知识库管理

- `GET /api/admin/knowledge`
- `GET /api/admin/knowledge/{id}`
- `POST /api/admin/knowledge`
- `POST /api/admin/knowledge/batch-upsert`
- `PUT /api/admin/knowledge/{id}`
- `PUT /api/admin/knowledge/{id}/status`
- `DELETE /api/admin/knowledge/{id}`
- `POST /api/admin/knowledge/retrieve-preview`

### 7.5 平台统计

`GET /api/admin/dashboard/overview`

返回字段：

- `orderCount`
- `turnover`
- `refundRate`
- `farmStayCount`
- `activeOperatorCount`
