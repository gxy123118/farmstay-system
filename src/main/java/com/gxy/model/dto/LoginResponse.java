package com.gxy.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoginResponse {

    private String token;

    private String loginType;

    private Long expire;

    private Long userId;

    private String username;

    private String displayName;

    private String status;

    private BigDecimal balance;
}
