package com.gxy.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class RefundOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String refundNo;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private BigDecimal refundAmount;

    private String refundChannel;

    private String status;

    private String reason;

    private Date createdAt;

    private Date refundedAt;

    private Date updatedAt;
}
