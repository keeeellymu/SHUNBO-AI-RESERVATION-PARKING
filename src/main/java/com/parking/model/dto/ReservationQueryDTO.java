package com.parking.model.dto;

import lombok.Data;
import java.util.Date;

/**
 * 预约查询DTO
 */
@Data
public class ReservationQueryDTO {
    
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
     * 预约状态
     */
    private Integer status;
    
    /**
     * 开始时间范围 - 从
     */
    private Date startTimeFrom;
    
    /**
     * 开始时间范围 - 到
     */
    private Date startTimeTo;
    
    /**
     * 结束时间范围 - 从
     */
    private Date endTimeFrom;
    
    /**
     * 结束时间范围 - 到
     */
    private Date endTimeTo;
    
    /**
     * 预约编号
     */
    private String reservationNo;
    
    /**
     * 车辆信息
     */
    private String vehicleInfo;
    
    /**
     * 页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}