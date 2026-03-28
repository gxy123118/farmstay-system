package com.gxy.service.impl;

import com.gxy.model.dto.AiCitationResponse;
import com.gxy.model.dto.ai.AiQuestionAnalysisResult;
import com.gxy.model.dto.ai.AiQuestionSlots;
import com.gxy.service.AiPromptBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AiPromptBuilderImpl implements AiPromptBuilder {

    @Override
    public String buildChatPrompt(Long farmStayId,
                                  String scene,
                                  String question,
                                  List<String> recentMessages,
                                  List<AiCitationResponse> citations,
                                  AiQuestionAnalysisResult analysis) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("当前场景: ").append(scene == null ? "" : scene).append('\n');
        prompt.append("民宿ID: ").append(farmStayId == null ? "" : farmStayId).append('\n');
        prompt.append("用户原始问题: ").append(question == null ? "" : question).append('\n');
        prompt.append("问题意图: ").append(analysis == null ? "" : safe(analysis.getIntent())).append('\n');
        if (analysis != null && StringUtils.hasText(analysis.getFaqQuery())) {
            prompt.append("FAQ 检索改写: ").append(analysis.getFaqQuery()).append('\n');
        }
        if (analysis != null && analysis.getSlots() != null) {
            appendSlots(prompt, analysis.getSlots());
        }
        prompt.append("最近会话:\n");
        if (recentMessages == null || recentMessages.isEmpty()) {
            prompt.append("(无)\n");
        } else {
            for (String message : recentMessages) {
                prompt.append("- ").append(message).append('\n');
            }
        }
        prompt.append("可用知识片段:\n");
        for (int i = 0; i < citations.size(); i++) {
            AiCitationResponse citation = citations.get(i);
            prompt.append(i + 1)
                    .append(". [")
                    .append(citation.getSourceType())
                    .append('#')
                    .append(citation.getSourceId())
                    .append("] ")
                    .append(citation.getSnippet())
                    .append('\n');
        }
        prompt.append('\n');
        prompt.append(buildInstruction(analysis));
        return prompt.toString();
    }

    private void appendSlots(StringBuilder prompt, AiQuestionSlots slots) {
        prompt.append("提取到的推荐条件:\n");
        prompt.append("- 城市: ").append(safe(slots.getCity())).append('\n');
        prompt.append("- 出行人群: ").append(safe(slots.getTravelGroup())).append('\n');
        prompt.append("- 最低预算: ").append(slots.getBudgetMin() == null ? "" : slots.getBudgetMin()).append('\n');
        prompt.append("- 最高预算: ").append(slots.getBudgetMax() == null ? "" : slots.getBudgetMax()).append('\n');
        prompt.append("- 偏好: ").append(slots.getPreferences() == null ? "" : String.join("、", slots.getPreferences())).append('\n');
        prompt.append("- 主题: ").append(safe(slots.getTopic())).append('\n');
        prompt.append("- 时间范围: ").append(safe(slots.getTimeRange())).append('\n');
    }

    private String buildInstruction(AiQuestionAnalysisResult analysis) {
        String intent = analysis == null ? "" : safe(analysis.getIntent());
        if (AiQuestionAnalysisServiceImpl.INTENT_RECOMMENDATION.equals(intent)) {
            return """
                    请根据知识片段给出中文推荐建议：
                    1. 先直接给出推荐结论，再解释推荐理由。
                    2. 优先引用价格、人群、偏好、房型、活动、餐饮、优惠等信息。
                    3. 如果知识不足，不要编造，明确说明信息有限，并引导用户补充条件。
                    4. 不要输出 JSON、Markdown 代码块、<think> 或推理过程。
                    """;
        }
        return """
                请基于以上知识片段回答用户问题：
                1. 必须使用中文。
                2. 优先引用已提供的知识，不要编造未出现的事实。
                3. 若知识不足，可以明确说明信息有限，并建议联系人工客服或补充条件。
                4. 不要输出 JSON、Markdown 代码块、<think> 或推理过程。
                """;
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}
