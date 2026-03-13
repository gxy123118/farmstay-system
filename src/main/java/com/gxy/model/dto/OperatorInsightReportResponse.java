package com.gxy.model.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class OperatorInsightReportResponse {

    private Long reportId;

    private Long farmStayId;

    private Integer periodDays;

    private Integer reviewCount;

    private Double averageRating;

    private String summary;

    private Date generatedAt;

    private String generationMode;

    private String model;

    private List<OperatorInsightIssueResponse> issues;

    private List<OperatorInsightActionResponse> actions;
}
