package com.parking.controller;

import com.parking.model.vo.NavigationRoute;
import com.parking.service.NavigationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 导航控制器
 * 处理导航相关的API请求
 */
@RestController
@RequestMapping("/api/v1/navigation")
public class NavigationController {
    
    @Autowired
    private NavigationService navigationService;
    
    /**
     * 生成导航路线
     * @param fromLongitude 起点经度
     * @param fromLatitude 起点纬度
     * @param toLongitude 终点经度
     * @param toLatitude 终点纬度
     * @return 导航路线信息
     */
    @GetMapping("/route")
    public ResponseEntity<NavigationRoute> generateRoute(
            @RequestParam double fromLongitude,
            @RequestParam double fromLatitude,
            @RequestParam double toLongitude,
            @RequestParam double toLatitude) {
        
        NavigationRoute route = navigationService.generateRoute(
            fromLongitude, fromLatitude, toLongitude, toLatitude
        );
        return ResponseEntity.ok(route);
    }
    
    /**
     * 获取停车场内部导航路线
     * @param parkingSpaceId 车位ID
     * @param userId 用户ID
     * @return 内部导航路线
     */
    @GetMapping("/internal/{parkingSpaceId}")
    public ResponseEntity<NavigationRoute> getParkingInternalRoute(
            @PathVariable Long parkingSpaceId,
            @RequestAttribute("userId") Long userId) {
        
        NavigationRoute route = navigationService.getParkingInternalRoute(userId, parkingSpaceId);
        return ResponseEntity.ok(route);
    }
    
    /**
     * 获取语音导航指令
     * @param route 导航路线信息
     * @return 语音导航指令列表
     */
    @PostMapping("/voice-commands")
    public ResponseEntity<List<String>> generateVoiceNavigationCommands(
            @RequestBody NavigationRoute route) {
        
        List<String> voiceCommands = navigationService.generateVoiceNavigationCommands(route);
        return ResponseEntity.ok(voiceCommands);
    }
}