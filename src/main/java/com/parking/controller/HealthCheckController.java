package com.parking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 完全独立，不依赖任何外部服务
 */
@RestController
@RequestMapping("/health")
public class HealthCheckController {

    /**
     * 基础健康检查端点
     */
    @GetMapping("/check")
    public Map<String, String> healthCheck() {
        System.out.println("Health check endpoint called");
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "服务运行正常");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return response;
    }
    
    /**
     * 更简单的端点，直接返回字符串
     */
    @GetMapping("/simple")
    public String simpleCheck() {
        System.out.println("Simple health check endpoint called");
        return "Service is running";
    }
    
    /**
     * 非常简单的端点，只返回数字
     */
    @GetMapping("/number")
    public int numberCheck() {
        System.out.println("Number check endpoint called");
        return 42;
    }
}