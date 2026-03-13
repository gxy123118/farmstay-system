package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gxy.common.auth.AuthGuard;
import com.gxy.common.exception.BusinessException;
import com.gxy.config.OperatorInsightAiProperties;
import com.gxy.mapper.BookingOrderMapper;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.mapper.OperatorInsightReportMapper;
import com.gxy.mapper.ReviewMapper;
import com.gxy.model.dto.OperatorInsightActionResponse;
import com.gxy.model.dto.OperatorInsightIssueResponse;
import com.gxy.model.dto.OperatorInsightReportResponse;
import com.gxy.model.dto.ai.OperatorInsightAiAction;
import com.gxy.model.dto.ai.OperatorInsightAiIssue;
import com.gxy.model.dto.ai.OperatorInsightAiResult;
import com.gxy.model.dto.ai.OperatorInsightAnalysisContext;
import com.gxy.model.entity.BookingOrder;
import com.gxy.model.entity.FarmStay;
import com.gxy.model.entity.OperatorInsightReportRecord;
import com.gxy.model.entity.Review;
import com.gxy.service.OperatorInsightAiService;
import com.gxy.service.OperatorInsightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorInsightServiceImpl implements OperatorInsightService {

    private static final DateTimeFormatter REVIEW_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_REFUNDED = "REFUNDED";

    private final ReviewMapper reviewMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final FarmStayMapper farmStayMapper;
    private final OperatorInsightReportMapper operatorInsightReportMapper;
    private final ObjectProvider<OperatorInsightAiService> operatorInsightAiServiceProvider;
    private final OperatorInsightAiProperties aiProperties;
    private final ObjectMapper objectMapper;

    private final AtomicLong reportIdGen = new AtomicLong(5000);
    private final Map<Long, CopyOnWriteArrayList<OperatorInsightReportResponse>> reportStore = new ConcurrentHashMap<>();

    @Override
    public OperatorInsightReportResponse generate(Long farmStayId, Integer periodDays) {
        AuthGuard.enforceOperator();
        FarmStay farmStay = ensureOwnerAndGetFarmStay(farmStayId);
        int days = normalizePeriodDays(periodDays);
        Date cutoff = new Date(System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L);

        List<Review> reviews = filterReviewsByPeriod(reviewMapper.listByFarmStay(farmStayId), cutoff);
        List<BookingOrder> orders = filterOrdersByPeriod(bookingOrderMapper.selectByFarmStay(farmStayId), cutoff);

        OperatorInsightReportResponse report = buildReport(farmStay, days, reviews, orders);
        saveReport(farmStay, report);
        reportStore.computeIfAbsent(farmStayId, key -> new CopyOnWriteArrayList<>()).add(0, report);
        return report;
    }

    @Override
    public OperatorInsightReportResponse latest(Long farmStayId) {
        AuthGuard.enforceOperator();
        ensureOwnerAndGetFarmStay(farmStayId);
        List<OperatorInsightReportResponse> reports = reportStore.get(farmStayId);
        if (!CollectionUtils.isEmpty(reports)) {
            return reports.get(0);
        }
        OperatorInsightReportResponse latest = loadLatestFromDb(farmStayId);
        if (latest != null) {
            cacheReports(farmStayId, Collections.singletonList(latest));
            return latest;
        }
        return generate(farmStayId, 30);
    }

    @Override
    public List<OperatorInsightReportResponse> history(Long farmStayId) {
        AuthGuard.enforceOperator();
        ensureOwnerAndGetFarmStay(farmStayId);
        List<OperatorInsightReportResponse> reports = reportStore.get(farmStayId);
        if (!CollectionUtils.isEmpty(reports)) {
            return new ArrayList<>(reports);
        }
        List<OperatorInsightReportResponse> dbReports = loadHistoryFromDb(farmStayId);
        cacheReports(farmStayId, dbReports);
        return dbReports;
    }

    @Override
    public OperatorInsightReportResponse historyDetail(Long farmStayId, Long reportId) {
        AuthGuard.enforceOperator();
        ensureOwnerAndGetFarmStay(farmStayId);
        List<OperatorInsightReportResponse> reports = reportStore.get(farmStayId);
        if (!CollectionUtils.isEmpty(reports)) {
            for (OperatorInsightReportResponse report : reports) {
                if (Objects.equals(report.getReportId(), reportId)) {
                    return report;
                }
            }
        }
        OperatorInsightReportResponse detail = loadDetailFromDb(farmStayId, reportId);
        if (detail == null) {
            throw new BusinessException("历史报告不存在");
        }
        appendCacheReport(farmStayId, detail);
        return detail;
    }

    @Override
    public boolean deleteHistory(Long farmStayId, Long reportId) {
        AuthGuard.enforceOperator();
        ensureOwnerAndGetFarmStay(farmStayId);
        int changed = operatorInsightReportMapper.softDeleteByReportId(farmStayId, reportId);
        if (changed == 0) {
            throw new BusinessException("历史报告不存在");
        }
        CopyOnWriteArrayList<OperatorInsightReportResponse> reports = reportStore.get(farmStayId);
        if (!CollectionUtils.isEmpty(reports)) {
            reports.removeIf(report -> Objects.equals(report.getReportId(), reportId));
            if (reports.isEmpty()) {
                reportStore.remove(farmStayId);
            }
        }
        log.info("Operator insight history deleted. farmStayId={}, reportId={}", farmStayId, reportId);
        return true;
    }

    @Override
    public List<OperatorInsightIssueResponse> issues(Long farmStayId) {
        return latest(farmStayId).getIssues();
    }

    private OperatorInsightReportResponse buildReport(FarmStay farmStay,
                                                      int periodDays,
                                                      List<Review> reviews,
                                                      List<BookingOrder> orders) {
        int reviewCount = reviews.size();
        int negativeReviewCount = countNegativeReviews(reviews);
        double averageRating = calculateAverageRating(reviews);
        List<OperatorInsightIssueResponse> heuristicIssues = buildHeuristicIssues(reviews);
        List<OperatorInsightActionResponse> fallbackActions = buildFallbackActions(heuristicIssues);
        String fallbackSummary = buildFallbackSummary(reviewCount, negativeReviewCount, heuristicIssues);

        OperatorInsightReportResponse report = new OperatorInsightReportResponse();
        report.setReportId(reportIdGen.incrementAndGet());
        report.setFarmStayId(farmStay.getId());
        report.setPeriodDays(periodDays);
        report.setReviewCount(reviewCount);
        report.setAverageRating(averageRating);
        report.setGeneratedAt(new Date());
        report.setModel(aiProperties.getModel());

        if (reviewCount == 0) {
            log.info("Operator insight skipped AI generation because there are no reviews. farmStayId={}, periodDays={}",
                    farmStay.getId(), periodDays);
            report.setSummary(fallbackSummary);
            report.setIssues(heuristicIssues);
            report.setActions(fallbackActions);
            report.setGenerationMode("fallback");
            return report;
        }

        try {
            OperatorInsightAiService operatorInsightAiService = operatorInsightAiServiceProvider.getIfAvailable();
            if (operatorInsightAiService == null) {
                throw new IllegalStateException("Operator insight AI service is not available");
            }
            log.info("Operator insight AI bean available. farmStayId={}, periodDays={}", farmStay.getId(), periodDays);
            OperatorInsightAnalysisContext context = buildAnalysisContext(
                    farmStay, periodDays, reviews, orders, negativeReviewCount, averageRating, heuristicIssues);
            OperatorInsightAiResult aiResult = operatorInsightAiService.analyze(context);
            report.setSummary(StringUtils.hasText(aiResult.getSummary()) ? aiResult.getSummary() : fallbackSummary);
            report.setIssues(mergeIssues(aiResult.getIssues(), heuristicIssues));
            report.setActions(mergeActions(aiResult.getActions(), report.getIssues(), fallbackActions));
            report.setGenerationMode("llm");
            log.info("Operator insight AI generation succeeded. farmStayId={}, periodDays={}, generationMode=llm",
                    farmStay.getId(), periodDays);
        } catch (Exception ex) {
            log.warn("Operator insight AI generation failed, fallback to heuristic report. farmStayId={}", farmStay.getId(), ex);
            report.setSummary(fallbackSummary);
            report.setIssues(heuristicIssues);
            report.setActions(fallbackActions);
            report.setGenerationMode("fallback");
        }
        return report;
    }

    private OperatorInsightAnalysisContext buildAnalysisContext(FarmStay farmStay,
                                                                int periodDays,
                                                                List<Review> reviews,
                                                                List<BookingOrder> orders,
                                                                int negativeReviewCount,
                                                                double averageRating,
                                                                List<OperatorInsightIssueResponse> heuristicIssues) {
        OperatorInsightAnalysisContext context = new OperatorInsightAnalysisContext();
        context.setFarmStayId(farmStay.getId());
        context.setFarmStayName(farmStay.getName());
        context.setCity(farmStay.getCity());
        context.setDescription(farmStay.getDescription());
        context.setTags(farmStay.getTags());
        context.setPriceRange(farmStay.getPriceRange());
        context.setPriceLevel(farmStay.getPriceLevel());
        context.setPeriodDays(periodDays);
        context.setReviewCount(reviews.size());
        context.setAverageRating(averageRating);
        context.setNegativeReviewCount(negativeReviewCount);
        context.setOrderCount(orders.size());
        context.setPaidOrderCount(countOrdersByStatus(orders, STATUS_PAID));
        context.setRefundedOrderCount(countOrdersByStatus(orders, STATUS_REFUNDED));
        context.setCancelledOrderCount(countOrdersByStatus(orders, STATUS_CANCELLED));
        context.setAverageOrderAmount(calculateAverageOrderAmount(orders));
        context.setHeuristicIssues(toAiIssues(heuristicIssues));
        context.setReviewSamples(buildReviewSamples(reviews));
        return context;
    }

    private List<Review> filterReviewsByPeriod(List<Review> reviews, Date cutoff) {
        if (reviews == null) {
            return Collections.emptyList();
        }
        return reviews.stream()
                .filter(Objects::nonNull)
                .filter(review -> review.getCreatedAt() != null && !review.getCreatedAt().before(cutoff))
                .collect(Collectors.toList());
    }

    private List<BookingOrder> filterOrdersByPeriod(List<BookingOrder> orders, Date cutoff) {
        if (orders == null) {
            return Collections.emptyList();
        }
        return orders.stream()
                .filter(Objects::nonNull)
                .filter(order -> order.getCreatedAt() != null && !order.getCreatedAt().before(cutoff))
                .collect(Collectors.toList());
    }

    private List<OperatorInsightIssueResponse> buildHeuristicIssues(List<Review> reviews) {
        Map<String, TopicStat> statMap = new HashMap<>();
        Map<String, List<String>> evidenceMap = new HashMap<>();

        for (Review review : reviews) {
            String topic = detectTopic(review.getContent());
            TopicStat stat = statMap.computeIfAbsent(topic, key -> new TopicStat());
            stat.total++;
            if (review.getRating() != null && review.getRating() <= 2) {
                stat.negative++;
                if (StringUtils.hasText(review.getContent())) {
                    evidenceMap.computeIfAbsent(topic, key -> new ArrayList<>()).add(trimEvidence(review.getContent()));
                }
            }
        }

        List<OperatorInsightIssueResponse> issueResponses = new ArrayList<>();
        for (Map.Entry<String, TopicStat> entry : statMap.entrySet()) {
            OperatorInsightIssueResponse issue = new OperatorInsightIssueResponse();
            TopicStat value = entry.getValue();
            issue.setTopic(entry.getKey());
            issue.setIssueCount(value.total);
            issue.setNegativeRatio(value.total == 0 ? 0.0 : round2((double) value.negative / value.total));
            int impactScore = (int) Math.min(100, value.total * 8 + value.negative * 12);
            issue.setImpactScore(impactScore);
            issue.setPriority(priorityByScore(impactScore));
            issue.setEvidenceCount(value.negative);
            issue.setEvidenceSamples(limitList(evidenceMap.get(entry.getKey()), 3));
            issueResponses.add(issue);
        }

        issueResponses.sort(Comparator.comparing(OperatorInsightIssueResponse::getImpactScore).reversed());
        return issueResponses;
    }

    private List<OperatorInsightActionResponse> buildFallbackActions(List<OperatorInsightIssueResponse> issues) {
        List<OperatorInsightActionResponse> actions = new ArrayList<>();
        for (int i = 0; i < issues.size() && i < 3; i++) {
            OperatorInsightIssueResponse issue = issues.get(i);
            OperatorInsightActionResponse action = new OperatorInsightActionResponse();
            action.setTitle(issue.getTopic() + "专项优化");
            action.setTopic(issue.getTopic());
            action.setAction(actionByTopic(issue.getTopic()));
            action.setLevel(i == 0 ? "short_term" : "mid_term");
            action.setExpectedBenefit("预计该主题差评率下降 10%-20%");
            action.setOwner("运营主管");
            action.setReason("当前主题影响分为 " + issue.getImpactScore() + "，需要优先处理。");
            actions.add(action);
        }
        return actions;
    }

    private String buildFallbackSummary(int reviewCount,
                                        int negativeCount,
                                        List<OperatorInsightIssueResponse> issues) {
        if (reviewCount == 0) {
            return "近周期暂无可分析评论，建议先引导住客评价并累计样本后再生成建议。";
        }
        String topTopic = CollectionUtils.isEmpty(issues) ? "综合体验" : issues.get(0).getTopic();
        return "近 " + reviewCount + " 条评论中负向反馈 " + negativeCount + " 条，当前最需要优先处理的是" + topTopic + "问题。";
    }

    private List<OperatorInsightIssueResponse> mergeIssues(List<OperatorInsightAiIssue> aiIssues,
                                                           List<OperatorInsightIssueResponse> heuristicIssues) {
        if (CollectionUtils.isEmpty(aiIssues)) {
            return heuristicIssues;
        }

        Map<String, OperatorInsightIssueResponse> heuristicMap = heuristicIssues.stream()
                .collect(Collectors.toMap(OperatorInsightIssueResponse::getTopic, item -> item, (first, second) -> first));
        List<OperatorInsightIssueResponse> merged = new ArrayList<>();
        LinkedHashSet<String> topicOrder = new LinkedHashSet<>();

        for (OperatorInsightAiIssue aiIssue : aiIssues) {
            if (aiIssue == null || !StringUtils.hasText(aiIssue.getTopic())) {
                continue;
            }
            topicOrder.add(aiIssue.getTopic());
            OperatorInsightIssueResponse fallback = heuristicMap.get(aiIssue.getTopic());
            OperatorInsightIssueResponse item = new OperatorInsightIssueResponse();
            item.setTopic(aiIssue.getTopic());
            item.setPriority(StringUtils.hasText(aiIssue.getPriority())
                    ? aiIssue.getPriority()
                    : fallback == null ? "P2" : fallback.getPriority());
            item.setIssueCount(defaultInt(aiIssue.getIssueCount(), fallback == null ? 0 : fallback.getIssueCount()));
            item.setNegativeRatio(defaultDouble(aiIssue.getNegativeRatio(), fallback == null ? 0.0 : fallback.getNegativeRatio()));
            item.setImpactScore(defaultInt(aiIssue.getImpactScore(), fallback == null ? 0 : fallback.getImpactScore()));
            item.setEvidenceCount(defaultInt(aiIssue.getEvidenceCount(), fallback == null ? 0 : fallback.getEvidenceCount()));
            item.setEvidenceSamples(CollectionUtils.isEmpty(aiIssue.getEvidenceSamples())
                    ? fallback == null ? new ArrayList<>() : fallback.getEvidenceSamples()
                    : limitList(aiIssue.getEvidenceSamples(), 3));
            merged.add(item);
        }

        for (OperatorInsightIssueResponse heuristicIssue : heuristicIssues) {
            if (topicOrder.size() >= 5) {
                break;
            }
            if (!topicOrder.contains(heuristicIssue.getTopic())) {
                merged.add(heuristicIssue);
                topicOrder.add(heuristicIssue.getTopic());
            }
        }

        merged.sort(Comparator.comparing(OperatorInsightIssueResponse::getImpactScore).reversed());
        return limitList(merged, 5);
    }

    private List<OperatorInsightActionResponse> mergeActions(List<OperatorInsightAiAction> aiActions,
                                                             List<OperatorInsightIssueResponse> mergedIssues,
                                                             List<OperatorInsightActionResponse> fallbackActions) {
        if (CollectionUtils.isEmpty(aiActions)) {
            return fallbackActions;
        }

        Map<String, OperatorInsightIssueResponse> issueMap = mergedIssues.stream()
                .collect(Collectors.toMap(OperatorInsightIssueResponse::getTopic, item -> item, (first, second) -> first));
        List<OperatorInsightActionResponse> merged = new ArrayList<>();

        for (OperatorInsightAiAction aiAction : aiActions) {
            if (aiAction == null) {
                continue;
            }
            OperatorInsightIssueResponse relatedIssue = issueMap.get(aiAction.getTopic());
            OperatorInsightActionResponse item = new OperatorInsightActionResponse();
            item.setTitle(StringUtils.hasText(aiAction.getTitle())
                    ? aiAction.getTitle()
                    : buildDefaultActionTitle(aiAction.getTopic(), relatedIssue));
            item.setTopic(StringUtils.hasText(aiAction.getTopic()) ? aiAction.getTopic() : "综合体验");
            item.setAction(StringUtils.hasText(aiAction.getAction())
                    ? aiAction.getAction()
                    : actionByTopic(item.getTopic()));
            item.setLevel(normalizeActionLevel(aiAction.getLevel()));
            item.setExpectedBenefit(StringUtils.hasText(aiAction.getExpectedBenefit())
                    ? aiAction.getExpectedBenefit()
                    : "预计改善相关差评并提升复购转化。");
            item.setOwner(StringUtils.hasText(aiAction.getOwner()) ? aiAction.getOwner() : "运营主管");
            item.setReason(StringUtils.hasText(aiAction.getReason())
                    ? aiAction.getReason()
                    : buildDefaultActionReason(relatedIssue));
            merged.add(item);
        }

        if (merged.isEmpty()) {
            return fallbackActions;
        }
        return limitList(merged, 3);
    }

    private List<OperatorInsightAiIssue> toAiIssues(List<OperatorInsightIssueResponse> heuristicIssues) {
        List<OperatorInsightAiIssue> aiIssues = new ArrayList<>();
        for (OperatorInsightIssueResponse issue : heuristicIssues) {
            OperatorInsightAiIssue aiIssue = new OperatorInsightAiIssue();
            aiIssue.setTopic(issue.getTopic());
            aiIssue.setPriority(issue.getPriority());
            aiIssue.setIssueCount(issue.getIssueCount());
            aiIssue.setNegativeRatio(issue.getNegativeRatio());
            aiIssue.setImpactScore(issue.getImpactScore());
            aiIssue.setEvidenceCount(issue.getEvidenceCount());
            aiIssue.setEvidenceSamples(issue.getEvidenceSamples());
            aiIssues.add(aiIssue);
        }
        return aiIssues;
    }

    private List<OperatorInsightAnalysisContext.ReviewSample> buildReviewSamples(List<Review> reviews) {
        List<OperatorInsightAnalysisContext.ReviewSample> samples = new ArrayList<>();
        int maxSamples = Math.max(aiProperties.getMaxReviewSamples(), 1);
        for (int i = 0; i < reviews.size() && i < maxSamples; i++) {
            Review review = reviews.get(i);
            OperatorInsightAnalysisContext.ReviewSample sample = new OperatorInsightAnalysisContext.ReviewSample();
            sample.setRating(review.getRating());
            sample.setCreatedAt(formatReviewTime(review.getCreatedAt()));
            sample.setContent(trimEvidence(review.getContent()));
            samples.add(sample);
        }
        return samples;
    }

    private double calculateAverageRating(List<Review> reviews) {
        int ratingCount = 0;
        int ratingSum = 0;
        for (Review review : reviews) {
            if (review.getRating() != null) {
                ratingCount++;
                ratingSum += review.getRating();
            }
        }
        return ratingCount == 0 ? 0.0 : round2((double) ratingSum / ratingCount);
    }

    private int countNegativeReviews(List<Review> reviews) {
        int count = 0;
        for (Review review : reviews) {
            if (review.getRating() != null && review.getRating() <= 2) {
                count++;
            }
        }
        return count;
    }

    private int countOrdersByStatus(List<BookingOrder> orders, String status) {
        int count = 0;
        for (BookingOrder order : orders) {
            if (Objects.equals(status, order.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private double calculateAverageOrderAmount(List<BookingOrder> orders) {
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (BookingOrder order : orders) {
            if (order.getTotalAmount() != null) {
                total = total.add(order.getTotalAmount());
                count++;
            }
        }
        if (count == 0) {
            return 0.0;
        }
        return round2(total.doubleValue() / count);
    }

    private int normalizePeriodDays(Integer periodDays) {
        if (periodDays == null || periodDays <= 0) {
            return 30;
        }
        return Math.min(periodDays, 180);
    }

    private String normalizeActionLevel(String level) {
        if ("short_term".equals(level) || "mid_term".equals(level) || "long_term".equals(level)) {
            return level;
        }
        return "short_term";
    }

    private String buildDefaultActionTitle(String topic, OperatorInsightIssueResponse relatedIssue) {
        String safeTopic = StringUtils.hasText(topic) ? topic : "综合体验";
        if (relatedIssue == null) {
            return safeTopic + "优化计划";
        }
        return safeTopic + "优化计划（优先级 " + relatedIssue.getPriority() + "）";
    }

    private String buildDefaultActionReason(OperatorInsightIssueResponse issue) {
        if (issue == null) {
            return "结合近期评论波动，建议先做小步快跑式优化。";
        }
        return "该主题当前负向占比为 " + issue.getNegativeRatio() + "，影响分为 " + issue.getImpactScore() + "。";
    }

    private String priorityByScore(int impactScore) {
        if (impactScore >= 70) {
            return "P1";
        }
        if (impactScore >= 40) {
            return "P2";
        }
        return "P3";
    }

    private String detectTopic(String content) {
        if (!StringUtils.hasText(content)) {
            return "综合体验";
        }
        String text = content.toLowerCase();
        Map<String, String[]> mapping = new LinkedHashMap<>();
        mapping.put("卫生", new String[]{"卫生", "脏", "异味", "清洁"});
        mapping.put("服务", new String[]{"服务", "态度", "前台", "响应"});
        mapping.put("餐饮", new String[]{"早餐", "餐饮", "口味", "饭菜"});
        mapping.put("设施", new String[]{"设施", "空调", "热水", "隔音", "wifi"});
        mapping.put("活动", new String[]{"活动", "项目", "无聊", "体验"});
        mapping.put("价格", new String[]{"价格", "贵", "性价比", "收费"});
        for (Map.Entry<String, String[]> entry : mapping.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return "综合体验";
    }

    private String actionByTopic(String topic) {
        if ("卫生".equals(topic)) {
            return "建立客房清洁复检清单，并在高峰时段增加抽检。";
        }
        if ("服务".equals(topic)) {
            return "针对前台和客服补充标准话术与响应时效培训。";
        }
        if ("餐饮".equals(topic)) {
            return "优化早餐菜单结构，并收集住客对口味和分量的反馈。";
        }
        if ("设施".equals(topic)) {
            return "优先排查空调、热水、网络和隔音问题，形成维修闭环。";
        }
        if ("活动".equals(topic)) {
            return "重排活动时段与动线，补充活动前告知和现场引导。";
        }
        if ("价格".equals(topic)) {
            return "重新梳理套餐组合和价格分层，突出可感知价值。";
        }
        return "建立问题跟踪清单，按周复盘整改结果。";
    }

    private String formatReviewTime(Date createdAt) {
        if (createdAt == null) {
            return null;
        }
        return REVIEW_TIME_FORMATTER.format(Instant.ofEpochMilli(createdAt.getTime()));
    }

    private String trimEvidence(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 60 ? normalized : normalized.substring(0, 60) + "...";
    }

    private double round2(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private Integer defaultInt(Integer value, Integer fallback) {
        return value != null ? value : fallback;
    }

    private Double defaultDouble(Double value, Double fallback) {
        return value != null ? round2(value) : round2(fallback);
    }

    private <T> List<T> limitList(List<T> list, int limit) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list.stream().limit(limit).collect(Collectors.toList()));
    }

    private FarmStay ensureOwnerAndGetFarmStay(Long farmStayId) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        FarmStay farmStay = farmStayMapper.selectByIdAndOwner(farmStayId, ownerId);
        if (farmStay == null) {
            throw new BusinessException("仅可查看和分析自己名下的店铺");
        }
        return farmStay;
    }

    private void saveReport(FarmStay farmStay, OperatorInsightReportResponse report) {
        try {
            OperatorInsightReportRecord record = new OperatorInsightReportRecord();
            record.setReportId(report.getReportId());
            record.setFarmStayId(report.getFarmStayId());
            record.setOwnerId(farmStay.getOwnerId());
            record.setPeriodDays(report.getPeriodDays());
            record.setGenerationMode(report.getGenerationMode());
            record.setModel(report.getModel());
            record.setReviewCount(report.getReviewCount());
            record.setAverageRating(report.getAverageRating());
            record.setSummary(report.getSummary());
            record.setReportJson(objectMapper.writeValueAsString(report));
            record.setGeneratedAt(report.getGeneratedAt());
            operatorInsightReportMapper.insert(record);
        } catch (Exception ex) {
            log.error("Failed to persist operator insight report. farmStayId={}, reportId={}",
                    farmStay.getId(), report.getReportId(), ex);
            throw new IllegalStateException("Failed to persist operator insight report", ex);
        }
    }

    private OperatorInsightReportResponse loadLatestFromDb(Long farmStayId) {
        OperatorInsightReportRecord record = operatorInsightReportMapper.selectLatestByFarmStayId(farmStayId);
        return toReport(record);
    }

    private List<OperatorInsightReportResponse> loadHistoryFromDb(Long farmStayId) {
        List<OperatorInsightReportRecord> records = operatorInsightReportMapper.selectByFarmStayId(farmStayId);
        List<OperatorInsightReportResponse> reports = new ArrayList<>();
        for (OperatorInsightReportRecord record : records) {
            OperatorInsightReportResponse report = toReport(record);
            if (report != null) {
                reports.add(report);
            }
        }
        return reports;
    }

    private OperatorInsightReportResponse loadDetailFromDb(Long farmStayId, Long reportId) {
        return toReport(operatorInsightReportMapper.selectByReportId(farmStayId, reportId));
    }

    private OperatorInsightReportResponse toReport(OperatorInsightReportRecord record) {
        if (record == null || !StringUtils.hasText(record.getReportJson())) {
            return null;
        }
        try {
            return objectMapper.readValue(record.getReportJson(), OperatorInsightReportResponse.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize operator insight report. reportId={}", record.getReportId(), ex);
            return null;
        }
    }

    private void cacheReports(Long farmStayId, List<OperatorInsightReportResponse> reports) {
        if (CollectionUtils.isEmpty(reports)) {
            return;
        }
        reportStore.put(farmStayId, new CopyOnWriteArrayList<>(reports));
    }

    private void appendCacheReport(Long farmStayId, OperatorInsightReportResponse report) {
        if (report == null) {
            return;
        }
        CopyOnWriteArrayList<OperatorInsightReportResponse> reports =
                reportStore.computeIfAbsent(farmStayId, key -> new CopyOnWriteArrayList<>());
        boolean exists = reports.stream().anyMatch(item -> Objects.equals(item.getReportId(), report.getReportId()));
        if (!exists) {
            reports.add(report);
        }
    }

    private static class TopicStat {
        private int total;
        private int negative;
    }
}
