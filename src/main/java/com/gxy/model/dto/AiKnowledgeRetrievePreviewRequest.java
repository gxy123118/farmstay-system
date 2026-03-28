package com.gxy.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiKnowledgeRetrievePreviewRequest {

    private Long farmStayId;

    private String scene;

    @NotBlank(message = "question不能为空")
    private String question;
}
