package com.gxy.util;

import cn.hutool.crypto.digest.DigestUtil;

import java.util.Optional;

public class PasswordUtil {

    private PasswordUtil() {
    }

    /**
     * 使用盐值验证密码
     */
    public static boolean matches(String rawPassword, String salt, String encodedPassword) {
        String merged = rawPassword + Optional.ofNullable(salt).orElse("");
        return DigestUtil.sha256Hex(merged).equalsIgnoreCase(encodedPassword);
    }

    public static String hashWithSalt(String rawPassword, String salt) {
        String merged = rawPassword + Optional.ofNullable(salt).orElse("");
        return DigestUtil.sha256Hex(merged);
    }
}
