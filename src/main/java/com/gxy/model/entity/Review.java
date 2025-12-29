package com.gxy.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 游客评价实体
 */
@Data
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private Long farmStayId;

    private Long visitorId;

    private Integer rating;

    private String content;

    private String status;

    private Date createdAt;
}
