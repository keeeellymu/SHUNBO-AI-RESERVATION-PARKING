package com.parking.service;

import com.parking.model.vo.NavigationRoute;
import java.util.List;

/**
 * 导航服务接口
 * 负责生成从用户位置到目标车位的导航路线
 */
public interface NavigationService {
    
    /**
     * 生成导航路线
     * @param fromLongitude 起点经度
     * @param fromLatitude 起点纬度
     * @param toLongitude 终点经度
     * @param toLatitude 终点纬度
     * @return 导航路线信息
     */
    NavigationRoute generateRoute(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude);
    
    /**
     * 获取停车场内部导航路线
     * @param userId 用户ID
     * @param parkingSpaceId 车位ID
     * @return 内部导航路线
     */
    NavigationRoute getParkingInternalRoute(Long userId, Long parkingSpaceId);
    
    /**
     * 根据路径点生成语音导航指令
     * @param route 导航路线
     * @return 语音导航指令列表
     */
    List<String> generateVoiceNavigationCommands(NavigationRoute route);
    
    /**
     * 计算预计到达时间
     * @param distance 距离（米）
     * @param averageSpeed 平均速度（米/秒）
     * @return 预计到达时间（分钟）
     */
    int calculateEstimatedTime(double distance, double averageSpeed);
}