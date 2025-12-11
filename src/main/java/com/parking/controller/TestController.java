package com.parking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试控制器 - 一个完全独立的、没有任何依赖的简单控制器
 */
@RestController
public class TestController {
    
    /**
     * 最简单的测试端点
     */
    @GetMapping("/test")
    public String test() {
        System.out.println("TestController.test() 方法被调用");
        return "Test successful!";
    }
}