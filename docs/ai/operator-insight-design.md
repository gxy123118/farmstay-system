# 运营洞察 AI 接口设计与对接说明

## 1. 文档目标
本文档说明“评论驱动的运营洞察”接口如何工作、前端如何调用、后端如何配置大模型，以及返回数据的字段含义。

适用对象：
- 前端：需要接运营后台“AI 经营建议”页面
- 后端：需要配置 Spring AI 与 OpenAI 兼容模型
- 联调同学：需要明确触发方式、返回结构、异常处理

## 2. 这次改动做了什么
当前版本已经将原来的“数据库查询 + 本地 Map 拼装”升级为“数据库聚合 + 大模型结构化生成”。

后端生成流程：
1. 校验当前登录人是否为该民宿的经营者
2. 拉取指定周期内的评论、订单、民宿基础信息
3. 先做一层基础规则分析，得到主题、负向占比、影响分
4. 将结构化数据交给大模型生成 `summary / issues / actions`
5. 后端会先清洗模型原始输出，再自动提取 JSON 主体后做解析和字段兜底
6. 如果模型调用失败，自动降级为本地规则结果，接口仍可返回

## 3. 技术实现说明
### 3.1 使用的依赖
- `spring-ai-starter-model-openai`
- Spring Boot 3.4.x
- OpenAI 兼容接口协议

### 3.2 当前支持的模型接入方式
只要模型服务兼容 OpenAI Chat Completions 协议即可接入，例如：
- OpenAI
- DeepSeek（OpenAI 兼容模式）
- 其他提供 OpenAI 兼容网关的模型平台

补充说明：
- 当前项目默认接的是 MiniMax
- 由于部分模型可能返回推理文本、`<think>` 标签或代码块包裹内容，后端已增加响应清洗和 JSON 提取逻辑
- 也就是说，即使模型不是“纯 JSON 开头”，只要返回体里包含完整 JSON 对象，后端仍会尝试解析

### 3.3 配置项
文件：`src/main/resources/application.yml`

```yaml
spring:
  ai:
    model:
      chat: openai
    openai:
      api-key: REPLACE_WITH_MINIMAX_API_KEY
      base-url: https://api.minimaxi.com/v1
      chat:
        options:
          model: MiniMax-M2.5
          temperature: 0.2

app:
  ai:
    operator-insight:
      enabled: true
      model: MiniMax-M2.5
      max-review-samples: 20
```

说明：
- 当前默认按 MiniMax 的 OpenAI 兼容接口接入，模型默认是 `MiniMax-M2.5`
- 需要把 `api-key` 从 `REPLACE_WITH_MINIMAX_API_KEY` 改成真实的 MiniMax Key
- `base-url` 当前默认是 `https://api.minimaxi.com/v1`
- `max-review-samples` 表示单次送给模型的评论样本上限
- `enabled=false` 时，会直接走本地规则降级逻辑

## 4. 接口清单
接口前缀：`/api/operator/insights/reviews`

权限要求：
- 需要登录
- 需要 `OPERATOR+`
- 只能分析当前经营者自己名下的民宿

### 4.1 生成经营建议
`POST /api/operator/insights/reviews/{farmStayId}/generate`

用途：
- 主动触发一次 AI 生成
- 建议前端点击“重新生成建议”按钮时调用

请求参数：
- 路径参数 `farmStayId`：民宿 ID
- 查询参数 `periodDays`：分析周期，单位天，默认 `30`

推荐值：
- `7`
- `30`
- `90`

请求示例：
```http
POST /api/operator/insights/reviews/1001/generate?periodDays=30
Authorization: Bearer {token}
```

成功响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reportId": 5001,
    "farmStayId": 1001,
    "periodDays": 30,
    "reviewCount": 26,
    "averageRating": 4.12,
    "summary": "近 30 天差评主要集中在卫生和早餐体验，建议先做客房复检和早餐标准化。",
    "generatedAt": "2026-03-12T12:40:00.000+08:00",
    "generationMode": "llm",
    "model": "MiniMax-M2.5",
    "issues": [
      {
        "topic": "卫生",
        "priority": "P1",
        "issueCount": 8,
        "negativeRatio": 0.5,
        "impactScore": 76,
        "evidenceCount": 4,
        "evidenceSamples": [
          "房间有异味，卫生间地面还有水渍",
          "床品不够干净，入住体验一般"
        ]
      },
      {
        "topic": "餐饮",
        "priority": "P2",
        "issueCount": 6,
        "negativeRatio": 0.33,
        "impactScore": 52,
        "evidenceCount": 2,
        "evidenceSamples": [
          "早餐种类太少，口味比较普通"
        ]
      }
    ],
    "actions": [
      {
        "title": "客房清洁复检专项",
        "topic": "卫生",
        "action": "建立退房后与入住前两次复检流程，高峰期增加主管抽检。",
        "level": "short_term",
        "expectedBenefit": "预计 2 到 4 周内卫生相关差评率下降 10% 到 20%",
        "owner": "运营主管",
        "reason": "卫生主题影响分最高，且证据样本集中。"
      }
    ]
  }
}
```

### 4.2 获取最新一份建议
`GET /api/operator/insights/reviews/{farmStayId}`

用途：
- 页面首次进入时获取最新报告
- 如果当前民宿还没有生成过，后端会自动按 `30` 天周期生成一份

请求示例：
```http
GET /api/operator/insights/reviews/1001
Authorization: Bearer {token}
```

前端建议：
- 页面初始化优先调用这个接口
- 如果需要切换时间范围，再调用 `generate`

### 4.3 获取历史报告列表
`GET /api/operator/insights/reviews/{farmStayId}/history`

用途：
- 展示“历史生成记录”列表

请求示例：
```http
GET /api/operator/insights/reviews/1001/history
Authorization: Bearer {token}
```

成功响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "reportId": 5003,
      "farmStayId": 1001,
      "periodDays": 30,
      "reviewCount": 26,
      "averageRating": 4.12,
      "summary": "近 30 天差评主要集中在卫生和早餐体验。",
      "generatedAt": "2026-03-12T12:40:00.000+08:00",
      "generationMode": "llm",
      "model": "MiniMax-M2.5",
      "issues": [],
      "actions": []
    }
  ]
}
```

当前版本说明：
- 历史记录保存在应用内存中
- 服务重启后，历史记录会清空
- 如果后续需要长期保存，可再扩展数据库表

### 4.4 获取单条历史报告详情
`GET /api/operator/insights/reviews/{farmStayId}/history/{reportId}`

用途：
- 点击历史报告卡片后，获取该条历史报告的完整详情
- 前端建议不要只依赖历史列表中的缓存对象，详情页或弹窗打开时应调用这个接口

请求示例：
```http
GET /api/operator/insights/reviews/1001/history/5003
Authorization: Bearer {token}
```

### 4.5 删除历史报告
`DELETE /api/operator/insights/reviews/{farmStayId}/history/{reportId}`

用途：
- 删除某一条历史报告
- 用于前端“删除记录”按钮，避免内存中的历史报告无限增长

请求示例：
```http
DELETE /api/operator/insights/reviews/1001/history/5003
Authorization: Bearer {token}
```

成功响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

### 4.6 仅获取问题主题列表
`GET /api/operator/insights/reviews/{farmStayId}/issues`

用途：
- 页面只想展示“问题分布卡片”时使用
- 实际数据来源于最新报告中的 `issues`

请求示例：
```http
GET /api/operator/insights/reviews/1001/issues
Authorization: Bearer {token}
```

## 5. 字段说明
### 5.1 报告对象 `OperatorInsightReportResponse`
| 字段 | 类型 | 含义 | 前端建议 |
|---|---|---|---|
| `reportId` | `number` | 报告 ID | 可作为列表 key |
| `farmStayId` | `number` | 民宿 ID | 页面上下文字段 |
| `periodDays` | `number` | 本次分析周期（天） | 显示为 7/30/90 天筛选结果 |
| `reviewCount` | `number` | 本次参与分析的评论数 | 可显示在头部统计卡 |
| `averageRating` | `number` | 本次样本平均评分 | 建议保留两位小数显示 |
| `summary` | `string` | AI 生成的总体结论 | 页面摘要区主文案 |
| `generatedAt` | `string` | 生成时间 | 直接展示即可 |
| `generationMode` | `string` | `llm` 或 `fallback` | 若为 `fallback` 建议前端显示“已使用降级结果” |
| `model` | `string` | 本次使用的模型标识 | 可展示在调试信息或隐藏 |
| `issues` | `array` | 问题主题列表 | 渲染问题排行 |
| `actions` | `array` | 可执行建议列表 | 渲染行动卡片 |

### 5.2 问题对象 `OperatorInsightIssueResponse`
| 字段 | 类型 | 含义 | 前端建议 |
|---|---|---|---|
| `topic` | `string` | 问题主题，如卫生/服务/餐饮 | 作为分类标题 |
| `priority` | `string` | 优先级：`P1/P2/P3` | 用颜色标签显示 |
| `issueCount` | `number` | 该主题相关评论数 | 显示数量 |
| `negativeRatio` | `number` | 该主题负向占比 | 可转百分比显示 |
| `impactScore` | `number` | 影响分，范围 0-100 | 可用于排序或进度条 |
| `evidenceCount` | `number` | 证据条数 | 显示“涉及 x 条差评” |
| `evidenceSamples` | `string[]` | 评论证据摘录 | hover、tooltip 或展开详情 |

### 5.3 建议对象 `OperatorInsightActionResponse`
| 字段 | 类型 | 含义 | 前端建议 |
|---|---|---|---|
| `title` | `string` | 建议标题 | 卡片标题 |
| `topic` | `string` | 关联主题 | 显示标签 |
| `action` | `string` | 具体动作 | 主内容 |
| `level` | `string` | `short_term/mid_term/long_term` | 显示为短期/中期/长期 |
| `expectedBenefit` | `string` | 预期收益 | 放在次级说明区 |
| `owner` | `string` | 建议负责人角色 | 例如运营主管/店长 |
| `reason` | `string` | 这条建议的原因 | 可放说明文案 |

## 6. 前端联调建议
### 6.1 推荐调用顺序
页面进入时：
1. 调用 `GET /api/operator/insights/reviews/{farmStayId}`
2. 渲染摘要、问题列表、建议列表
3. 若用户切换分析周期，调用 `POST /generate?periodDays=xx`
4. 若用户打开历史记录列表，调用 `/history`
5. 若用户点击某条历史记录，调用 `/history/{reportId}`
6. 若用户删除某条历史记录，调用 `DELETE /history/{reportId}` 后刷新列表

### 6.2 建议的页面模块
- 顶部摘要卡：`summary`、`reviewCount`、`averageRating`、`generatedAt`
- 问题排行卡：`issues`
- 行动建议卡：`actions`
- 历史记录抽屉：`history`
- 历史详情弹窗：`history/{reportId}`
- 删除历史按钮：`DELETE history/{reportId}`
- 降级提示条：当 `generationMode = fallback` 时显示

### 6.3 前端需要注意的点
- `issues` 和 `actions` 可能为空数组，不要按必有数据写死
- `generationMode=fallback` 不代表接口失败，只代表模型调用失败后用了本地规则结果
- `negativeRatio` 是小数，例如 `0.38`，前端如果显示百分比请转成 `38%`
- `generatedAt` 是标准时间字符串，前端按本地时区格式化展示即可

## 7. 权限与安全
### 7.1 权限规则
只有经营者可以访问，并且只能分析自己名下的民宿。

### 7.2 隐私保护
送给模型的数据不包含：
- 游客手机号
- 联系人手机号
- 游客姓名
- 用户名等个人敏感信息

### 7.3 模型输出约束
后端要求模型只输出结构化 JSON，并且会做二次解析与兜底，避免前端直接面对自由文本脏数据。

## 8. 降级与异常处理
### 8.1 降级逻辑
以下情况会自动降级：
- 模型不可用
- API Key 未配置或配置错误
- 模型响应超时
- 模型返回的不是合法 JSON
- 模型返回中虽然带有解释文本，但无法成功提取完整 JSON 对象

降级后：
- 接口仍返回 `200`
- `generationMode = fallback`
- `summary/issues/actions` 使用本地规则结果

### 8.2 常见错误码
| code | 含义 | 前端处理建议 |
|---|---|---|
| `200` | 成功 | 正常渲染 |
| `400` | 业务错误，例如民宿不属于当前经营者 | 弹出 message |
| `401` | 未登录或登录失效 | 跳转登录 |
| `500` | 系统异常 | 提示“稍后重试” |

## 9. 历史报告落库说明
### 9.1 当前存储策略
- 生成报告后，后端会先把完整报告 JSON 落库到 `operator_insight_report`
- 同时把报告放进内存缓存 `reportStore`
- 查询时优先读缓存，缓存未命中再查数据库
- 删除时先做数据库逻辑删除，再删缓存
- 服务重启后，`latest/history/historyDetail` 仍可从数据库恢复历史报告

### 9.2 表设计
表名：`operator_insight_report`

主要字段：
- `report_id`：业务报告 ID，对应接口返回的 `reportId`
- `farm_stay_id`：民宿 ID
- `owner_id`：经营者 ID
- `period_days`：分析周期
- `generation_mode`：`llm` 或 `fallback`
- `model`：模型名称，例如 `MiniMax-M2.5`
- `summary`：摘要
- `report_json`：完整报告 JSON，包含 `issues` 和 `actions`
- `deleted`：逻辑删除标记
- `generated_at`：报告生成时间

建表 SQL 见：
- `sql/operator_insight_report.sql`

## 10. 给后端的补充说明
### 10.1 当前版本的边界
当前版本已经完成：
- Spring AI 接入
- 运营洞察结构化生成
- 失败自动降级
- 前端可消费的统一返回结构
- 历史报告落库 + 缓存读取

当前版本暂未完成：
- 异步任务队列
- 定时刷新报告
- Prompt/结果审计日志

### 10.2 后续可扩展方向
- 增加“对比上一周期变化”的字段
- 增加“建议完成状态”回写
- 历史趋势图和分页查询
- 统一复用到 AI 客服模块

## 11. 联调结论
前端如果需要完整历史管理，建议接入这 6 个接口：
- `POST /api/operator/insights/reviews/{farmStayId}/generate`
- `GET /api/operator/insights/reviews/{farmStayId}`
- `GET /api/operator/insights/reviews/{farmStayId}/history`
- `GET /api/operator/insights/reviews/{farmStayId}/history/{reportId}`
- `DELETE /api/operator/insights/reviews/{farmStayId}/history/{reportId}`
- `GET /api/operator/insights/reviews/{farmStayId}/issues`

如果页面只做一期 MVP，建议最少接这两个：
- `GET /api/operator/insights/reviews/{farmStayId}`
- `POST /api/operator/insights/reviews/{farmStayId}/generate`
