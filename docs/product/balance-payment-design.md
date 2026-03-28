# 余额支付与支付宝充值设计说明

## 1. 文档目标
本文档用于定义农家乐平台交易闭环的第一阶段实现方案。

本阶段目标：
- 用户下单后使用平台余额支付
- 用户充值时只接入支付宝
- 退款只退回平台余额
- 为后续扩展真实“订单直连支付宝支付”预留空间

本阶段不做：
- 微信支付
- 订单直接拉起支付宝支付
- 原路退回支付宝
- 分账

## 2. 为什么选余额方案
直接把订单支付接支付宝，复杂度会明显上升，原因包括：
- 需要支付下单、签名、回调验签
- 需要处理支付成功异步通知
- 需要处理支付失败、超时关单
- 需要处理退款原路退回
- 需要对账和异常补偿

当前项目更适合先做“平台余额方案”：
- 充值链路才接支付宝
- 下单支付只扣余额
- 退款只退余额
- 交易链路简单、可控、容易联调

## 3. 当前代码现状
当前项目已有订单骨架，但还不是真实交易闭环：

- [BookingServiceImpl.java](E:\code\farmstay-system\src\main\java\com\gxy\service\impl\BookingServiceImpl.java)
  - 创建订单后状态为 `CREATED`
  - 支付接口只是模拟改成 `PAID`
  - 退款接口只是模拟改成 `REFUNDED`

- [BookingOrder.java](E:\code\farmstay-system\src\main\java\com\gxy\model\entity\BookingOrder.java)
  - 当前只有订单基础字段
  - 没有资金流水、充值单、退款单

也就是说，当前订单模块更接近“演示版”，不是“资金可追踪版”。

## 4. 方案概述
本阶段按下面的业务边界实现：

### 4.1 充值
- 用户发起充值单
- 支付方式固定为 `ALIPAY`
- 支付宝支付成功后
  - 充值单状态改成功
  - 用户余额增加
  - 记录余额流水

### 4.2 下单
- 用户先创建订单
- 订单初始状态：`CREATED`
- 支付时只允许使用余额
- 余额扣减成功后
  - 订单状态改为 `PAID`
  - 支付渠道记为 `BALANCE`
  - 记录余额流水

### 4.3 退款
- 只有已支付订单允许退款
- 退款成功后
  - 订单状态改为 `REFUNDED`
  - 退款金额退回用户余额
  - 记录余额流水

### 4.4 资金方向
- 充值：支付宝 -> 平台余额
- 支付：平台余额 -> 订单
- 退款：订单 -> 平台余额

## 5. 推荐数据结构

### 5.1 `user_account` 表
当前建议在 `user_account` 表增加余额字段：

对应 SQL 已整理到：
- [balance_payment.sql](E:\code\farmstay-system\sql\balance_payment.sql)

说明：
- 可以保留在 `user_account` 表上，便于当前项目快速落地
- 但不能只有这个字段，必须配余额流水

### 5.2 余额流水表 `user_balance_flow`
用途：
- 记录每一次充值、扣款、退款
- 支持审计、排查、对账

建议字段：
- 见 [balance_payment.sql](E:\code\farmstay-system\sql\balance_payment.sql)

### 5.3 充值单表 `recharge_order`
用途：
- 记录充值请求和支付宝回调结果
- 见 [balance_payment.sql](E:\code\farmstay-system\sql\balance_payment.sql)

### 5.4 退款单表 `refund_order`
用途：
- 记录订单退款申请和退款结果
- 见 [balance_payment.sql](E:\code\farmstay-system\sql\balance_payment.sql)

## 6. 订单状态机
建议订单状态统一收敛为：

- `CREATED`
  - 已创建，待支付
- `PAID`
  - 已支付
- `CANCELLED`
  - 未支付取消
- `REFUNDED`
  - 已退款
- `COMPLETED`
  - 已完成入住

状态迁移：

```text
CREATED -> PAID
CREATED -> CANCELLED
PAID -> REFUNDED
PAID -> COMPLETED
```

规则：
- `CREATED` 状态允许余额支付
- `PAID` 状态允许申请退款
- `COMPLETED` 后通常不再允许直接退款，是否支持售后可后续再扩

## 7. 充值状态机
充值单状态建议：

- `PENDING`
- `SUCCESS`
- `FAILED`
- `CLOSED`

状态迁移：

```text
PENDING -> SUCCESS
PENDING -> FAILED
PENDING -> CLOSED
```

说明：
- 支付宝回调成功后，充值单改 `SUCCESS`
- 然后再给用户加余额
- 余额增加与流水写入要在同一事务里

## 8. 退款状态机
退款单状态建议：

- `PENDING`
- `SUCCESS`
- `FAILED`

状态迁移：

```text
PENDING -> SUCCESS
PENDING -> FAILED
```

本阶段退款逻辑：
- 退款不走支付宝
- 直接退回平台余额

## 9. 接口设计

### 9.1 充值相关

#### 创建充值单
`POST /api/account/recharges`

请求示例：

```json
{
  "amount": 500.00
}
```

响应示例：

```json
{
  "rechargeNo": "RC202603220001",
  "amount": 500.00,
  "payMethod": "ALIPAY",
  "status": "PENDING",
  "payForm": "这里是支付宝表单串或支付链接"
}
```

#### 模拟充值成功
`POST /api/account/recharges/{rechargeNo}/mock-pay`

说明：
- 当前阶段不接真实支付宝
- 通过 mock-pay 接口模拟充值成功入账
- 后续接支付宝时，替换为真实回调处理逻辑

#### 支付宝回调
`POST /api/account/recharges/alipay/notify`

说明：
- 支付宝异步通知后端
- 后端验签成功后更新充值单状态
- 同步增加 `user_account.balance`
- 写入 `user_balance_flow`

#### 查询充值单
`GET /api/account/recharges/{rechargeNo}`

### 9.2 余额相关

#### 查询账户余额
`GET /api/account/balance`

#### 当前用户信息补余额
`GET /api/auth/me`

说明：
- 当前用户信息接口同步返回 `balance`
- 前端个人中心可直接使用该字段展示余额

## 15. 当前沙箱配置说明
当前项目已经接入支付宝沙箱配置，默认读取：

- `app.payment.alipay.enabled=true`
- `gateway-url=https://openapi-sandbox.dl.alipaydev.com/gateway.do`
- `app-id=9021000162644762`
- `notify-url=https://109042c4.r40.cpolar.top/api/account/recharges/alipay/notify`

说明：
- 创建充值单时，后端会尝试调用 `alipay.trade.precreate`
- 成功后返回 `qrCode`
- 前端使用 `qrCode` 生成支付宝付款二维码
- 支付宝完成支付后，通过异步回调更新充值单状态与用户余额

当前仍保留：
- `POST /api/account/recharges/{rechargeNo}/mock-pay`

用途：
- 当本地沙箱联调异常、回调未打通或前端需要脱离支付宝调试时，可继续使用 mock 充值链路

响应示例：

```json
{
  "balance": 1280.00
}
```

#### 查询余额流水
`GET /api/account/balance/flows`

### 9.3 订单相关

#### 创建订单
继续复用现有：
`POST /api/bookings`

#### 余额支付
建议调整现有支付接口语义：
`POST /api/bookings/pay`

请求示例：

```json
{
  "orderId": 1001,
  "channel": "BALANCE"
}
```

规则：
- 本阶段 `channel` 只允许 `BALANCE`
- 若余额不足，返回错误码和余额不足提示

#### 取消订单
`POST /api/bookings/{orderId}/cancel`

规则：
- 只允许未支付订单取消

#### 申请退款
`POST /api/bookings/{orderId}/refund`

规则：
- 只允许已支付订单退款
- 退款金额回到平台余额

## 10. 支付宝接入边界
本阶段支付宝只用于充值，不用于订单直接支付。

后端职责：
- 创建充值单
- 生成支付宝支付参数
- 接收支付宝回调
- 验签
- 更新充值单状态
- 增加用户余额
- 写入余额流水

不建议当前阶段做的事情：
- 订单直接调支付宝支付
- 退款直接调支付宝退回

## 11. 幂等与事务要求

### 11.1 充值回调幂等
支付宝回调可能重复触发，所以必须保证：
- 同一个 `recharge_no` 只加一次余额
- 充值单从 `PENDING` 到 `SUCCESS` 的更新要做状态保护

### 11.2 余额扣款幂等
支付订单时必须保证：
- 同一个订单不能重复扣余额
- 订单状态必须从 `CREATED` 才能改到 `PAID`

### 11.3 退款幂等
退款时必须保证：
- 同一个订单只能生成一笔成功退款
- 不能重复退余额

### 11.4 事务要求
下面这些操作必须放同一事务：

充值成功：
- 更新充值单
- 更新用户余额
- 写余额流水

余额支付成功：
- 校验余额
- 扣减余额
- 更新订单状态
- 写余额流水

退款成功：
- 更新订单状态
- 增加余额
- 写余额流水
- 写退款单

## 12. 前端对接重点
前端需要明确三件事：

1. 订单支付方式
- 本阶段只支持余额支付

2. 充值方式
- 本阶段只支持支付宝充值

3. 退款去向
- 本阶段退款退回平台余额
- 不原路退回支付宝

建议页面：
- 账户余额页
- 充值页
- 余额流水页
- 下单确认页
- 订单详情页
- 退款结果页

## 13. 推荐实施顺序
建议按这个顺序实现：

1. 给用户表加 `balance`
2. 建 `user_balance_flow`
3. 建 `recharge_order`
4. 建 `refund_order`
5. 改造订单支付为 `BALANCE`
6. 改造退款为退余额
7. 接支付宝充值回调
8. 最后补前端余额页和充值页

## 14. 后续扩展
后续如果要升级，可以平滑扩展到：
- 微信充值
- 订单直接支付宝支付
- 订单直接微信支付
- 原路退款
- 分账与对账

但第一阶段建议严格控制范围，只做：
- 支付宝充值
- 余额支付
- 余额退款
