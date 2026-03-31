package com.gxy.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawCreateRequest {

    @NotNull(message = "提现金额不能为空")
    @DecimalMin(value = "0.01", message = "提现金额必须大于 0")
    private BigDecimal amount;

    @NotBlank(message = "提现渠道不能为空")
    private String channel;

    @NotBlank(message = "收款人不能为空")
    private String accountName;

    @NotBlank(message = "收款账号不能为空")
    private String accountNo;

    private String remark;
}
