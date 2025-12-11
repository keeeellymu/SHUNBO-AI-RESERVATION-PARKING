package com.parking.model.vo;

import lombok.Data;
import java.util.List;

/**
 * 导航路线信息
 */
@Data
public class NavigationRoute {
    
    /**
     * 总距离（米）
     */
    private double totalDistance;
    
    /**
     * 预计时间（分钟）
     */
    private int estimatedTime;
    
    /**
     * 路径点列表
     */
    private List<PathPoint> pathPoints;
    
    /**
     * 导航指令列表
     */
    private List<String> instructions;
    
    /**
     * 起点名称
     */
    private String startName;
    
    /**
     * 终点名称
     */
    private String endName;
    
    /**
     * 导航路径点
     */
    @Data
    public static class PathPoint {
        private double longitude;
        private double latitude;
        private String instruction;
        private int distanceFromStart;
    }
}