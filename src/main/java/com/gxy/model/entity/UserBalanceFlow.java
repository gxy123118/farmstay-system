package com.gxy.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class UserBalanceFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String flowNo;

    private Long userId;

    private String changeType;

    private String bizNo;

    private BigDecimal amount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private String remark;

    private Date createdAt;
}
