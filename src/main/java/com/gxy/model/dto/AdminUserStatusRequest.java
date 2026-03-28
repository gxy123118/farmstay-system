package com.gxy.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminUserStatusRequest {

    @NotBlank(message = "status不能为空")
    private String status;
}
