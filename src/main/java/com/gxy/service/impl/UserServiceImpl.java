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

    private final UserMapper userMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        extracted(request.getUsername(), request.getPassword());
        UserType userType = UserType.fromCode(request.getUserType());
        if (userType == null) {
            throw new BusinessException("当前只支持游客或经营者登录");
        }
        User user = userMapper.selectByUsernameAndType(request.getUsername(), userType.getCode());
        if (user == null) {
            throw new BusinessException("账号或角色不存在");
        }
        if (!PasswordUtil.matches(request.getPassword(), user.getSalt(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        StpUtil.login(user.getId(), userType.getCode());
        StpUtil.getSession().set("userType", userType.getCode());
        return new LoginResponse(StpUtil.getTokenValue(), userType.getCode(), StpUtil.getTokenTimeout(), user.getId());
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        extracted(request.getUsername(), request.getPassword());
        UserType userType = UserType.fromCode(request.getUserType());
        if (userType == null) {
            throw new BusinessException("当前只支持游客或经营者注册");
        }
        if (userMapper.selectByUsernameAndType(request.getUsername(), userType.getCode()) != null) {
            throw new BusinessException("当前账号已存在");
        }
        String salt = RandomUtil.randomString(6);
        User user = new User();
        user.setUsername(request.getUsername());
        user.setSalt(salt);
        user.setDisplayName(Optional.ofNullable(request.getDisplayName()).orElse(request.getUsername()));
        user.setUserType(userType.getCode());
        user.setStatus("ACTIVE");
        user.setPassword(PasswordUtil.hashWithSalt(request.getPassword(), salt));
        userMapper.insertUser(user);
        StpUtil.login(user.getId(), userType.getCode());
        StpUtil.getSession().set("userType", userType.getCode());
        return new LoginResponse(StpUtil.getTokenValue(), userType.getCode(), StpUtil.getTokenTimeout(), user.getId());
    }

    private void extracted(String username, String password) {
        // 验证账号格式：只能包含字母、数字和特定符号（如：!@#$%^&*()_+-=[]{}|;':\",./<>?）
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException("账号不能为空");
        }
        if (!isValidAccount(username)) {
            throw new BusinessException("账号只能包含字母、数字和特定符号");
        }

        // 验证密码格式：至少6位，只能包含字母、数字和特定符号
        if (password == null || password.length() < 6) {
            throw new BusinessException("密码长度不能少于6位");
        }
        if (!isValidPassword(password)) {
            throw new BusinessException("密码只能包含字母、数字和特定符号");
        }
    }

    // 验证账号是否只包含字母、数字和特定符号
    private boolean isValidAccount(String account) {
        return account != null && account.matches("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?`~]*$");
    }

    // 验证密码是否只包含字母、数字和特定符号且长度>=6
    private boolean isValidPassword(String password) {
        return password != null &&
                password.length() >= 6 &&
                password.matches("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?`~]*$");
    }
}
