package com.gxy.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class AdminReviewResponse {

    private Long id;

    private Long orderId;

    private Long farmStayId;

    private String farmStayName;

    private Long visitorId;

    private String visitorUsername;

    private Integer rating;

    private String content;

    private Date createdAt;
}
