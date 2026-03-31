package com.gxy.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminWithdrawReviewRequest {

    @NotBlank(message = "审核备注不能为空")
    private String reviewRemark;
}
