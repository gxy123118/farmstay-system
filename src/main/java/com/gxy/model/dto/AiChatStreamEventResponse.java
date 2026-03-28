package com.gxy.model.dto;

import lombok.Data;

@Data
public class AiChatStreamEventResponse {

    private String type;

    private Long sessionId;

    private Long messageId;

    private String model;

    private Boolean fallback;

    private String content;

    private String message;

    private AiCitationResponse citation;
}
