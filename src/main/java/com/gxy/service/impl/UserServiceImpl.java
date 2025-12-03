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
        return new LoginResponse(StpUtil.getTokenValue(), userType.getCode(), StpUtil.getTokenTimeout());
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
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
        user.setDisplayName(request.getDisplayName());
        user.setUserType(userType.getCode());
        user.setStatus("ACTIVE");
        user.setPassword(PasswordUtil.hashWithSalt(request.getPassword(), salt));
        userMapper.insertUser(user);
        StpUtil.login(user.getId(), userType.getCode());
        return new LoginResponse(StpUtil.getTokenValue(), userType.getCode(), StpUtil.getTokenTimeout());
    }
}
