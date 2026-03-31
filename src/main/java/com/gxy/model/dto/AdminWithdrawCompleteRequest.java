package com.gxy.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminWithdrawCompleteRequest {

    @NotBlank(message = "打款凭证号不能为空")
    private String transferNo;

    private String reviewRemark;
}
