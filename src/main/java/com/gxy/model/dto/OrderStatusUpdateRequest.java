package com.gxy.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 订单状态更新请求
 */
@Data
public class OrderStatusUpdateRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotBlank(message = "目标状态不能为空")
    private String status;

    private String paymentChannel;
}
