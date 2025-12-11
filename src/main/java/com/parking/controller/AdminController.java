package com.parking.controller;

import com.parking.model.dto.ReservationDTO;
import com.parking.model.vo.PageResult;
import com.parking.model.vo.SystemMonitorData;
import com.parking.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员控制器
 * 处理管理员相关的系统监控、异常处理和数据管理请求
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    
    @Autowired
    private AdminService adminService;
    
    /**
     * 获取异常预约列表
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @return 异常预约分页结果
     */
    @GetMapping("/abnormal-reservations")
    public ResponseEntity<PageResult<ReservationDTO>> getAbnormalReservations(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<ReservationDTO> result = adminService.getAbnormalReservations(pageNum, pageSize);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 手动取消异常预约
     * @param reservationId 预约ID
     * @return 是否取消成功
     */
    @PostMapping("/reservations/{id}/cancel")
    public ResponseEntity<Boolean> cancelAbnormalReservation(@PathVariable("id") Long reservationId) {
        boolean result = adminService.cancelAbnormalReservation(reservationId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取系统监控数据
     * @return 系统监控数据
     */
    @GetMapping("/monitor")
    public ResponseEntity<SystemMonitorData> getSystemMonitorData() {
        SystemMonitorData data = adminService.getSystemMonitorData();
        return ResponseEntity.ok(data);
    }
    
    /**
     * 强制释放超时预约的车位
     * @param reservationId 预约ID
     * @return 是否释放成功
     */
    @PostMapping("/reservations/{id}/force-release")
    public ResponseEntity<Boolean> forceReleaseReservation(@PathVariable("id") Long reservationId) {
        boolean result = adminService.forceReleaseReservation(reservationId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 查看预约异常日志
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @return 异常日志分页结果
     */
    @GetMapping("/error-logs")
    public ResponseEntity<PageResult<Map<String, Object>>> getReservationErrorLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<Map<String, Object>> result = adminService.getReservationErrorLogs(pageNum, pageSize);
        return ResponseEntity.ok(result);
    }
}