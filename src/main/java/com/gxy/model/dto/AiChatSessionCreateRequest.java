package com.gxy.model.dto;

import lombok.Data;

@Data
public class AiChatSessionCreateRequest {

    /**
     * 可选，指定上下文民宿。
     */
    private Long farmStayId;

    /**
     * 可选，会话场景，例如 booking / policy / travel。
     */
    private String scene;
}