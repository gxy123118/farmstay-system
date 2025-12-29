package com.gxy.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 支付下单请求（接口框架，暂不对接第三方）
 */
@Data
public class PaymentRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotBlank(message = "支付渠道不能为空")
    private String channel;
}
