package com.parking.controller;

import com.parking.model.dto.UserDTO;
import com.parking.model.dto.UserLoginRequestDTO;
import com.parking.model.dto.UserLoginResponseDTO;
import com.parking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 处理用户认证相关请求
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 用户登录接口
     * 支持用户名密码登录和微信小程序登录
     * @param loginRequest 登录请求参数
     * @return 登录响应，包含token和用户信息
     */
    @PostMapping("/login")
    public UserLoginResponseDTO login(@RequestBody UserLoginRequestDTO loginRequest) {
        // 调用用户服务进行登录验证
        return userService.login(loginRequest);
    }
    
    /**
     * 刷新token接口
     * @param userId 当前用户ID
     * @return 新的token
     */
    @GetMapping("/refresh")
    public UserLoginResponseDTO refreshToken(@RequestAttribute("userId") Long userId) {
        // 调用用户服务刷新token
        return userService.refreshToken(userId);
    }
}