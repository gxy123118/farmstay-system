package com.gxy.common.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.gxy.common.exception.BusinessException;
import com.gxy.model.enums.UserType;

import java.util.Objects;

/**
 * 统一登录身份校验工具。
 */
public final class AuthGuard {

    private static final String SESSION_KEY_USER_TYPE = "userType";

    private AuthGuard() {
    }

    public static void enforceVisitor() {
        enforceAtLeast(UserType.VISITOR, "仅游客及以上角色可执行该操作");
    }

    public static void enforceAtLeastVisitor() {
        enforceAtLeast(UserType.VISITOR, "仅游客及以上角色可执行该操作");
    }

    public static void enforceOperator() {
        enforceUserType(UserType.OPERATOR, "仅经营者可执行该操作");
    }

    public static void enforceAtLeastOperator() {
        enforceAtLeast(UserType.OPERATOR, "仅经营者及以上角色可执行该操作");
    }

    public static void enforceAdmin() {
        enforceUserType(UserType.ADMIN, "仅管理员可执行该操作");
    }

    public static boolean isAdmin() {
        return Objects.equals(UserType.ADMIN, currentUserType());
    }

    private static void enforceUserType(UserType expected, String message) {
        UserType current = currentUserType();
        if (!Objects.equals(expected, current)) {
            throw new BusinessException(message);
        }
    }

    private static void enforceAtLeast(UserType expected, String message) {
        UserType current = currentUserType();
        if (rank(current) < rank(expected)) {
            throw new BusinessException(message);
        }
    }

    private static UserType currentUserType() {
        Object userTypeValue = StpUtil.getSession().get(SESSION_KEY_USER_TYPE);
        UserType current = UserType.fromCode(userTypeValue == null ? null : String.valueOf(userTypeValue));
        if (current == null) {
            throw new BusinessException("登录身份无效，请重新登录");
        }
        return current;
    }

    private static int rank(UserType userType) {
        if (userType == UserType.ADMIN) {
            return 3;
        }
        if (userType == UserType.OPERATOR) {
            return 2;
        }
        if (userType == UserType.VISITOR) {
            return 1;
        }
        return 0;
    }
}
