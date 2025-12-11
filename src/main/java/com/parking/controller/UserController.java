package com.parking.controller;

import com.parking.model.dto.UserDTO;
import com.parking.model.dto.UserLoginRequestDTO;
import com.parking.model.dto.UserLoginResponseDTO;
import com.parking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public UserLoginResponseDTO login(@RequestBody UserLoginRequestDTO loginRequest) {
        return userService.login(loginRequest);
    }
    
    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    public UserDTO getUserInfo(@RequestAttribute("userId") Long userId) {
        return userService.getUserById(userId);
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public UserDTO updateUserInfo(@RequestBody UserDTO userDTO, @RequestAttribute("userId") Long currentUserId) {
        System.out.println("========== 更新用户信息 ==========");
        System.out.println("前端传递的用户ID: " + userDTO.getId());
        System.out.println("拦截器设置的当前用户ID: " + currentUserId);
        
        // 强制使用当前登录用户的ID，避免前端传递错误的ID
        // 先设置ID，确保后续逻辑使用正确的用户ID
        userDTO.setId(currentUserId);
        
        System.out.println("最终使用的用户ID: " + userDTO.getId());
        System.out.println("要更新的车牌号: " + userDTO.getLicensePlate());
        
        return userService.updateUserInfo(userDTO);
    }
    
}