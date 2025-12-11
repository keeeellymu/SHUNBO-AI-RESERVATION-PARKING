package com.parking.service;

import java.util.List;
import java.util.Set;

/**
 * 停车场名称服务
 * 用于管理停车场名称列表和实现模糊匹配/联想功能
 */
public interface ParkingNameService {
    
    /**
     * 获取所有停车场名称列表
     * @return 停车场名称集合
     */
    Set<String> getAllParkingNames();
    
    /**
     * 模糊匹配停车场名称
     * 支持联想功能，例如"天河场"可以匹配到"天河城停车场"
     * @param input 用户输入的关键词
     * @return 匹配到的停车场名称列表（按匹配度排序）
     */
    List<String> fuzzyMatch(String input);
    
    /**
     * 检查输入是否可能是停车场名称
     * @param input 用户输入
     * @return 如果可能是停车场名称返回true
     */
    boolean isPossibleParkingName(String input);
    
    /**
     * 刷新停车场名称缓存（当停车场数据更新时调用）
     */
    void refreshCache();
}

