package com.parking.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class UserEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String openid;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String email;
    private Integer gender;
    private String licensePlate;
    private String idCardNumber;
    private Integer status; // 0: 正常, 1: 禁用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 构造函数和getter/setter通过Lombok自动生成
}