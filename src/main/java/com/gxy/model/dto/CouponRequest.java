package com.gxy.model.dto;

import lombok.Data;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券创建请求
 */
@Data
public class CouponRequest {

    @NotBlank(message = "优惠券标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "优惠金额不能为空")
    private BigDecimal discountAmount;

    @NotNull(message = "使用门槛不能为空")
    private BigDecimal minimumSpend;

    @NotNull(message = "生效时间不能为空")
    @Future(message = "生效时间需晚于当前时间")
    private Date validFrom;

    @NotNull(message = "失效时间不能为空")
    @Future(message = "失效时间需晚于当前时间")
    private Date validTo;

    /**
     * 为空则表示全平台可用
     */
    private Long farmStayId;

    @NotNull(message = "发放数量不能为空")
    private Integer totalCount;

    private String status;
}
