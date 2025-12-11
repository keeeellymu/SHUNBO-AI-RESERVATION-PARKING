package com.parking.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.util.Date;

/**
 * 预约实体类
 */
@Data
@TableName("reservation")
public class ReservationEntity {
    
    /**
     * 预约ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 车位ID
     */
    private Long parkingSpaceId;
    
    /**
     * 停车场ID
     */
    private Long parkingId;
    
    /**
     * 预约编号
     */
    private String reservationNo;
    
    /**
     * 预约状态（0-待使用，1-已使用，2-已取消，3-已超时）
     */
    private Integer status;
    
    /**
     * 支付状态（0-未支付，1-已支付）
     */
    @TableField("payment_status")
    private Integer paymentStatus;
    
    /**
     * 退款状态（0-无退款，1-退款中，2-退款成功，3-退款失败）
     */
    private Integer refundStatus;
    
    /**
     * 预约开始时间
     */
    private Date startTime;
    
    /**
     * 预约结束时间
     */
    private Date endTime;
    
    /**
     * 实际入场时间
     */
    private Date actualEntryTime;
    
    /**
     * 实际出场时间
     */
    private Date actualExitTime;
    
    /**
     * 车牌号
     */
    private String plateNumber;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 车辆信息
     */
    private String vehicleInfo;
    
    /**
     * 备注信息
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private Date createdAt;
    
    /**
     * 更新时间
     */
    private Date updatedAt;
    
    /**
     * 乐观锁版本号
     */
   
    private Integer version;
    
    /**
     * 预约状态枚举
     */
    public enum ReservationStatus {
        PENDING(0, "待使用"),
        USED(1, "已使用"),
        CANCELLED(2, "已取消"),
        TIMEOUT(3, "已超时");
        
        private final Integer code;
        private final String desc;
        
        ReservationStatus(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }
        
        public Integer getCode() {
            return code;
        }
        
        public String getDesc() {
            return desc;
        }
    }
    
    /**
     * 退款状态枚举
     */
    public enum RefundStatus {
        NO_REFUND(0, "无退款"),
        REFUNDING(1, "退款中"),
        REFUND_SUCCESS(2, "退款成功"),
        REFUND_FAILED(3, "退款失败");
        
        private final Integer code;
        private final String desc;
        
        RefundStatus(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }
        
        public Integer getCode() {
            return code;
        }
        
        public String getDesc() {
            return desc;
        }
    }
}