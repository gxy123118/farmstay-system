package com.gxy.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预订订单实体
 */
@Data
public class BookingOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    private Long visitorId;

    private Long farmStayId;

    private Long roomTypeId;

    private Date checkInDate;

    private Date checkOutDate;

    private Integer guests;

    private BigDecimal diningAmount;

    private BigDecimal activityAmount;

    private BigDecimal totalAmount;

    private String status;

    private String paymentChannel;

    private String contactName;

    private String contactPhone;

    private String couponCode;

    private String remarks;

    private Date createdAt;

    private Date updatedAt;
}
