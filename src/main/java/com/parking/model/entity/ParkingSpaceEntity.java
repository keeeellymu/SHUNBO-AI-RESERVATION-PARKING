package com.parking.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 车位实体类
 */
@Data
@TableName("parking_space")
public class ParkingSpaceEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("space_number")
    private String spaceNumber; // 车位编号
    
    // 停车场ID字段：明确映射到parking_id，避免MyBatis-Plus自动SQL生成使用错误的字段名
    @TableField("parking_id")
    private Long parkingId;
    private String status; // 兼容现有状态：AVAILABLE, OCCUPIED, RESERVED
    private String type; // SMALL, MEDIUM, LARGE
    private boolean isDisabled;
    
    // 新增字段
    private String name; // 车位名称
    private String location; // 车位位置
    private String floor; // 楼层
    private Integer category; // 车位类型：0-普通，1-VIP，2-残疾人专用
    private Integer state; // 数字状态：0-空闲，1-锁定，2-占用（与原有status保持映射）
    private BigDecimal hourlyRate; // 小时费率
    private BigDecimal dailyRate; // 日费率
    private String description; // 描述
    private String imageUrl; // 车位图片
    private Integer isAvailable; // 是否可用：0-不可用，1-可用
    

    private Integer version; // 乐观锁版本号
    
    private Date createdAt;
    private Date updatedAt;
    
    // 状态常量
    public static final class SpaceStatus {
        public static final Integer FREE = 0; // 空闲
        public static final Integer LOCKED = 1; // 锁定
        public static final Integer OCCUPIED = 2; // 占用
    }
    
    // 类型常量
    public static final class SpaceType {
        public static final Integer NORMAL = 0; // 普通
        public static final Integer VIP = 1; // VIP
        public static final Integer DISABLED = 2; // 残疾人专用
    }
}