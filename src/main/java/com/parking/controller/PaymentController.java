package com.parking.controller;

import com.parking.model.dto.PaymentRequestDTO;
import com.parking.model.vo.PaymentResultVO;
import com.parking.model.vo.PaymentStatusVO;
import com.parking.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器
 * 处理支付相关的API请求
 */
@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {
    
    @Autowired
    private PaymentService paymentService;
    
    /**
     * 创建支付订单
     * @param paymentRequest 支付请求信息
     * @return 支付结果
     */
    @PostMapping("/create")
    public ResponseEntity<PaymentResultVO> createPayment(
            @Valid @RequestBody PaymentRequestDTO paymentRequest) {
        
        PaymentResultVO result = paymentService.createPayment(paymentRequest);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 查询支付状态
     * @param paymentId 支付ID
     * @return 支付状态信息
     */
    @GetMapping("/status/{paymentId}")
    public ResponseEntity<PaymentStatusVO> queryPaymentStatus(
            @PathVariable String paymentId) {
        
        PaymentStatusVO status = paymentService.queryPaymentStatus(paymentId);
        return ResponseEntity.ok(status);
    }
    
    /**
     * 微信支付回调接口
     * @param request HTTP请求
     * @return 回调响应
     */
    @PostMapping("/notify/wechat")
    public ResponseEntity<String> wechatPayNotify(HttpServletRequest request) {
        Map<String, String> notifyData = convertRequestToMap(request);
        boolean success = paymentService.handlePaymentNotify(notifyData);
        
        if (success) {
            return ResponseEntity.ok("<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>");
        } else {
            return ResponseEntity.ok("<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[处理失败]]></return_msg></xml>");
        }
    }
    
    /**
     * 支付宝支付回调接口
     * @param request HTTP请求
     * @return 回调响应
     */
    @PostMapping("/notify/alipay")
    public ResponseEntity<String> alipayNotify(HttpServletRequest request) {
        Map<String, String> notifyData = convertRequestToMap(request);
        boolean success = paymentService.handlePaymentNotify(notifyData);
        
        if (success) {
            return ResponseEntity.ok("success");
        } else {
            return ResponseEntity.ok("fail");
        }
    }
    
    /**
     * 关闭支付订单
     * @param paymentId 支付ID
     * @return 是否关闭成功
     */
    @PostMapping("/close/{paymentId}")
    public ResponseEntity<Boolean> closePayment(
            @PathVariable String paymentId) {
        
        boolean result = paymentService.closePayment(paymentId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 申请退款
     * @param paymentId 支付ID
     * @param refundRequest 退款请求信息
     * @return 是否退款成功
     */
    @PostMapping("/refund/{paymentId}")
    public ResponseEntity<Boolean> refund(
            @PathVariable String paymentId,
            @RequestBody RefundRequestDTO refundRequest) {
        
        boolean result = paymentService.refund(
            paymentId, 
            refundRequest.getAmount(), 
            refundRequest.getReason()
        );
        return ResponseEntity.ok(result);
    }
    
    // 将HTTP请求参数转换为Map
    private Map<String, String> convertRequestToMap(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            result.put(name, request.getParameter(name));
        }
        
        return result;
    }
    
    // 退款请求DTO内部类
    static class RefundRequestDTO {
        private double amount;
        private String reason;
        
        // Getters and Setters
        public double getAmount() {
            return amount;
        }
        
        public void setAmount(double amount) {
            this.amount = amount;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}