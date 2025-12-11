package com.parking.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.model.dto.ParkingSpaceQueryDTO;
import com.parking.model.entity.ParkingSpaceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ParkingSpaceMapper extends BaseMapper<ParkingSpaceEntity> {
    /**
     * 根据ID查询车位信息
     */
    ParkingSpaceEntity selectById(Long id);
    
    /**
     * 查询可用车位列表（兼容原有方法）
     */
    List<ParkingSpaceEntity> selectAvailableSpaces(Long parkingId);
    
    /**
     * 查询可用车位列表（带条件）
     * @param queryDTO 查询条件
     * @return 车位列表
     */
    List<ParkingSpaceEntity> selectAvailableSpacesByCondition(@Param("queryDTO") ParkingSpaceQueryDTO queryDTO);
    
    /**
     * 锁定车位（悲观锁）
     */
    int lockSpace(Long spaceId);
    
    /**
     * 更新车位状态（兼容原有方法）
     */
    int updateStatus(@Param("spaceId") Long spaceId, @Param("status") String status);
    
    /**
     * 更新状态为可用
     */
    int updateToAvailable(Long spaceId);
    
    /**
     * 更新车位状态（数字状态）
     * @param id 车位ID
     * @param newStatus 新状态
     * @param oldStatus 旧状态（用于乐观锁）
     * @return 更新行数
     */
    int updateState(@Param("id") Long id, @Param("newState") Integer newState, 
                    @Param("oldState") Integer oldState);
    
    /**
     * 使用乐观锁更新车位状态
     * @param id 车位ID
     * @param newState 新状态
     * @param oldState 旧状态
     * @param version 版本号
     * @return 更新行数
     */
    int updateStateWithVersion(@Param("id") Long id, @Param("newState") Integer newState,
                               @Param("oldState") Integer oldState, @Param("version") Integer version);
    
    /**
     * 根据车位编号查询车位
     * @param spaceNumber 车位编号
     * @return 车位实体
     */
    ParkingSpaceEntity selectBySpaceNumber(@Param("spaceNumber") String spaceNumber);
    
    /**
     * 根据楼层查询车位
     * @param floor 楼层
     * @return 车位列表
     */
    List<ParkingSpaceEntity> selectByFloor(@Param("floor") String floor);
    
    /**
     * 统计当前系统中可用的空车位数量
     * 可用于给大模型提供实时数据（例如在线客服回答“现在还有多少车位？”）
     */
    int countAvailableSpaces();
}