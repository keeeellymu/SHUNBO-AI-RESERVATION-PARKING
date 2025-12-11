package com.parking.model.vo;

import lombok.Data;

/**
 * 系统监控数据
 */
@Data
public class SystemMonitorData {
    
    /**
     * 总预约数
     */
    private int totalReservations;
    
    /**
     * 今日预约数
     */
    private int todayReservations;
    
    /**
     * 异常预约数
     */
    private int abnormalReservations;
    
    /**
     * 系统运行时间
     */
    private String systemUpTime;
    
    /**
     * 可用车位总数
     */
    private int availableSpaces;
    
    /**
     * 占用车位总数
     */
    private int occupiedSpaces;
}