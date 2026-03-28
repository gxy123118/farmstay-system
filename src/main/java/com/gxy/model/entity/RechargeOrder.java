package com.gxy.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class RechargeOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String rechargeNo;

    private Long userId;

    private BigDecimal amount;

    private String payMethod;

    private String status;

    private String thirdTradeNo;

    private String subject;

    private String notifyContent;

    private Date createdAt;

    private Date paidAt;

    private Date updatedAt;
}
