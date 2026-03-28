# 运营洞察 AI 接口设计与对接说明

## 1. 文档目标
本文档说明“评论驱动的运营洞察”接口如何工作、前端如何调用、后端如何配置 MiniMax，以及返回字段含义。

适用对象：
- 前端：运营后台“AI 经营建议”页面
- 后端：运营洞察服务维护人员
- 联调同学：需要明确请求、响应、降级行为、历史记录使用方式

## 2. 当前方案概览
当前版本已将原来的“数据库查询 + 本地 Map 拼装”升级为：

`数据库聚合 + 规则预分析 + MiniMax 结构化生成 + 后端 JSON 清洗解析 + 降级兜底`

生成流程：
1. 校验当前经营者是否有权限访问该民宿
2. 拉取评论、订单、民宿信息等基础数据
3. 先做一层规则预分析，得到问题主题、负向占比、影响分
4. 把结构化上下文交给 MiniMax 生成 `summary / issues / actions`
5. 后端清洗模型原始返回，并提取 JSON 主体后解析
6. 若模型不可用或解析失败，则自动降级为本地规则结果

## 3. 当前已实现能力
当前后端已完成：
- 生成最新经营建议
- 获取最新报告
- 获取历史报告列表
- 获取单条历史报告详情
- 删除历史报告
- 获取问题主题列表
- 报告落库
- 内存缓存加速读取

历史报告当前策略：
- 数据库是主数据源
- 内存 `reportStore` 是一级缓存
- 重启后历史报告不会丢失

## 4. 模型接入方式
当前统一通过 Spring AI + OpenAI 兼容协议接入 MiniMax。

默认配置：
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
- 只需要把 `api-key` 换成真实 MiniMax key
- `enabled=false` 时，接口直接走本地规则降级
- `max-review-samples` 表示单次送给模型的评论样本上限

## 5. 模型输出治理
由于兼容模型可能返回：
- `<think>...</think>`
- Markdown 代码块
- JSON 前后的解释性文本

后端已增加输出治理逻辑：
1. 移除 `<think>`
2. 移除代码块包装
3. 从返回文本中提取第一个完整 JSON 对象
4. 再交给 Jackson 解析

因此：
- 不是必须“第一字符就是 `{`”
- 只要响应里包含完整 JSON 主体，后端就会尝试恢复

## 6. 接口清单
接口前缀：`/api/operator/insights/reviews`

权限要求：
- 已登录
- 经营者角色
- 只能分析自己名下民宿

### 6.1 生成经营建议
`POST /api/operator/insights/reviews/{farmStayId}/generate`

参数：
- `farmStayId`：民宿 ID
- `periodDays`：分析周期，默认 `30`

推荐值：
- `7`
- `30`
- `90`

请求示例：
```http
POST /api/operator/insights/reviews/1001/generate?periodDays=30
```

### 6.2 获取最新报告
`GET /api/operator/insights/reviews/{farmStayId}`

说明：
- 用于页面首次进入
- 若还没有历史报告，后端会自动生成一份默认 `30` 天报告

### 6.3 获取历史列表
`GET /api/operator/insights/reviews/{farmStayId}/history`

用途：
- 历史记录列表页
- 时间轴
- 侧边栏历史卡片

### 6.4 获取单条历史详情
`GET /api/operator/insights/reviews/{farmStayId}/history/{reportId}`

用途：
- 点击某条历史记录后查看完整报告

前端建议：
- 列表页点击后再调用该接口，不要只依赖列表接口缓存

### 6.5 删除历史报告
`DELETE /api/operator/insights/reviews/{farmStayId}/history/{reportId}`

用途：
- 删除某条历史记录
- 控制无意义历史报告积累

### 6.6 获取问题主题列表
`GET /api/operator/insights/reviews/{farmStayId}/issues`

用途：
- 页面只展示问题主题卡片时可直接调用
- 数据实际来自最新报告的 `issues`

## 7. 返回结构

### 7.1 报告对象
`OperatorInsightReportResponse`

字段说明：
- `reportId`：报告 ID
- `farmStayId`：民宿 ID
- `periodDays`：分析周期
- `reviewCount`：本次分析的评论数
- `averageRating`：平均评分
- `summary`：AI 总结
- `generatedAt`：生成时间
- `generationMode`：`llm` 或 `fallback`
- `model`：模型名，如 `MiniMax-M2.5`
- `issues`：问题主题列表
- `actions`：改进建议列表

前端建议：
- 若 `generationMode=fallback`，建议展示“当前为降级结果”

### 7.2 问题对象
`OperatorInsightIssueResponse`

字段说明：
- `topic`：问题主题
- `priority`：优先级，`P1/P2/P3`
- `issueCount`：相关问题数
- `negativeRatio`：负向占比
- `impactScore`：影响分
- `evidenceCount`：证据条数
- `evidenceSamples`：证据样本

### 7.3 建议对象
`OperatorInsightActionResponse`

字段说明：
- `title`：建议标题
- `topic`：对应问题主题
- `action`：具体执行动作
- `level`：建议周期，如 `short_term`、`mid_term`
- `expectedBenefit`：预期收益
- `owner`：建议负责角色
- `reason`：建议原因

## 8. 响应示例
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
    "summary": "近30天差评主要集中在卫生和早餐体验，建议先做客房复检和早餐标准化。",
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
      }
    ],
    "actions": [
      {
        "title": "客房清洁复检专项",
        "topic": "卫生",
        "action": "建立退房后与入住前双重复检流程，高峰期增加主管抽检。",
        "level": "short_term",
        "expectedBenefit": "预计2到4周内卫生相关差评率下降10%到20%",
        "owner": "运营主管",
        "reason": "卫生主题影响分最高，且证据样本集中。"
      }
    ]
  }
}
```

## 9. 历史报告存储策略
当前实现采用“数据库主存 + 内存缓存”：

- 生成报告时：
  - 先生成 DTO
  - 再落库
  - 再写入缓存

- 查询最新或历史时：
  - 优先查缓存
  - 缓存未命中再查数据库

- 删除历史时：
  - 先删数据库记录
  - 再删缓存

当前表：
- `operator_insight_report`

主存内容：
- 报告完整 JSON
- 摘要
- 模型信息
- 生成模式
- 评论数和评分

## 10. 前端对接建议
推荐页面加载顺序：
1. 调 `GET /{farmStayId}` 获取最新报告
2. 调 `GET /{farmStayId}/history` 渲染历史列表
3. 点击历史卡片后调 `GET /history/{reportId}` 拉详情
4. 点击重新生成时调 `POST /generate`
5. 点击删除历史时调 `DELETE /history/{reportId}`

推荐页面展示：
- 顶部：总评摘要、评分、评论数、生成时间
- 中部：问题主题卡片
- 下部：行动建议卡片
- 侧边：历史报告列表

## 11. 降级与异常
当前已实现以下降级：

### 11.1 AI 关闭
行为：
- 不调模型
- 直接返回规则分析结果
- `generationMode=fallback`

### 11.2 模型调用失败
行为：
- 返回规则分析结果
- `generationMode=fallback`

### 11.3 模型返回非纯 JSON
行为：
- 后端先清洗和抽取 JSON
- 抽取成功则继续解析
- 抽取失败才降级

## 12. 当前联调结论
当前运营洞察能力已经具备：
- AI 生成
- 结构化问题和建议
- 输出清洗
- 历史详情
- 历史删除
- 报告落库

前端联调时重点关注：
- `generationMode`
- `model`
- `issues[].evidenceSamples`
- `actions[].title / owner / reason`
