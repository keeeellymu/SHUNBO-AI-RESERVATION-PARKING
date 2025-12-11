package com.parking.service.impl;

import com.parking.dao.ReservationMapper;
import com.parking.service.ParkingNameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 停车场名称服务实现类
 * 实现停车场名称的模糊匹配和联想功能
 */
@Service
public class ParkingNameServiceImpl implements ParkingNameService {
    
    @Autowired
    private ReservationMapper reservationMapper;
    
    // 停车场名称缓存（使用Set去重）
    private Set<String> parkingNamesCache = ConcurrentHashMap.newKeySet();
    
    @PostConstruct
    public void init() {
        refreshCache();
    }
    
    @Override
    public Set<String> getAllParkingNames() {
        if (parkingNamesCache.isEmpty()) {
            refreshCache();
        }
        return new HashSet<>(parkingNamesCache);
    }
    
    @Override
    public List<String> fuzzyMatch(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // 确保缓存已加载
        if (parkingNamesCache.isEmpty()) {
            refreshCache();
        }
        
        String normalizedInput = normalizeInput(input);
        List<MatchResult> matches = new ArrayList<>();
        
        for (String parkingName : parkingNamesCache) {
            String normalizedName = normalizeParkingName(parkingName);
            
            // 计算匹配度
            double score = calculateMatchScore(normalizedInput, normalizedName, parkingName);
            
            if (score > 0) {
                matches.add(new MatchResult(parkingName, score));
            }
        }
        
        // 按匹配度降序排序
        matches.sort((a, b) -> Double.compare(b.score, a.score));
        
        // 返回前10个最匹配的结果
        return matches.stream()
                .limit(10)
                .map(m -> m.name)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean isPossibleParkingName(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        if (parkingNamesCache.isEmpty()) {
            refreshCache();
        }
        
        String normalizedInput = normalizeInput(input);
        
        // 检查是否与任何停车场名称有相似性
        for (String parkingName : parkingNamesCache) {
            String normalizedName = normalizeParkingName(parkingName);
            if (calculateMatchScore(normalizedInput, normalizedName, parkingName) > 0.3) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public void refreshCache() {
        try {
            System.out.println("========== 刷新停车场名称缓存 ==========");
            List<Map<String, Object>> parkings = reservationMapper.selectAllParkingLots(null);
            
            Set<String> newNames = new HashSet<>();
            
            if (parkings != null) {
                for (Map<String, Object> parking : parkings) {
                    Object nameObj = parking.get("name");
                    if (nameObj != null) {
                        String name = nameObj.toString().trim();
                        if (!name.isEmpty()) {
                            newNames.add(name);
                        }
                    }
                }
            }
            
            this.parkingNamesCache = newNames;
            
            System.out.println("成功加载 " + parkingNamesCache.size() + " 个停车场名称");
            if (!parkingNamesCache.isEmpty()) {
                System.out.println("示例停车场名称（前5个）:");
                int count = 0;
                for (String name : parkingNamesCache) {
                    if (count++ >= 5) break;
                    System.out.println("  - " + name);
                }
            }
            System.out.println("========================================");
        } catch (Exception e) {
            System.err.println("刷新停车场名称缓存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 标准化用户输入
     */
    private String normalizeInput(String input) {
        return input.trim()
                .replaceAll("停车场|停车位|车位|停车|泊车|停车库|停车楼", "") // 移除停车场相关后缀
                .replaceAll("\\s+", "") // 移除空格
                .toLowerCase();
    }
    
    /**
     * 标准化停车场名称
     */
    private String normalizeParkingName(String name) {
        return name.replaceAll("停车场|停车位|车位|停车|泊车|停车库|停车楼", "")
                .replaceAll("\\s+", "")
                .toLowerCase();
    }
    
    /**
     * 计算匹配度分数
     * @param input 用户输入（已标准化）
     * @param normalizedName 停车场名称（已标准化）
     * @param originalName 原始停车场名称
     * @return 匹配度分数（0-1之间）
     */
    private double calculateMatchScore(String input, String normalizedName, String originalName) {
        if (input.isEmpty() || normalizedName.isEmpty()) {
            return 0;
        }
        
        double score = 0;
        
        // 1. 完全匹配（最高分）
        if (normalizedName.equals(input)) {
            return 1.0;
        }
        
        // 2. 包含匹配（高优先级）
        if (normalizedName.contains(input)) {
            score = 0.8 + (input.length() * 0.1 / normalizedName.length()); // 输入越长，分数越高
        } else if (input.contains(normalizedName)) {
            score = 0.6;
        }
        
        // 3. 前缀匹配（例如："天河场"匹配"天河城"）
        if (normalizedName.startsWith(input) || input.startsWith(normalizedName)) {
            score = Math.max(score, 0.7);
        }
        
        // 4. 字符相似度（编辑距离）
        int editDistance = levenshteinDistance(input, normalizedName);
        int maxLen = Math.max(input.length(), normalizedName.length());
        double similarity = 1.0 - (double) editDistance / maxLen;
        
        // 如果相似度较高，使用相似度分数
        if (similarity > 0.5) {
            score = Math.max(score, similarity * 0.9);
        }
        
        // 5. 部分字符匹配（例如："天河场"中的"天河"匹配"天河城"中的"天河"）
        int commonChars = countCommonChars(input, normalizedName);
        if (commonChars > 0) {
            double commonScore = (double) commonChars / Math.max(input.length(), normalizedName.length());
            score = Math.max(score, commonScore * 0.8);
        }
        
        // 6. 如果原始名称包含输入，额外加分
        if (originalName.contains(input)) {
            score = Math.max(score, 0.75);
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * 计算编辑距离（Levenshtein距离）
     */
    private int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1;
                }
            }
        }
        
        return dp[m][n];
    }
    
    /**
     * 计算共同字符数
     */
    private int countCommonChars(String s1, String s2) {
        Set<Character> set1 = new HashSet<>();
        for (char c : s1.toCharArray()) {
            set1.add(c);
        }
        
        int count = 0;
        Set<Character> counted = new HashSet<>();
        for (char c : s2.toCharArray()) {
            if (set1.contains(c) && !counted.contains(c)) {
                count++;
                counted.add(c);
            }
        }
        return count;
    }
    
    /**
     * 匹配结果内部类
     */
    private static class MatchResult {
        String name;
        double score;
        
        MatchResult(String name, double score) {
            this.name = name;
            this.score = score;
        }
    }
}

