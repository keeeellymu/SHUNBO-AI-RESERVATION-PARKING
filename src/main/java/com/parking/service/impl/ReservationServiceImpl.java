package com.parking.service.impl;

import com.parking.model.entity.ReservationEntity;
import com.parking.model.entity.ParkingSpaceEntity;
import com.parking.model.dto.ReservationDTO;
import com.parking.model.dto.ParkingSpaceDTO;
import com.parking.model.dto.ReservationCreateRequestDTO;
import com.parking.model.dto.ReservationQueryDTO;
import com.parking.dao.ReservationMapper;
import com.parking.dao.ParkingSpaceMapper;
import com.parking.service.ReservationService;
import com.parking.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.parking.exception.ParkingException;

/**
 * 预约服务实现类
 */
@Service
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, ReservationEntity> implements ReservationService {
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private ReservationMapper reservationMapper;
    
    @Autowired
    private ParkingSpaceMapper parkingSpaceMapper;
    
    @Autowired(required = false)
    private com.parking.dao.UserMapper userMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationDTO createReservation(ReservationCreateRequestDTO requestDTO, Long userId) {
        // 验证用户是否存在（如果UserMapper可用）
        if (userMapper != null) {
            com.parking.model.entity.UserEntity user = userMapper.selectById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在，请先登录或注册");
            }
        }

        // 限制：如果存在“待支付”订单，则不允许创建新的预约
        // 这里通过 payment_status = 0 且 status = 1 的记录来判断
        try {
            Long unpaidReservationId = reservationMapper.findLatestUnpaidReservationByUser(userId);
            if (unpaidReservationId != null) {
                // 使用业务异常，并用特殊前缀携带预约ID，方便前端解析并跳转
                throw new ParkingException(409, "UNPAID_ORDER:" + unpaidReservationId);
            }
        } catch (ParkingException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("检查未支付预约失败: " + e.getMessage());
            // 检查失败不阻止预约，但记录日志
        }
        
        // 验证车位是否存在并获取车位信息
        ParkingSpaceEntity parkingSpace = parkingSpaceMapper.selectById(requestDTO.getParkingSpaceId());
        if (parkingSpace == null) {
            throw new RuntimeException("车位不存在");
        }
        
        System.out.println("========== 预约创建 - 停车场ID验证 ==========");
        System.out.println("车位ID: " + requestDTO.getParkingSpaceId());
        
        // 关键修复：直接从车位信息中获取parkingId
        Long parkingId = parkingSpace.getParkingId();
        if (parkingId == null) {
            throw new RuntimeException("无法确定停车场ID，车位数据异常");
        }
        
        System.out.println("最终使用的停车场ID: " + parkingId);
        
        // 验证停车场是否存在
        Map<String, Object> parkingLotInfo = reservationMapper.selectParkingLotInfo(parkingId);
        if (parkingLotInfo != null && !parkingLotInfo.isEmpty()) {
            System.out.println("停车场信息: " + parkingLotInfo.get("name") + " - " + parkingLotInfo.get("address"));
        } else {
            System.out.println("警告：停车场ID=" + parkingId + " 的停车场信息未找到");
        }
        
        
        // 在检查可用性之前，先清理过期的预约
        try {
            Date currentTime = new Date();
            int timeoutCount = reservationMapper.updateTimeoutReservations(currentTime);
            if (timeoutCount > 0) {
                System.out.println("已自动更新 " + timeoutCount + " 个过期预约为TIMEOUT状态");
            }
        } catch (Exception e) {
            System.err.println("清理过期预约失败: " + e.getMessage());
            // 不阻止预约流程，继续执行
        }
        
        // 检查车位是否可预约（只检查有效的预约：状态为PENDING=0的预约）
        System.out.println("========== 检查车位可用性 ==========");
        System.out.println("车位ID: " + requestDTO.getParkingSpaceId());
        System.out.println("开始时间: " + requestDTO.getStartTime());
        System.out.println("结束时间: " + requestDTO.getEndTime());
        
        // 先查询所有重叠的预约（包括已取消的），以便调试
        QueryWrapper<ReservationEntity> allWrapper = new QueryWrapper<>();
        allWrapper.eq("parking_space_id", requestDTO.getParkingSpaceId());
        allWrapper.lt("start_time", requestDTO.getEndTime());
        allWrapper.gt("end_time", requestDTO.getStartTime());
        List<ReservationEntity> allOverlappingReservations = this.list(allWrapper);
        
        System.out.println("找到 " + allOverlappingReservations.size() + " 个时间段重叠的预约（所有状态）:");
        for (ReservationEntity res : allOverlappingReservations) {
            String statusText = res.getStatus() == null ? "null" : 
                (res.getStatus() == 0 ? "PENDING(待使用)" : 
                 res.getStatus() == 1 ? "USED(已使用)" : 
                 res.getStatus() == 2 ? "CANCELLED(已取消)" : 
                 res.getStatus() == 3 ? "TIMEOUT(已超时)" : "未知(" + res.getStatus() + ")");
            System.out.println("  - 预约ID: " + res.getId() + ", 状态: " + statusText + 
                ", 时间: " + res.getStartTime() + " ~ " + res.getEndTime());
        }
        
        // 只检查状态为PENDING(0)的有效预约
        Integer overlappingCount = reservationMapper.checkOverlappingReservation(
                requestDTO.getParkingSpaceId(), requestDTO.getStartTime(), requestDTO.getEndTime());
        
        System.out.println("有效的重叠预约数量（status=0=PENDING）: " + overlappingCount);
        
        if (overlappingCount != null && overlappingCount > 0) {
            // 只查询状态为PENDING的重叠预约
            QueryWrapper<ReservationEntity> wrapper = new QueryWrapper<>();
            wrapper.eq("parking_space_id", requestDTO.getParkingSpaceId());
            wrapper.eq("status", 0); // PENDING状态
            wrapper.lt("start_time", requestDTO.getEndTime());
            wrapper.gt("end_time", requestDTO.getStartTime());
            List<ReservationEntity> overlappingReservations = this.list(wrapper);
            
            System.out.println("有效的重叠预约详情:");
            for (ReservationEntity res : overlappingReservations) {
                System.out.println("  - 预约ID: " + res.getId() + ", 状态: PENDING" + 
                    ", 时间: " + res.getStartTime() + " ~ " + res.getEndTime());
            }
            
            throw new RuntimeException("该时间段车位已被预约（存在 " + overlappingCount + " 个有效预约）");
        }
        
        System.out.println("车位可用，无有效重叠预约");
        
        // 锁定车位
        if (parkingSpaceMapper.lockSpace(requestDTO.getParkingSpaceId()) <= 0) {
            throw new RuntimeException("车位锁定失败，请重试");
        }
        
        // 创建预约实体
        ReservationEntity entity = new ReservationEntity();
        // 先复制基本属性，但不复制parkingId（避免被错误覆盖）
        BeanUtils.copyProperties(requestDTO, entity);
        // 手动设置关键字段，确保使用正确的值
        entity.setUserId(userId);
        entity.setParkingSpaceId(requestDTO.getParkingSpaceId()); // 确保使用正确的车位ID
        entity.setParkingId(parkingId); // 确保使用从车位信息获取的停车场ID（而不是前端传递的可能错误的ID）
        entity.setStatus(ReservationEntity.ReservationStatus.PENDING.getCode());
        entity.setReservationNo(generateReservationNo());
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
        
        System.out.println("保存预约信息:");
        System.out.println("  - 预约ID: " + entity.getId());
        System.out.println("  - 用户ID: " + entity.getUserId());
        System.out.println("  - 车位ID: " + entity.getParkingSpaceId());
        System.out.println("  - 停车场ID: " + entity.getParkingId());
        System.out.println("  - 车牌号: " + entity.getPlateNumber());
        System.out.println("  - 开始时间: " + entity.getStartTime());
        System.out.println("  - 结束时间: " + entity.getEndTime());
        
        // 保存预约
        if (!this.save(entity)) {
            throw new RuntimeException("预约创建失败");
        }
        
        System.out.println("预约创建成功，预约ID: " + entity.getId());
        
        // 更新停车场的可用车位数和总车位数（减1）
        try {
            int updated = reservationMapper.decreaseParkingLotSpaces(parkingId);
            if (updated > 0) {
                System.out.println("停车场车位数更新成功，停车场ID: " + parkingId);
            } else {
                System.out.println("警告：停车场车位数更新失败，可能车位数已为0，停车场ID: " + parkingId);
            }
        } catch (Exception e) {
            System.err.println("更新停车场车位数失败: " + e.getMessage());
            e.printStackTrace();
            // 不阻止预约流程，只记录错误日志
        }
        
        return convertToDTO(entity);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelReservation(Long id, Long userId) {
        try {
            System.out.println("========== 开始取消预约 ==========");
            System.out.println("预约ID: " + id);
            System.out.println("用户ID: " + userId);
            
            ReservationEntity entity = this.getById(id);
            if (entity == null) {
                System.out.println("错误：预约不存在，ID: " + id);
                return false;
            }
            
            System.out.println("预约信息:");
            System.out.println("  - ID: " + entity.getId());
            System.out.println("  - 状态: " + entity.getStatus() + " (类型: " + (entity.getStatus() != null ? entity.getStatus().getClass().getName() : "null") + ")");
            System.out.println("  - 用户ID: " + entity.getUserId());
            System.out.println("  - 车位ID: " + entity.getParkingSpaceId());
            
            // 验证是否为用户本人的预约
            if (!entity.getUserId().equals(userId)) {
                System.out.println("权限验证失败：用户ID不匹配");
                throw new RuntimeException("无权限操作此预约");
            }
            
            // 只能取消待使用状态的预约（状态0 = PENDING）
            Integer pendingStatus = ReservationEntity.ReservationStatus.PENDING.getCode();
            Integer currentStatus = entity.getStatus();
            
            System.out.println("状态验证:");
            System.out.println("  - 当前状态: " + currentStatus);
            System.out.println("  - 期望状态: " + pendingStatus);
            
            if (currentStatus == null || !currentStatus.equals(pendingStatus)) {
                System.out.println("状态验证失败：当前状态=" + currentStatus + ", 期望状态=" + pendingStatus);
                throw new RuntimeException("只能取消待使用状态的预约，当前状态：" + currentStatus);
            }
            
            System.out.println("状态验证通过");
            
            // 更新预约状态为已取消（状态2 = CANCELLED）
            entity.setStatus(ReservationEntity.ReservationStatus.CANCELLED.getCode());
            entity.setUpdatedAt(new Date());
            
            System.out.println("更新预约状态为已取消...");
            System.out.println("  - 设置状态为: " + entity.getStatus());
            System.out.println("  - 设置更新时间为: " + entity.getUpdatedAt());
            System.out.println("  - 当前版本号: " + entity.getVersion());
            
            // 使用UpdateWrapper更新，避免乐观锁问题
            // 直接使用SQL更新，不依赖乐观锁版本号
            UpdateWrapper<ReservationEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", entity.getId());
            updateWrapper.set("status", ReservationEntity.ReservationStatus.CANCELLED.getCode());
            updateWrapper.set("updated_at", new Date());
            
            boolean updateResult = this.update(updateWrapper);
            
            System.out.println("updateById返回: " + updateResult);
            
            if (!updateResult) {
                System.out.println("错误：更新预约状态失败，可能的原因：");
                System.out.println("  1. 预约ID不存在");
                System.out.println("  2. 数据库更新失败");
                System.out.println("  3. 乐观锁冲突");
                return false;
            }
            System.out.println("预约状态更新成功");
            
            // 重新查询预约，确认更新成功
            ReservationEntity updatedEntity = this.getById(id);
            if (updatedEntity != null) {
                System.out.println("确认更新后的预约状态: " + updatedEntity.getStatus());
            } else {
                System.out.println("警告：更新后无法查询到预约，ID: " + id);
            }
            
            // 释放车位（使用MyBatis-Plus直接更新，最简单可靠）
            try {
                System.out.println("开始释放车位，车位ID: " + entity.getParkingSpaceId());
                
                // 查询车位当前状态
                ParkingSpaceEntity spaceEntity = parkingSpaceMapper.selectById(entity.getParkingSpaceId());
                if (spaceEntity == null) {
                    System.out.println("警告：车位不存在，ID: " + entity.getParkingSpaceId());
                } else {
                    System.out.println("车位当前状态:");
                    System.out.println("  - status: " + spaceEntity.getStatus());
                    System.out.println("  - state: " + spaceEntity.getState());
                    System.out.println("  - is_available: " + spaceEntity.getIsAvailable());
                    
                    // 保存原始版本号（如果使用乐观锁）
                    Integer originalVersion = spaceEntity.getVersion();
                    System.out.println("  - 原始版本号: " + originalVersion);
                    
                    // 直接更新为可用状态
                    spaceEntity.setStatus("AVAILABLE");
                    spaceEntity.setState(0);
                    spaceEntity.setIsAvailable(1);
                    spaceEntity.setUpdatedAt(new Date());
                    // 如果使用乐观锁，保持版本号不变（MyBatis-Plus会自动处理）
                    
                    // 使用UpdateWrapper更新，避免乐观锁问题
                    com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ParkingSpaceEntity> spaceUpdateWrapper = 
                        new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
                    spaceUpdateWrapper.eq("id", spaceEntity.getId());
                    spaceUpdateWrapper.set("status", "AVAILABLE");
                    spaceUpdateWrapper.set("state", 0);
                    spaceUpdateWrapper.set("is_available", 1);
                    spaceUpdateWrapper.set("updated_at", java.time.LocalDateTime.now());
                    
                    // 使用BaseMapper的update方法，避免乐观锁问题
                    int updated = parkingSpaceMapper.update(null, spaceUpdateWrapper);
                    System.out.println("车位更新结果: " + updated);
                    
                    if (updated > 0) {
                        System.out.println("车位释放成功");
                        
                        // 增加停车场的可用车位数和总车位数（+1）
                        try {
                            int parkingUpdated = reservationMapper.increaseParkingLotSpaces(entity.getParkingId());
                            if (parkingUpdated > 0) {
                                System.out.println("停车场车位数还原成功，停车场ID: " + entity.getParkingId());
                            } else {
                                System.out.println("警告：停车场车位数还原失败，停车场ID: " + entity.getParkingId());
                            }
                        } catch (Exception e) {
                            System.err.println("更新停车场车位数失败: " + e.getMessage());
                            e.printStackTrace();
                            // 不阻止取消预约流程，只记录错误日志
                        }
                    } else {
                        System.out.println("警告：车位更新返回0，可能车位不存在或状态未变化");
                    }
                }
            } catch (Exception e) {
                // 释放车位失败不应该阻止取消预约，记录错误日志
                System.err.println("========== 释放车位异常 ==========");
                System.err.println("异常类型: " + e.getClass().getName());
                System.err.println("异常消息: " + e.getMessage());
                System.err.println("异常堆栈:");
                e.printStackTrace();
                System.err.println("===================================");
                // 即使释放失败，预约已经取消，返回true
            }
            
            System.out.println("取消预约成功");
            return true;
            
        } catch (RuntimeException e) {
            System.err.println("========== 取消预约失败（业务异常）==========");
            System.err.println("异常类型: " + e.getClass().getName());
            System.err.println("异常消息: " + e.getMessage());
            System.err.println("异常堆栈:");
            e.printStackTrace();
            throw e; // 重新抛出，让Controller捕获
        } catch (Exception e) {
            System.err.println("========== 取消预约失败（系统异常）==========");
            System.err.println("异常类型: " + e.getClass().getName());
            System.err.println("异常消息: " + e.getMessage());
            System.err.println("异常堆栈:");
            e.printStackTrace();
            throw new RuntimeException("取消预约失败: " + e.getMessage(), e);
        } finally {
            System.out.println("========== 取消预约流程结束 ==========");
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean useReservation(Long id) {
        try {
            System.out.println("========== 开始使用预约 ==========");
            System.out.println("预约ID: " + id);
            
            ReservationEntity entity = this.getById(id);
            if (entity == null) {
                System.out.println("错误：预约不存在，ID: " + id);
                return false;
            }
            
            System.out.println("预约信息:");
            System.out.println("  - ID: " + entity.getId());
            System.out.println("  - 状态: " + entity.getStatus());
            System.out.println("  - 开始时间: " + entity.getStartTime());
            System.out.println("  - 结束时间: " + entity.getEndTime());
            
            // 检查预约是否有效
            if (entity.getStatus() != ReservationEntity.ReservationStatus.PENDING.getCode()) {
                System.out.println("状态验证失败：当前状态=" + entity.getStatus() + ", 期望状态=0(PENDING)");
                throw new RuntimeException("预约状态无效，当前状态：" + entity.getStatus());
            }
            
            // 新规则：只有到开始时间之后才能解锁/使用预约
            Date now = new Date();
            if (entity.getStartTime() != null && now.before(entity.getStartTime())) {
                System.out.println("预约尚未开始，当前时间: " + now + "，开始时间: " + entity.getStartTime());
                throw new ParkingException(400, "NOT_STARTED:预约尚未开始，暂时不能解锁");
            }
            
            if (new Date().after(entity.getEndTime())) {
                System.out.println("预约已超时");
                throw new RuntimeException("预约已超时");
            }
            
            System.out.println("状态验证通过，开始更新...");
            
            // 使用UpdateWrapper更新，避免乐观锁问题
            UpdateWrapper<ReservationEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", entity.getId());
            updateWrapper.set("status", ReservationEntity.ReservationStatus.USED.getCode());
            updateWrapper.set("actual_entry_time", new Date());
            updateWrapper.set("updated_at", new Date());
            
            boolean updateResult = this.update(updateWrapper);
            
            System.out.println("更新结果: " + updateResult);
            
            if (!updateResult) {
                System.out.println("错误：更新预约状态失败");
                return false;
            }
            
            System.out.println("预约状态更新成功，状态已改为：已使用(1)");
            System.out.println("========== 使用预约流程结束 ==========");
            
            return true;
        } catch (Exception e) {
            System.err.println("使用预约失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("使用预约失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeReservation(Long id) {
        try {
            System.out.println("========== 开始完成预约 ==========");
            System.out.println("预约ID: " + id);
            
            ReservationEntity entity = this.getById(id);
            if (entity == null) {
                System.out.println("错误：预约不存在，ID: " + id);
                return false;
            }
            
            System.out.println("预约信息:");
            System.out.println("  - ID: " + entity.getId());
            System.out.println("  - 状态: " + entity.getStatus());
            System.out.println("  - 车位ID: " + entity.getParkingSpaceId());
            
            // 检查预约是否已使用
            // 只有USED状态（1）才能结束订单，PENDING状态（0）需要先解锁（调用use接口）
            if (entity.getStatus() != ReservationEntity.ReservationStatus.USED.getCode()) {
                System.out.println("状态验证失败：当前状态=" + entity.getStatus() + ", 期望状态=1(USED)");
                String statusDesc;
                if (entity.getStatus() == ReservationEntity.ReservationStatus.PENDING.getCode()) {
                    statusDesc = "待使用（请先点击'立即解锁'）";
                } else if (entity.getStatus() == ReservationEntity.ReservationStatus.CANCELLED.getCode()) {
                    statusDesc = "已取消";
                } else if (entity.getStatus() == ReservationEntity.ReservationStatus.TIMEOUT.getCode()) {
                    statusDesc = "已超时";
                } else {
                    statusDesc = "未知状态";
                }
                throw new RuntimeException("订单未使用，当前状态：" + statusDesc);
            }
            
            System.out.println("状态验证通过，开始更新...");
            
            // 使用UpdateWrapper更新，只添加结束时间，不改变状态
            // 结束订单后状态变为"待支付"，支付成功后才会变为"已完成"
            UpdateWrapper<ReservationEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", entity.getId());
            updateWrapper.set("end_time", new Date()); // 设置预约结束时间
            updateWrapper.set("actual_exit_time", new Date()); // 设置实际出场时间
            updateWrapper.set("updated_at", new Date());
            // 注意：不改变status，保持为1（已使用），也不释放车位（等支付完成后再释放）
            
            boolean updateResult = this.update(updateWrapper);
            
            System.out.println("预约更新结果: " + updateResult);
            
            if (!updateResult) {
                System.out.println("错误：更新预约失败");
                return false;
            }
            
            System.out.println("订单已结束，等待支付。状态保持为：已使用(1)，支付状态：未支付(0)");
            System.out.println("========== 完成预约流程结束 ==========");
            
            return true;
        } catch (Exception e) {
            System.err.println("完成预约失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("完成预约失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyRefund(Long id, Long userId) {
        // 查询预约信息
        ReservationEntity entity = this.getById(id);
        if (entity == null) {
            return false;
        }
        
        // 验证是否为用户本人的预约
        if (!entity.getUserId().equals(userId)) {
            throw new RuntimeException("无权限操作此预约");
        }
        
        // 检查是否已经申请过退款
        if (entity.getRefundStatus() != null && entity.getRefundStatus() != ReservationEntity.RefundStatus.NO_REFUND.getCode()) {
            throw new RuntimeException("该预约已经申请过退款");
        }
        
        // 检查预约状态是否允许退款
        if (entity.getStatus() != ReservationEntity.ReservationStatus.CANCELLED.getCode() && 
            entity.getStatus() != ReservationEntity.ReservationStatus.USED.getCode()) {
            throw new RuntimeException("只有已取消或已完成的预约可以申请退款");
        }
        
        // 更新退款状态为退款中
        entity.setRefundStatus(ReservationEntity.RefundStatus.REFUNDING.getCode());
        entity.setUpdatedAt(new Date());
        
        if (!this.updateById(entity)) {
            return false;
        }
        
        try {
            // 调用支付服务进行退款 - 暂时跳过实际退款逻辑
            boolean refundResult = true; // paymentService.refund方法暂时模拟成功
            
            if (refundResult) {
                // 更新退款状态为退款成功
                entity.setRefundStatus(ReservationEntity.RefundStatus.REFUND_SUCCESS.getCode());
            } else {
                // 更新退款状态为退款失败
                entity.setRefundStatus(ReservationEntity.RefundStatus.REFUND_FAILED.getCode());
            }
            
            entity.setUpdatedAt(new Date());
            this.updateById(entity);
            
            return refundResult;
        } catch (Exception e) {
            // 更新退款状态为退款失败
            entity.setRefundStatus(ReservationEntity.RefundStatus.REFUND_FAILED.getCode());
            entity.setUpdatedAt(new Date());
            this.updateById(entity);
            
            throw new RuntimeException("申请退款失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ReservationDTO getReservationById(Long id) {
        try {
            ReservationEntity entity = this.getById(id);
            if (entity == null) {
                System.out.println("预约不存在，ID: " + id);
                return null;
            }
            
            // 确保 paymentStatus 有默认值（如果数据库中没有该字段或为NULL）
            if (entity.getPaymentStatus() == null) {
                entity.setPaymentStatus(0); // 默认为未支付
            }
            
            ReservationDTO dto = convertToDTO(entity);
            if (dto == null) {
                System.err.println("转换DTO失败，entity: " + entity);
                return null;
            }
            
            // 加载关联数据 - 车位信息
            if (entity.getParkingSpaceId() != null) {
                try {
                    ParkingSpaceEntity parkingSpace = parkingSpaceMapper.selectById(entity.getParkingSpaceId());
                    if (parkingSpace != null) {
                        ParkingSpaceDTO parkingSpaceDTO = new ParkingSpaceDTO();
                        BeanUtils.copyProperties(parkingSpace, parkingSpaceDTO);
                        dto.setParkingSpace(parkingSpaceDTO);
                    }
                } catch (Exception e) {
                    System.err.println("加载车位信息失败: " + e.getMessage());
                    e.printStackTrace();
                    // 不阻止返回，继续执行
                }
            }
            
            // 加载停车场信息
            if (entity.getParkingId() != null) {
                try {
                    // 使用Mapper查询停车场信息
                    Map<String, Object> parkingLotInfo = reservationMapper.selectParkingLotInfo(entity.getParkingId());
                    if (parkingLotInfo != null && !parkingLotInfo.isEmpty()) {
                        // 将停车场信息添加到DTO中
                        Object nameObj = parkingLotInfo.get("name");
                        Object addressObj = parkingLotInfo.get("address");
                        Object hourlyRateObj = parkingLotInfo.get("hourly_rate");
                        
                        dto.setParkingLotName(nameObj != null ? nameObj.toString() : null);
                        dto.setParkingLotAddress(addressObj != null ? addressObj.toString() : null);
                        
                        if (hourlyRateObj != null) {
                            if (hourlyRateObj instanceof Number) {
                                dto.setParkingLotHourlyRate(((Number) hourlyRateObj).doubleValue());
                            } else if (hourlyRateObj instanceof String) {
                                try {
                                    dto.setParkingLotHourlyRate(Double.parseDouble((String) hourlyRateObj));
                                } catch (NumberFormatException e) {
                                    System.err.println("解析小时费率失败: " + hourlyRateObj);
                                }
                            }
                        }
                    } else {
                        System.out.println("警告：停车场ID=" + entity.getParkingId() + " 的停车场信息未找到");
                    }
                } catch (Exception e) {
                    System.err.println("加载停车场信息失败: " + e.getMessage());
                    e.printStackTrace();
                    // 不阻止返回，继续执行
                }
            }
            
            return dto;
        } catch (Exception e) {
            System.err.println("获取预约详情异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取预约详情失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<ReservationDTO> getUserReservations(Long userId, Integer pageNum, Integer pageSize) {
        try {
            List<ReservationEntity> entities = reservationMapper.selectByUserId(userId, pageNum, pageSize);
            if (entities == null || entities.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            
            return entities.stream()
                .map(entity -> {
                    try {
                        ReservationDTO dto = convertToDTO(entity);
                        if (dto == null) {
                            return null;
                        }
                        
                        // 加载关联数据 - 车位信息
                        if (entity.getParkingSpaceId() != null) {
                            try {
                                ParkingSpaceEntity parkingSpace = parkingSpaceMapper.selectById(entity.getParkingSpaceId());
                                if (parkingSpace != null) {
                                    ParkingSpaceDTO parkingSpaceDTO = new ParkingSpaceDTO();
                                    BeanUtils.copyProperties(parkingSpace, parkingSpaceDTO);
                                    dto.setParkingSpace(parkingSpaceDTO);
                                }
                            } catch (Exception e) {
                                System.err.println("加载车位信息失败，预约ID: " + entity.getId() + ", 错误: " + e.getMessage());
                                // 不阻止返回，继续执行
                            }
                        }
                        
                        // 加载停车场信息
                        if (entity.getParkingId() != null) {
                            try {
                                Map<String, Object> parkingLotInfo = reservationMapper.selectParkingLotInfo(entity.getParkingId());
                                if (parkingLotInfo != null && !parkingLotInfo.isEmpty()) {
                                    Object nameObj = parkingLotInfo.get("name");
                                    Object addressObj = parkingLotInfo.get("address");
                                    Object hourlyRateObj = parkingLotInfo.get("hourly_rate");
                                    
                                    dto.setParkingLotName(nameObj != null ? nameObj.toString() : null);
                                    dto.setParkingLotAddress(addressObj != null ? addressObj.toString() : null);
                                    
                                    if (hourlyRateObj != null) {
                                        if (hourlyRateObj instanceof Number) {
                                            dto.setParkingLotHourlyRate(((Number) hourlyRateObj).doubleValue());
                                        } else if (hourlyRateObj instanceof String) {
                                            try {
                                                dto.setParkingLotHourlyRate(Double.parseDouble((String) hourlyRateObj));
                                            } catch (NumberFormatException e) {
                                                System.err.println("解析小时费率失败: " + hourlyRateObj);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("加载停车场信息失败，预约ID: " + entity.getId() + ", 错误: " + e.getMessage());
                                // 不阻止返回，继续执行
                            }
                        }
                        
                        return dto;
                    } catch (Exception e) {
                        System.err.println("转换预约DTO失败，预约ID: " + (entity != null ? entity.getId() : "null") + ", 错误: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("获取用户预约列表异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取用户预约列表失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<ReservationDTO> queryReservations(ReservationQueryDTO queryDTO) {
        // 构建查询条件
        QueryWrapper<ReservationEntity> wrapper = new QueryWrapper<>();
        
        if (queryDTO.getUserId() != null) {
            wrapper.eq("user_id", queryDTO.getUserId());
        }
        if (queryDTO.getParkingSpaceId() != null) {
            wrapper.eq("parking_space_id", queryDTO.getParkingSpaceId());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq("status", queryDTO.getStatus());
        }
        if (queryDTO.getStartTimeFrom() != null) {
            wrapper.ge("start_time", queryDTO.getStartTimeFrom());
        }
        if (queryDTO.getStartTimeTo() != null) {
            wrapper.le("start_time", queryDTO.getStartTimeTo());
        }
        if (queryDTO.getEndTimeFrom() != null) {
            wrapper.ge("end_time", queryDTO.getEndTimeFrom());
        }
        if (queryDTO.getEndTimeTo() != null) {
            wrapper.le("end_time", queryDTO.getEndTimeTo());
        }
        
        // 执行查询
        List<ReservationEntity> entities = reservationMapper.selectList(wrapper);
        
        // 转换为DTO
        return entities.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePaymentStatus(Long reservationId, int paymentStatus) {
        try {
            ReservationEntity entity = reservationMapper.selectById(reservationId);
            if (entity == null) {
                System.out.println("预约不存在，ID: " + reservationId);
                return false;
            }
            
            System.out.println("开始更新支付状态，预约ID: " + reservationId + ", 支付状态: " + paymentStatus);
            System.out.println("当前预约状态: " + entity.getStatus() + ", 当前支付状态: " + entity.getPaymentStatus());
            
            // 使用UpdateWrapper更新，确保字段映射正确（使用数据库列名）
            UpdateWrapper<ReservationEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", reservationId);
            updateWrapper.set("payment_status", paymentStatus);
            updateWrapper.set("updated_at", new Date());
            
            boolean updateResult = this.update(updateWrapper);
            System.out.println("支付状态更新结果: " + updateResult);
            
            if (!updateResult) {
                System.err.println("警告：支付状态更新失败，预约ID: " + reservationId);
                return false;
            }
            
            // 如果支付成功（paymentStatus = 1），释放车位
            if (paymentStatus == 1 && entity.getStatus() == ReservationEntity.ReservationStatus.USED.getCode()) {
                System.out.println("支付成功，开始释放车位，车位ID: " + entity.getParkingSpaceId());
                try {
                    // 使用UpdateWrapper更新车位状态，避免乐观锁问题
                    com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ParkingSpaceEntity> spaceUpdateWrapper = 
                        new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
                    spaceUpdateWrapper.eq("id", entity.getParkingSpaceId());
                    spaceUpdateWrapper.set("state", 0);
                    spaceUpdateWrapper.set("status", "AVAILABLE");
                    spaceUpdateWrapper.set("is_available", 1);
                    spaceUpdateWrapper.set("updated_at", java.time.LocalDateTime.now());
                    
                    int spaceUpdated = parkingSpaceMapper.update(null, spaceUpdateWrapper);
                    System.out.println("车位释放结果: " + spaceUpdated);
                    
                    if (spaceUpdated > 0) {
                        System.out.println("车位释放成功");
                        
                        // 增加停车场的可用车位数和总车位数（+1）
                        try {
                            int parkingUpdated = reservationMapper.increaseParkingLotSpaces(entity.getParkingId());
                            if (parkingUpdated > 0) {
                                System.out.println("停车场车位数还原成功，停车场ID: " + entity.getParkingId());
                            } else {
                                System.out.println("警告：停车场车位数还原失败，停车场ID: " + entity.getParkingId());
                            }
                        } catch (Exception e) {
                            System.err.println("更新停车场车位数失败: " + e.getMessage());
                            e.printStackTrace();
                            // 不阻止支付流程，只记录错误日志
                        }
                    } else {
                        System.out.println("警告：车位释放失败，但支付状态已更新");
                    }
                } catch (Exception e) {
                    System.err.println("释放车位时出错: " + e.getMessage());
                    // 不阻止支付状态更新，只记录错误
                }
            }
            
            return updateResult;
        } catch (Exception e) {
            throw new RuntimeException("更新支付状态失败", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRefundStatus(Long reservationId, int refundStatus) {
        try {
            ReservationEntity entity = reservationMapper.selectById(reservationId);
            if (entity == null) {
                return false;
            }
            
            // 这里假设实体类中有refund_status字段
            // 如果没有，需要先添加该字段
            entity.setRefundStatus(refundStatus);
            entity.setUpdatedAt(new Date());
            
            return reservationMapper.updateById(entity) > 0;
        } catch (Exception e) {
            throw new RuntimeException("更新退款状态失败", e);
        }
    }
    
    @Override
    public boolean checkSpaceAvailability(Long parkingSpaceId, Date startTime, Date endTime, Long excludeId) {
        Integer overlappingCount;
        if (excludeId != null) {
            overlappingCount = reservationMapper.checkOverlappingReservationExclude(
                    parkingSpaceId, startTime, endTime, excludeId);
        } else {
            overlappingCount = reservationMapper.checkOverlappingReservation(
                    parkingSpaceId, startTime, endTime);
        }
        return overlappingCount == null || overlappingCount <= 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTimeoutReservations() {
        int count = reservationMapper.updateTimeoutReservations(new Date());
        return count;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCompletedAndCancelledReservations(Long userId) {
        try {
            System.out.println("========== 开始删除已完成、已取消和已超时的预约记录 ==========");
            System.out.println("用户ID: " + userId);
            
            // 查询该用户的所有已完成、已取消和已超时的预约
            // 状态1（已使用）+ 已支付 = 已完成
            // 状态2 = 已取消
            // 状态3 = 已超时
            QueryWrapper<ReservationEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId);
            queryWrapper.and(wrapper -> {
                // 已完成：状态1（已使用）且已支付
                wrapper.or(orWrapper1 -> {
                    orWrapper1.eq("status", 1)
                              .eq("payment_status", 1);
                });
                // 已取消：状态2
                wrapper.or(orWrapper2 -> {
                    orWrapper2.eq("status", 2);
                });
                // 已超时：状态3
                wrapper.or(orWrapper3 -> {
                    orWrapper3.eq("status", 3);
                });
            });
            
            List<ReservationEntity> toDelete = this.list(queryWrapper);
            int count = toDelete != null ? toDelete.size() : 0;
            
            if (count > 0) {
                // 批量删除
                boolean result = this.remove(queryWrapper);
                if (result) {
                    System.out.println("成功删除 " + count + " 条已完成、已取消和已超时的预约记录");
                } else {
                    System.out.println("删除失败");
                    return 0;
                }
            } else {
                System.out.println("没有需要删除的记录");
            }
            
            return count;
        } catch (Exception e) {
            System.err.println("删除预约记录异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("删除预约记录失败: " + e.getMessage(), e);
        }
    }
    

    
    /**
     * 生成预约编号
     * @return 预约编号
     */
    private String generateReservationNo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "RES" + timestamp + random;
    }
    
    /**
     * 实体转DTO
     * @param entity 实体对象
     * @return DTO对象
     */
    @Override
    public ReservationDTO convertToDTO(ReservationEntity entity) {
        if (entity == null) {
            return null;
        }
        
        try {
            ReservationDTO dto = new ReservationDTO();
            BeanUtils.copyProperties(entity, dto);
            
            // 确保关键字段被正确复制
            if (entity.getPlateNumber() != null) {
                dto.setPlateNumber(entity.getPlateNumber());
            }
            if (entity.getContactPhone() != null) {
                dto.setContactPhone(entity.getContactPhone());
            }
            
            // 设置状态描述
            if (entity.getStatus() != null) {
                for (ReservationEntity.ReservationStatus status : ReservationEntity.ReservationStatus.values()) {
                    if (status.getCode() == entity.getStatus()) {
                        dto.setStatusDesc(status.getDesc());
                        break;
                    }
                }
            }
            
            // 设置支付状态（确保正确复制）
            if (entity.getPaymentStatus() != null) {
                dto.setPaymentStatus(entity.getPaymentStatus());
            } else {
                dto.setPaymentStatus(0); // 默认为未支付
            }
            
            // 设置退款状态描述
            if (entity.getRefundStatus() != null) {
                for (ReservationEntity.RefundStatus refundStatus : ReservationEntity.RefundStatus.values()) {
                    if (refundStatus.getCode() == entity.getRefundStatus()) {
                        dto.setRefundStatusDesc(refundStatus.getDesc());
                        break;
                    }
                }
            }
            
            return dto;
        } catch (Exception e) {
            System.err.println("转换ReservationDTO异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("转换预约DTO失败: " + e.getMessage(), e);
        }
    }
}