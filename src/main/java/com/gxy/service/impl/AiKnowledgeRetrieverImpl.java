package com.gxy.service.impl;

import com.gxy.mapper.ActivityMapper;
import com.gxy.mapper.AiKnowledgeDocumentMapper;
import com.gxy.mapper.CouponMapper;
import com.gxy.mapper.DiningMapper;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.mapper.RoomTypeMapper;
import com.gxy.model.dto.AiCitationResponse;
import com.gxy.model.entity.ActivityItem;
import com.gxy.model.entity.AiKnowledgeDocument;
import com.gxy.model.entity.Coupon;
import com.gxy.model.entity.DiningItem;
import com.gxy.model.entity.FarmStay;
import com.gxy.model.entity.RoomType;
import com.gxy.service.AiKnowledgeRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiKnowledgeRetrieverImpl implements AiKnowledgeRetriever {

    private static final int KNOWLEDGE_LIMIT = 3;
    private static final int TOTAL_LIMIT = 5;
    private static final Pattern LATIN_TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9]{3,}");
    private static final Pattern CJK_TOKEN_PATTERN = Pattern.compile("[\u4e00-\u9fa5]{2,}");

    private final AiKnowledgeDocumentMapper aiKnowledgeDocumentMapper;
    private final FarmStayMapper farmStayMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final DiningMapper diningMapper;
    private final ActivityMapper activityMapper;
    private final CouponMapper couponMapper;

    /**
     * 轻量 RAG 检索入口。
     * 先按问题拆词从知识片段表中召回，再补充民宿业务数据片段。
     */
    @Override
    public List<AiCitationResponse> retrieve(Long farmStayId, String scene, String question, boolean operator) {
        Map<String, AiCitationResponse> citationMap = new LinkedHashMap<>();
        List<String> keywords = extractKeywords(question, scene);
        log.info("AiChat retrieval keywords. farmStayId={}, scene={}, question={}, keywords={}",
                farmStayId, scene, question, keywords);
        for (String keyword : keywords) {
            List<AiKnowledgeDocument> documents = aiKnowledgeDocumentMapper.search(keyword, farmStayId, operator ? 1 : 0, KNOWLEDGE_LIMIT);
            for (AiKnowledgeDocument document : documents) {
                citationMap.putIfAbsent(document.getKnowledgeCode(),
                        new AiCitationResponse("knowledge", document.getKnowledgeCode(), snippet(document.getContent())));
                if (citationMap.size() >= TOTAL_LIMIT) {
                    log.info("AiChat retrieval completed from document layer. citationCount={}", citationMap.size());
                    return new ArrayList<>(citationMap.values());
                }
            }
        }
        List<AiCitationResponse> citations = new ArrayList<>(citationMap.values());
        appendFarmStayKnowledge(farmStayId, citations);
        log.info("AiChat retrieval completed. citationCount={}, farmStayId={}", citations.size(), farmStayId);
        return citations;
    }

    private List<String> extractKeywords(String question, String scene) {
        Set<String> keywords = new LinkedHashSet<>();
        if (StringUtils.hasText(question)) {
            String normalized = question.trim();
            keywords.add(normalized);
            addMatches(keywords, LATIN_TOKEN_PATTERN.matcher(normalized.toLowerCase(Locale.ROOT)));
            addMatches(keywords, CJK_TOKEN_PATTERN.matcher(normalized));
        }
        if (StringUtils.hasText(scene)) {
            keywords.add(scene.trim());
        }
        if (keywords.isEmpty()) {
            return List.of("");
        }
        return new ArrayList<>(keywords);
    }

    private void addMatches(Set<String> keywords, Matcher matcher) {
        while (matcher.find()) {
            String token = matcher.group();
            if (StringUtils.hasText(token)) {
                keywords.add(token);
            }
        }
    }

    private void appendFarmStayKnowledge(Long farmStayId, List<AiCitationResponse> citations) {
        if (farmStayId == null || citations.size() >= TOTAL_LIMIT) {
            return;
        }
        FarmStay farmStay = farmStayMapper.selectById(farmStayId);
        if (farmStay != null) {
            citations.add(new AiCitationResponse("farmstay", String.valueOf(farmStayId),
                    "民宿：" + safe(farmStay.getName()) + "，城市：" + safe(farmStay.getCity()) + "，标签：" + safe(farmStay.getTags())));
        }
        if (citations.size() >= TOTAL_LIMIT) {
            return;
        }
        List<RoomType> rooms = roomTypeMapper.selectByFarmStayId(farmStayId);
        if (!rooms.isEmpty()) {
            RoomType room = rooms.get(0);
            citations.add(new AiCitationResponse("room", String.valueOf(room.getId()),
                    "房型：" + safe(room.getName()) + "，价格：" + room.getPrice()));
        }
        if (citations.size() >= TOTAL_LIMIT) {
            return;
        }
        List<DiningItem> diningItems = diningMapper.selectByFarmStayId(farmStayId);
        if (!diningItems.isEmpty()) {
            DiningItem dining = diningItems.get(0);
            citations.add(new AiCitationResponse("dining", String.valueOf(dining.getId()),
                    "餐饮服务：" + safe(dining.getName()) + "，价格：" + dining.getPrice()));
        }
        if (citations.size() >= TOTAL_LIMIT) {
            return;
        }
        List<ActivityItem> activities = activityMapper.selectByFarmStayId(farmStayId);
        if (!activities.isEmpty()) {
            ActivityItem activity = activities.get(0);
            citations.add(new AiCitationResponse("activity", String.valueOf(activity.getId()),
                    "活动：" + safe(activity.getName()) + "，排期：" + safe(activity.getSchedule())));
        }
        if (citations.size() >= TOTAL_LIMIT) {
            return;
        }
        List<Coupon> coupons = couponMapper.listAvailable(new Date(), farmStayId);
        if (!coupons.isEmpty()) {
            Coupon coupon = coupons.get(0);
            citations.add(new AiCitationResponse("coupon", String.valueOf(coupon.getId()),
                    "优惠券：" + safe(coupon.getTitle()) + "，满减金额：" + coupon.getDiscountAmount()));
        }
    }

    private String snippet(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}
