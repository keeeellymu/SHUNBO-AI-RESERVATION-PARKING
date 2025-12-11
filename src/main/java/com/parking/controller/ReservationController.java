package com.parking.controller;

import com.parking.model.dto.ReservationDTO;
import com.parking.model.dto.ReservationCreateRequestDTO;
import com.parking.model.dto.ReservationQueryDTO;
import com.parking.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 预约管理控制器
 */
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {
    
    @Autowired
    private ReservationService reservationService;
    
    /**
     * 创建预约
     * @param requestDTO 预约请求信息
     * @return 创建的预约信息
     */
    @PostMapping
    public ResponseEntity<ReservationDTO> createReservation(@Valid @RequestBody ReservationCreateRequestDTO requestDTO) {
        // 从请求上下文获取用户ID（实际应从认证信息中获取）
        Long userId = 1L; // 临时硬编码，实际应从JWT或Session中获取
        ReservationDTO reservationDTO = reservationService.createReservation(requestDTO, userId);
        return ResponseEntity.ok(reservationDTO);
    }
    
    /**
     * 取消预约
     * @param id 预约ID
     * @return 操作结果
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelReservation(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("Controller: 收到取消预约请求，ID: " + id);
            
            // 从请求上下文获取用户ID
            Long userId = 1L; // 临时硬编码
            System.out.println("Controller: 使用用户ID: " + userId);
            
            boolean result = reservationService.cancelReservation(id, userId);
            System.out.println("Controller: 服务返回结果: " + result);
            
            if (result) {
                response.put("success", true);
                response.put("message", "取消预约成功");
                response.put("data", true);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "取消预约失败：预约不存在或无法取消");
                response.put("data", null);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (RuntimeException e) {
            System.err.println("Controller: RuntimeException - " + e.getMessage());
            e.printStackTrace();
            // 捕获业务异常并返回友好的错误信息
            response.put("success", false);
            response.put("message", "取消预约失败：" + e.getMessage());
            response.put("data", null);
            response.put("code", 500);
            return ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            System.err.println("Controller: Exception - " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            // 捕获其他异常
            response.put("success", false);
            response.put("message", "取消预约失败：系统内部错误 - " + e.getMessage());
            response.put("data", null);
            response.put("code", 500);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 使用预约（入场）
     * @param id 预约ID
     * @return 操作结果
     */
    @PostMapping("/{id}/use")
    public ResponseEntity<Boolean> useReservation(@PathVariable Long id) {
        boolean result = reservationService.useReservation(id);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 完成预约（出场）
     * @param id 预约ID
     * @return 操作结果
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Boolean> completeReservation(@PathVariable Long id) {
        boolean result = reservationService.completeReservation(id);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 申请退款
     * @param id 预约ID
     * @return 操作结果
     */
    @PostMapping("/{id}/refund")
    public ResponseEntity<Boolean> applyRefund(@PathVariable Long id) {
        // 从请求上下文获取用户ID
        Long userId = 1L; // 临时硬编码，实际应从JWT或Session中获取
        boolean result = reservationService.applyRefund(id, userId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取预约详情
     * @param id 预约ID
     * @return 预约详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getReservationById(@PathVariable Long id) {
        ReservationDTO reservationDTO = reservationService.getReservationById(id);
        if (reservationDTO != null) {
            return ResponseEntity.ok(reservationDTO);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取用户的预约列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 预约列表
     */
    @GetMapping("/user")
    public ResponseEntity<List<ReservationDTO>> getUserReservations(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 从请求上下文获取用户ID
        Long userId = 1L; // 临时硬编码
        List<ReservationDTO> reservationDTOs = reservationService.getUserReservations(userId, pageNum, pageSize);
        return ResponseEntity.ok(reservationDTOs);
    }
    
    /**
     * 根据条件查询预约列表
     * @param queryDTO 查询条件
     * @return 预约列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<ReservationDTO>> queryReservations(ReservationQueryDTO queryDTO) {
        List<ReservationDTO> reservationDTOs = reservationService.queryReservations(queryDTO);
        return ResponseEntity.ok(reservationDTOs);
    }
    
    /**
     * 检查车位是否可预约
     * @param parkingSpaceId 车位ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 是否可预约
     */
    @GetMapping("/check-availability")
    public ResponseEntity<Boolean> checkSpaceAvailability(
            @RequestParam Long parkingSpaceId,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        try {
            // 解析时间字符串
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date startDate = sdf.parse(startTime);
            java.util.Date endDate = sdf.parse(endTime);
            
            boolean available = reservationService.checkSpaceAvailability(parkingSpaceId, startDate, endDate, null);
            return ResponseEntity.ok(available);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 支付预约订单
     * @param id 预约ID
     * @return 操作结果
     */
    @PostMapping("/{id}/pay")
    public ResponseEntity<Map<String, Object>> payReservation(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean result = reservationService.updatePaymentStatus(id, 1); // 1表示已支付
            if (result) {
                response.put("success", true);
                response.put("message", "支付成功");
                response.put("data", true);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "支付失败：预约不存在或状态无效");
                response.put("data", null);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "支付失败：" + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 清空已完成和已取消的预约记录
     * @return 删除的记录数量
     */
    @DeleteMapping("/user/completed-cancelled")
    public ResponseEntity<Map<String, Object>> clearCompletedAndCancelledReservations() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 从请求上下文获取用户ID
            Long userId = 1L; // 临时硬编码，实际应从JWT或Session中获取
            
            int deletedCount = reservationService.deleteCompletedAndCancelledReservations(userId);
            
            response.put("success", true);
            response.put("message", "清空成功，已删除 " + deletedCount + " 条记录");
            response.put("data", deletedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("清空预约记录异常: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "清空失败：" + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }
}