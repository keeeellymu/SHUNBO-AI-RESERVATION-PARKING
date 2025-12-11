package com.parking.controller;

import com.parking.model.dto.ParkingSpaceDTO;
import com.parking.model.dto.ParkingSpaceQueryDTO;
import com.parking.service.ParkingSpaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 车位管理控制器
 */
@RestController
@RequestMapping("/api/v1/parking-spaces")
public class ParkingSpaceController {
    
    @Autowired
    private ParkingSpaceService parkingSpaceService;
    
    /**
     * 获取车位详情
     * @param id 车位ID
     * @return 车位详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ParkingSpaceDTO> getParkingSpaceById(@PathVariable Long id) {
        ParkingSpaceDTO parkingSpaceDTO = parkingSpaceService.getParkingSpaceById(id);
        if (parkingSpaceDTO != null) {
            return ResponseEntity.ok(parkingSpaceDTO);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 查询可用车位列表
     * @param parkingId 停车场ID
     * @return 可用车位列表
     */
    @GetMapping("/available")
    public ResponseEntity<List<ParkingSpaceDTO>> getAvailableSpaces(@RequestParam Long parkingId) {
        List<ParkingSpaceDTO> parkingSpaceDTOs = parkingSpaceService.getAvailableSpaces(parkingId);
        return ResponseEntity.ok(parkingSpaceDTOs);
    }
    
    /**
     * 带条件查询车位列表
     * @param queryDTO 查询条件
     * @return 车位列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<ParkingSpaceDTO>> searchSpaces(ParkingSpaceQueryDTO queryDTO) {
        List<ParkingSpaceDTO> parkingSpaceDTOs = parkingSpaceService.searchSpaces(queryDTO);
        return ResponseEntity.ok(parkingSpaceDTOs);
    }
    
    /**
     * 锁定车位
     * @param spaceId 车位ID
     * @return 操作结果
     */
    @PostMapping("/{spaceId}/lock")
    public ResponseEntity<Boolean> lockSpace(@PathVariable Long spaceId) {
        boolean result = parkingSpaceService.lockSpace(spaceId);
        if (result) {
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.badRequest().body(false);
        }
    }
    
    /**
     * 释放车位
     * @param spaceId 车位ID
     * @return 操作结果
     */
    @PostMapping("/{spaceId}/release")
    public ResponseEntity<Boolean> releaseSpace(@PathVariable Long spaceId) {
        boolean result = parkingSpaceService.releaseSpace(spaceId);
        if (result) {
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.badRequest().body(false);
        }
    }
    
    /**
     * 创建新车位
     * @param parkingSpaceDTO 车位信息
     * @return 创建的车位信息
     */
    @PostMapping
    public ResponseEntity<ParkingSpaceDTO> createParkingSpace(@RequestBody ParkingSpaceDTO parkingSpaceDTO) {
        ParkingSpaceDTO createdDTO = parkingSpaceService.createParkingSpace(parkingSpaceDTO);
        return ResponseEntity.ok(createdDTO);
    }
    
    /**
     * 更新车位信息
     * @param id 车位ID
     * @param parkingSpaceDTO 车位信息
     * @return 更新后的车位信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<ParkingSpaceDTO> updateParkingSpace(@PathVariable Long id, @RequestBody ParkingSpaceDTO parkingSpaceDTO) {
        parkingSpaceDTO.setId(id);
        ParkingSpaceDTO updatedDTO = parkingSpaceService.updateParkingSpace(parkingSpaceDTO);
        return ResponseEntity.ok(updatedDTO);
    }
    
    /**
     * 删除车位
     * @param id 车位ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteParkingSpace(@PathVariable Long id) {
        boolean result = parkingSpaceService.deleteParkingSpace(id);
        if (result) {
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}