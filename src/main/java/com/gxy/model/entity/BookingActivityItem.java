package com.gxy.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class BookingActivityItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private Long activityItemId;

    private String itemName;

    private BigDecimal price;

    private Integer quantity;

    private Date createdAt;
}
