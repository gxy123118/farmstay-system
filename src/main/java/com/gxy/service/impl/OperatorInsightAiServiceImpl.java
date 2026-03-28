package com.gxy.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gxy.config.OperatorInsightAiProperties;
import com.gxy.model.dto.ai.OperatorInsightAnalysisContext;
import com.gxy.model.dto.ai.OperatorInsightAiResult;
import com.gxy.service.OperatorInsightAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class OperatorInsightAiServiceImpl implements OperatorInsightAiService {

    private static final String SYSTEM_PROMPT = """
            You are an operations insight assistant for a farm stay platform.
            Your job is to read structured business data and produce actionable operator suggestions.
            Follow these rules strictly:
            1. Output JSON only. Do not output Markdown, explanations, or code fences.
            2. Do not invent facts that are not supported by the input.
            3. Do not include personal data such as phone numbers or names.
            4. Keep summary within 80 Chinese characters or a similarly short length.
            5. Return at most 5 issues and at most 3 actions.
            6. priority must be one of P1, P2, P3.
            7. level must be one of short_term, mid_term, long_term.
            8. Every issue should be evidence-based and every action should be executable.
            Return JSON with this schema:
            {
              "summary": "string",
              "issues": [
                {
                  "topic": "string",
                  "priority": "P1|P2|P3",
                  "issueCount": 0,
                  "negativeRatio": 0.0,
                  "impactScore": 0,
                  "evidenceCount": 0,
                  "evidenceSamples": ["string"]
                }
              ],
              "actions": [
                {
                  "title": "string",
                  "topic": "string",
                  "action": "string",
                  "level": "short_term|mid_term|long_term",
                  "expectedBenefit": "string",
                  "owner": "string",
                  "reason": "string"
                }
              ]
            }
            """;

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectMapper objectMapper;
    private final OperatorInsightAiProperties properties;

    public OperatorInsightAiServiceImpl(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                        ObjectMapper objectMapper,
                                        OperatorInsightAiProperties properties) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 调用大模型生成运营洞察结构化结果。
     * 若模型未启用或客户端不可用，会直接抛异常并由上层走降级逻辑。
     */
    @Override
    public OperatorInsightAiResult analyze(OperatorInsightAnalysisContext context) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Operator insight AI is disabled");
        }

        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("ChatClient.Builder is not available");
        }

        log.info("Operator insight AI request started. model={}, farmStayId={}, reviewCount={}",
                properties.getModel(), context.getFarmStayId(), context.getReviewCount());

        String response = builder.build()
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(buildUserPrompt(context))
                .call()
                .content();

        if (!StringUtils.hasText(response)) {
            throw new IllegalStateException("Operator insight AI returned empty content");
        }

        log.info("Operator insight AI response received. model={}, farmStayId={}, contentLength={}",
                properties.getModel(), context.getFarmStayId(), response.length());

        String json = extractJson(response);
        log.info("Operator insight AI JSON extracted. model={}, farmStayId={}, extractedLength={}",
                properties.getModel(), context.getFarmStayId(), json.length());
        try {
            return objectMapper.readValue(json, OperatorInsightAiResult.class);
        } catch (Exception ex) {
            log.warn("Failed to parse operator insight AI response: {}", json, ex);
            throw new IllegalStateException("Operator insight AI returned invalid JSON", ex);
        }
    }

    private String buildUserPrompt(OperatorInsightAnalysisContext context) {
        try {
            return """
                    Generate operator insights in Chinese based on the input below.
                    The output must be valid JSON only.
                    Model label: %s
                    Input data:
                    %s
                    """.formatted(
                    properties.getModel(),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context)
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build operator insight AI prompt", ex);
        }
    }

    private String extractJson(String response) {
        String sanitized = sanitizeJson(response);
        int start = sanitized.indexOf('{');
        if (start < 0) {
            throw new IllegalStateException("Operator insight AI response does not contain JSON object");
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < sanitized.length(); i++) {
            char ch = sanitized.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return sanitized.substring(start, i + 1).trim();
                }
            }
        }
        throw new IllegalStateException("Operator insight AI response contains incomplete JSON object");
    }

    private String sanitizeJson(String response) {
        String trimmed = response.trim();
        trimmed = trimmed.replaceAll("(?is)<think>.*?</think>", "").trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            if (firstLineBreak >= 0) {
                trimmed = trimmed.substring(firstLineBreak + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}
