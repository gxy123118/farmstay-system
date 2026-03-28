package com.gxy.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class AdminUserResponse {

    private Long id;

    private String username;

    private String displayName;

    private String userType;

    private String status;

    private BigDecimal balance;

    private Date createdAt;

    private Date updatedAt;
}
