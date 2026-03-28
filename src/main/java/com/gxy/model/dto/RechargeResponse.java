package com.gxy.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class RechargeResponse {

    private String rechargeNo;

    private BigDecimal amount;

    private String payMethod;

    private String status;

    private String payInfo;

    private String qrCode;

    private Date createdAt;

    private Date paidAt;
}
