package com.gxy.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatSessionUpdateRequest {

    @NotBlank(message = "会话标题不能为空")
    @Size(max = 200, message = "会话标题长度不能超过200")
    private String title;
}
