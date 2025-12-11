package com.parking.service.impl;

import com.parking.model.entity.ParkingSpaceEntity;
import com.parking.model.dto.ParkingSpaceDTO;
import com.parking.model.dto.ParkingSpaceQueryDTO;
import com.parking.dao.ParkingSpaceMapper;
import com.parking.service.ParkingSpaceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 车位服务实现类
 */
@Service
public class ParkingSpaceServiceImpl extends ServiceImpl<ParkingSpaceMapper, ParkingSpaceEntity> implements ParkingSpaceService {
    
    private static final Logger log = LoggerFactory.getLogger(ParkingSpaceServiceImpl.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ParkingSpaceMapper parkingSpaceMapper;
    
    @Override
    public ParkingSpaceDTO getParkingSpaceById(Long id) {
        ParkingSpaceEntity entity = parkingSpaceMapper.selectById(id);
        return convertToDTO(entity);
    }
    
    @Override
    public List<ParkingSpaceDTO> getAvailableSpaces(Long parkingId) {
        List<ParkingSpaceEntity> entities = parkingSpaceMapper.selectAvailableSpaces(parkingId);
        return entities.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Override
    public List<ParkingSpaceDTO> searchSpaces(ParkingSpaceQueryDTO queryDTO) {
        // 直接传递查询DTO
        List<ParkingSpaceEntity> entities = parkingSpaceMapper.selectAvailableSpacesByCondition(queryDTO);
        return entities.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Override
    public boolean lockSpace(Long spaceId) {
        return parkingSpaceMapper.lockSpace(spaceId) > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseSpace(Long spaceId) {
        try {
            // 使用新的更新方法
            int result = parkingSpaceMapper.updateToAvailable(spaceId);
            
            if (result > 0) {
                // 查询车位信息获取停车场ID
                ParkingSpaceEntity entity = parkingSpaceMapper.selectById(spaceId);
                if (entity != null) {
                    // 更新Redis缓存
                    String cacheKey = "parking_space:" + spaceId;
                    redisTemplate.delete(cacheKey);
                    
                    // 更新停车场可用车位数
                    updateParkingLotAvailableSpaces(entity.getParkingId());
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("释放车位失败，spaceId: {}", spaceId, e);
            return false;
        }
    }
    
    @Override
    public boolean updateState(Long id, Integer newState, Integer oldState) {
        return parkingSpaceMapper.updateState(id, newState, oldState) > 0;
    }
    
    @Override
    public boolean updateStateWithVersion(Long id, Integer newState, Integer oldState, Integer version) {
        return parkingSpaceMapper.updateStateWithVersion(id, newState, oldState, version) > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParkingSpaceDTO createParkingSpace(ParkingSpaceDTO parkingSpaceDTO) {
        try {
            ParkingSpaceEntity entity = new ParkingSpaceEntity();
            BeanUtils.copyProperties(parkingSpaceDTO, entity);
            
            // 设置默认状态
            entity.setState(0);  // AVAILABLE
            entity.setStatus("AVAILABLE");
            entity.setIsAvailable(1);  // 1表示可用
            
            // 保存到数据库
            this.save(entity);
            
            // 更新Redis缓存
            String cacheKey = "parking_space:" + entity.getId();
            redisTemplate.opsForValue().set(cacheKey, entity, 10, TimeUnit.MINUTES);
            
            // 更新停车场可用车位数缓存
            updateParkingLotAvailableSpaces(entity.getParkingId());
            
            return convertToDTO(entity);
        } catch (DuplicateKeyException e) {
            throw new RuntimeException("车位编号已存在", e);
        } catch (Exception e) {
            throw new RuntimeException("创建车位失败", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParkingSpaceDTO updateParkingSpace(ParkingSpaceDTO parkingSpaceDTO) {
        try {
            // 先查询现有实体
            ParkingSpaceEntity existingEntity = parkingSpaceMapper.selectById(parkingSpaceDTO.getId());
            if (existingEntity == null) {
                throw new RuntimeException("车位不存在");
            }
            
            Integer oldState = existingEntity.getState();
            Long parkingId = existingEntity.getParkingId();
            
            // 复制属性
            ParkingSpaceEntity entity = new ParkingSpaceEntity();
            BeanUtils.copyProperties(parkingSpaceDTO, entity);
            entity.setCreatedAt(existingEntity.getCreatedAt()); // 保持创建时间不变
            
            // 使用乐观锁更新
            if (!updateStateWithVersion(entity.getId(), entity.getState(), oldState, existingEntity.getVersion())) {
                throw new RuntimeException("更新失败，数据已被其他用户修改");
            }
            
            // 更新Redis缓存
            String cacheKey = "parking_space:" + entity.getId();
            redisTemplate.delete(cacheKey); // 删除旧缓存
            
            // 如果状态发生变化，更新停车场可用车位数
            if (oldState != entity.getState()) {
                updateParkingLotAvailableSpaces(parkingId);
            }
            
            return convertToDTO(entity);
        } catch (Exception e) {
            throw new RuntimeException("更新车位失败", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteParkingSpace(Long id) {
        try {
            // 先查询实体获取停车场ID
            ParkingSpaceEntity entity = parkingSpaceMapper.selectById(id);
            if (entity == null) {
                return false;
            }
            
            Long parkingId = entity.getParkingId();
            
            // 删除数据库记录
            boolean result = this.removeById(id);
            
            if (result) {
                // 删除Redis缓存
                String cacheKey = "parking_space:" + id;
                redisTemplate.delete(cacheKey);
                
                // 更新停车场可用车位数
                updateParkingLotAvailableSpaces(parkingId);
            }
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("删除车位失败", e);
        }
    }
    
    /**
     * 将实体类转换为DTO
     * @param entity 实体类
     * @return DTO对象
     */
    private ParkingSpaceDTO convertToDTO(ParkingSpaceEntity entity) {
        if (entity == null) {
            return null;
        }
        ParkingSpaceDTO dto = new ParkingSpaceDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
    
    /**
     * 更新停车场可用车位数缓存
     * @param parkingId 停车场ID
     */
    private void updateParkingLotAvailableSpaces(Long parkingId) {
        try {
            // 查询可用车位数
            int availableCount = this.countAvailableSpaces(parkingId);
            
            // 更新Redis缓存
            String cacheKey = "parking_lot:available:" + parkingId;
            redisTemplate.opsForValue().set(cacheKey, availableCount, 5, TimeUnit.MINUTES);
            
            // 这里可以添加消息队列通知，让其他服务知道车位状态变化
        } catch (Exception e) {
            log.error("更新停车场可用车位数失败，parkingId: {}", parkingId, e);
            // 记录日志但不抛出异常，避免影响主流程
        }
    }
    
    /**
     * 统计停车场可用车位数
     * @param parkingId 停车场ID
     * @return 可用车位数
     */
    private int countAvailableSpaces(Long parkingId) {
        try {
            // 直接使用selectAvailableSpaces方法查询
            List<ParkingSpaceEntity> entities = parkingSpaceMapper.selectAvailableSpaces(parkingId);
            return entities != null ? entities.size() : 0;
        } catch (Exception e) {
            log.error("统计可用车位数失败，parkingId: {}", parkingId, e);
            return 0;
        }
    }
    

}