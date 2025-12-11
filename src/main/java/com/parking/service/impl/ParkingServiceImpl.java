package com.parking.service.impl;

import com.parking.dao.ParkingSpaceMapper;
import com.parking.dao.ReservationMapper;
import com.parking.exception.ParkingException;
import com.parking.model.dto.ReserveDTO;
import com.parking.model.dto.ResultDTO;
import com.parking.model.entity.ParkingSpaceEntity;
import com.parking.service.ParkingService;
import com.parking.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ParkingServiceImpl implements ParkingService {
    
    @Autowired
    private ParkingSpaceMapper parkingSpaceMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ReservationMapper reservationMapper;
    
    @Override
    @Transactional
    public ResultDTO reserve(ReserveDTO dto) {
        try {
            // 参数验证
            if (dto.getSpaceId() == null || dto.getSpaceId() <= 0) {
                return ResultDTO.fail("无效的车位ID");
            }
            
            // 验证预约时间是否为未来时间
            LocalDateTime reserveTime = dto.getReserveTime();
            if (reserveTime == null || !DateUtils.isFutureTime(reserveTime)) {
                return ResultDTO.fail("预约时间必须为未来时间");
            }
            
            // 悲观锁保证并发安全
            int affected = parkingSpaceMapper.lockSpace(dto.getSpaceId());
            if (affected == 0) {
                return ResultDTO.fail("车位已被占用或不存在");
            }
            
            // 验证成功后，更新车位状态为已预约
            parkingSpaceMapper.updateStatus(dto.getSpaceId(), "RESERVED");
            
            // 存入Redis，设置过期时间（30分钟）
            String key = "parking:reservation:" + dto.getSpaceId();
            redisTemplate.opsForValue().set(key, dto, 30, java.util.concurrent.TimeUnit.MINUTES);
            
            return ResultDTO.success("预约成功");
        } catch (Exception e) {
            throw new ParkingException("预约失败：" + e.getMessage());
        }
    }
    
    @Override
    @Cacheable(value = "parkingSpace", key = "#spaceId")
    public ResultDTO getParkingSpace(Long spaceId) {
        try {
            if (spaceId == null || spaceId <= 0) {
                return ResultDTO.fail("无效的车位ID");
            }
            
            ParkingSpaceEntity entity = parkingSpaceMapper.selectById(spaceId);
            if (entity == null) {
                return ResultDTO.fail("车位不存在");
            }
            
            // 缓存数据
            redisTemplate.opsForValue().set("parking:space:" + spaceId, entity);
            
            return ResultDTO.success(entity);
        } catch (Exception e) {
            throw new ParkingException("获取车位详情失败：" + e.getMessage());
        }
    }
    
    @Override
    public ResultDTO getNearbyParkings(Double longitude, Double latitude, Integer radius, String district) {
        try {
            // 提供默认值
            double defaultLongitude = longitude != null ? longitude : 113.3248; // 广州市中心经度
            double defaultLatitude = latitude != null ? latitude : 23.1288; // 广州市中心纬度
            int defaultRadius = radius != null ? radius : 10000; // 默认10公里
            
            // 详细参数验证
            if (defaultLongitude < -180 || defaultLongitude > 180) {
                return ResultDTO.fail("经度必须在-180到180之间");
            }
            if (defaultLatitude < -90 || defaultLatitude > 90) {
                return ResultDTO.fail("纬度必须在-90到90之间");
            }
            if (defaultRadius <= 0 || defaultRadius > 100000) {
                return ResultDTO.fail("搜索半径必须在1-100000米之间");
            }
            
            // 关键修复：从数据库查询所有停车场（支持按行政区过滤）
            List<Map<String, Object>> parkings;
            long startTime = System.currentTimeMillis();
            try {
                System.out.println("========== 查询停车场列表 ==========");
                System.out.println("区域参数: " + (district != null ? district : "全部"));
                System.out.println("区域参数类型: " + (district != null ? district.getClass().getName() : "null"));
                System.out.println("区域参数长度: " + (district != null ? district.length() : 0));
                if (district != null) {
                    System.out.println("区域参数内容: [" + district + "]");
                }
                parkings = reservationMapper.selectAllParkingLots(district);
                long queryTime = System.currentTimeMillis() - startTime;
                System.out.println("数据库查询成功 (区域: " + (district != null ? district : "全部") + "), 找到 " + (parkings != null ? parkings.size() : 0) + " 个停车场, 耗时: " + queryTime + "ms");
                if (parkings != null && !parkings.isEmpty()) {
                    System.out.println("返回的停车场列表（前3个）:");
                    for (int i = 0; i < Math.min(3, parkings.size()); i++) {
                        Map<String, Object> p = parkings.get(i);
                        System.out.println("  - ID: " + p.get("id") + ", 名称: " + p.get("name") + ", 区域: " + p.get("district"));
                    }
                }
                
                // 如果查询时间超过5秒，记录警告
                if (queryTime > 5000) {
                    System.err.println("警告：数据库查询耗时过长: " + queryTime + "ms，建议检查索引和表数据量");
                }
            } catch (Exception e) {
                long queryTime = System.currentTimeMillis() - startTime;
                System.err.println("查询停车场数据失败 (耗时: " + queryTime + "ms): " + e.getMessage());
                e.printStackTrace();
                return ResultDTO.fail("查询停车场数据失败: " + e.getMessage());
            }
            
            if (parkings == null || parkings.isEmpty()) {
                // 如果设置了区域但结果为空，返回空列表而不是错误
                if (district != null && !district.isEmpty()) {
                    System.out.println("区域 '" + district + "' 未找到停车场数据");
                    return ResultDTO.success(new java.util.ArrayList<>());
                }
                // 添加更详细的错误信息
                System.out.println("警告：未找到停车场数据，请检查数据库 parking_lot 表是否有数据");
                return ResultDTO.fail("未找到停车场数据，请确保数据库 parking_lot 表中有数据");
            }
            
            System.out.println("成功查询到 " + parkings.size() + " 个停车场");
            
            // 计算距离并过滤（未来优化：可以在SQL中计算距离并过滤）
            for (Map<String, Object> parking : parkings) {
                // 确保ID是数字类型
                Object idObj = parking.get("id");
                if (idObj != null) {
                    if (idObj instanceof Number) {
                        parking.put("id", idObj);
                    } else {
                        parking.put("id", Long.parseLong(String.valueOf(idObj)));
                    }
                }
                
                // 计算距离（如果停车场有经纬度）
                Object parkingLongitude = parking.get("longitude");
                Object parkingLatitude = parking.get("latitude");
                if (parkingLongitude != null && parkingLatitude != null) {
                    try {
                        double parkingLng = Double.parseDouble(String.valueOf(parkingLongitude));
                        double parkingLat = Double.parseDouble(String.valueOf(parkingLatitude));
                        double distance = calculateDistance(
                            defaultLongitude, defaultLatitude,
                            parkingLng, parkingLat
                        );
                        parking.put("distance", (int)(distance * 1000)); // 转换为米
                    } catch (Exception e) {
                        parking.put("distance", Integer.MAX_VALUE); // 无法计算时设为最大值
                    }
                } else {
                    parking.put("distance", Integer.MAX_VALUE);
                }
            }
            
            // 按距离排序
            parkings.sort((a, b) -> {
                int distA = (Integer) a.getOrDefault("distance", Integer.MAX_VALUE);
                int distB = (Integer) b.getOrDefault("distance", Integer.MAX_VALUE);
                return Integer.compare(distA, distB);
            });
            
            // 过滤半径内的停车场
            List<Map<String, Object>> nearbyParkings = new java.util.ArrayList<>();
            for (Map<String, Object> parking : parkings) {
                int distance = (Integer) parking.getOrDefault("distance", Integer.MAX_VALUE);
                if (distance <= defaultRadius) {
                    nearbyParkings.add(parking);
                }
            }
            
            // 如果设置了区域但结果为空，返回空列表而不是所有
            if (district != null && !district.isEmpty() && nearbyParkings.isEmpty() && parkings.isEmpty()) {
                return ResultDTO.success(new java.util.ArrayList<>());
            }
            
            // 返回结构化数据
            return ResultDTO.success(nearbyParkings.isEmpty() ? parkings : nearbyParkings);
        } catch (Exception e) {
            // 捕获异常并记录，返回友好的错误信息
            e.printStackTrace();
            return ResultDTO.fail("获取附近停车场失败，请稍后重试：" + e.getMessage());
        }
    }
    
    /**
     * 计算两点之间的距离（单位：米）
     */
    private double calculateDistance(double lng1, double lat1, double lng2, double lat2) {
        final int R = 6371000; // 地球半径（米）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
    @Override
    public ResultDTO searchParkings(String keyword) {
        try {
            // 参数验证
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResultDTO.fail("搜索关键词不能为空");
            }
            
            String trimmedKeyword = keyword.trim();
            if (trimmedKeyword.length() < 1) {
                return ResultDTO.fail("搜索关键词至少需要1个字符");
            }
            
            // 查询数据库
            List<Map<String, Object>> parkings;
            try {
                parkings = reservationMapper.searchParkingLots(trimmedKeyword);
                System.out.println("搜索关键词: " + trimmedKeyword + ", 找到 " + (parkings != null ? parkings.size() : 0) + " 个停车场");
            } catch (Exception e) {
                System.err.println("搜索停车场失败: " + e.getMessage());
                e.printStackTrace();
                return ResultDTO.fail("搜索停车场失败: " + e.getMessage());
            }
            
            if (parkings == null) {
                parkings = new java.util.ArrayList<>();
            }
            
            // 处理数据格式
            for (Map<String, Object> parking : parkings) {
                // 确保ID是数字类型
                Object idObj = parking.get("id");
                if (idObj != null && !(idObj instanceof Number)) {
                    try {
                        parking.put("id", Long.parseLong(String.valueOf(idObj)));
                    } catch (NumberFormatException e) {
                        System.err.println("无效的停车场ID: " + idObj);
                    }
                }
            }
            
            return ResultDTO.success(parkings);
        } catch (Exception e) {
            System.err.println("搜索停车场异常: " + e.getMessage());
            e.printStackTrace();
            return ResultDTO.fail("搜索停车场失败，请稍后重试：" + e.getMessage());
        }
    }
}