package com.parking.model.dto;

import lombok.Data;

/**
 * 用户登录请求DTO
 */
@Data
public class UserLoginRequestDTO {
    
    // 微信小程序登录字段
    private String code;
    private String encryptedData;
    private String iv;
    
    // 用户名密码登录字段
    private String username;
    private String password;
    
}