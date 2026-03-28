package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gxy.common.auth.AuthGuard;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.AiChatMessageMapper;
import com.gxy.mapper.AiChatSessionMapper;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.mapper.RoomTypeMapper;
import com.gxy.model.dto.AiChatFeedbackRequest;
import com.gxy.model.dto.AiChatMessageRequest;
import com.gxy.model.dto.AiChatMessageResponse;
import com.gxy.model.dto.AiChatSessionCreateRequest;
import com.gxy.model.dto.AiChatSessionResponse;
import com.gxy.model.dto.AiChatSessionUpdateRequest;
import com.gxy.model.dto.AiChatStreamEventResponse;
import com.gxy.model.dto.AiCitationResponse;
import com.gxy.model.dto.ai.AiQuestionAnalysisResult;
import com.gxy.model.dto.ai.AiQuestionSlots;
import com.gxy.model.entity.AiChatMessageRecord;
import com.gxy.model.entity.AiChatSessionRecord;
import com.gxy.model.entity.FarmStay;
import com.gxy.model.entity.RoomType;
import com.gxy.service.AiChatService;
import com.gxy.service.AiKnowledgeRetriever;
import com.gxy.service.AiPromptBuilder;
import com.gxy.service.AiQuestionAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private static final String SESSION_KEY_USER_TYPE = "userType";
    private static final String DEFAULT_SESSION_TITLE = "新会话";
    private static final String MODEL_NAME = "MiniMax-M2.5";
    private static final String FINAL_ANSWER_MARKER = "FINAL_ANSWER:";
    private static final String REASON_KNOWLEDGE_MISS = "KNOWLEDGE_MISS";
    private static final String REASON_MODEL_UNAVAILABLE = "MODEL_UNAVAILABLE";
    private static final String REASON_MODEL_ERROR = "MODEL_ERROR";
    private static final String REASON_STREAM_INTERRUPTED = "STREAM_INTERRUPTED";
    private static final Set<String> SUPPORTED_FARMSTAY_TAGS = Set.of(
            "亲子", "情侣", "团建", "家庭", "观景", "竹海", "下午茶", "安静", "度假", "露营"
    );
    private static final TypeReference<List<AiCitationResponse>> CITATION_LIST_TYPE = new TypeReference<>() {
    };

    private static final String SYSTEM_PROMPT = """
            You are a customer-service assistant for a farm stay platform.
            Follow these rules strictly:
            1. Answer in Chinese.
            2. Prefer the provided knowledge snippets and do not invent unsupported facts.
            3. If the knowledge is insufficient, say so directly and suggest contacting human support or asking a more specific question.
            4. Do not reveal personal data such as phone numbers or real names.
            5. Do not output JSON or Markdown code blocks.
            6. Do not output <think> tags or reasoning traces.
            """;

    private final AiKnowledgeRetriever aiKnowledgeRetriever;
    private final AiPromptBuilder aiPromptBuilder;
    private final AiQuestionAnalysisService aiQuestionAnalysisService;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final AiChatSessionMapper aiChatSessionMapper;
    private final AiChatMessageMapper aiChatMessageMapper;
    private final FarmStayMapper farmStayMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<AiChatSessionResponse> listSessions() {
        AuthGuard.enforceAtLeastVisitor();
        Long userId = StpUtil.getLoginIdAsLong();
        List<AiChatSessionRecord> sessions = aiChatSessionMapper.selectByUserId(userId);
        List<AiChatSessionResponse> responses = new ArrayList<>();
        for (AiChatSessionRecord session : sessions) {
            responses.add(toSessionResponse(session));
        }
        return responses;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatSessionResponse createSession(AiChatSessionCreateRequest request) {
        AuthGuard.enforceAtLeastVisitor();
        Long userId = StpUtil.getLoginIdAsLong();
        AiChatSessionRecord session = new AiChatSessionRecord();
        session.setUserId(userId);
        session.setFarmStayId(request == null ? null : request.getFarmStayId());
        session.setScene(request == null ? null : request.getScene());
        session.setTitle(DEFAULT_SESSION_TITLE);
        if (aiChatSessionMapper.insert(session) <= 0) {
            throw new BusinessException("创建会话失败");
        }
        return toSessionResponse(requireSession(session.getId()));
    }

    @Override
    public AiChatSessionResponse getSession(Long sessionId) {
        AuthGuard.enforceAtLeastVisitor();
        AiChatSessionRecord session = requireSession(sessionId);
        ensureOwner(session);
        return toSessionResponse(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatSessionResponse updateSession(Long sessionId, AiChatSessionUpdateRequest request) {
        AuthGuard.enforceAtLeastVisitor();
        AiChatSessionRecord session = requireSession(sessionId);
        ensureOwner(session);
        String title = request.getTitle().trim();
        if (aiChatSessionMapper.updateTitle(sessionId, title) <= 0) {
            throw new BusinessException("更新会话失败");
        }
        return toSessionResponse(requireSession(sessionId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatMessageResponse sendMessage(Long sessionId, AiChatMessageRequest request) {
        AuthGuard.enforceAtLeastVisitor();
        AiChatSessionRecord session = requireSession(sessionId);
        ensureOwner(session);

        AiChatMessageRecord userMessage = saveUserMessage(session, request.getQuestion());
        refreshSessionMeta(session, request.getQuestion());
        Answer answer = generateAnswer(session, request.getQuestion());
        AiChatMessageRecord assistantMessage = saveAssistantMessage(session.getId(), answer.content, answer.citations,
                answer.confidence, answer.refuseReason, answer.fallback);

        log.info("AiChat message generated. sessionId={}, userMessageId={}, assistantMessageId={}, fallback={}, reason={}",
                session.getId(), userMessage.getId(), assistantMessage.getId(), answer.fallback, answer.refuseReason);
        return toMessageResponse(assistantMessage);
    }

    @Override
    public Flux<ServerSentEvent<AiChatStreamEventResponse>> streamMessage(Long sessionId, AiChatMessageRequest request) {
        AuthGuard.enforceAtLeastVisitor();
        AiChatSessionRecord session = requireSession(sessionId);
        ensureOwner(session);

        saveUserMessage(session, request.getQuestion());
        refreshSessionMeta(session, request.getQuestion());
        PreparedAnswer prepared = prepareAnswer(session, request.getQuestion());
        AiChatMessageRecord assistantRecord = createAssistantPlaceholder(session.getId());

        if (prepared.directAnswer != null) {
            Answer answer = prepared.directAnswer;
            persistAssistantMessage(assistantRecord.getId(), answer.content, answer.citations,
                    answer.confidence, answer.refuseReason, answer.fallback);
            return Flux.concat(
                    Flux.just(sse("meta", metaPayload(session.getId(), assistantRecord.getId(), answer.fallback))),
                    buildChunkEvents(session.getId(), assistantRecord.getId(), answer.content, answer.fallback),
                    Flux.just(sse("done", donePayload(session.getId(), assistantRecord.getId(), answer.fallback)))
            );
        }

        StringBuilder rawContentBuffer = new StringBuilder();
        StreamingContentFilter streamFilter = new StreamingContentFilter();
        Flux<ServerSentEvent<AiChatStreamEventResponse>> metaEvent = Flux.just(
                sse("meta", metaPayload(session.getId(), assistantRecord.getId(), false))
        );

        Flux<ServerSentEvent<AiChatStreamEventResponse>> modelChunkEvents = prepared.builder.build()
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(prepared.prompt)
                .stream()
                .content()
                .filter(StringUtils::hasText)
                .doOnSubscribe(ignored -> log.info(
                        "AiChat MiniMax stream started. sessionId={}, farmStayId={}, intent={}, citationCount={}",
                        session.getId(), session.getFarmStayId(), prepared.analysis.getIntent(), prepared.citations.size()))
                .doOnNext(rawContentBuffer::append)
                .map(streamFilter::append)
                .filter(StringUtils::hasText)
                .map(chunk -> sse("chunk", chunkPayload(session.getId(), assistantRecord.getId(), chunk, false)));

        Flux<ServerSentEvent<AiChatStreamEventResponse>> completionEvents = Flux.defer(() -> {
            String visibleTail = streamFilter.finish();
            String content = sanitizeModelContent(rawContentBuffer.toString(), prepared.analysis);
            content = enrichRecommendationContent(content, prepared.analysis);
            if (!StringUtils.hasText(content)) {
                log.warn("AiChat MiniMax stream ended with empty content. sessionId={}", session.getId());
                return Flux.just(sse("error", errorPayload(session.getId(), assistantRecord.getId(), "AI 未返回有效内容")));
            }

            persistAssistantMessage(assistantRecord.getId(), content, prepared.citations, 0.86, null, false);
            log.info("AiChat MiniMax stream completed. sessionId={}, assistantMessageId={}, contentLength={}",
                    session.getId(), assistantRecord.getId(), content.length());

            return Flux.concat(
                    StringUtils.hasText(visibleTail)
                            ? Flux.just(sse("chunk", chunkPayload(session.getId(), assistantRecord.getId(), visibleTail, false)))
                            : Flux.empty(),
                    Flux.just(sse("done", donePayload(session.getId(), assistantRecord.getId(), false)))
            );
        });

        return Flux.concat(metaEvent, modelChunkEvents, completionEvents)
                .onErrorResume(ex -> {
                    log.warn("AiChat MiniMax stream failed. sessionId={}", session.getId(), ex);
                    String partialContent = sanitizeModelContent(rawContentBuffer.toString(), prepared.analysis);
                    if (StringUtils.hasText(partialContent)) {
                        persistAssistantMessage(assistantRecord.getId(), partialContent, prepared.citations, 0.55,
                                REASON_STREAM_INTERRUPTED, true);
                    }
                    return Flux.just(sse("error", errorPayload(session.getId(), assistantRecord.getId(), "AI 流式生成失败，请稍后重试")));
                });
    }

    @Override
    public List<AiChatMessageResponse> listMessages(Long sessionId) {
        AuthGuard.enforceAtLeastVisitor();
        AiChatSessionRecord session = requireSession(sessionId);
        ensureOwner(session);
        List<AiChatMessageRecord> messages = aiChatMessageMapper.selectBySessionId(sessionId);
        List<AiChatMessageResponse> responses = new ArrayList<>();
        for (AiChatMessageRecord message : messages) {
            responses.add(toMessageResponse(message));
        }
        return responses;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean feedback(AiChatFeedbackRequest request) {
        AuthGuard.enforceAtLeastVisitor();
        AiChatSessionRecord session = requireSession(request.getSessionId());
        ensureOwner(session);
        AiChatMessageRecord message = aiChatMessageMapper.selectById(request.getMessageId());
        if (message == null || !request.getSessionId().equals(message.getSessionId())) {
            throw new BusinessException("消息不存在");
        }
        if (aiChatMessageMapper.updateFeedback(message.getId(), request.getUseful(), request.getComment()) <= 0) {
            throw new BusinessException("消息反馈失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId) {
        AuthGuard.enforceAtLeastVisitor();
        AiChatSessionRecord session = requireSession(sessionId);
        ensureOwner(session);
        aiChatMessageMapper.deleteBySessionId(sessionId);
        if (aiChatSessionMapper.deleteById(sessionId) <= 0) {
            throw new BusinessException("删除会话失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearSessions() {
        AuthGuard.enforceAtLeastVisitor();
        Long userId = StpUtil.getLoginIdAsLong();
        aiChatMessageMapper.deleteByUserId(userId);
        aiChatSessionMapper.deleteByUserId(userId);
    }

    private Answer generateAnswer(AiChatSessionRecord session, String question) {
        PreparedAnswer prepared = prepareAnswer(session, question);
        if (prepared.directAnswer != null) {
            return prepared.directAnswer;
        }

        try {
            log.info("AiChat MiniMax request started. sessionId={}, farmStayId={}, intent={}, citationCount={}",
                    session.getId(), session.getFarmStayId(), prepared.analysis.getIntent(), prepared.citations.size());
            String content = prepared.builder.build()
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(prepared.prompt)
                    .call()
                    .content();
            content = sanitizeModelContent(content, prepared.analysis);
            content = enrichRecommendationContent(content, prepared.analysis);
            if (!StringUtils.hasText(content)) {
                log.warn("AiChat fallback because MiniMax returned empty content. sessionId={}", session.getId());
                return fallbackFaqAnswer(question, prepared.citations, REASON_MODEL_ERROR);
            }
            log.info("AiChat MiniMax response received. sessionId={}, contentLength={}", session.getId(), content.length());
            return new Answer(content.trim(), prepared.citations, 0.86, null, false);
        } catch (Exception ex) {
            log.warn("MiniMax chat generation failed. sessionId={}", session.getId(), ex);
            return fallbackFaqAnswer(question, prepared.citations, REASON_MODEL_ERROR);
        }
    }

    private PreparedAnswer prepareAnswer(AiChatSessionRecord session, String question) {
        boolean operator = "operator".equalsIgnoreCase(String.valueOf(StpUtil.getSession().get(SESSION_KEY_USER_TYPE)));
        List<String> recentMessages = buildRecentMessages(session.getId());
        Long effectiveFarmStayId = resolveEffectiveFarmStayId(session, question);
        AiQuestionAnalysisResult analysis = aiQuestionAnalysisService.analyze(session.getFarmStayId(),
                session.getScene(), question, recentMessages);
        String intent = safe(analysis.getIntent()).toLowerCase(Locale.ROOT);

        return switch (intent) {
            case AiQuestionAnalysisServiceImpl.INTENT_CAPABILITY -> PreparedAnswer.direct(
                    capabilityAnswer(), analysis, Collections.emptyList());
            case AiQuestionAnalysisServiceImpl.INTENT_CHITCHAT -> prepareChitchatAnswer(session, question, analysis);
            case AiQuestionAnalysisServiceImpl.INTENT_RECOMMENDATION -> prepareRecommendationAnswer(session, question, recentMessages, analysis, operator, effectiveFarmStayId);
            case AiQuestionAnalysisServiceImpl.INTENT_FAQ -> prepareFaqAnswer(session, question, recentMessages, analysis, operator, effectiveFarmStayId);
            default -> prepareFaqAnswer(session, question, recentMessages, analysis, operator, effectiveFarmStayId);
        };
    }

    private PreparedAnswer prepareFaqAnswer(AiChatSessionRecord session,
                                            String question,
                                            List<String> recentMessages,
                                            AiQuestionAnalysisResult analysis,
                                            boolean operator,
                                            Long effectiveFarmStayId) {
        String retrievalQuestion = StringUtils.hasText(analysis.getFaqQuery()) ? analysis.getFaqQuery() : question;
        List<AiCitationResponse> citations = aiKnowledgeRetriever.retrieve(effectiveFarmStayId,
                session.getScene(), retrievalQuestion, operator);
        if (citations.isEmpty()) {
            log.info("AiChat FAQ retrieval missed. sessionId={}, query={}", session.getId(), retrievalQuestion);
            return PreparedAnswer.direct(fallbackFaqAnswer(question, citations, REASON_KNOWLEDGE_MISS), analysis, citations);
        }

        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            log.warn("AiChat fallback because ChatClient.Builder is not available. sessionId={}", session.getId());
            return PreparedAnswer.direct(fallbackFaqAnswer(question, citations, REASON_MODEL_UNAVAILABLE), analysis, citations);
        }

        String prompt = aiPromptBuilder.buildChatPrompt(effectiveFarmStayId, session.getScene(), question,
                recentMessages, citations, analysis);
        return PreparedAnswer.model(builder, prompt, citations, analysis);
    }

    private PreparedAnswer prepareRecommendationAnswer(AiChatSessionRecord session,
                                                       String question,
                                                       List<String> recentMessages,
                                                       AiQuestionAnalysisResult analysis,
                                                       boolean operator,
                                                       Long effectiveFarmStayId) {
        if (shouldClarifyRecommendation(session, analysis)) {
            return PreparedAnswer.direct(clarificationAnswer(session.getFarmStayId(), analysis), analysis, Collections.emptyList());
        }

        String retrievalQuestion = buildRecommendationRetrievalQuery(question, analysis);
        log.info("AiChat recommendation retrieval query. sessionId={}, query={}, slots={}",
                session.getId(), retrievalQuestion, summarizeSlots(analysis.getSlots()));
        List<AiCitationResponse> citations = aiKnowledgeRetriever.retrieve(effectiveFarmStayId,
                session.getScene(), retrievalQuestion, operator);
        if (citations.isEmpty()) {
            citations = buildRecommendationBusinessCitations(session, analysis, effectiveFarmStayId);
            log.info("AiChat recommendation business fallback retrieval. sessionId={}, citationCount={}",
                    session.getId(), citations.size());
        }
        if (citations.isEmpty()) {
            return PreparedAnswer.direct(recommendationGuidanceAnswer(session.getFarmStayId(), analysis), analysis, citations);
        }

        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return PreparedAnswer.direct(recommendationGuidanceAnswer(session.getFarmStayId(), analysis), analysis, citations);
        }

        String prompt = aiPromptBuilder.buildChatPrompt(effectiveFarmStayId, session.getScene(), question,
                recentMessages, citations, analysis);
        return PreparedAnswer.model(builder, prompt, citations, analysis);
    }

    private PreparedAnswer prepareChitchatAnswer(AiChatSessionRecord session,
                                                 String question,
                                                 AiQuestionAnalysisResult analysis) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return PreparedAnswer.direct(chitchatFallbackAnswer(question), analysis, Collections.emptyList());
        }
        String prompt = """
                当前场景: %s
                用户问题: %s
                
                请直接回答用户问题，并严格遵守：
                要求：
                1. 用中文。
                2. 对简单数学、常见常识、简短闲聊可以直接回答。
                3. 不要编造高风险专业结论。
                4. 如果问题和农家乐业务有关，可以在回答末尾轻度引导用户继续提问业务问题。
                5. 不要输出 JSON、Markdown 代码块、<think>、分析过程、推理过程。
                6. 你的回复必须以 %s 开头，并且只输出最终答案，不要输出其他前缀。
                """.formatted(safe(session.getScene()), safe(question), FINAL_ANSWER_MARKER);
        return PreparedAnswer.model(builder, prompt, Collections.emptyList(), analysis);
    }

    private boolean shouldClarifyRecommendation(AiChatSessionRecord session, AiQuestionAnalysisResult analysis) {
        AiQuestionSlots slots = analysis.getSlots();
        boolean hasUsefulSlots = session.getFarmStayId() != null || (slots != null && (
                StringUtils.hasText(slots.getCity())
                        || StringUtils.hasText(slots.getTravelGroup())
                        || StringUtils.hasText(slots.getTopic())
                        || StringUtils.hasText(slots.getTimeRange())
                        || slots.getBudgetMin() != null
                        || slots.getBudgetMax() != null
                        || (slots.getPreferences() != null && !slots.getPreferences().isEmpty())
        ));
        if (!hasUsefulSlots) {
            return true;
        }
        return false;
    }

    private String buildRecommendationRetrievalQuery(String question, AiQuestionAnalysisResult analysis) {
        StringBuilder query = new StringBuilder();
        query.append(question == null ? "" : question);
        AiQuestionSlots slots = analysis.getSlots();
        if (slots != null) {
            if (StringUtils.hasText(slots.getTravelGroup())) {
                query.append(' ').append(slots.getTravelGroup());
            }
            if (StringUtils.hasText(slots.getTopic())) {
                query.append(' ').append(slots.getTopic());
            }
            if (StringUtils.hasText(slots.getCity())) {
                query.append(' ').append(slots.getCity());
            }
            if (StringUtils.hasText(slots.getTimeRange())) {
                query.append(' ').append(slots.getTimeRange());
            }
            if (slots.getPreferences() != null && !slots.getPreferences().isEmpty()) {
                query.append(' ').append(String.join(" ", slots.getPreferences()));
            }
        }
        return query.toString().trim();
    }

    private Answer capabilityAnswer() {
        String content = "我可以帮你解答退款、取消、支付、入住、房型、活动、优惠等问题，也可以按预算、出行人群、偏好和城市帮你做推荐。你可以直接告诉我你的问题，或者说“帮我推荐一个适合亲子的民宿”。";
        return new Answer(content, Collections.emptyList(), 0.92, null, false);
    }

    private Answer chitchatFallbackAnswer(String question) {
        String content = "我可以继续帮你处理农家乐相关问题，比如退款、入住、房型、活动和推荐。如果你愿意，也可以把问题再说具体一点。";
        if (StringUtils.hasText(question) && question.contains("多少")) {
            content = "我当前更适合回答农家乐相关问题。你也可以继续问我退款、入住、房型、活动或推荐相关内容。";
        }
        return new Answer(content, Collections.emptyList(), 0.72, null, false);
    }

    private Answer clarificationAnswer(Long farmStayId, AiQuestionAnalysisResult analysis) {
        String message = analysis.getClarificationQuestion();
        if (!StringUtils.hasText(message)) {
            message = farmStayId == null
                    ? "可以，我可以按城市、预算、同行人群和偏好帮你推荐。你先告诉我想去哪个城市，或者更看重亲子、情侣、观景还是安静？"
                    : "可以，我可以结合当前民宿信息给你建议。你更看重预算、同行人群、安静、观景还是活动体验？";
        }
        return new Answer(message, Collections.emptyList(), 0.88, null, false);
    }

    private Answer recommendationGuidanceAnswer(Long farmStayId, AiQuestionAnalysisResult analysis) {
        StringBuilder content = new StringBuilder("我可以继续帮你做推荐。");
        AiQuestionSlots slots = analysis.getSlots();
        boolean hasCity = slots != null && StringUtils.hasText(slots.getCity());
        boolean hasTravelGroup = slots != null && StringUtils.hasText(slots.getTravelGroup());
        boolean hasPreference = slots != null && slots.getPreferences() != null && !slots.getPreferences().isEmpty();
        boolean hasBudget = slots != null && (slots.getBudgetMin() != null || slots.getBudgetMax() != null);
        if (!hasCity && farmStayId == null) {
            content.append("为了给出更靠谱的建议，请补充一下城市或目标民宿范围。");
        } else if (!hasTravelGroup && !hasPreference && !hasBudget) {
            content.append("我已经拿到基础范围了。为了推荐得更准，请再补充一下预算、同行人群或偏好，例如亲子、情侣、观景、安静。");
        } else {
            content.append("当前范围内暂时没有足够合适的候选结果。你可以再补充预算、同行人群或偏好，我继续帮你缩小范围。");
        }
        if (slots != null && StringUtils.hasText(slots.getCity())) {
            content.append("当前已识别城市：").append(slots.getCity()).append("。");
        }
        if (slots != null && StringUtils.hasText(slots.getTravelGroup())) {
            content.append("我已经记下你关注“").append(slots.getTravelGroup()).append("”这一点。");
        }
        return new Answer(content.toString(), Collections.emptyList(), 0.78, null, false);
    }

    private String buildRecommendationFollowUp(AiQuestionAnalysisResult analysis) {
        AiQuestionSlots slots = analysis.getSlots();
        boolean hasTravelGroup = slots != null && StringUtils.hasText(slots.getTravelGroup());
        boolean hasPreference = slots != null && slots.getPreferences() != null && !slots.getPreferences().isEmpty();
        boolean hasBudget = slots != null && (slots.getBudgetMin() != null || slots.getBudgetMax() != null);
        StringBuilder followUp = new StringBuilder("如果你愿意，我可以继续");
        List<String> missing = new ArrayList<>();
        if (!hasBudget) {
            missing.add("按预算筛一轮");
        }
        if (!hasTravelGroup) {
            missing.add("按同行人群细化");
        }
        if (!hasPreference) {
            missing.add("按偏好再缩小范围");
        }
        if (missing.isEmpty()) {
            followUp.append("根据你补充的细节进一步缩小范围。");
        } else {
            followUp.append(String.join("、", missing)).append("。");
        }
        return followUp.toString();
    }

    private List<AiCitationResponse> buildRecommendationBusinessCitations(AiChatSessionRecord session,
                                                                          AiQuestionAnalysisResult analysis,
                                                                          Long effectiveFarmStayId) {
        List<AiCitationResponse> citations = new ArrayList<>();
        if (effectiveFarmStayId != null) {
            appendSingleFarmStayCitation(effectiveFarmStayId, citations);
            return citations;
        }
        AiQuestionSlots slots = analysis.getSlots();
        String city = slots == null ? null : slots.getCity();
        String tag = resolveFarmStayTag(slots);
        String priceLevel = inferPriceLevel(slots);
        log.info("AiChat recommendation farmstay filters. city={}, tag={}, priceLevel={}, slots={}",
                city, tag, priceLevel, summarizeSlots(slots));

        List<FarmStayQuery> queryPlans = buildRecommendationQueryPlans(city, tag, priceLevel);
        for (FarmStayQuery queryPlan : queryPlans) {
            List<FarmStay> candidates = farmStayMapper.selectPageByConditions(
                    "PUBLISHED", queryPlan.city(), null, queryPlan.priceLevel(), queryPlan.tag(), 0, 3);
            log.info("AiChat recommendation query plan. city={}, tag={}, priceLevel={}, resultCount={}",
                    queryPlan.city(), queryPlan.tag(), queryPlan.priceLevel(), candidates.size());
            for (FarmStay candidate : candidates) {
                appendFarmStayCandidateCitation(candidate, citations);
                if (citations.size() >= 5) {
                    return citations;
                }
            }
            if (!citations.isEmpty()) {
                return citations;
            }
        }
        return citations;
    }

    private List<FarmStayQuery> buildRecommendationQueryPlans(String city, String tag, String priceLevel) {
        List<FarmStayQuery> plans = new ArrayList<>();
        addQueryPlan(plans, city, tag, priceLevel);
        addQueryPlan(plans, city, tag, null);
        addQueryPlan(plans, city, null, priceLevel);
        addQueryPlan(plans, city, null, null);
        addQueryPlan(plans, null, tag, priceLevel);
        addQueryPlan(plans, null, tag, null);
        addQueryPlan(plans, null, null, priceLevel);
        addQueryPlan(plans, null, null, null);
        return plans;
    }

    private void addQueryPlan(List<FarmStayQuery> plans, String city, String tag, String priceLevel) {
        FarmStayQuery plan = new FarmStayQuery(city, tag, priceLevel);
        if (!plans.contains(plan)) {
            plans.add(plan);
        }
    }

    private void appendSingleFarmStayCitation(Long farmStayId, List<AiCitationResponse> citations) {
        FarmStay farmStay = farmStayMapper.selectById(farmStayId);
        if (farmStay == null) {
            return;
        }
        appendFarmStayCandidateCitation(farmStay, citations);
    }

    private void appendFarmStayCandidateCitation(FarmStay farmStay, List<AiCitationResponse> citations) {
        StringBuilder snippet = new StringBuilder();
        snippet.append("民宿：").append(safe(farmStay.getName()));
        snippet.append("，城市：").append(safe(farmStay.getCity()));
        if (StringUtils.hasText(farmStay.getTags())) {
            snippet.append("，标签：").append(farmStay.getTags());
        }
        if (StringUtils.hasText(farmStay.getPriceRange())) {
            snippet.append("，价格区间：").append(farmStay.getPriceRange());
        }
        if (StringUtils.hasText(farmStay.getDescription())) {
            snippet.append("，简介：").append(snippet(farmStay.getDescription()));
        }
        citations.add(new AiCitationResponse("farmstay_candidate", String.valueOf(farmStay.getId()), snippet.toString()));

        List<RoomType> rooms = roomTypeMapper.selectByFarmStayId(farmStay.getId());
        if (!rooms.isEmpty()) {
            RoomType room = rooms.get(0);
            citations.add(new AiCitationResponse("room_candidate", String.valueOf(room.getId()),
                    "推荐房型：" + safe(room.getName()) + "，可住人数：" + room.getMaxGuests() + "，价格：" + room.getPrice()));
        }
    }

    private String inferPriceLevel(AiQuestionSlots slots) {
        if (slots == null || (slots.getBudgetMin() == null && slots.getBudgetMax() == null)) {
            return null;
        }
        Integer budgetMax = slots.getBudgetMax();
        if (budgetMax == null) {
            return null;
        }
        if (budgetMax < 200) {
            return null;
        }
        if (budgetMax <= 800) {
            return "standard";
        }
        return "premium";
    }

    private String resolveFarmStayTag(AiQuestionSlots slots) {
        if (slots == null) {
            return null;
        }
        if (isSupportedFarmStayTag(slots.getTravelGroup())) {
            return slots.getTravelGroup();
        }
        if (slots.getPreferences() != null) {
            for (String preference : slots.getPreferences()) {
                if (isSupportedFarmStayTag(preference)) {
                    return preference;
                }
            }
        }
        if (isSupportedFarmStayTag(slots.getTopic())) {
            return slots.getTopic();
        }
        return null;
    }

    private boolean isSupportedFarmStayTag(String value) {
        return StringUtils.hasText(value) && SUPPORTED_FARMSTAY_TAGS.contains(value.trim());
    }

    private String summarizeSlots(AiQuestionSlots slots) {
        if (slots == null) {
            return "{}";
        }
        return "{city=" + safe(slots.getCity())
                + ", travelGroup=" + safe(slots.getTravelGroup())
                + ", budgetMin=" + slots.getBudgetMin()
                + ", budgetMax=" + slots.getBudgetMax()
                + ", preferences=" + (slots.getPreferences() == null ? List.of() : slots.getPreferences())
                + ", topic=" + safe(slots.getTopic())
                + ", timeRange=" + safe(slots.getTimeRange())
                + "}";
    }

    private Long resolveEffectiveFarmStayId(AiChatSessionRecord session, String question) {
        if (session.getFarmStayId() != null) {
            return session.getFarmStayId();
        }
        if (!containsFarmStayReference(question)) {
            return null;
        }
        List<AiChatMessageRecord> messages = aiChatMessageMapper.selectBySessionId(session.getId());
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiChatMessageRecord message = messages.get(i);
            if (!"assistant".equalsIgnoreCase(message.getRole())) {
                continue;
            }
            Long farmStayId = extractFarmStayIdFromCitations(message.getCitationsJson());
            if (farmStayId != null) {
                log.info("AiChat resolved contextual farmStayId. sessionId={}, question={}, farmStayId={}",
                        session.getId(), question, farmStayId);
                return farmStayId;
            }
        }
        return null;
    }

    private boolean containsFarmStayReference(String question) {
        if (!StringUtils.hasText(question)) {
            return false;
        }
        String normalized = question.trim();
        return normalized.contains("这家")
                || normalized.contains("这个")
                || normalized.contains("它")
                || normalized.contains("该农家乐")
                || normalized.contains("这家农家乐")
                || normalized.contains("这家民宿");
    }

    private Long extractFarmStayIdFromCitations(String citationsJson) {
        List<AiCitationResponse> citations = readCitations(citationsJson);
        for (AiCitationResponse citation : citations) {
            if (citation == null) {
                continue;
            }
            if ("farmstay_candidate".equalsIgnoreCase(citation.getSourceType())
                    || "farmstay".equalsIgnoreCase(citation.getSourceType())) {
                try {
                    return Long.valueOf(citation.getSourceId());
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String enrichRecommendationContent(String content, AiQuestionAnalysisResult analysis) {
        if (!StringUtils.hasText(content) || analysis == null) {
            return content;
        }
        if (!AiQuestionAnalysisServiceImpl.INTENT_RECOMMENDATION.equalsIgnoreCase(safe(analysis.getIntent()))) {
            return content;
        }
        String followUp = buildRecommendationFollowUp(analysis);
        if (!StringUtils.hasText(followUp) || content.contains(followUp)) {
            return content;
        }
        return content.trim() + "\n\n" + followUp;
    }

    private Answer fallbackFaqAnswer(String question, List<AiCitationResponse> citations, String refuseReason) {
        String normalized = safe(question).toLowerCase(Locale.ROOT);
        String content;
        if (normalized.contains("退款") || normalized.contains("退订") || normalized.contains("cancel")) {
            content = "已支付订单可以在订单详情页发起退款申请，未支付订单可以直接取消。如果页面无法操作，建议联系人工客服处理。";
        } else if (normalized.contains("支付") || normalized.contains("pay")) {
            content = "平台支持下单后进入支付流程，支付成功后订单状态会更新为已支付。如果支付失败，建议稍后重试或更换支付方式。";
        } else if (!citations.isEmpty()) {
            content = "我先根据当前可用信息给你一个简要答复：" + citations.get(0).getSnippet();
        } else {
            content = "暂时没有找到足够准确的信息。你可以换一种更具体的问法，或者联系人工客服确认。";
        }
        return new Answer(content, citations, citations.isEmpty() ? 0.45 : 0.68, refuseReason, true);
    }

    private List<String> buildRecentMessages(Long sessionId) {
        List<AiChatMessageRecord> messages = aiChatMessageMapper.selectBySessionId(sessionId);
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> recentMessages = new ArrayList<>();
        int start = Math.max(0, messages.size() - 6);
        for (int i = start; i < messages.size(); i++) {
            AiChatMessageRecord message = messages.get(i);
            recentMessages.add(message.getRole() + ": " + safe(message.getContent()));
        }
        return recentMessages;
    }

    private AiChatMessageRecord saveUserMessage(AiChatSessionRecord session, String question) {
        return insertMessage(session.getId(), "user", question, Collections.emptyList(), null, null, false);
    }

    private AiChatMessageRecord saveAssistantMessage(Long sessionId,
                                                     String content,
                                                     List<AiCitationResponse> citations,
                                                     Double confidence,
                                                     String refuseReason,
                                                     boolean fallback) {
        return insertMessage(sessionId, "assistant", content, citations, confidence, refuseReason, fallback);
    }

    private AiChatMessageRecord insertMessage(Long sessionId,
                                              String role,
                                              String content,
                                              List<AiCitationResponse> citations,
                                              Double confidence,
                                              String refuseReason,
                                              boolean fallback) {
        AiChatMessageRecord record = new AiChatMessageRecord();
        record.setSessionId(sessionId);
        record.setRole(role);
        record.setContent(content);
        record.setCitationsJson(writeCitations(citations));
        record.setConfidence(confidence);
        record.setRefuseReason(refuseReason);
        record.setFallback(fallback);
        if (aiChatMessageMapper.insert(record) <= 0) {
            throw new BusinessException("聊天消息保存失败");
        }
        return record;
    }

    private AiChatMessageRecord createAssistantPlaceholder(Long sessionId) {
        return insertMessage(sessionId, "assistant", "", Collections.emptyList(), null, null, false);
    }

    private void persistAssistantMessage(Long id,
                                         String content,
                                         List<AiCitationResponse> citations,
                                         Double confidence,
                                         String refuseReason,
                                         boolean fallback) {
        if (aiChatMessageMapper.updateMessageContent(id, content, writeCitations(citations), confidence, refuseReason, fallback) <= 0) {
            throw new BusinessException("AI 回复保存失败");
        }
    }

    private void refreshSessionMeta(AiChatSessionRecord session, String question) {
        String title = session.getTitle();
        if (!StringUtils.hasText(title) || DEFAULT_SESSION_TITLE.equals(title)) {
            title = buildSessionTitle(question);
            session.setTitle(title);
        }
        aiChatSessionMapper.updateConversationMeta(session.getId(), title);
    }

    private String buildSessionTitle(String question) {
        if (!StringUtils.hasText(question)) {
            return DEFAULT_SESSION_TITLE;
        }
        String normalized = question.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 24 ? normalized : normalized.substring(0, 24) + "...";
    }

    private Flux<ServerSentEvent<AiChatStreamEventResponse>> buildChunkEvents(Long sessionId,
                                                                              Long messageId,
                                                                              String text,
                                                                              boolean fallback) {
        return Flux.fromIterable(splitText(text, 12))
                .delayElements(Duration.ofMillis(35))
                .map(chunk -> sse("chunk", chunkPayload(sessionId, messageId, chunk, fallback)));
    }

    private List<String> splitText(String text, int chunkSize) {
        if (!StringUtils.hasText(text)) {
            return Collections.singletonList("");
        }
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }
        return chunks;
    }

    private ServerSentEvent<AiChatStreamEventResponse> sse(String event, AiChatStreamEventResponse payload) {
        return ServerSentEvent.<AiChatStreamEventResponse>builder()
                .event(event)
                .data(payload)
                .build();
    }

    private AiChatStreamEventResponse metaPayload(Long sessionId, Long messageId, boolean fallback) {
        AiChatStreamEventResponse response = new AiChatStreamEventResponse();
        response.setType("meta");
        response.setSessionId(sessionId);
        response.setMessageId(messageId);
        response.setModel(MODEL_NAME);
        response.setFallback(fallback);
        return response;
    }

    private AiChatStreamEventResponse chunkPayload(Long sessionId, Long messageId, String content, boolean fallback) {
        AiChatStreamEventResponse response = new AiChatStreamEventResponse();
        response.setType("chunk");
        response.setSessionId(sessionId);
        response.setMessageId(messageId);
        response.setContent(content);
        response.setFallback(fallback);
        return response;
    }

    private AiChatStreamEventResponse donePayload(Long sessionId, Long messageId, boolean fallback) {
        AiChatStreamEventResponse response = new AiChatStreamEventResponse();
        response.setType("done");
        response.setSessionId(sessionId);
        response.setMessageId(messageId);
        response.setFallback(fallback);
        return response;
    }

    private AiChatStreamEventResponse errorPayload(Long sessionId, Long messageId, String message) {
        AiChatStreamEventResponse response = new AiChatStreamEventResponse();
        response.setType("error");
        response.setSessionId(sessionId);
        response.setMessageId(messageId);
        response.setMessage(message);
        response.setFallback(false);
        return response;
    }

    private AiChatSessionRecord requireSession(Long sessionId) {
        AiChatSessionRecord session = aiChatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        return session;
    }

    private void ensureOwner(AiChatSessionRecord session) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!userId.equals(session.getUserId())) {
            throw new BusinessException("无权访问该会话");
        }
    }

    private String writeCitations(List<AiCitationResponse> citations) {
        try {
            return objectMapper.writeValueAsString(citations == null ? Collections.emptyList() : citations);
        } catch (Exception ex) {
            throw new BusinessException("引用信息序列化失败");
        }
    }

    private List<AiCitationResponse> readCitations(String citationsJson) {
        if (!StringUtils.hasText(citationsJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(citationsJson, CITATION_LIST_TYPE);
        } catch (Exception ex) {
            log.warn("Failed to parse AI chat citations JSON", ex);
            return Collections.emptyList();
        }
    }

    private AiChatSessionResponse toSessionResponse(AiChatSessionRecord record) {
        AiChatSessionResponse response = new AiChatSessionResponse();
        response.setSessionId(record.getId());
        response.setUserId(record.getUserId());
        response.setFarmStayId(record.getFarmStayId());
        response.setScene(record.getScene());
        response.setTitle(record.getTitle());
        response.setLastMessageAt(record.getLastMessageAt());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }

    private AiChatMessageResponse toMessageResponse(AiChatMessageRecord record) {
        AiChatMessageResponse response = new AiChatMessageResponse();
        response.setMessageId(record.getId());
        response.setRole(record.getRole());
        response.setContent(record.getContent());
        response.setCreatedAt(record.getCreatedAt());
        response.setCitations(null);
        response.setConfidence(record.getConfidence());
        response.setRefuseReason(record.getRefuseReason());
        response.setFallback(record.getFallback());
        return response;
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private String snippet(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80) + "...";
    }

    private String sanitizeModelContent(String content, AiQuestionAnalysisResult analysis) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String sanitized = content.replace("\uFEFF", "");
        sanitized = sanitized.replaceAll("(?is)<think>.*?</think>", "");
        sanitized = sanitized.replace("<think>", "").replace("</think>", "");
        sanitized = sanitized.replace("```json", "").replace("```", "");
        sanitized = sanitized.replaceAll("(?im)^.*不要输出.*(?:\\r?\\n)?", "");
        sanitized = sanitized.replaceAll("(?im)^.*推理过程。?(?:\\r?\\n)?", "");
        sanitized = sanitized.replaceAll("(?im)^.*Markdown 代码块。?(?:\\r?\\n)?", "");
        sanitized = sanitized.replaceAll("(?im)^.*JSON。?(?:\\r?\\n)?", "");
        sanitized = sanitized.replaceAll("(?im)^.*<think>.*(?:\\r?\\n)?", "");
        sanitized = sanitized.replaceAll("(?im)^.*</think>.*(?:\\r?\\n)?", "");
        sanitized = sanitized.replace(FINAL_ANSWER_MARKER, "");
        sanitized = stripMarkdownFormatting(sanitized);
        if (analysis != null && AiQuestionAnalysisServiceImpl.INTENT_CHITCHAT.equalsIgnoreCase(safe(analysis.getIntent()))) {
            sanitized = extractFinalAnswer(sanitized);
        }
        return sanitized.trim();
    }

    private String extractFinalAnswer(String content) {
        int markerIndex = content.lastIndexOf(FINAL_ANSWER_MARKER);
        if (markerIndex >= 0) {
            return content.substring(markerIndex + FINAL_ANSWER_MARKER.length()).trim();
        }
        String sanitized = content;
        sanitized = sanitized.replaceAll("(?is)^.*?(我需要|我应该|因为我没有|好的\\s*)", "");
        return sanitized.trim();
    }

    private static String stripMarkdownFormatting(String content) {
        String sanitized = content;
        sanitized = sanitized.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        sanitized = sanitized.replaceAll("__(.*?)__", "$1");
        sanitized = sanitized.replaceAll("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)", "$1");
        sanitized = sanitized.replaceAll("(?<!_)_(?!_)(.*?)(?<!_)_(?!_)", "$1");
        sanitized = sanitized.replace("`", "");
        sanitized = sanitized.replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "");
        sanitized = sanitized.replaceAll("(?m)^\\s*>+\\s*", "");
        sanitized = sanitized.replaceAll("(?m)^\\s*[-+•]\\s+", "");
        sanitized = sanitized.replaceAll("(?m)^\\s*\\d+\\.\\s+", "");
        sanitized = sanitized.replaceAll("(?m)^\\s*\\d+\\)\\s+", "");
        sanitized = sanitized.replaceAll("(?m)^\\s*[-*]\\s*\\[[ xX]\\]\\s+", "");
        sanitized = sanitized.replaceAll("\\[(.*?)\\]\\((.*?)\\)", "$1");
        sanitized = sanitized.replaceAll("(?m)^\\s*---+\\s*$", "");
        sanitized = sanitized.replaceAll("(?m)^\\s*===+\\s*$", "");
        sanitized = sanitized.replaceAll("[ \t]+", " ");
        sanitized = sanitized.replaceAll("(?m)^[ \t]+", "");
        return sanitized;
    }

    private static class Answer {
        private final String content;
        private final List<AiCitationResponse> citations;
        private final Double confidence;
        private final String refuseReason;
        private final boolean fallback;

        private Answer(String content, List<AiCitationResponse> citations, Double confidence, String refuseReason, boolean fallback) {
            this.content = content;
            this.citations = citations;
            this.confidence = confidence;
            this.refuseReason = refuseReason;
            this.fallback = fallback;
        }
    }

    private record FarmStayQuery(String city, String tag, String priceLevel) {
    }

    private static class PreparedAnswer {
        private final ChatClient.Builder builder;
        private final String prompt;
        private final List<AiCitationResponse> citations;
        private final Answer directAnswer;
        private final AiQuestionAnalysisResult analysis;

        private PreparedAnswer(ChatClient.Builder builder,
                               String prompt,
                               List<AiCitationResponse> citations,
                               Answer directAnswer,
                               AiQuestionAnalysisResult analysis) {
            this.builder = builder;
            this.prompt = prompt;
            this.citations = citations;
            this.directAnswer = directAnswer;
            this.analysis = analysis;
        }

        private static PreparedAnswer direct(Answer answer, AiQuestionAnalysisResult analysis, List<AiCitationResponse> citations) {
            return new PreparedAnswer(null, null, citations, answer, analysis);
        }

        private static PreparedAnswer model(ChatClient.Builder builder,
                                            String prompt,
                                            List<AiCitationResponse> citations,
                                            AiQuestionAnalysisResult analysis) {
            return new PreparedAnswer(builder, prompt, citations, null, analysis);
        }
    }

    private static class StreamingContentFilter {
        private static final String THINK_START = "<think>";
        private static final String THINK_END = "</think>";

        private final StringBuilder pending = new StringBuilder();
        private boolean inThink;

        private String append(String chunk) {
            pending.append(chunk);
            StringBuilder visible = new StringBuilder();
            while (pending.length() > 0) {
                if (inThink) {
                    int endIndex = pending.indexOf(THINK_END);
                    if (endIndex >= 0) {
                        pending.delete(0, endIndex + THINK_END.length());
                        inThink = false;
                        continue;
                    }
                    trimPendingForPartialTag(THINK_END);
                    break;
                }

                int startIndex = pending.indexOf(THINK_START);
                if (startIndex >= 0) {
                    visible.append(pending, 0, startIndex);
                    pending.delete(0, startIndex + THINK_START.length());
                    inThink = true;
                    continue;
                }

                int overlap = suffixPrefixOverlap(pending.toString(), THINK_START);
                int emitLength = pending.length() - overlap;
                if (emitLength > 0) {
                    visible.append(pending, 0, emitLength);
                    pending.delete(0, emitLength);
                }
                break;
            }
            return stripStreamingNoise(visible.toString());
        }

        private String finish() {
            String tail;
            if (inThink) {
                tail = "";
            } else {
                tail = pending.toString();
            }
            pending.setLength(0);
            inThink = false;
            return stripStreamingNoise(tail);
        }

        private void trimPendingForPartialTag(String tag) {
            int overlap = suffixPrefixOverlap(pending.toString(), tag);
            if (pending.length() > overlap) {
                pending.delete(0, pending.length() - overlap);
            }
        }

        private int suffixPrefixOverlap(String text, String prefix) {
            int max = Math.min(text.length(), prefix.length() - 1);
            for (int len = max; len > 0; len--) {
                if (text.regionMatches(text.length() - len, prefix, 0, len)) {
                    return len;
                }
            }
            return 0;
        }

        private String stripStreamingNoise(String text) {
            return stripMarkdownFormatting(text
                    .replace("```json", "")
                    .replace("```", "")
                    .replace(FINAL_ANSWER_MARKER, "")
                    .replace("<think>", "")
                    .replace("</think>", "")
                    .replace("???? JSON", "")
                    .replace("???? Markdown ???", "")
                    .replace("?????", "")
                    .replace("??????", ""));
        }
    }
}
