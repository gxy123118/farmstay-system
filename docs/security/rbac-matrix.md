# RBAC 权限矩阵（V2）

## 1. 角色层级
- `GUEST`：未登录用户。
- `VISITOR`：游客用户。
- `OPERATOR`：经营者用户，继承 `VISITOR` 全部能力并增加经营管理权限。

角色继承关系：
- `GUEST < VISITOR < OPERATOR`

## 2. 权限原则
- 公开读接口对 `GUEST` 开放。
- 订单、评价、支付等用户行为对 `VISITOR` 开放，`OPERATOR` 默认可用。
- 经营管理与统计分析只对 `OPERATOR` 开放，且必须通过店铺归属校验。

## 3. 业务能力矩阵
| 业务域 | GUEST | VISITOR | OPERATOR |
|---|---|---|---|
| 首页/推荐/搜索/详情查看 | 允许 | 允许 | 允许 |
| 查看公开评论 | 允许 | 允许 | 允许 |
| 注册/登录 | 允许 | 允许 | 允许 |
| 创建订单 | 禁止 | 允许 | 允许 |
| 取消本人订单 | 禁止 | 允许 | 允许 |
| 申请退款（本人订单） | 禁止 | 允许 | 允许 |
| 支付本人订单 | 禁止 | 允许 | 允许 |
| 查看本人订单 | 禁止 | 允许 | 允许 |
| 创建/修改本人评论 | 禁止 | 允许 | 允许 |
| 创建/修改农家乐 | 禁止 | 禁止 | 允许（仅本人店铺） |
| 创建/修改房型/餐饮/活动 | 禁止 | 禁止 | 允许（仅本人店铺） |
| 创建优惠券 | 禁止 | 禁止 | 允许（仅本人店铺或平台级配置） |
| 查看店铺订单并更新状态 | 禁止 | 禁止 | 允许（仅本人店铺） |
| 查看经营统计/财务报表 | 禁止 | 禁止 | 允许（仅本人店铺） |
| AI 客服 | 禁止 | 允许 | 允许 |
| AI 经营建议 | 禁止 | 禁止 | 允许（仅本人店铺） |

## 4. 关键校验规则
- 用户级资源：
1. `booking.visitor_id == current_user_id`
2. `review.visitor_id == current_user_id`
- 店铺级资源：
1. `farm_stay.owner_id == current_user_id`
2. 子资源（房型/活动/餐饮/订单）通过 `farmStayId` 追溯 owner 校验。
- AI 访问控制：
1. 会话按 `userId + role + tenantId` 绑定。
2. 检索时按角色与 tenant 过滤索引文档。

## 5. 代码层改造建议
- 现状：`AuthGuard.enforceVisitor()` 与 `enforceOperator()` 是严格匹配。
- V2 建议：
1. 新增 `enforceAtLeastVisitor()`，允许 `VISITOR` 和 `OPERATOR`。
2. `enforceOperator()` 保持仅经营者。
3. 订单/评价接口改用 `enforceAtLeastVisitor()`。

## 5.1 已落地（2026-02-26）
- `AuthGuard.enforceVisitor()` 已升级为“至少游客权限”校验。
- 新增 `AuthGuard.enforceAtLeastVisitor()` 供新接口语义化使用。
- `AuthGuard.enforceOperator()` 仍保持经营者专属校验。

## 6. 审计与合规
- 所有越权访问记录审计日志（用户、角色、资源、时间）。
- 管理操作记录操作人和前后变更快照。
- AI 输出审计保留引用来源，支持问题追溯。
