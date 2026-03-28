package com.gxy.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiKnowledgeDocumentRequest {

    @NotBlank(message = "knowledgeCode不能为空")
    @Size(max = 64, message = "knowledgeCode长度不能超过64")
    private String knowledgeCode;

    @NotBlank(message = "title不能为空")
    @Size(max = 200, message = "title长度不能超过200")
    private String title;

    @NotBlank(message = "content不能为空")
    private String content;

    @Size(max = 500, message = "summary长度不能超过500")
    private String summary;

    @Size(max = 500, message = "keywords长度不能超过500")
    private String keywords;

    @NotBlank(message = "docType不能为空")
    @Size(max = 32, message = "docType长度不能超过32")
    private String docType;

    @NotBlank(message = "scope不能为空")
    @Size(max = 32, message = "scope长度不能超过32")
    private String scope;

    private Long farmStayId;

    @Max(value = 999999, message = "priority过大")
    private Integer priority;

    @Size(max = 16, message = "status长度不能超过16")
    private String status;
}
