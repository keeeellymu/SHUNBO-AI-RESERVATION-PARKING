package com.parking.model.dto;

import lombok.Data;

/**
 * 用户数据传输对象
 */
@Data
public class UserDTO {
    
    private Long id;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String licensePlate;
    private Integer status;
    
}