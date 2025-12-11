package com.parking.dao;

import com.parking.model.entity.ReservationEntity;
import com.parking.model.dto.ReservationQueryDTO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Date;

/**
 * 预约Mapper接口
 */
@Mapper
public interface ReservationMapper extends BaseMapper<ReservationEntity> {
    
    /**
     * 查询用户的预约列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 预约列表
     */
    List<ReservationEntity> selectByUserId(@Param("userId") Long userId, 
                                          @Param("pageNum") Integer pageNum, 
                                          @Param("pageSize") Integer pageSize);
    
    /**
     * 根据条件查询预约列表
     * @param query 查询条件
     * @return 预约列表
     */
    List<ReservationEntity> selectByCondition(ReservationQueryDTO query);
    
    /**
     * 检查是否有重叠的预约
     * @param parkingSpaceId 车位ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 重叠预约数量
     */
    Integer checkOverlappingReservation(@Param("parkingSpaceId") Long parkingSpaceId,
                                       @Param("startTime") Date startTime,
                                       @Param("endTime") Date endTime);
    
    /**
     * 检查是否有重叠的预约（排除特定预约）
     * @param parkingSpaceId 车位ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param excludeId 排除的预约ID（用于更新场景）
     * @return 重叠预约数量
     */
    Integer checkOverlappingReservationExclude(@Param("parkingSpaceId") Long parkingSpaceId,
                                             @Param("startTime") Date startTime,
                                             @Param("endTime") Date endTime,
                                             @Param("excludeId") Long excludeId);
    
    /**
     * 更新预约状态
     * @param id 预约ID
     * @param status 新状态
     * @param version 版本号
     * @return 更新结果
     */
    int updateStatusWithVersion(@Param("id") Long id,
                               @Param("status") Integer status,
                               @Param("version") Integer version);
    
    /**
     * 更新超时预约
     * @param currentTime 当前时间
     * @return 更新数量
     */
    int updateTimeoutReservations(@Param("currentTime") Date currentTime);
    
    /**
     * 根据预约编号查询预约
     * @param reservationNo 预约编号
     * @return 预约信息
     */
    ReservationEntity selectByReservationNo(@Param("reservationNo") String reservationNo);
    
    /**
     * 查询停车场信息
     * @param parkingId 停车场ID
     * @return 停车场信息Map（包含name, address, hourly_rate等字段）
     */
    java.util.Map<String, Object> selectParkingLotInfo(@Param("parkingId") Long parkingId);
    
    /**
     * 查询所有停车场列表（用于附近停车场接口）
     * @param district 按行政区过滤，可为null
     * @return 停车场列表
     */
    List<java.util.Map<String, Object>> selectAllParkingLots(@Param("district") String district);
    
    /**
     * 搜索停车场（根据名称或地址）
     * @param keyword 搜索关键词
     * @return 停车场列表
     */
    List<java.util.Map<String, Object>> searchParkingLots(@Param("keyword") String keyword);
    
    /**
     * 更新停车场车位数（预约后减少）
     * @param parkingId 停车场ID
     * @return 更新结果
     */
    int decreaseParkingLotSpaces(@Param("parkingId") Long parkingId);
    
    /**
     * 更新停车场车位数（取消预约或支付成功后增加）
     * @param parkingId 停车场ID
     * @return 更新结果
     */
    int increaseParkingLotSpaces(@Param("parkingId") Long parkingId);

    /**
     * 查询用户最新的一条未支付预约（用于限制新预约）
     * 通常指 status = 1(已使用，待支付) 且 payment_status = 0
     * @param userId 用户ID
     * @return 未支付预约ID，如果不存在则为null
     */
    Long findLatestUnpaidReservationByUser(@Param("userId") Long userId);
}