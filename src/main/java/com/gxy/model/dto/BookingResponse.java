package com.gxy.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class BookingResponse {

    private Long id;

    private String orderNo;

    private Long visitorId;

    private Long farmStayId;

    private Long roomTypeId;

    private Date checkInDate;

    private Date checkOutDate;

    private Integer guests;

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
