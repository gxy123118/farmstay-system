package com.gxy.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class OperatorInsightReportRecord {

    private Long id;

    private Long reportId;

    private Long farmStayId;

    private Long ownerId;

    private Integer periodDays;

    private String generationMode;

    private String model;

    private Integer reviewCount;

    private Double averageRating;

    private String summary;

    private String reportJson;

    private Integer deleted;

    private Date generatedAt;

    private Date createdAt;

    private Date updatedAt;
}
