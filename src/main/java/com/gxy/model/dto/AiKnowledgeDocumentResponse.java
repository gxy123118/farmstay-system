package com.gxy.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class AiKnowledgeDocumentResponse {

    private Long id;

    private String knowledgeCode;

    private String title;

    private String content;

    private String summary;

    private String keywords;

    private String scope;

    private Long farmStayId;

    private String status;

    private Long createdBy;

    private Long updatedBy;

    private Date createdAt;

    private Date updatedAt;
}
