package com.parking.service.impl;

import com.parking.model.dto.ParkingSpaceDTO;
import com.parking.model.vo.NavigationRoute;
import com.parking.service.LocationService;
import com.parking.service.NavigationService;
import com.parking.service.ParkingSpaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 导航服务实现类
 */
@Service
public class NavigationServiceImpl implements NavigationService {
    
    @Autowired
    private LocationService locationService;
    
    @Autowired
    private ParkingSpaceService parkingSpaceService;
    
    // 行人平均速度（米/秒）
    private static final double PEDESTRIAN_AVG_SPEED = 1.4;
    
    @Override
    public NavigationRoute generateRoute(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude) {
        NavigationRoute route = new NavigationRoute();
        
        // 计算总距离
        double distance = locationService.calculateDistance(fromLongitude, fromLatitude, toLongitude, toLatitude);
        route.setTotalDistance(distance);
        
        // 计算预计时间
        int estimatedTime = calculateEstimatedTime(distance, PEDESTRIAN_AVG_SPEED);
        route.setEstimatedTime(estimatedTime);
        
        // 生成路径点（简化实现，实际应调用地图API）
        List<NavigationRoute.PathPoint> pathPoints = generatePathPoints(fromLongitude, fromLatitude, toLongitude, toLatitude);
        route.setPathPoints(pathPoints);
        
        // 生成导航指令
        List<String> instructions = generateInstructions(pathPoints);
        route.setInstructions(instructions);
        
        return route;
    }
    
    @Override
    public NavigationRoute getParkingInternalRoute(Long userId, Long parkingSpaceId) {
        try {
            // 获取用户位置
            Map<String, Double> userLocation = locationService.getUserLocation(userId);
            double userLon = userLocation.get("longitude");
            double userLat = userLocation.get("latitude");
            
            // 获取车位位置
            ParkingSpaceDTO space = parkingSpaceService.getParkingSpaceById(parkingSpaceId);
            if (space == null) {
                throw new RuntimeException("车位不存在");
            }
            
            // 生成内部导航路线 - 暂时不使用经纬度，直接创建导航路线
            NavigationRoute route = new NavigationRoute();
            route.setStartName("当前位置");
            route.setEndName("车位 " + space.getSpaceNumber());
            route.setTotalDistance(0); // 临时值
            route.setEstimatedTime(5); // 临时值：预计5分钟到达
            
            return route;
        } catch (Exception e) {
            throw new RuntimeException("生成内部导航路线失败", e);
        }
    }
    
    @Override
    public List<String> generateVoiceNavigationCommands(NavigationRoute route) {
        List<String> voiceCommands = new ArrayList<>();
        
        // 添加开始导航指令
        voiceCommands.add("开始导航到" + route.getEndName() + "，总距离约" + 
                          Math.round(route.getTotalDistance()) + "米，预计需要" + route.getEstimatedTime() + "分钟");
        
        // 添加详细导航指令
        for (String instruction : route.getInstructions()) {
            voiceCommands.add(instruction);
        }
        
        // 添加到达指令
        voiceCommands.add("您已到达目的地，祝您停车愉快");
        
        return voiceCommands;
    }
    
    @Override
    public int calculateEstimatedTime(double distance, double averageSpeed) {
        if (averageSpeed <= 0) {
            return 0;
        }
        
        // 计算时间（秒）
        double timeInSeconds = distance / averageSpeed;
        // 转换为分钟并四舍五入
        return (int) Math.round(timeInSeconds / 60);
    }
    
    // 生成路径点（简化实现）
    private List<NavigationRoute.PathPoint> generatePathPoints(double fromLon, double fromLat, double toLon, double toLat) {
        List<NavigationRoute.PathPoint> pathPoints = new ArrayList<>();
        
        // 添加起点
        NavigationRoute.PathPoint startPoint = new NavigationRoute.PathPoint();
        startPoint.setLongitude(fromLon);
        startPoint.setLatitude(fromLat);
        startPoint.setInstruction("从当前位置出发");
        startPoint.setDistanceFromStart(0);
        pathPoints.add(startPoint);
        
        // 添加中间点（简化实现）
        NavigationRoute.PathPoint middlePoint = new NavigationRoute.PathPoint();
        middlePoint.setLongitude((fromLon + toLon) / 2);
        middlePoint.setLatitude((fromLat + toLat) / 2);
        middlePoint.setInstruction("继续直行");
        middlePoint.setDistanceFromStart((int) (locationService.calculateDistance(fromLon, fromLat, toLon, toLat) / 2));
        pathPoints.add(middlePoint);
        
        // 添加终点
        NavigationRoute.PathPoint endPoint = new NavigationRoute.PathPoint();
        endPoint.setLongitude(toLon);
        endPoint.setLatitude(toLat);
        endPoint.setInstruction("到达目的地");
        endPoint.setDistanceFromStart((int) locationService.calculateDistance(fromLon, fromLat, toLon, toLat));
        pathPoints.add(endPoint);
        
        return pathPoints;
    }
    
    // 生成导航指令（简化实现）
    private List<String> generateInstructions(List<NavigationRoute.PathPoint> pathPoints) {
        if (pathPoints.size() < 2) {
            return new ArrayList<>();
        }
        
        List<String> instructions = new ArrayList<>();
        
        // 简化实现，实际应根据路径点生成更详细的指令
        instructions.add("从当前位置出发，前往目标车位");
        
        if (pathPoints.size() > 2) {
            for (int i = 1; i < pathPoints.size() - 1; i++) {
                NavigationRoute.PathPoint point = pathPoints.get(i);
                instructions.add("距离起点约" + point.getDistanceFromStart() + "米，" + point.getInstruction());
            }
        }
        
        NavigationRoute.PathPoint endPoint = pathPoints.get(pathPoints.size() - 1);
        instructions.add("即将到达车位，请确认车位编号并停车");
        
        return instructions;
    }
}