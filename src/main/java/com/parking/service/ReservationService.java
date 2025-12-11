package com.parking.service;

import com.parking.model.entity.ReservationEntity;
import com.parking.model.dto.ReservationDTO;
import com.parking.model.dto.ReservationCreateRequestDTO;
import com.parking.model.dto.ReservationQueryDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 预约服务接口
 */
public interface ReservationService extends IService<ReservationEntity> {
    
    /**
     * 创建预约
     * @param requestDTO 预约请求信息
     * @param userId 用户ID
     * @return 创建的预约信息
     */
    ReservationDTO createReservation(ReservationCreateRequestDTO requestDTO, Long userId);
    
    /**
     * 取消预约
     * @param id 预约ID
     * @param userId 用户ID
     * @return 是否取消成功
     */
    boolean cancelReservation(Long id, Long userId);
    
    /**
     * 使用预约（入场）
     * @param id 预约ID
     * @return 是否使用成功
     */
    boolean useReservation(Long id);
    
    /**
     * 完成预约（出场）
     * @param id 预约ID
     * @return 是否完成成功
     */
    boolean completeReservation(Long id);
    
    /**
     * 申请退款
     * @param id 预约ID
     * @param userId 用户ID
     * @return 是否申请成功
     */
    boolean applyRefund(Long id, Long userId);
    
    /**
     * 获取预约详情
     * @param id 预约ID
     * @return 预约详情
     */
    ReservationDTO getReservationById(Long id);
    
    /**
     * 查询用户的预约列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 预约列表
     */
    List<ReservationDTO> getUserReservations(Long userId, Integer pageNum, Integer pageSize);
    
    /**
     * 根据条件查询预约列表
     * @param queryDTO 查询条件
     * @return 预约列表
     */
    List<ReservationDTO> queryReservations(ReservationQueryDTO queryDTO);
    
    /**
     * 更新预约支付状态
     * @param reservationId 预约ID
     * @param paymentStatus 支付状态：0-未支付，1-已支付
     * @return 是否更新成功
     */
    boolean updatePaymentStatus(Long reservationId, int paymentStatus);
    
    /**
     * 更新预约退款状态
     * @param reservationId 预约ID
     * @param refundStatus 退款状态：0-无退款，1-退款中，2-退款成功，3-退款失败
     * @return 是否更新成功
     */
    boolean updateRefundStatus(Long reservationId, int refundStatus);
    
    /**
     * 将实体转换为DTO
     * @param entity 预约实体
     * @return 预约DTO
     */
    ReservationDTO convertToDTO(ReservationEntity entity);
    
    /**
     * 检查车位是否可预约
     * @param parkingSpaceId 车位ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param excludeId 排除的预约ID
     * @return 是否可预约
     */
    boolean checkSpaceAvailability(Long parkingSpaceId, java.util.Date startTime, java.util.Date endTime, Long excludeId);
    
    /**
     * 更新超时预约
     * @return 更新的预约数量
     */
    int updateTimeoutReservations();
    
    /**
     * 批量删除已完成和已取消的预约记录
     * @param userId 用户ID
     * @return 删除的记录数量
     */
    int deleteCompletedAndCancelledReservations(Long userId);
}