package com.parking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parking.dao.ReservationMapper;
import com.parking.dao.ParkingSpaceMapper;
import com.parking.dao.LogMapper;
import com.parking.model.dto.ReservationDTO;
import com.parking.model.entity.ParkingSpaceEntity;
import com.parking.model.entity.ReservationEntity;
import com.parking.model.vo.PageResult;
import com.parking.model.vo.SystemMonitorData;
import com.parking.service.AdminService;
import com.parking.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ❌ 删除：import java.time.LocalDateTime; 
import java.util.Date; // ✅ 确保只有这一个时间导入
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {
    
    @Autowired
    private ReservationMapper reservationMapper;
    
    @Autowired
    private ParkingSpaceMapper parkingSpaceMapper;
    
    @Autowired
    private LogMapper logMapper;
    
    @Autowired
    private ReservationService reservationService;
    
    @Override
    public PageResult<ReservationDTO> getAbnormalReservations(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        QueryWrapper<ReservationEntity> wrapper = new QueryWrapper<>();
        wrapper.in("status", 2, 3).orderByDesc("updated_at");
        
        List<ReservationEntity> entities = reservationMapper.selectList(wrapper);
        
        if (entities == null || entities.isEmpty()) {
            return new PageResult<>(java.util.Collections.emptyList(), 0, pageNum, pageSize);
        }

        int total = entities.size();
        int end = Math.min(offset + pageSize, total);
        
        if (offset >= total) {
            entities = java.util.Collections.emptyList();
        } else {
            entities = entities.subList(offset, end);
        }
        
        List<ReservationDTO> dtos = entities.stream()
                .map(entity -> reservationService.convertToDTO(entity))
                .collect(Collectors.toList());
        
        return new PageResult<>(dtos, total, pageNum, pageSize);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelAbnormalReservation(Long reservationId) {
        ReservationEntity entity = reservationMapper.selectById(reservationId);
        if (entity == null) return false;
        
        if (entity.getStatus() != 2 && entity.getStatus() != 3) {
            throw new RuntimeException("只能取消异常预约");
        }
        
        return releaseParkingSpace(entity.getParkingSpaceId());
    }
    
    @Override
    public SystemMonitorData getSystemMonitorData() {
        SystemMonitorData data = new SystemMonitorData();
        QueryWrapper<ReservationEntity> totalWrapper = new QueryWrapper<>();
        data.setTotalReservations(reservationMapper.selectCount(totalWrapper).intValue());
        
        QueryWrapper<ReservationEntity> todayWrapper = new QueryWrapper<>();
        // 使用 24L 确保长整型运算
        todayWrapper.ge("created_at", new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000));
        data.setTodayReservations(reservationMapper.selectCount(todayWrapper).intValue());
        
        QueryWrapper<ReservationEntity> abnormalWrapper = new QueryWrapper<>();
        abnormalWrapper.in("status", 2, 3);
        data.setAbnormalReservations(reservationMapper.selectCount(abnormalWrapper).intValue());
        
        data.setSystemUpTime("正常运行中");
        return data;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean forceReleaseReservation(Long reservationId) {
        ReservationEntity entity = reservationMapper.selectById(reservationId);
        if (entity == null) return false;
        
        // 1. 更新预约状态
        entity.setStatus(2);
        entity.setUpdatedAt(new Date()); 
        reservationMapper.updateById(entity);
        
        // 2. 强制释放车位
        return releaseParkingSpace(entity.getParkingSpaceId());
    }
    
    // 统一释放逻辑
    private boolean releaseParkingSpace(Long parkingSpaceId) {
        if (parkingSpaceId == null) return true;

        QueryWrapper<ParkingSpaceEntity> updateWrapper = new QueryWrapper<>();
        updateWrapper.eq("id", parkingSpaceId);
        
        ParkingSpaceEntity updateEntity = new ParkingSpaceEntity();
        
        // 更新为可用状态
        updateEntity.setStatus("AVAILABLE"); // 必须是字符串
        updateEntity.setState(0);            // 0=空闲
        updateEntity.setIsAvailable(1);      // 1=可用
        
        // ✅ 重点：使用 new Date()，绝对不要用 LocalDateTime.now()
        updateEntity.setUpdatedAt(new Date()); 
        
        parkingSpaceMapper.update(updateEntity, updateWrapper);
        return true;
    }
    
    @Override
    public PageResult<Map<String, Object>> getReservationErrorLogs(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> logs = logMapper.selectErrorLogs(null, offset, pageSize);
        int total = logMapper.countErrorLogs(null);
        return new PageResult<>(logs, total, pageNum, pageSize);
    }
}