# 支付、结算与提现设计

## 1. 当前资金方案

当前项目采用以下资金链路：

- 游客充值使用支付宝
- 订单支付只使用平台余额
- 游客退款退回游客余额
- 经营者核销完成后，订单金额结算到经营者余额
- 经营者提现采用“申请 + 管理员审核 + 人工打款确认”模式

这样设计的目的，是在保证业务闭环的前提下降低第三方支付直连的复杂度。

## 2. 为什么订单不直接接支付宝

如果订单支付直接接支付宝，需要同时处理：

- 订单预下单与签名
- 异步回调验签
- 支付状态补偿查询
- 超时关单
- 原路退款
- 多渠道差异

当前阶段先采用“充值接支付宝，订单走余额”的方案，可以显著降低交易链路复杂度，同时保留后续扩展空间。

## 3. 账户模型

核心数据结构：

- `user_account.balance`
  - 用户当前账户余额
- `user_balance_flow`
  - 余额流水
- `recharge_order`
  - 充值单
- `refund_order`
  - 游客退款单
- `withdraw_order`
  - 经营者提现申请单

相关 SQL：

- [balance_payment.sql](E:\code\farmstay-system\sql\balance_payment.sql)
- [withdraw_order.sql](E:\code\farmstay-system\sql\withdraw_order.sql)

## 4. 游客充值流程

### 4.1 流程说明

1. 前端创建充值单
2. 后端调用 `alipay.trade.precreate`
3. 后端返回 `qrCode`
4. 前端渲染二维码
5. 用户扫码支付
6. 支付宝回调后端，或后端主动查询补偿
7. 充值单状态更新为 `SUCCESS`
8. 用户余额增加
9. 写入余额流水 `RECHARGE`

### 4.2 接口

- `POST /api/account/recharges`
- `GET /api/account/recharges/{rechargeNo}`
- `POST /api/account/recharges/alipay/notify`
- `POST /api/account/recharges/{rechargeNo}/mock-pay`

### 4.3 前端注意事项

- `qrCode` 不是图片，而是二维码内容字符串
- 前端负责渲染二维码
- 充值成功后，应重新拉取余额，不要只更新本地变量

## 5. 游客订单支付流程

### 5.1 流程说明

1. 用户创建订单，订单状态为 `CREATED`
2. 用户发起余额支付
3. 后端校验余额是否充足
4. 扣减游客余额
5. 写入余额流水 `PAY_ORDER`
6. 订单状态变为 `PAID`

### 5.2 余额不足处理

余额不足时：

- 支付失败
- 后端返回明确错误提示
- 前端应引导用户先去充值

## 6. 游客退款流程

### 6.1 流程说明

1. 游客对已支付订单发起退款
2. 后端校验订单当前是否允许退款
3. 金额退回游客余额
4. 写入退款单 `refund_order`
5. 写入余额流水 `REFUND`
6. 订单状态变为 `REFUNDED`

### 6.2 退款限制

- 仅 `PAID` 且尚未完成履约的订单允许退款
- 一旦经营者完成核销，订单变为 `COMPLETED`
- `COMPLETED` 后，游客不能再退款

## 7. 经营者履约与结算

### 7.1 核销完成

经营者对本人店铺的 `PAID` 订单执行核销完成后：

- 订单状态 `PAID -> COMPLETED`
- 订单金额结算到经营者余额
- 写入经营者余额流水 `ORDER_SETTLEMENT`

### 7.2 设计意义

这样可以把“游客支付”和“经营者收入”区分开：

- 游客支付成功，不代表经营者已经收入到账
- 只有经营者履约完成后，系统才将收入结算到经营者余额

## 8. 经营者提现流程

### 8.1 为什么先做人工打款

当前阶段不直接接自动转账接口，而采用人工打款，是因为：

- 不需要企业付款资质接入
- 业务流程更可控
- 便于快速形成完整闭环
- 适合毕业设计和项目演示

### 8.2 提现状态

- `PENDING`
  - 已申请，待管理员审核
- `APPROVED`
  - 审核通过，待人工打款
- `SUCCESS`
  - 已确认人工打款完成
- `REJECTED`
  - 审核拒绝，金额已退回余额

### 8.3 提现流程

1. 经营者发起提现申请
2. 后端校验：
   - 金额大于 0
   - 余额充足
   - 渠道为 `ALIPAY`
   - 收款账号按支付宝手机号填写，且必须是 11 位大陆手机号
3. 申请成功后：
   - 扣减经营者余额
   - 创建 `withdraw_order`
   - 状态为 `PENDING`
   - 写入余额流水 `WITHDRAW_APPLY`
4. 管理员审核：
   - 通过：`PENDING -> APPROVED`
   - 拒绝：`PENDING -> REJECTED`
5. 如果拒绝：
   - 金额退回经营者余额
   - 写入余额流水 `WITHDRAW_REJECT_RETURN`
6. 管理员线下人工打款后，在后台确认：
   - `APPROVED -> SUCCESS`
   - 记录 `transferNo`

### 8.4 接口

经营者侧：

- `POST /api/account/withdraws`
- `GET /api/account/withdraws`

管理员侧：

- `GET /api/admin/withdraws`
- `POST /api/admin/withdraws/{withdrawId}/approve`
- `POST /api/admin/withdraws/{withdrawId}/reject`
- `POST /api/admin/withdraws/{withdrawId}/complete-transfer`

## 9. 整体资金闭环

当前系统已经形成如下完整闭环：

1. 游客支付宝充值
2. 游客余额支付订单
3. 经营者核销完成后获得结算收入
4. 游客在允许条件下可退款
5. 经营者可对已结算余额发起提现
6. 管理员审核并线下人工打款确认

## 10. 后续可扩展方向

如果后续要增强财务能力，可以按以下方向升级：

- 订单支付直接接支付宝或微信
- 经营者提现改为自动打款
- 增加平台抽成字段
- 增加提现失败状态与重试逻辑
- 增加提现审核日志与操作日志
