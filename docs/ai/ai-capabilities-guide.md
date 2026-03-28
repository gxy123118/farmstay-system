# AI 能力设计说明

## 1. AI 能力范围
当前项目包含两类 AI 能力：
- AI 助手
- 经营者运营建议

## 2. AI 助手

### 2.1 目标
AI 助手面向游客与经营者提供：
- FAQ 问答
- 农家乐推荐
- 能力说明
- 简单闲聊

### 2.2 当前方案
当前采用：
- 轻量 RAG
- LLM 意图识别
- FAQ 改写
- Recommendation 槽位提取
- SSE 流式输出

说明：
- 当前不是向量 RAG
- 检索层仍然是关键词匹配增强

### 2.3 意图类型
- `faq`
- `capability`
- `recommendation`
- `chitchat`

### 2.4 FAQ 检索说明
FAQ 处理流程：
1. 先识别 `intent=faq`
2. 生成更适合检索的 `faqQuery`
3. 从知识库 `title/content/keywords` 中做关键词匹配
4. 命中片段后交给模型生成答案

示例：
- 用户原问题：`钱付了还能退吗`
- 改写后的 `faqQuery`：`已支付订单退款规则`

### 2.5 Recommendation 说明
推荐流程：
1. 先识别 `intent=recommendation`
2. 提取槽位
3. 优先查知识库
4. 若知识库无结果，再查农家乐业务表
5. 返回候选结果，并引导用户继续补充条件

支持的槽位：
- `city`
- `travelGroup`
- `budgetMin`
- `budgetMax`
- `preferences`
- `topic`
- `timeRange`

### 2.6 多轮上下文
如果用户说：
- `这家农家乐适合什么人`
- `它适合亲子吗`

后端会尝试从最近一轮推荐消息的内部引用信息中恢复目标民宿，再继续 FAQ 或推荐链路。

### 2.7 输出协议
普通消息返回：
- `content`
- `confidence`
- `refuseReason`
- `fallback`

流式返回 SSE 事件：
- `meta`
- `chunk`
- `done`
- `error`

### 2.8 意图 JSON 示例
FAQ：
```json
{
  "intent": "faq",
  "faqQuery": "已支付订单退款规则",
  "needsClarification": false,
  "clarificationQuestion": "",
  "slots": {
    "city": null,
    "travelGroup": null,
    "budgetMin": null,
    "budgetMax": null,
    "preferences": [],
    "topic": null,
    "timeRange": null
  }
}
```

Capability：
```json
{
  "intent": "capability",
  "faqQuery": "",
  "needsClarification": false,
  "clarificationQuestion": "",
  "slots": {
    "city": null,
    "travelGroup": null,
    "budgetMin": null,
    "budgetMax": null,
    "preferences": [],
    "topic": null,
    "timeRange": null
  }
}
```

Recommendation：
```json
{
  "intent": "recommendation",
  "faqQuery": "",
  "needsClarification": false,
  "clarificationQuestion": "",
  "slots": {
    "city": null,
    "travelGroup": "亲子",
    "budgetMin": null,
    "budgetMax": 800,
    "preferences": ["安静"],
    "topic": null,
    "timeRange": "周末"
  }
}
```

Chitchat：
```json
{
  "intent": "chitchat",
  "faqQuery": "",
  "needsClarification": false,
  "clarificationQuestion": "",
  "slots": {
    "city": null,
    "travelGroup": null,
    "budgetMin": null,
    "budgetMax": null,
    "preferences": [],
    "topic": null,
    "timeRange": null
  }
}
```

## 3. 经营者运营建议

### 3.1 目标
面向经营者提供“基于评论数据的 AI 经营建议”，帮助经营者快速看到：
- 当前评价概况
- 主要问题
- 改进方向
- 可执行动作

### 3.2 当前方案
当前采用：
- 评论、订单、民宿信息聚合
- 本地规则预分析
- MiniMax 结构化生成
- JSON 清洗与解析
- 模型失败时本地规则兜底

### 3.3 主要接口
前缀：
`/api/operator/insights/reviews`

主要接口：
- `POST /api/operator/insights/reviews/{farmStayId}/generate`
- `GET /api/operator/insights/reviews/{farmStayId}`
- `GET /api/operator/insights/reports`
- `GET /api/operator/insights/reports/{reportId}`
- `DELETE /api/operator/insights/reports/{reportId}`

### 3.4 核心返回结构
- `summary`
- `issues`
- `actions`
- `reviewCount`
- `averageRating`
- `generatedAt`

### 3.5 报告持久化
报告会落库到：
- `operator_insight_report`

当前已支持：
- 最新报告读取
- 历史报告列表
- 报告详情
- 删除历史报告

### 3.6 ID 生成说明
`report_id` 为业务展示号，不直接依赖应用内固定起始值。
当前实现会：
- 先与数据库已有最大 `report_id` 对齐
- 遇到重复键时自动重试
