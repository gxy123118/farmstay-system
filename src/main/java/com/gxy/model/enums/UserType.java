package com.gxy.model.enums;

import lombok.Getter;

@Getter
public enum UserType {

    VISITOR("visitor", "游客"),
    OPERATOR("operator", "经营者");

    private final String code;

    private final String description;

    UserType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UserType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (UserType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
