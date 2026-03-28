package com.gxy.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class AiChatSessionRecord {

    private Long id;

    private Long userId;

    private Long farmStayId;

    private String scene;

    private String title;

    private Date lastMessageAt;

    private Date createdAt;

    private Date updatedAt;
}
