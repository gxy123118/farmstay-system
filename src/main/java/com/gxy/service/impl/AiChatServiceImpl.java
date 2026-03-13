package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gxy.common.auth.AuthGuard;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.ActivityMapper;
import com.gxy.mapper.CouponMapper;
import com.gxy.mapper.DiningMapper;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.mapper.RoomTypeMapper;
import com.gxy.model.dto.AiCitationResponse;
import com.gxy.model.dto.AiChatFeedbackRequest;
import com.gxy.model.dto.AiChatMessageRequest;
import com.gxy.model.dto.AiChatMessageResponse;
import com.gxy.model.dto.AiChatSessionCreateRequest;
import com.gxy.model.dto.AiChatSessionResponse;
import com.gxy.model.entity.ActivityItem;
import com.gxy.model.entity.Coupon;
import com.gxy.model.entity.DiningItem;
import com.gxy.model.entity.FarmStay;
import com.gxy.model.entity.RoomType;
import com.gxy.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private static final String SYSTEM_PROMPT = """
            You are a customer-service assistant for a farm stay platform.
            Follow these rules strictly:
            1. Answer in Chinese.
            2. Prefer the provided knowledge snippets and do not invent unsupported facts.
            3. If the knowledge is insufficient, say so directly and suggest contacting human support.
            4. Do not reveal personal data such as phone numbers or real names.
            5. Do not output JSON or Markdown code blocks.
            """;

    private final FarmStayMapper farmStayMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final DiningMapper diningMapper;
    private final ActivityMapper activityMapper;
    private final CouponMapper couponMapper;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    private final AtomicLong sessionIdGen = new AtomicLong(1000);
    private final AtomicLong messageIdGen = new AtomicLong(10000);
    private final ConcurrentHashMap<Long, SessionContext> sessionStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> messageFeedbackStore = new ConcurrentHashMap<>();

    @Override
    public AiChatSessionResponse createSession(AiChatSessionCreateRequest request) {
        AuthGuard.enforceAtLeastVisitor();
        Long userId = StpUtil.getLoginIdAsLong();
        long sessionId = sessionIdGen.incrementAndGet();
        SessionContext context = new SessionContext();
        context.sessionId = sessionId;
        context.userId = userId;
        context.farmStayId = request == null ? null : request.getFarmStayId();
        context.scene = request == null ? null : request.getScene();
        context.createdAt = new Date();
        context.messages = new ArrayList<>();
        sessionStore.put(sessionId, context);
        return toSessionResponse(context);
    }

    @Override
    public AiChatSessionResponse getSession(Long sessionId) {
        AuthGuard.enforceAtLeastVisitor();
        SessionContext context = requireSession(sessionId);
        ensureOwner(context);
        return toSessionResponse(context);
    }

    @Override
    public AiChatMessageResponse sendMessage(Long sessionId, AiChatMessageRequest request) {
        AuthGuard.enforceAtLeastVisitor();
        SessionContext context = requireSession(sessionId);
        ensureOwner(context);

        ChatMessage userMessage = new ChatMessage();
        userMessage.messageId = messageIdGen.incrementAndGet();
        userMessage.role = "user";
        userMessage.content = request.getQuestion();
        userMessage.createdAt = new Date();
        userMessage.citations = Collections.emptyList();
        userMessage.confidence = null;
        context.messages.add(userMessage);

        Answer answer = buildAnswer(context, request.getQuestion());
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.messageId = messageIdGen.incrementAndGet();
        assistantMessage.role = "assistant";
        assistantMessage.content = answer.content;
        assistantMessage.createdAt = new Date();
        assistantMessage.citations = answer.citations;
        assistantMessage.confidence = answer.confidence;
        assistantMessage.refuseReason = answer.refuseReason;
        context.messages.add(assistantMessage);
        return toMessageResponse(assistantMessage);
    }

    @Override
    public List<AiChatMessageResponse> listMessages(Long sessionId) {
        AuthGuard.enforceAtLeastVisitor();
        SessionContext context = requireSession(sessionId);
        ensureOwner(context);
        List<AiChatMessageResponse> responses = new ArrayList<>();
        for (ChatMessage message : context.messages) {
            responses.add(toMessageResponse(message));
        }
        return responses;
    }

    @Override
    public boolean feedback(AiChatFeedbackRequest request) {
        AuthGuard.enforceAtLeastVisitor();
        SessionContext context = requireSession(request.getSessionId());
        ensureOwner(context);
        boolean found = false;
        for (ChatMessage message : context.messages) {
            if (message.messageId.equals(request.getMessageId())) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new BusinessException("消息不存在");
        }
        messageFeedbackStore.put(request.getMessageId(), request.getUseful());
        return true;
    }

    private Answer buildAnswer(SessionContext context, String question) {
        List<AiCitationResponse> citations = new ArrayList<>();
        loadPolicyKnowledge(question, citations);
        loadFarmStayKnowledge(context.farmStayId, citations);

        if (citations.isEmpty()) {
            log.info("AiChat fallback because no citations were found. sessionId={}", context.sessionId);
            return fallbackAnswer(question, citations, "知识库未命中");
        }

        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            log.warn("AiChat fallback because ChatClient.Builder is not available. sessionId={}", context.sessionId);
            return fallbackAnswer(question, citations, "模型服务未启用");
        }

        try {
            log.info("AiChat MiniMax request started. sessionId={}, farmStayId={}, citationCount={}",
                    context.sessionId, context.farmStayId, citations.size());
            String content = builder.build()
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(buildUserPrompt(context, question, citations))
                    .call()
                    .content();
            if (!StringUtils.hasText(content)) {
                log.warn("AiChat fallback because MiniMax returned empty content. sessionId={}", context.sessionId);
                return fallbackAnswer(question, citations, "模型无返回内容");
            }
            log.info("AiChat MiniMax response received. sessionId={}, contentLength={}", context.sessionId, content.length());
            return new Answer(content.trim(), citations, 0.86, null);
        } catch (Exception ex) {
            log.warn("MiniMax chat generation failed. sessionId={}", context.sessionId, ex);
            return fallbackAnswer(question, citations, "模型调用失败");
        }
    }

    private String buildUserPrompt(SessionContext context, String question, List<AiCitationResponse> citations) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("会话场景：").append(safe(context.scene)).append('\n');
        prompt.append("问题：").append(safe(question)).append('\n');
        prompt.append("最近会话：\n").append(buildRecentConversation(context)).append('\n');
        prompt.append("可用知识片段：\n");
        for (int i = 0; i < citations.size(); i++) {
            AiCitationResponse citation = citations.get(i);
            prompt.append(i + 1)
                    .append(". [")
                    .append(safe(citation.getSourceType()))
                    .append("#")
                    .append(safe(citation.getSourceId()))
                    .append("] ")
                    .append(safe(citation.getSnippet()))
                    .append('\n');
        }
        prompt.append("请基于以上知识片段回答用户问题。如果无法确定，就明确说不知道，并建议联系人工客服。\n");
        return prompt.toString();
    }

    private String buildRecentConversation(SessionContext context) {
        if (context.messages == null || context.messages.isEmpty()) {
            return "无";
        }
        StringBuilder builder = new StringBuilder();
        int start = Math.max(0, context.messages.size() - 6);
        for (int i = start; i < context.messages.size(); i++) {
            ChatMessage message = context.messages.get(i);
            builder.append(message.role).append(": ").append(safe(message.content)).append('\n');
        }
        return builder.toString();
    }

    private Answer fallbackAnswer(String question, List<AiCitationResponse> citations, String refuseReason) {
        String normalized = question == null ? "" : question.toLowerCase();
        String content;
        if (normalized.contains("退款") || normalized.contains("退订") || normalized.contains("cancel")) {
            content = "已支付订单可以在订单详情页发起退款申请，未支付订单可以直接取消。若页面无法操作，建议联系人工客服处理。";
        } else if (normalized.contains("支付") || normalized.contains("pay")) {
            content = "平台支持在下单后进入支付流程，支付成功后订单状态会更新为 PAID。若支付失败，建议稍后重试或更换支付方式。";
        } else if (!citations.isEmpty()) {
            content = "我先根据当前可用信息给你一个简要答复：" + citations.get(0).getSnippet();
        } else {
            content = "暂时没有命中足够的知识信息，建议联系人工客服，或者换一种更具体的问法。";
        }
        return new Answer(content, citations, citations.isEmpty() ? 0.45 : 0.68, refuseReason);
    }

    private void loadPolicyKnowledge(String question, List<AiCitationResponse> citations) {
        if (citations.size() >= 4 || !StringUtils.hasText(question)) {
            return;
        }
        String normalized = question.toLowerCase();
        if (normalized.contains("退款") || normalized.contains("退订") || normalized.contains("cancel")) {
            citations.add(new AiCitationResponse(
                    "policy",
                    "refund_rule",
                    "平台通用规则：未支付订单可直接取消，已支付订单可在订单详情页申请退款，结果以订单状态更新为准。"
            ));
        }
        if (citations.size() >= 4) {
            return;
        }
        if (normalized.contains("支付") || normalized.contains("pay")) {
            citations.add(new AiCitationResponse(
                    "policy",
                    "payment_rule",
                    "平台通用规则：订单支付成功后状态会更新为 PAID，若支付失败可重新发起支付。"
            ));
        }
    }

    private void loadFarmStayKnowledge(Long farmStayId, List<AiCitationResponse> citations) {
        if (farmStayId == null || citations.size() >= 4) {
            return;
        }
        FarmStay farmStay = farmStayMapper.selectById(farmStayId);
        if (farmStay != null) {
            citations.add(new AiCitationResponse(
                    "farmstay",
                    String.valueOf(farmStayId),
                    "农家乐：" + safe(farmStay.getName()) + "，城市：" + safe(farmStay.getCity()) + "，价格档位：" + safe(farmStay.getPriceLevel())
            ));
        }
        if (citations.size() >= 4) {
            return;
        }
        List<RoomType> rooms = roomTypeMapper.selectByFarmStayId(farmStayId);
        if (!rooms.isEmpty()) {
            RoomType room = rooms.get(0);
            citations.add(new AiCitationResponse(
                    "room",
                    String.valueOf(room.getId()),
                    "房型：" + safe(room.getName()) + "，价格：" + String.valueOf(room.getPrice())
            ));
        }
        if (citations.size() >= 4) {
            return;
        }
        List<DiningItem> diningItems = diningMapper.selectByFarmStayId(farmStayId);
        if (!diningItems.isEmpty()) {
            DiningItem dining = diningItems.get(0);
            citations.add(new AiCitationResponse(
                    "dining",
                    String.valueOf(dining.getId()),
                    "餐饮服务：" + safe(dining.getName()) + "，价格：" + String.valueOf(dining.getPrice())
            ));
        }
        if (citations.size() >= 4) {
            return;
        }
        List<ActivityItem> activities = activityMapper.selectByFarmStayId(farmStayId);
        if (!activities.isEmpty()) {
            ActivityItem item = activities.get(0);
            citations.add(new AiCitationResponse(
                    "activity",
                    String.valueOf(item.getId()),
                    "活动：" + safe(item.getName()) + "，排期：" + safe(item.getSchedule())
            ));
        }
        if (citations.size() >= 4) {
            return;
        }
        List<Coupon> coupons = couponMapper.listAvailable(new Date(), farmStayId);
        if (!coupons.isEmpty()) {
            Coupon coupon = coupons.get(0);
            citations.add(new AiCitationResponse(
                    "coupon",
                    String.valueOf(coupon.getId()),
                    "优惠券：" + safe(coupon.getTitle()) + "，满减金额：" + String.valueOf(coupon.getDiscountAmount())
            ));
        }
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private SessionContext requireSession(Long sessionId) {
        SessionContext context = sessionStore.get(sessionId);
        if (context == null) {
            throw new BusinessException("会话不存在");
        }
        return context;
    }

    private void ensureOwner(SessionContext context) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!context.userId.equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }
    }

    private AiChatSessionResponse toSessionResponse(SessionContext context) {
        AiChatSessionResponse response = new AiChatSessionResponse();
        response.setSessionId(context.sessionId);
        response.setUserId(context.userId);
        response.setFarmStayId(context.farmStayId);
        response.setScene(context.scene);
        response.setCreatedAt(context.createdAt);
        return response;
    }

    private AiChatMessageResponse toMessageResponse(ChatMessage message) {
        AiChatMessageResponse response = new AiChatMessageResponse();
        response.setMessageId(message.messageId);
        response.setRole(message.role);
        response.setContent(message.content);
        response.setCreatedAt(message.createdAt);
        response.setCitations(message.citations);
        response.setConfidence(message.confidence);
        response.setRefuseReason(message.refuseReason);
        return response;
    }

    private static class SessionContext {
        private Long sessionId;
        private Long userId;
        private Long farmStayId;
        private String scene;
        private Date createdAt;
        private List<ChatMessage> messages;
    }

    private static class ChatMessage {
        private Long messageId;
        private String role;
        private String content;
        private Date createdAt;
        private List<AiCitationResponse> citations;
        private Double confidence;
        private String refuseReason;
    }

    private static class Answer {
        private final String content;
        private final List<AiCitationResponse> citations;
        private final Double confidence;
        private final String refuseReason;

        private Answer(String content, List<AiCitationResponse> citations, Double confidence, String refuseReason) {
            this.content = content;
            this.citations = citations;
            this.confidence = confidence;
            this.refuseReason = refuseReason;
        }
    }
}