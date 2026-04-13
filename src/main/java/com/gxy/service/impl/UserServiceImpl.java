package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.UserMapper;
import com.gxy.model.dto.LoginRequest;
import com.gxy.model.dto.LoginResponse;
import com.gxy.model.dto.RegisterRequest;
import com.gxy.model.entity.User;
import com.gxy.model.enums.UserType;
import com.gxy.service.UserService;
import com.gxy.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final UserMapper userMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        validateCredentials(request.getUsername(), request.getPassword());

        UserType userType = UserType.fromCode(request.getUserType());
        if (userType == null) {
            throw new BusinessException("Only visitor/operator/admin can login");
        }

        User user = userMapper.selectByUsernameAndType(request.getUsername(), userType.getCode());
        if (user == null) {
            throw new BusinessException("Account or role not found");
        }
        ensureUserActive(user);

        if (!PasswordUtil.matches(request.getPassword(), user.getSalt(), user.getPassword())) {
            throw new BusinessException("Invalid username or password");
        }

        StpUtil.login(user.getId(), userType.getCode());
        StpUtil.getSession().set("userType", userType.getCode());
        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        validateCredentials(request.getUsername(), request.getPassword());

        UserType userType = UserType.fromCode(request.getUserType());
        if (userType == null || userType == UserType.ADMIN) {
            throw new BusinessException("Only visitor or operator can register");
        }
        if (userMapper.selectByUsernameAndType(request.getUsername(), userType.getCode()) != null) {
            throw new BusinessException("Account already exists");
        }

        String salt = RandomUtil.randomString(6);
        User user = new User();
        user.setUsername(request.getUsername());
        user.setSalt(salt);
        user.setDisplayName(Optional.ofNullable(request.getDisplayName()).orElse(request.getUsername()));
        user.setUserType(userType.getCode());
        user.setStatus(STATUS_ACTIVE);
        user.setPassword(PasswordUtil.hashWithSalt(request.getPassword(), salt));
        userMapper.insertUser(user);

        StpUtil.login(user.getId(), userType.getCode());
        StpUtil.getSession().set("userType", userType.getCode());
        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse currentUser() {
        StpUtil.checkLogin();
        User user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BusinessException("Current user not found");
        }
        ensureUserActive(user);
        return buildLoginResponse(user);
    }

    private void validateCredentials(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException("Username is required");
        }
        if (!isValidAccount(username)) {
            throw new BusinessException("Username contains unsupported characters");
        }
        if (password == null || password.length() < 6) {
            throw new BusinessException("Password length must be >= 6");
        }
        if (!isValidPassword(password)) {
            throw new BusinessException("Password contains unsupported characters");
        }
    }

    private boolean isValidAccount(String account) {
        return account != null && account.matches("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?`~]*$");
    }

    private boolean isValidPassword(String password) {
        return password != null
                && password.length() >= 6
                && password.matches("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?`~]*$");
    }

    private void ensureUserActive(User user) {
        if (!STATUS_ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException("Account has been disabled");
        }
    }

    private LoginResponse buildLoginResponse(User user) {
        LoginResponse response = new LoginResponse();
        response.setToken(StpUtil.getTokenValue());
        response.setLoginType(user.getUserType());
        response.setExpire(StpUtil.getTokenTimeout());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setDisplayName(Optional.ofNullable(user.getDisplayName()).filter(name -> !name.isBlank()).orElse(user.getUsername()));
        response.setStatus(user.getStatus());
        response.setBalance(user.getBalance());
        return response;
    }
}

