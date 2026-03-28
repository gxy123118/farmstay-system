package com.gxy.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gxy.model.dto.ai.AiQuestionAnalysisResult;
import com.gxy.model.dto.ai.AiQuestionSlots;
import com.gxy.service.AiQuestionAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuestionAnalysisServiceImpl implements AiQuestionAnalysisService {

    public static final String INTENT_FAQ = "faq";
    public static final String INTENT_CAPABILITY = "capability";
    public static final String INTENT_RECOMMENDATION = "recommendation";
    public static final String INTENT_CHITCHAT = "chitchat";
    private static final Set<String> GENERIC_RECOMMENDATION_WORDS = Set.of(
            "农家乐", "民宿", "推荐", "项目", "项目推荐", "农家乐推荐", "民宿推荐", "住宿", "酒店", "城市", "地方", "周边"
    );

    private static final String ANALYSIS_SYSTEM_PROMPT = """
            You are an intent classifier for a farm-stay AI assistant.
            Your job is to analyze the user's latest question and return JSON only.
            Allowed intents are: faq, capability, recommendation, chitchat.
            Rules:
            1. faq: specific questions about refund, cancellation, payment, room, check-in, breakfast, parking, activity, coupon, booking rules.
            2. capability: asking what the assistant can do.
            3. recommendation: asking for suggestions, filtering, choosing, comparing, or recommending a farm stay / activity / trip plan.
            4. chitchat: greetings, small talk, unclear social text not requiring knowledge retrieval.
            5. For faq, rewrite faqQuery into a concise canonical query for retrieval.
            6. For recommendation, extract slots when possible: city, travelGroup, budgetMin, budgetMax, preferences, topic, timeRange.
            7. For recommendation, if information is not enough to give a useful recommendation, set needsClarification=true and provide a short clarificationQuestion in Chinese.
            8. Return strict JSON only. No markdown, no code block, no explanation, no <think>.
            JSON schema:
            {
              "intent": "faq|capability|recommendation|chitchat",
              "faqQuery": "string or empty",
              "needsClarification": true,
              "clarificationQuestion": "string or empty",
              "slots": {
                "city": "string or empty",
                "travelGroup": "string or empty",
                "budgetMin": 0,
                "budgetMax": 0,
                "preferences": ["string"],
                "topic": "string or empty",
                "timeRange": "string or empty"
              }
            }
            """;

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectMapper objectMapper;

    @Override
    public AiQuestionAnalysisResult analyze(Long farmStayId, String scene, String question, List<String> recentMessages) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return fallbackAnalysis(question);
        }

        try {
            String content = builder.build()
                    .prompt()
                    .system(ANALYSIS_SYSTEM_PROMPT)
                    .user(buildAnalysisPrompt(farmStayId, scene, question, recentMessages))
                    .call()
                    .content();
            log.info("AiChat analysis raw output. question={}, raw={}", question, sanitize(content));
            String json = extractJson(sanitize(content));
            log.info("AiChat analysis extracted JSON. question={}, json={}", question, json);
            AiQuestionAnalysisResult result = objectMapper.readValue(json, AiQuestionAnalysisResult.class);
            normalize(result, question);
            log.info("AiChat question analyzed. question={}, intent={}, needsClarification={}, normalized={}",
                    question, result.getIntent(), result.getNeedsClarification(), objectMapper.writeValueAsString(result));
            return result;
        } catch (Exception ex) {
            log.warn("AiChat question analysis failed, using local fallback. question={}", question, ex);
            return fallbackAnalysis(question);
        }
    }

    private String buildAnalysisPrompt(Long farmStayId, String scene, String question, List<String> recentMessages) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("farmStayId=").append(farmStayId == null ? "" : farmStayId).append('\n');
        prompt.append("scene=").append(scene == null ? "" : scene).append('\n');
        prompt.append("question=").append(question == null ? "" : question).append('\n');
        prompt.append("recentMessages:\n");
        if (recentMessages == null || recentMessages.isEmpty()) {
            prompt.append("(empty)\n");
        } else {
            int start = Math.max(0, recentMessages.size() - 4);
            for (int i = start; i < recentMessages.size(); i++) {
                prompt.append("- ").append(recentMessages.get(i)).append('\n');
            }
        }
        return prompt.toString();
    }

    private void normalize(AiQuestionAnalysisResult result, String originalQuestion) {
        if (result == null) {
            throw new IllegalStateException("analysis result is null");
        }
        String intent = safe(result.getIntent()).toLowerCase(Locale.ROOT);
        if (!List.of(INTENT_FAQ, INTENT_CAPABILITY, INTENT_RECOMMENDATION, INTENT_CHITCHAT).contains(intent)) {
            intent = INTENT_FAQ;
        }
        result.setIntent(intent);
        if (!StringUtils.hasText(result.getFaqQuery()) && INTENT_FAQ.equals(intent)) {
            result.setFaqQuery(originalQuestion);
        }
        if (result.getNeedsClarification() == null) {
            result.setNeedsClarification(false);
        }
        if (result.getSlots() == null) {
            result.setSlots(new AiQuestionSlots());
        }
        normalizeSlots(result.getSlots());
    }

    private void normalizeSlots(AiQuestionSlots slots) {
        if (slots.getPreferences() == null) {
            slots.setPreferences(new ArrayList<>());
        }
        if (slots.getBudgetMin() != null && slots.getBudgetMin() <= 0) {
            slots.setBudgetMin(null);
        }
        if (slots.getBudgetMax() != null && slots.getBudgetMax() <= 0) {
            slots.setBudgetMax(null);
        }
        slots.setTopic(cleanSlotValue(slots.getTopic()));
        slots.setTravelGroup(cleanSlotValue(slots.getTravelGroup()));

        Set<String> cleanedPreferences = new LinkedHashSet<>();
        for (String preference : slots.getPreferences()) {
            String cleaned = cleanSlotValue(preference);
            if (StringUtils.hasText(cleaned)) {
                cleanedPreferences.add(cleaned);
            }
        }
        slots.setPreferences(new ArrayList<>(cleanedPreferences));
    }

    private String cleanSlotValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String cleaned = value.trim().replace("类", "").replace("型", "");
        if (GENERIC_RECOMMENDATION_WORDS.contains(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private AiQuestionAnalysisResult fallbackAnalysis(String question) {
        String normalized = safe(question).toLowerCase(Locale.ROOT);
        AiQuestionAnalysisResult result = new AiQuestionAnalysisResult();
        result.setSlots(new AiQuestionSlots());
        if (normalized.contains("推荐") || normalized.contains("适合") || normalized.contains("帮我选")) {
            result.setIntent(INTENT_RECOMMENDATION);
            result.setNeedsClarification(true);
            result.setClarificationQuestion("可以，我可以按预算、同行人群、偏好和城市帮你推荐。你更看重哪一项？");
            return result;
        }
        if (normalized.contains("能帮我") || normalized.contains("你会什么") || normalized.contains("可以做什么")) {
            result.setIntent(INTENT_CAPABILITY);
            result.setNeedsClarification(false);
            return result;
        }
        if (normalized.contains("你好") || normalized.contains("在吗") || normalized.contains("hello")) {
            result.setIntent(INTENT_CHITCHAT);
            result.setNeedsClarification(false);
            return result;
        }
        result.setIntent(INTENT_FAQ);
        result.setFaqQuery(question);
        result.setNeedsClarification(false);
        return result;
    }

    private String sanitize(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String sanitized = content.replace("\uFEFF", "");
        sanitized = sanitized.replaceAll("(?is)<think>.*?</think>", "");
        sanitized = sanitized.replace("```json", "").replace("```", "");
        return sanitized.trim();
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        if (start < 0) {
            throw new IllegalStateException("analysis JSON start not found");
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char current = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        throw new IllegalStateException("analysis JSON end not found");
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}
