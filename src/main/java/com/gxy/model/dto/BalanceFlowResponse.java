package com.gxy.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class BalanceFlowResponse {

    private String flowNo;

    private String changeType;

    private String bizNo;

    private BigDecimal amount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private String remark;

    private Date createdAt;
}
