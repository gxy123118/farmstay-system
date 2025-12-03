package com.gxy.model.entity;

import com.gxy.model.enums.UserType;
import lombok.Data;

import java.io.Serializable;

@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String password;

    private String salt;

    private String userType;

    private String displayName;

    private String status;

    public UserType toUserType() {
        return UserType.fromCode(userType);
    }
}
