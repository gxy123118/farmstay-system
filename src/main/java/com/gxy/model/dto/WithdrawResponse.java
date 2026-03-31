package com.gxy.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class WithdrawResponse {

    private Long id;

    private String withdrawNo;

    private BigDecimal amount;

    private String channel;

    private String accountName;

    private String accountNo;

    private String status;

    private String remark;

    private String reviewRemark;

    private String transferNo;

    private Date createdAt;

    private Date reviewedAt;

    private Date paidAt;
}
