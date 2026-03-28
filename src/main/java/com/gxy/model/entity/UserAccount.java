package com.gxy.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class UserAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String displayName;

    private String userType;

    private String status;

    private BigDecimal balance;
}
