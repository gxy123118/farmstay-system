# 后端接口对接指南

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
- `userType` 支持：`visitor`、`operator`、`admin`
- `admin` 不允许注册，但允许登录

### 1.2 注册
`POST /api/auth/register`

说明：
- 注册页仅允许 `visitor`、`operator`

### 1.3 当前用户信息
`GET /api/auth/me`

返回重点字段：
- `id`
- `username`
- `displayName`
- `userType`
- `status`
- `balance`

## 2. 订单、支付与退款

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
  "payMethod": "BALANCE"
}
```

成功后：
- 订单状态更新为 `PAID`
- 用户余额扣减
- 写入余额流水

余额不足时：
- 返回明确失败提示

### 2.3 退款
`POST /api/bookings/{orderId}/refund`

说明：
- 仅已支付订单允许退款
- 退款成功后回退余额

### 2.4 查询余额
`GET /api/account/balance`

### 2.5 查询余额流水
`GET /api/account/balance/flows`

## 3. 充值接口

### 3.1 创建充值单
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

### 3.2 查询充值单状态
`GET /api/account/recharges/{rechargeNo}`

说明：
- 本地状态为 `PENDING` 时，后端会主动调用支付宝查询作为补偿

### 3.3 支付宝回调
`POST /api/account/recharges/alipay/notify`

### 3.4 本地模拟充值
`POST /api/account/recharges/{rechargeNo}/mock-pay`

## 4. AI 助手接口

### 4.1 会话接口
- `GET /api/ai/chat/sessions`
- `POST /api/ai/chat/sessions`
- `GET /api/ai/chat/sessions/{sessionId}`
- `PUT /api/ai/chat/sessions/{sessionId}`
- `DELETE /api/ai/chat/sessions/{sessionId}`
- `DELETE /api/ai/chat/sessions`

### 4.2 聊天接口
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

### 4.3 知识库管理接口
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

## 5. 经营者运营建议接口
前缀：
`/api/operator/insights/reviews`

主要接口：
- `POST /api/operator/insights/reviews/{farmStayId}/generate`
- `GET /api/operator/insights/reviews/{farmStayId}`
- `GET /api/operator/insights/reports`
- `GET /api/operator/insights/reports/{reportId}`
- `DELETE /api/operator/insights/reports/{reportId}`

## 6. 管理员后台接口

### 6.1 登录
管理员使用统一登录接口：
`POST /api/auth/login`

### 6.2 用户管理
- `GET /api/admin/users`
- `PUT /api/admin/users/{userId}/status`

### 6.3 评论治理
- `GET /api/admin/reviews`
- `DELETE /api/admin/reviews/{reviewId}`

### 6.4 知识库管理
- `GET /api/admin/knowledge`
- `GET /api/admin/knowledge/{id}`
- `POST /api/admin/knowledge`
- `POST /api/admin/knowledge/batch-upsert`
- `PUT /api/admin/knowledge/{id}`
- `PUT /api/admin/knowledge/{id}/status`
- `DELETE /api/admin/knowledge/{id}`
- `POST /api/admin/knowledge/retrieve-preview`

### 6.5 平台统计
`GET /api/admin/dashboard/overview`

返回字段：
- `orderCount`
- `turnover`
- `refundRate`
- `farmStayCount`
- `activeOperatorCount`
