package com.parking.service;

import java.util.Map;

/**
 * 高德地图服务接口
 * 负责地理编码（地址转坐标）功能
 */
public interface AmapService {
    
    /**
     * 地理编码：将地址转换为经纬度坐标
     * @param address 地址字符串（如"北京路"）
     * @return 包含 longitude 和 latitude 的 Map，失败返回 null
     */
    Map<String, Double> geocode(String address);
}

