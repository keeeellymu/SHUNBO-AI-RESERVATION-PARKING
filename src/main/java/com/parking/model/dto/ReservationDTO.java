package com.parking.model.dto;

import lombok.Data;
import java.util.Date;

/**
 * 预约DTO
 */
@Data
public class ReservationDTO {
    
    /**
     * 预约ID
     */
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
     * 预约状态描述
     */
    private String statusDesc;
    
    /**
     * 支付状态（0-未支付，1-已支付）
     */
    private Integer paymentStatus;
    
    /**
     * 退款状态（0-无退款，1-退款中，2-退款成功，3-退款失败）
     */
    private Integer refundStatus;
    
    /**
     * 退款状态描述
     */
    private String refundStatusDesc;
    
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
     * 车位信息
     */
    private ParkingSpaceDTO parkingSpace;
    
    /**
     * 用户信息
     */
    private UserDTO user;
    
    /**
     * 停车场名称
     */
    private String parkingLotName;
    
    /**
     * 停车场地址
     */
    private String parkingLotAddress;
    
    /**
     * 停车场小时费率
     */
    private Double parkingLotHourlyRate;
    
    /**
     * 车牌号
     */
    private String plateNumber;
    
    /**
     * 联系电话
     */
    private String contactPhone;
}