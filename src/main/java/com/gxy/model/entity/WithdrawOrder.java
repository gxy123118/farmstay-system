package com.gxy.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class WithdrawOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String withdrawNo;

    private Long userId;

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

    private Date updatedAt;
}
