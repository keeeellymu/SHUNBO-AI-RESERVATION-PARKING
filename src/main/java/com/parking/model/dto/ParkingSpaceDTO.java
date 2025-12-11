package com.parking.model.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 车位DTO
 */
@Data
public class ParkingSpaceDTO {
    
    private Long id;
    private String spaceNumber;
    private String name;
    private String location;
    private String floor;
    private Integer category;
    private Integer state;
    private String status;
    private String type;
    private BigDecimal hourlyRate;
    private BigDecimal dailyRate;
    private String description;
    private String imageUrl;
    private Boolean isAvailable;
    private Boolean isDisabled;
    
}