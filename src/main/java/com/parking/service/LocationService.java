package com.parking.service;

import java.util.Map;

/**
 * 位置服务接口
 * 负责处理用户位置相关的功能
 */
public interface LocationService {
    
    /**
     * 获取用户当前位置
     * @param userId 用户ID
     * @return 包含经度和纬度的Map
     */
    Map<String, Double> getUserLocation(Long userId);
    
    /**
     * 更新用户位置
     * @param userId 用户ID
     * @param longitude 经度
     * @param latitude 纬度
     * @return 是否更新成功
     */
    boolean updateUserLocation(Long userId, double longitude, double latitude);
    
    /**
     * 计算两点之间的距离（米）
     * @param lon1 第一个点的经度
     * @param lat1 第一个点的纬度
     * @param lon2 第二个点的经度
     * @param lat2 第二个点的纬度
     * @return 距离（米）
     */
    double calculateDistance(double lon1, double lat1, double lon2, double lat2);
}