package com.gxy.model.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class OperatorInsightAnalysisContext {

    private Long farmStayId;

    private String farmStayName;

    private String city;

    private String description;

    private String tags;

    private String priceRange;

    private String priceLevel;

    private Integer periodDays;

    private Integer reviewCount;

    private Double averageRating;

    private Integer negativeReviewCount;

    private Integer orderCount;

    private Integer paidOrderCount;

    private Integer refundedOrderCount;

    private Integer cancelledOrderCount;

    private Double averageOrderAmount;

    private List<OperatorInsightAiIssue> heuristicIssues;

    private List<ReviewSample> reviewSamples;

    @Data
    public static class ReviewSample {
        private Integer rating;
        private String createdAt;
        private String content;
    }
}
