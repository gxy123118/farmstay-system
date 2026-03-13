package com.gxy.model.dto;

import lombok.Data;

@Data
public class AiChatSessionCreateRequest {

    /**
     * 可选，指定上下文店铺。
     */
    private Long farmStayId;

    /**
     * 可选，会话场景（booking / policy / travel）。
     */
    private String scene;
}

