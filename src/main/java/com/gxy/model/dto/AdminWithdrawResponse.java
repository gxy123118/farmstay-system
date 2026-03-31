package com.gxy.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class AdminWithdrawResponse {

    private Long id;

    private String withdrawNo;

    private Long userId;

    private String username;

    private String displayName;

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
