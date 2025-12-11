package com.parking.dao;

import org.apache.ibatis.annotations.Mapper;
import java.util.Map;

/**
 * 用户位置Mapper接口
 */
@Mapper
public interface UserLocationMapper {
    
    /**
     * 根据用户ID查询位置
     * @param userId 用户ID
     * @return 包含经度和纬度的Map
     */
    Map<String, Double> selectByUserId(Long userId);
    
    /**
     * 根据用户ID更新位置
     * @param locationData 位置数据，包含userId、longitude、latitude
     * @return 更新行数
     */
    int updateByUserId(Map<String, Object> locationData);
    
    /**
     * 插入用户位置
     * @param locationData 位置数据，包含userId、longitude、latitude
     * @return 插入行数
     */
    int insert(Map<String, Object> locationData);
}