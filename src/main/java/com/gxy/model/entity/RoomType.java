package com.gxy.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 房型与库存信息
 */
@Data
public class RoomType implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long farmStayId;

    private String name;

    private String description;

    private String bedType;

    private Integer maxGuests;

    private BigDecimal price;

    private Integer stock;

    private String tags;

    private String status;

    private Date createdAt;

    private Date updatedAt;
}
