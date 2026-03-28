package com.gxy.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class AiChatMessageRecord {

    private Long id;

    private Long sessionId;

    private String role;

    private String content;

    private String citationsJson;

    private Double confidence;

    private String refuseReason;

    private Boolean fallback;

    private Boolean useful;

    private String feedbackComment;

    private Date createdAt;

    private Date updatedAt;
}
