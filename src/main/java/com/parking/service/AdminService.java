package com.parking.service;

import com.parking.model.dto.ReservationDTO;
import com.parking.model.vo.PageResult;
import com.parking.model.vo.SystemMonitorData;
import java.util.Map;

/**
 * 管理员服务接口
 * 负责管理员相关的系统监控、异常处理和数据管理功能
 */
public interface AdminService {
    
    /**
     * 获取异常预约列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 异常预约分页结果
     */
    PageResult<ReservationDTO> getAbnormalReservations(int pageNum, int pageSize);
    
    /**
     * 手动取消异常预约
     * @param reservationId 预约ID
     * @return 是否取消成功
     */
    boolean cancelAbnormalReservation(Long reservationId);
    
    /**
     * 获取系统监控数据
     * @return 系统监控数据
     */
    SystemMonitorData getSystemMonitorData();
    
    /**
     * 强制释放超时预约的车位
     * @param reservationId 预约ID
     * @return 是否释放成功
     */
    boolean forceReleaseReservation(Long reservationId);
    
    /**
     * 查看预约异常日志
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 异常日志分页结果
     */
    PageResult<Map<String, Object>> getReservationErrorLogs(int pageNum, int pageSize);
}