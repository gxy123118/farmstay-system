package com.gxy.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentResponse {

    /**
     * 示例：跳转URL或二维码内容
     */
    private String payInfo;

    /**
     * 订单状态
     */
    private String status;
}
