package com.gxy.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OperatorOrderSummaryResponse {

    private Long farmStayCount;

    private Long orderCount;

    private Long paidOrderCount;

    private Long refundedOrderCount;

    private BigDecimal grossTransactionAmount;

    private BigDecimal refundAmount;

    private BigDecimal netTransactionAmount;

    private Double refundRate;
}
