# 支付与充值设计

## 1. 当前交易方案
当前项目采用：
- 充值使用支付宝
- 订单支付仅支持余额支付
- 退款退回余额

## 2. 为什么这样设计
直接把订单支付接入支付宝会引入：
- 下单签名
- 回调验签
- 支付状态补偿
- 关单
- 原路退款

当前阶段先采用“充值接支付宝、订单走余额”的方式，可以明显降低交易链路复杂度。

## 3. 数据模型
核心表：
- `user_account`：余额字段
- `user_balance_flow`：余额流水
- `recharge_order`：充值单
- `refund_order`：退款单

SQL 见：
- [balance_payment.sql](E:\code\farmstay-system\sql\balance_payment.sql)

## 4. 订单链路
1. 创建订单
2. 用户使用余额支付
3. 余额扣减成功后订单变为 `PAID`

## 5. 退款链路
1. 用户对已支付订单发起退款
2. 退款成功后金额回退到余额
3. 写入退款单与余额流水

## 6. 余额不足
余额不足时：
- 支付失败
- 后端返回明确提示
- 前端引导用户先去充值

## 7. 支付宝充值闭环

### 7.1 目标
完成：
- 创建充值单
- 调用支付宝预下单
- 返回 `qrCode`
- 用户扫码支付
- 回调或主动查询补偿入账
- 余额更新

### 7.2 接口
- `POST /api/account/recharges`
- `GET /api/account/recharges/{rechargeNo}`
- `POST /api/account/recharges/alipay/notify`
- `POST /api/account/recharges/{rechargeNo}/mock-pay`

### 7.3 时序
1. 前端创建充值单
2. 后端调用 `alipay.trade.precreate`
3. 返回 `qrCode`
4. 前端生成二维码
5. 用户扫码支付
6. 支付宝回调后端，或后端查询补偿
7. 更新 `recharge_order`
8. 增加 `user_account.balance`
9. 写入 `user_balance_flow`

### 7.4 前端对接重点
- `qrCode` 不是图片，而是二维码内容字符串
- 前端负责把 `qrCode` 渲染为二维码
- 充值成功后重新拉余额，不要只改本地状态

### 7.5 本地联调
如果支付宝回调链暂时不稳定：
- 可以先用 `mock-pay`
- 正式联调再切回真实回调链
