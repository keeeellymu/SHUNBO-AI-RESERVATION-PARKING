package com.parking.service;

import com.parking.model.entity.ParkingSpaceEntity;
import com.parking.model.dto.ParkingSpaceDTO;
import com.parking.model.dto.ParkingSpaceQueryDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 车位服务接口
 */
public interface ParkingSpaceService extends IService<ParkingSpaceEntity> {
    
    /**
     * 获取车位详情
     * @param id 车位ID
     * @return 车位详情DTO
     */
    ParkingSpaceDTO getParkingSpaceById(Long id);
    
    /**
     * 查询可用车位列表
     * @param parkingId 停车场ID
     * @return 可用车位列表
     */
    List<ParkingSpaceDTO> getAvailableSpaces(Long parkingId);
    
    /**
     * 带条件查询车位列表
     * @param queryDTO 查询条件
     * @return 车位列表
     */
    List<ParkingSpaceDTO> searchSpaces(ParkingSpaceQueryDTO queryDTO);
    
    /**
     * 锁定车位
     * @param spaceId 车位ID
     * @return 是否锁定成功
     */
    boolean lockSpace(Long spaceId);
    
    /**
     * 释放车位
     * @param spaceId 车位ID
     * @return 是否释放成功
     */
    boolean releaseSpace(Long spaceId);
    
    /**
     * 更新车位状态
     * @param id 车位ID
     * @param newState 新状态
     * @param oldState 旧状态
     * @return 是否更新成功
     */
    boolean updateState(Long id, Integer newState, Integer oldState);
    
    /**
     * 使用乐观锁更新车位状态
     * @param id 车位ID
     * @param newState 新状态
     * @param oldState 旧状态
     * @param version 版本号
     * @return 是否更新成功
     */
    boolean updateStateWithVersion(Long id, Integer newState, Integer oldState, Integer version);
    
    /**
     * 创建新车位
     * @param parkingSpaceDTO 车位信息
     * @return 创建的车位信息
     */
    ParkingSpaceDTO createParkingSpace(ParkingSpaceDTO parkingSpaceDTO);
    
    /**
     * 更新车位信息
     * @param parkingSpaceDTO 车位信息
     * @return 更新后的车位信息
     */
    ParkingSpaceDTO updateParkingSpace(ParkingSpaceDTO parkingSpaceDTO);
    
    /**
     * 删除车位
     * @param id 车位ID
     * @return 是否删除成功
     */
    boolean deleteParkingSpace(Long id);
}