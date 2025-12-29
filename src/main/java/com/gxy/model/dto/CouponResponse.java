package com.gxy.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class CouponResponse {

    private Long id;

    private String code;

    private String title;

    private String description;

    private BigDecimal discountAmount;

    private BigDecimal minimumSpend;

    private Date validFrom;

    private Date validTo;

    private Long farmStayId;

    private Integer totalCount;

    private Integer usedCount;

    private String status;
}
