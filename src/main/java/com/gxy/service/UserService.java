package com.gxy.service;

import com.gxy.model.dto.LoginRequest;
import com.gxy.model.dto.LoginResponse;
import com.gxy.model.dto.RegisterRequest;

public interface UserService {

    /**
     * 登录并返回 Sa-Token 信息
     */
    LoginResponse login(LoginRequest request);

    /**
     * 注册并自动登录
     */
    LoginResponse register(RegisterRequest request);
}
