package com.gxy.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ActivityResponse {

    private Long id;

    private Long farmStayId;

    private String name;

    private String description;

    private String schedule;

    private Integer capacity;

    private BigDecimal price;

    private String coverImage;

    private String tags;

    private String status;

    private Date createdAt;

    private Date updatedAt;
}
