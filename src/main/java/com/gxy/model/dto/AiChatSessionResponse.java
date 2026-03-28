package com.gxy.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class AiChatSessionResponse {

    private Long sessionId;

    private Long userId;

    private Long farmStayId;

    private String scene;

    private String title;

    private Date lastMessageAt;

    private Date createdAt;
}
