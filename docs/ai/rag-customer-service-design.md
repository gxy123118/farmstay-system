# AI 智能客服设计（RAG）

## 1. 目标
- 为游客与经营者提供可追溯、可控、可扩展的智能问答能力。
- 回答基于平台真实业务数据和规则，不依赖纯大模型记忆。
- 支持证据引用、拒答策略、权限隔离。

## 2. 适用场景
- 游客：咨询房型、价格、活动、优惠、取消退款规则、订单流程。
- 经营者：咨询店铺配置、订单处理规则、活动上架建议、系统操作问题。

## 3. 架构总览
- `Data Ingestion`：采集业务库数据与规则文档。
- `Knowledge Index`：清洗、切片、向量化、元数据标注。
- `Retrieval`：多路召回（向量 + 关键词）+ 重排。
- `Answering`：统一通过 MiniMax（`MiniMax-M2.5`）生成回答。
- `Guardrail`：权限校验、敏感信息过滤、越权拦截。
- `Observability`：日志、延迟、召回质量、用户反馈。

## 3.1 当前实现说明
- 当前项目内所有 AI 业务统一走 MiniMax。
- 接入方式使用 Spring AI 的 OpenAI 兼容接口，默认模型为 `MiniMax-M2.5`。
- AI 客服接口保留现有 session/message API，不改前端调用方式。
- 当 MiniMax 调用失败时，后端会自动降级到本地规则回答，避免接口直接不可用。

## 4. 知识库来源
- 结构化数据：
1. 农家乐、房型、餐饮、活动、优惠券。
2. 订单规则、取消/退款策略。
- 非结构化数据：
1. FAQ 与运营公告。
2. 平台帮助文档。
- 元数据标签：
1. `scope`: public / operator-only
2. `farmStayId`: 店铺范围
3. `updatedAt`: 新鲜度
4. `docType`: policy / faq / product

## 5. RAG 处理流程
1. 用户请求进入 API，先做鉴权与角色识别。
2. Query Rewrite：将自然问题规范化为检索查询。
3. 检索：向量召回 + 关键词召回，合并去重。
4. 重排：按相关性、时效性、权限匹配排序。
5. 构建 Prompt：加入会话历史和证据片段。
6. LLM 生成回答，附证据引用。
7. Guardrail 二次审查（敏感词、越权片段、幻觉检测）。
8. 返回回答与引用，并记录审计日志。

## 6. API 设计
- `POST /api/ai/chat/sessions`：创建会话，返回 `sessionId`。
- `POST /api/ai/chat/sessions/{sessionId}/messages`：发送消息，返回：
1. `answer`
2. `citations[]`（来源 ID 与摘要）
3. `confidence`
4. `refuseReason`（如拒答）
- `POST /api/ai/chat/feedback`：提交“有帮助/无帮助”反馈。

## 7. 权限与隔离
- 游客仅可检索公开知识与本人相关订单规则。
- 经营者可检索公开知识 + 自己店铺经营知识。
- 严禁返回其他经营者店铺数据。
- 会话记录按 `userId + role + tenant` 绑定。

## 8. 工程建议
- 组件拆分：
1. `ai-gateway`（接口与鉴权）
2. `rag-retriever`（检索与重排）
3. `ai-provider`（模型调用适配层）
4. `knowledge-sync`（索引构建任务）
- 存储建议：
1. 向量库：Milvus / pgvector（二选一）
2. 文档索引：Elastic/OpenSearch（可选）
3. 会话与审计：MySQL + 对象存储

## 9. 质量与验收指标
- 命中率：客服问题可回答率 >= 90%（有有效证据）。
- 准确率：抽检正确率 >= 85%。
- 平均延迟：P95 <= 3s（非流式）。
- 反馈：无帮助率连续两周下降。

## 10. 失败降级
- 检索失败：回退到 FAQ 模板回答。
- 模型不可用：返回“人工客服/工单入口”。
- 权限不通过：拒答并提示权限限制。
