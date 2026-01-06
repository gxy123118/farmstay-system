package com.gxy.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class ReviewResponse {

    private Long id;

    private Long orderId;

    private Long farmStayId;

    private Long visitorId;

    private Integer rating;

    private String content;

    private String visitorName;

    private Date createdAt;
}
