package com.gxy.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiChatFeedbackRequest {

    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @NotNull(message = "消息ID不能为空")
    private Long messageId;

    @NotNull(message = "反馈状态不能为空")
    private Boolean useful;

    private String comment;
}