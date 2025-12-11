package com.parking.service;

import com.parking.model.dto.UserDTO;
import com.parking.model.dto.UserLoginRequestDTO;
import com.parking.model.dto.UserLoginResponseDTO;
import com.parking.model.entity.UserEntity;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    UserLoginResponseDTO login(UserLoginRequestDTO loginRequest);
    
    /**
     * 根据openid获取用户信息
     * @param openid 微信openid
     * @return 用户实体
     */
    UserEntity getUserByOpenid(String openid);
    
    /**
     * 更新用户信息
     * @param userDTO 用户信息
     * @return 更新后的用户信息
     */
    UserDTO updateUserInfo(UserDTO userDTO);
    
    /**
     * 根据id获取用户信息
     * @param id 用户id
     * @return 用户DTO
     */
    UserDTO getUserById(Long id);
    
    /**
     * 刷新token
     * @param userId 用户id
     * @return 登录响应，包含新的token
     */
    UserLoginResponseDTO refreshToken(Long userId);
    
}