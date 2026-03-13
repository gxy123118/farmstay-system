# Farmstay System V2 API Outline

## 1. 说明
- 本文档为接口大纲，不是完整 OpenAPI JSON。
- `权限` 采用三类：`PUBLIC`、`VISITOR+`、`OPERATOR+`。
- `VISITOR+` 表示游客可用，经营者默认继承可用。

### 1.1 当前实现进度（2026-02-26）
- 已落地（MVP）：
1. `POST /api/ai/chat/sessions`
2. `GET /api/ai/chat/sessions/{sessionId}`
3. `POST /api/ai/chat/sessions/{sessionId}/messages`
4. `GET /api/ai/chat/sessions/{sessionId}/messages`
5. `POST /api/ai/chat/feedback`
6. `POST /api/operator/insights/reviews/{farmStayId}/generate`
7. `GET /api/operator/insights/reviews/{farmStayId}`
8. `GET /api/operator/insights/reviews/{farmStayId}/history`
9. `GET /api/operator/insights/reviews/{farmStayId}/issues`
- 已落地（权限改造）：
1. 鉴权升级为“经营者继承游客权限”（`VISITOR+` 可由经营者访问）。
- 待落地：
1. `/api/payments/*` 正式支付与回调验签。

## 2. 现有接口基线
| 模块 | 方法 | 路径 | 权限 | 状态 |
|---|---|---|---|---|
| 认证 | POST | `/api/auth/login` | PUBLIC | 已有 |
| 认证 | POST | `/api/auth/register` | PUBLIC | 已有 |
| 首页 | GET | `/api/home/overview` | PUBLIC | 已有 |
| 首页 | GET | `/api/home/recommendations` | PUBLIC | 已有 |
| 农家乐 | GET | `/api/farmstays/search` | PUBLIC | 已有 |
| 农家乐 | GET | `/api/farmstays/detail/{id}` | PUBLIC | 已有 |
| 农家乐 | GET | `/api/farmstays/owner` | OPERATOR+ | 已有 |
| 农家乐 | POST | `/api/farmstays` | OPERATOR+ | 已有 |
| 农家乐 | PUT | `/api/farmstays/{id}` | OPERATOR+ | 已有 |
| 农家乐 | DELETE | `/api/farmstays/{id}` | OPERATOR+ | 已有 |
| 房型 | GET | `/api/rooms?farmStayId=` | PUBLIC | 已有 |
| 房型 | POST | `/api/rooms` | OPERATOR+ | 已有 |
| 房型 | PUT | `/api/rooms/{id}` | OPERATOR+ | 已有 |
| 餐饮 | GET | `/api/dinings?farmStayId=` | PUBLIC | 已有 |
| 餐饮 | POST | `/api/dinings` | OPERATOR+ | 已有 |
| 餐饮 | PUT | `/api/dinings/{id}` | OPERATOR+ | 已有 |
| 活动 | GET | `/api/activities?farmStayId=` | PUBLIC | 已有 |
| 活动 | POST | `/api/activities` | OPERATOR+ | 已有 |
| 活动 | PUT | `/api/activities/{id}` | OPERATOR+ | 已有 |
| 优惠券 | GET | `/api/coupons` | PUBLIC | 已有 |
| 优惠券 | POST | `/api/coupons` | OPERATOR+ | 已有 |
| 订单 | POST | `/api/bookings` | VISITOR+ | 已有 |
| 订单 | POST | `/api/bookings/{orderId}/cancel` | VISITOR+ | 已有 |
| 订单 | POST | `/api/bookings/{orderId}/refund` | VISITOR+ | 已有（模拟退款） |
| 订单 | POST | `/api/bookings/pay` | VISITOR+ | 已有（模拟支付） |
| 订单 | PUT | `/api/bookings/status` | OPERATOR+ | 已有 |
| 订单 | GET | `/api/bookings/mine` | VISITOR+ | 已有 |
| 订单 | GET | `/api/bookings?farmStayId=` | OPERATOR+ | 已有 |
| 评论 | POST | `/api/reviews` | VISITOR+ | 已有 |
| 评论 | GET | `/api/reviews?farmStayId=` | PUBLIC | 已有 |
| 评论 | GET | `/api/reviews/order/{orderId}` | VISITOR+ | 已有 |
| 评论 | PUT | `/api/reviews/order/{orderId}` | VISITOR+ | 已有 |

## 3. 新增接口（V2 目标）
## 3.1 支付标准化
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/payments/orders` | VISITOR+ | 创建支付单，返回渠道参数 |
| GET | `/api/payments/orders/{paymentOrderNo}` | VISITOR+ | 查询支付状态 |
| POST | `/api/payments/callback/alipay` | PUBLIC(签名验签) | 支付宝回调 |
| POST | `/api/payments/callback/wechat` | PUBLIC(签名验签) | 微信回调 |
| POST | `/api/payments/refunds` | VISITOR+/OPERATOR+ | 发起退款（按策略） |

兼容策略：
- 保留 `/api/bookings/pay` 和 `/refund` 作为兼容接口。
- 新客户端优先接入 `/api/payments/*`。

## 3.2 AI 客服（RAG）
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/ai/chat/sessions` | VISITOR+ | 创建会话 |
| GET | `/api/ai/chat/sessions/{sessionId}` | VISITOR+ | 获取会话详情 |
| POST | `/api/ai/chat/sessions/{sessionId}/messages` | VISITOR+ | 发送问题，返回回答和引用 |
| GET | `/api/ai/chat/sessions/{sessionId}/messages` | VISITOR+ | 会话消息分页 |
| POST | `/api/ai/chat/feedback` | VISITOR+ | 用户反馈回答有用性 |

## 3.3 经营建议（评论驱动）
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/operator/insights/reviews/{farmStayId}/generate` | OPERATOR+ | 触发建议生成任务 |
| GET | `/api/operator/insights/reviews/{farmStayId}` | OPERATOR+ | 获取最新建议报告 |
| GET | `/api/operator/insights/reviews/{farmStayId}/history` | OPERATOR+ | 历史报告列表 |
| GET | `/api/operator/insights/reviews/{farmStayId}/issues` | OPERATOR+ | 问题主题明细 |

## 3.4 经营统计与财务
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/operator/stats/{farmStayId}/overview` | OPERATOR+ | 经营总览 |
| GET | `/api/operator/stats/{farmStayId}/orders` | OPERATOR+ | 订单指标 |
| GET | `/api/operator/finance/{farmStayId}/summary` | OPERATOR+ | 收入/退款/待结算 |
| GET | `/api/operator/finance/{farmStayId}/export` | OPERATOR+ | 报表导出 |

## 4. 权限与租户隔离约束
- 公开接口不得返回手机号、支付信息等敏感字段。
- `OPERATOR+` 接口必须执行 `farmStay.owner_id == login_user_id`。
- `VISITOR+` 接口必须执行 `order.visitor_id == login_user_id`。
- AI 会话和建议报告均绑定 `tenantId/farmStayId`，禁止跨租户查询。

## 5. 错误码建议
- `AUTH_401` 未登录或 token 失效。
- `AUTH_403` 角色不匹配或租户越权。
- `BIZ_404` 资源不存在。
- `BIZ_409` 状态冲突（重复支付、重复退款）。
- `PAY_422` 渠道参数错误或回调验签失败。
- `AI_429` AI 请求限流。
- `AI_503` 模型或检索服务不可用。
## 6. 2026-03-11 Interface Alignment
- `GET /api/auth/me` is implemented and should be used by the frontend to refresh the current user profile after page reload.
- `POST /api/auth/login`, `POST /api/auth/register`, and `GET /api/auth/me` now return the same user payload fields: `token`, `loginType`, `expire`, `userId`, `username`, `displayName`, `status`.
- `GET /api/bookings/mine` and `GET /api/bookings?farmStayId=` now expose visitor display fields in each order item: `visitorId`, `visitorUsername`, `visitorName`, `contactName`, `contactPhone`, `guests`.
- `visitorName` means `displayName` first and falls back to `username` when no display name is set.
