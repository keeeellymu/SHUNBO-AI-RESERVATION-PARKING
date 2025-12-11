package com.parking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 首页控制器
 * 处理根路径请求
 */
@RestController
public class HomeController {

    /**
     * 处理根路径和index路径请求
     * @return 系统信息
     */
    @GetMapping({"/", "/index"})
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "智能停车场系统后端服务");
        response.put("status", "running");
        response.put("version", "1.0.0");
        response.put("api", "/api/v1/*");
        response.put("docs", "请参考API文档");
        // 明确返回200状态码
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * 处理403访问拒绝路径
     * 与WebSecurityConfig中的accessDeniedPage配置配合使用
     * @return 403错误信息页面（返回200状态码以便客户端可以正常接收错误信息）
     */
    @GetMapping("/403")
    public ResponseEntity<Map<String, Object>> handleAccessDenied() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "权限不足");
        response.put("message", "您没有权限访问此资源");
        response.put("status", "forbidden");
        response.put("errorCode", "20001");
        // 返回200状态码以便客户端可以正常接收错误信息
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}