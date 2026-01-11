package com.gxy.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class DiningItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long farmStayId;

    private String name;

    private String description;

    private BigDecimal price;

    private String coverImage;

    private String tags;

    private String status;

    private Date createdAt;

    private Date updatedAt;
}
