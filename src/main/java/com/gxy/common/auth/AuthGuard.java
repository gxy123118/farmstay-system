package com.gxy.common.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.gxy.common.exception.BusinessException;
import com.gxy.model.enums.UserType;

import java.util.Objects;

/**
 * 统一的登录身份校验工具。
 * <p>
 * 说明：当前项目将用户身份（游客/经营者）存储在 Sa-Token 的 Session 中（key: userType），
 * 因此这里通过 Session 值进行校验。
 */
public final class AuthGuard {

    private static final String SESSION_KEY_USER_TYPE = "userType";

    private AuthGuard() {
    }

    /**
     * 仅允许游客执行。
     */
    public static void enforceVisitor() {
        enforceUserType(UserType.VISITOR, "仅游客可执行该操作");
    }

    /**
     * 仅允许经营者执行。
     */
    public static void enforceOperator() {
        enforceUserType(UserType.OPERATOR, "仅经营者可执行该操作");
    }

    private static void enforceUserType(UserType expected, String message) {
        Object userType = StpUtil.getSession().get(SESSION_KEY_USER_TYPE);
        if (!Objects.equals(expected.getCode(), userType)) {
            throw new BusinessException(message);
        }
    }
}

