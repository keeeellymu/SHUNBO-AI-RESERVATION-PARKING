package com.parking.model.dto;

import lombok.Data;

/**
 * 车位查询DTO
 */
@Data
public class ParkingSpaceQueryDTO {
    
    private Long parkingId; // 停车场ID
    
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String floor;
    private Integer category;
    private Integer state;
    private String status;
    private String type;
    private Boolean isAvailable;
    private Boolean isDisabled;
    private String keyword; // 搜索关键词
    
}