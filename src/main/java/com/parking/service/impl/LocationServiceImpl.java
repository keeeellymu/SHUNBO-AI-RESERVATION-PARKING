package com.parking.service.impl;

import com.parking.dao.UserLocationMapper;
import com.parking.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

/**
 * 位置服务实现类
 */
@Service
public class LocationServiceImpl implements LocationService {
    
    @Autowired
    private UserLocationMapper userLocationMapper;
    
    // 地球半径（米）
    private static final double EARTH_RADIUS = 6371000;
    
    @Override
    public Map<String, Double> getUserLocation(Long userId) {
        try {
            // 尝试从数据库获取用户位置
            Map<String, Double> location = userLocationMapper.selectByUserId(userId);
            if (location != null && location.containsKey("longitude") && location.containsKey("latitude")) {
                return location;
            }
        } catch (Exception e) {
            // 数据库查询失败，返回默认位置
        }
        
        // 返回默认位置（例如停车场入口）
        Map<String, Double> defaultLocation = new HashMap<>();
        defaultLocation.put("longitude", 116.397428); // 示例经度
        defaultLocation.put("latitude", 39.90923);   // 示例纬度
        return defaultLocation;
    }
    
    @Override
    public boolean updateUserLocation(Long userId, double longitude, double latitude) {
        try {
            // 验证经纬度范围
            if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
                return false;
            }
            
            // 构建位置数据
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("userId", userId);
            locationData.put("longitude", longitude);
            locationData.put("latitude", latitude);
            
            // 尝试更新，如果不存在则插入
            if (userLocationMapper.updateByUserId(locationData) <= 0) {
                userLocationMapper.insert(locationData);
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public double calculateDistance(double lon1, double lat1, double lon2, double lat2) {
        // 将经纬度转换为弧度
        double lon1Rad = Math.toRadians(lon1);
        double lat1Rad = Math.toRadians(lat1);
        double lon2Rad = Math.toRadians(lon2);
        double lat2Rad = Math.toRadians(lat2);
        
        // Haversine公式
        double dlon = lon2Rad - lon1Rad;
        double dlat = lat2Rad - lat1Rad;
        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }
}