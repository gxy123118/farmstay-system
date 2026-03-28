package com.gxy.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiKnowledgeStatusRequest {

    @NotBlank(message = "status不能为空")
    @Size(max = 16, message = "status长度不能超过16")
    private String status;
}
