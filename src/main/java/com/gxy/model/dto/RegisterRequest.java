package com.gxy.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RegisterRequest {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String displayName;

    /**
     * 用户类型：visitor 或 operator
     */
    @NotBlank(message = "角色类型不能为空")
    private String userType;
}
