# 余额充值闭环补充说明

## 1. 当前闭环策略
支付宝充值链路采用“两段确认”：

- 第一段：支付宝异步回调
- 第二段：前端查询触发的主动补偿

这意味着后端不会只依赖 `notify` 回调来完成入账。

## 2. 接口闭环

### 2.1 创建充值单
`POST /api/account/recharges`

后端动作：
- 写入本地 `recharge_order`
- 调用 `alipay.trade.precreate`
- 返回 `qrCode`

### 2.2 支付宝异步回调
`POST /api/account/recharges/alipay/notify`

后端动作：
- 验签
- 更新 `recharge_order.status=SUCCESS`
- 增加 `user_account.balance`
- 写入 `user_balance_flow`

### 2.3 充值状态查询
`GET /api/account/recharges/{rechargeNo}`

后端动作：
- 若本地已是 `SUCCESS`，直接返回
- 若本地仍是 `PENDING`，主动调用 `alipay.trade.query`
- 若支付宝已支付成功，则在本次查询内补做入账

## 3. 前端建议
- 创建充值单后展示二维码
- 每 2 到 3 秒轮询充值状态
- 查询到 `SUCCESS` 后刷新 `/api/auth/me` 或 `/api/account/balance`

## 4. 这样设计的原因
- 避免只依赖回调导致状态卡在 `PENDING`
- 内网穿透偶发抖动时仍能收口
- 前端不需要猜测支付是否成功，只认后端状态
