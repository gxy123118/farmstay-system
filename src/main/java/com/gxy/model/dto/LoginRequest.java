package com.gxy.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 角色类型：visitor=游客，operator=经营者
     */
    @NotBlank(message = "角色类型不能为空")
    private String userType;
}
