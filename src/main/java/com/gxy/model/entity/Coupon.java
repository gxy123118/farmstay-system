package com.gxy.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券实体
 */
@Data
public class Coupon implements Serializable {

    private static final long serialVersionUID = 1L;

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

    private Date createdAt;

    private Date updatedAt;
}
