package com.gxy.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class AiChatSessionResponse {

    private Long sessionId;

    private Long userId;

    private Long farmStayId;

    private String scene;

    private Date createdAt;
}

