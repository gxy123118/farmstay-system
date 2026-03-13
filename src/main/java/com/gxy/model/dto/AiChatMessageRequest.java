package com.gxy.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class AiChatMessageRequest {

    @NotBlank(message = "问题不能为空")
    private String question;
}
