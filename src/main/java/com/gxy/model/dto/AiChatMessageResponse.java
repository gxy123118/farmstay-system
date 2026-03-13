package com.gxy.model.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class AiChatMessageResponse {

    private Long messageId;

    private String role;

    private String content;

    private Date createdAt;

    private List<AiCitationResponse> citations;

    private Double confidence;

    private String refuseReason;
}

