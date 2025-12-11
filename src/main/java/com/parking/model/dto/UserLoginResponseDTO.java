package com.parking.model.dto;

import lombok.Data;

/**
 * 用户登录响应DTO
 */
@Data
public class UserLoginResponseDTO {
    
    private String token;
    private UserDTO user;
    private Boolean isNewUser;
    
}