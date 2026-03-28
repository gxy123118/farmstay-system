package com.gxy.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminDashboardResponse {

    private Long orderCount;

    private BigDecimal turnover;

    private Double refundRate;

    private Long farmStayCount;

    private Long activeOperatorCount;
}
