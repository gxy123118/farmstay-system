package com.gxy.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class RoomResponse {

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
