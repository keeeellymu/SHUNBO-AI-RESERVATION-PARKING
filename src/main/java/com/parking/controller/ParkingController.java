package com.parking.controller;

import com.parking.model.dto.ReserveDTO;
import com.parking.model.dto.ResultDTO;
import com.parking.service.ParkingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/parking")
public class ParkingController {
    
    @Autowired
    private ParkingService parkingService;
    
    /**
     * 预约停车位
     */
    @PostMapping("/reserve")
    public ResultDTO reserveParking(@RequestBody @Valid ReserveDTO reserveDTO) {
        return parkingService.reserve(reserveDTO);
    }
    
    /**
     * 获取附近停车场列表 - 接受可空参数
     */
    @GetMapping("/nearby")
    public ResultDTO getNearbyParkings(
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) String district) {
        return parkingService.getNearbyParkings(longitude, latitude, radius, district);
    }
    
    /**
     * 测试端点
     */
    @GetMapping("/simple-test")
    public ResultDTO testEndpoint() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Test successful");
        data.put("timestamp", System.currentTimeMillis());
        return ResultDTO.success(data);
    }
    
    /**
     * 超简单测试端点 - 只返回字符串，带异常捕获
     */
    @GetMapping("/very-simple-test")
    public String verySimpleTest() {
        try {
            System.out.println("verySimpleTest方法开始执行");
            String result = "Hello from Parking API"; // 直接返回字符串，不经过ResultDTO
            System.out.println("verySimpleTest方法执行成功，返回值: " + result);
            return result;
        } catch (Exception e) {
            // 直接在方法内捕获并打印异常，这样可以看到具体的错误
            System.err.println("verySimpleTest方法执行异常:");
            System.err.println("异常类型: " + e.getClass().getName());
            System.err.println("异常消息: " + e.getMessage());
            System.err.println("异常堆栈:");
            e.printStackTrace();
            throw e; // 重新抛出异常
        }
    }
    
    /**
     * 获取停车场统计信息
     */
    @GetMapping("/stats")
    public ResultDTO getParkingStats() {
        Map<String, Object> statsData = new HashMap<>();
        statsData.put("totalParkings", 3);
        statsData.put("totalSpaces", 450);
        statsData.put("availableSpaces", 270);
        statsData.put("occupiedSpaces", 180);
        statsData.put("hourlyRate", 5.0);
        
        return ResultDTO.success(statsData);
    }
    
    /**
     * 获取停车位详情 - 使用更明确的路径，避免与其他端点冲突
     */
    @GetMapping("/space/{spaceId}")
    public ResultDTO getParkingSpace(@PathVariable Long spaceId) {
        return parkingService.getParkingSpace(spaceId);
    }
    
    /**
     * 搜索停车场（根据名称或地址）
     */
    @GetMapping("/search")
    public ResultDTO searchParkings(@RequestParam String keyword) {
        return parkingService.searchParkings(keyword);
    }
}