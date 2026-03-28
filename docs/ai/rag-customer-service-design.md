# AI 助手设计与对接说明

## 1. 文档目标
本文档用于说明农家乐项目 AI 助手的当前实现方案、前后端对接方式、轻量 RAG 检索逻辑，以及最新加入的“意图识别 + FAQ 改写 + 推荐槽位提取”能力。

适用对象：
- 前端：聊天页、会话列表、知识库管理页、流式打字机效果
- 后端：MiniMax 配置、知识检索、问题分析、SSE 流式输出
- 联调同学：接口、字段、事件协议、降级行为

## 2. 当前方案结论
当前 AI 助手采用的是“轻量 RAG + 问题分析器 + MiniMax 真流式输出”方案。

当前能力包括：
- MiniMax-M2.5 统一生成
- 轻量 RAG 检索
- LLM 意图识别
- FAQ 问题改写
- 推荐问题槽位提取
- SSE 流式输出
- 会话和消息持久化
- 知识库管理后台接口

说明：
- 当前仍然属于 RAG，因为整体流程仍然是“先检索，再增强，再生成”
- 当前不是向量 RAG，检索层仍是关键词召回 + 业务数据补充
- AI 主要新增在“问题理解”和“回答生成”两层

## 3. 整体链路
用户提问后，后端不再直接拿原句去检索，而是先做问题分析。

整体流程：
1. 用户发送问题
2. `AiQuestionAnalysisService` 调用 MiniMax 做问题分析
3. 返回结构化结果：
   - `intent`
   - `faqQuery`
   - `needsClarification`
   - `clarificationQuestion`
   - `slots`
4. 按意图分流：
   - `faq`：先做 FAQ 改写，再检索，再生成
   - `capability`：直接返回能力说明
   - `recommendation`：先提取推荐条件，不足则追问，足够再检索和生成
   - `chitchat`：短回复并引导回业务问题
5. 如果当前问题出现“这家 / 这个 / 它”这类指代词，后端会先尝试从当前会话最近一轮推荐结果中恢复目标民宿
5. 如果是需要模型生成的分支，再调用 MiniMax
6. 普通接口返回完整文本；流式接口返回 SSE 事件

## 4. 意图设计
当前定义 4 类意图：

### 4.1 `faq`
适用于具体业务问答，例如：
- 退款怎么处理
- 钱付了还能退吗
- 早餐几点开始
- 能带宠物吗

处理方式：
- 先让模型把自然语言问题改写成更适合检索的 FAQ 问法
- 再去知识库和业务数据里检索

### 4.2 `capability`
适用于用户询问助手能做什么，例如：
- 你能帮我做什么
- 你会什么
- 你能回答哪些问题

处理方式：
- 不走知识库命中判断
- 直接返回内置能力说明

### 4.3 `recommendation`
适用于推荐、筛选、比较类问题，例如：
- 帮我推荐一个项目
- 推荐一个适合亲子的民宿
- 预算 800 左右，想找个安静一点的

处理方式：
- 先提取推荐条件
- 只要已经拿到部分有效条件，就先返回一批候选结果
- 在候选结果后面再提示用户继续补充预算、人群、偏好等细节
- 只有完全没有有效条件时，才先追问

### 4.4 `chitchat`
适用于闲聊或不完整社交表达，例如：
- 你好
- 在吗
- 你是谁
- 9 乘以 18 等于多少

处理方式：
- 直接走 LLM 回答
- 对简单数学、轻量常识、简短闲聊可以直接答
- 如果问题接近业务场景，可在结尾轻度引导回农家乐问题
- 只有模型不可用时，才退回本地固定兜底文案

## 5. FAQ 改写
FAQ 的难点不是“有没有条件”，而是“同义表达很多”。

例如：
- 退款怎么处理
- 钱付了还能退吗
- 不想住了能不能退
- 取消后钱能回来吗

如果直接做关键词检索，容易漏召回。当前新增了一层 FAQ 改写：

示例：
- 原问题：`钱付了还能退吗`
- 改写后：`已支付订单退款规则`

改写后的问题会作为检索词传给轻量 RAG。

这样做的价值：
- 降低表达差异带来的漏检
- 保持当前数据库检索方案不变
- 为后续升级到向量检索预留接口

## 6. 槽位设计
槽位就是推荐问题中的关键条件。

当前支持的槽位：
- `city`
- `travelGroup`
- `budgetMin`
- `budgetMax`
- `preferences`
- `topic`
- `timeRange`

示例：

用户问题：
```text
帮我推荐一个适合亲子周末住的，预算 800 左右，最好安静一点
```

分析结果：
```json
{
  "intent": "recommendation",
  "needsClarification": false,
  "slots": {
    "city": "",
    "travelGroup": "亲子",
    "budgetMin": null,
    "budgetMax": 800,
    "preferences": ["安静"],
    "topic": "",
    "timeRange": "周末"
  }
}
```

如果槽位不足，例如用户只说：
```text
帮我推荐一个项目
```

则后端会优先追问，例如：
```text
可以，我可以按城市、预算、同行人群和偏好帮你推荐。你先告诉我想去哪个城市，或者更看重亲子、情侣、观景还是安静？
```

## 7. 当前轻量 RAG 怎么检索
当前检索不是向量相似度，而是轻量方案。

### 7.1 会话指代消解
当用户在上一轮已经拿到推荐结果后，继续问：
- 这家农家乐适合什么类型的人
- 这个适合亲子吗
- 它大概多少钱

后端不会把它当成完全无上下文的新问题，而是会：
1. 检查当前问题里是否包含“这家 / 这个 / 它 / 这家农家乐 / 这家民宿”等指代词
2. 从当前会话最近一轮 AI 消息里读取内部保存的 `citations_json`
3. 找出最近推荐的 `farmstay_candidate`
4. 恢复对应的 `farmStayId`
5. 再把这个 `farmStayId` 带入 FAQ 或 recommendation 检索链路

这样即使前端不再接收 `citations`，后端仍然能利用会话历史完成上下文理解。

### 7.2 FAQ 检索顺序
1. 先用 FAQ 改写后的问题去 `ai_knowledge_document` 检索
2. 命中后取前几条知识片段
3. 如果会话中带了 `farmStayId`，再补充该民宿的业务数据片段

### 7.3 Recommendation 检索顺序
1. 先用“原问题 + 槽位条件”拼成检索语句
2. 先查 `ai_knowledge_document`
3. 如果 `document` 没命中，则直接走农家乐业务表兜底召回
4. 业务表兜底不是只查一轮，而是从“严格条件”到“宽松条件”逐级放宽
5. 先返回候选结果，再在末尾追加“可继续补充条件”的引导
6. 再把候选民宿和房型片段交给模型生成推荐结果

当前会补的业务数据：
- 民宿基础信息
- 房型
- 餐饮
- 活动
- 优惠券

Recommendation 的业务表兜底说明：
- 当 `document` 层没有命中时，不会立刻放弃
- 如果已识别到 `city / travelGroup / preference / budget` 等条件，会直接从 `farmstay` 表召回候选民宿
- 会同时补充第一条可用房型，形成推荐依据
- 槽位不会被机械地全部映射到 SQL 条件，而是按字段语义做匹配：
  - `city` 只匹配 `farmstay.city`
  - `budget` 只映射 `priceLevel`
- `travelGroup / preference / topic` 只有在属于已支持标签时才映射到 `farmstay.tags`
- 像“农家乐推荐”“项目推荐”这种泛词不会进入 `tags` 条件，避免把 SQL 过滤得过死
- 召回时会从严到松逐步尝试：
  - `city + tag + priceLevel`
  - `city + tag`
  - `city`
  - `tag`
  - `priceLevel`
  - 无额外过滤的基础候选

说明：
- FAQ 场景主要依赖“改写后的问题”去提升召回
- 推荐场景主要依赖“槽位 + 原问题”拼出的检索词去召回候选信息
- 当 `document` 层无结果时，推荐场景会继续查业务表，不再直接提示“知识库未命中”
- 这一步还不是真正的语义检索，后续可以升级到向量库

## 8. 为什么“知识库未命中”不再作为总兜底
旧逻辑中，只要没有命中文档，就会直接返回：
- `fallback=true`
- `refuseReason=知识库未命中`

这对用户体验不合理，因为：
- 对能力型问题不适用
- 对推荐型问题不适用
- 它暴露了系统内部检索状态，不是用户关心的表达

现在的处理方式是：
- `capability`：直接答能力范围
- `recommendation`：优先返回当前条件下可推荐的候选，再引导继续细化
- `recommendation`：条件够时会先查 `document`，再查农家乐业务表
- `faq`：检索真的没命中时，才进入 FAQ 兜底
- `chitchat`：简短回复或引导

也就是说：
- “知识库未命中”只保留为 FAQ 内部失败原因
- 不再作为整个助手的统一外显文案

## 9. 对外接口
接口前缀：`/api/ai/chat`

### 9.1 会话列表
`GET /api/ai/chat/sessions`

### 9.2 创建会话
`POST /api/ai/chat/sessions`

请求示例：
```json
{
  "farmStayId": 33,
  "scene": "customer_service"
}
```

### 9.3 会话详情
`GET /api/ai/chat/sessions/{sessionId}`

### 9.4 修改会话标题
`PUT /api/ai/chat/sessions/{sessionId}`

### 9.5 删除单个会话
`DELETE /api/ai/chat/sessions/{sessionId}`

### 9.6 清空全部会话
`DELETE /api/ai/chat/sessions`

### 9.7 非流式聊天
`POST /api/ai/chat/sessions/{sessionId}/messages`

请求示例：
```json
{
  "question": "钱付了还能退吗"
}
```

响应关键字段：
- `content`：AI 回复正文
- `confidence`：置信度
- `refuseReason`：内部降级原因码
- `fallback`：是否走了兜底链路

说明：
- 聊天接口对前端默认不返回 `citations`
- 引用依据仍会保存在数据库中，供后端排查和后台扩展使用

说明：
- `fallback=false`
  - 包括 FAQ 正常命中生成
  - 也包括 capability、recommendation 追问、chitchat 这类非兜底直接回答
- `fallback=true`
  - 仅表示真正走了兜底逻辑，例如 FAQ 检索失败、模型不可用、模型生成失败

推荐的 `refuseReason` 码值：
- `KNOWLEDGE_MISS`
- `MODEL_UNAVAILABLE`
- `MODEL_ERROR`
- `STREAM_INTERRUPTED`

前端不要把 `refuseReason` 直接展示给用户。

### 9.8 流式聊天
`POST /api/ai/chat/sessions/{sessionId}/stream`

请求头：
```text
Content-Type: application/json
Accept: text/event-stream
```

请求体与非流式一致：
```json
{
  "question": "帮我推荐一个适合亲子的民宿"
}
```

## 10. SSE 事件协议
当前流式接口返回以下事件：

### 10.1 `meta`
表示本次响应元信息。

示例：
```json
{
  "type": "meta",
  "sessionId": 12,
  "messageId": 88,
  "model": "MiniMax-M2.5",
  "fallback": false
}
```

### 10.2 `chunk`
表示一段增量文本。

前端做法：
- 每收到一个 `chunk` 就拼接到当前 AI 消息末尾
- 这就是“打字机效果”的数据基础

### 10.3 `done`
表示本轮流式输出结束。

### 10.4 `error`
表示流式生成中断或异常。

前端建议：
- 当前回答区域显示“本次回答中断，请稍后重试”
- 保留已经收到的 `chunk`

## 11. 前端打字机效果说明
后端负责的是“流式传输”，真正的打字机视觉效果主要由前端实现。

推荐前端处理方式：
1. 连接 `/stream`
2. 监听 `chunk`
3. 每收到一个 `chunk`，拼接到当前消息内容
4. 使用前端渲染节奏控制动画
5. 收到 `done` 后结束本轮

说明：
- 如果前端直接把 `chunk` 累加显示，已经会有基础打字机效果
- 如果想更细腻，可以在前端再做逐字渲染

## 12. 知识库管理接口
接口前缀：`/api/ai/knowledge`

已支持：
- `GET /api/ai/knowledge`
- `GET /api/ai/knowledge/{id}`
- `POST /api/ai/knowledge`
- `PUT /api/ai/knowledge/{id}`
- `PUT /api/ai/knowledge/{id}/status`
- `DELETE /api/ai/knowledge/{id}`
- `POST /api/ai/knowledge/batch-upsert`
- `POST /api/ai/knowledge/retrieve-preview`

用途：
- 维护 FAQ / 帮助文档 / 规则类知识
- 验证某个问题最终会命中哪些知识片段

## 13. 降级策略
### 13.1 FAQ 检索失败
行为：
- 返回友好兜底答复
- `fallback=true`
- `refuseReason=KNOWLEDGE_MISS`

### 13.2 Recommendation `document` 未命中
行为：
- 不直接视为失败
- 会继续从 `farmstay` 和 `room_type` 里召回候选数据
- 只有业务表也无法提供候选时，才回到补充条件引导

### 13.3 模型不可用
行为：
- FAQ 走兜底答复
- 推荐/能力说明尽量走直接引导
- FAQ 场景下 `fallback=true`

### 13.4 模型生成失败
行为：
- FAQ 回退到兜底答复
- `fallback=true`
- `refuseReason=MODEL_ERROR`

### 13.5 流式中断
行为：
- 返回 `error` 事件
- 已经生成的内容尽量保留
- `refuseReason=STREAM_INTERRUPTED`

## 14. 后续升级方向
当前这版是“RAG v1”，后续可以升级到：
- Spring AI VectorStore
- pgvector / Milvus / Elasticsearch 向量检索
- Embedding 构建和增量更新
- 推荐意图下的候选排序
- 多民宿横向比较
- 更细粒度的前端展示字段

升级后变化会主要集中在“检索层”，不会推翻当前的：
- 会话接口
- SSE 协议
- 意图分流结构
- FAQ 改写和推荐槽位提取思路
