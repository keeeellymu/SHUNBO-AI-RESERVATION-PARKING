package com.parking.service.impl;

import com.parking.model.dto.PaymentRequestDTO;
import com.parking.model.vo.PaymentResultVO;
import com.parking.model.vo.PaymentStatusVO;
import com.parking.service.PaymentService;
import com.parking.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.Random;

/**
 * 支付服务实现类
 * 对接第三方支付平台，处理支付相关业务逻辑
 */
@Service
public class PaymentServiceImpl implements PaymentService {
    
    @Autowired
    private ReservationService reservationService;
    
    // 支付平台配置
    @Value("${payment.wechat.appid}")
    private String wechatAppId;
    
    @Value("${payment.wechat.mchId}")
    private String wechatMchId;
    
    @Value("${payment.timeout:1800000}") // 默认30分钟超时
    private long paymentTimeout;
    
    // 模拟支付平台客户端（实际应使用SDK）
    private final Random random = new Random();
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResultVO createPayment(PaymentRequestDTO paymentRequest) {
        try {
            // 验证预约是否存在且状态有效
            boolean reservationValid = validateReservation(paymentRequest.getReservationId());
            if (!reservationValid) {
                throw new RuntimeException("预约不存在或状态无效");
            }
            
            // 生成支付ID
            String paymentId = generatePaymentId();
            
            // 根据支付类型创建不同的支付订单
            PaymentResultVO result = new PaymentResultVO();
            result.setPaymentId(paymentId);
            result.setStatus("WAITING");
            result.setAmount(paymentRequest.getAmount());
            result.setTimestamp(System.currentTimeMillis());
            result.setExpireTime(System.currentTimeMillis() + paymentTimeout);
            
            // 根据支付类型生成不同的支付信息
            if ("WECHAT".equals(paymentRequest.getPaymentType())) {
                // 微信支付 - 生成二维码数据
                String qrCodeData = generateWechatQrCode(paymentId, paymentRequest.getAmount());
                result.setQrCodeData(qrCodeData);
            } else if ("ALIPAY".equals(paymentRequest.getPaymentType())) {
                // 支付宝 - 生成支付链接
                String payUrl = generateAlipayUrl(paymentId, paymentRequest.getAmount());
                result.setPayUrl(payUrl);
            }
            
            // 保存支付订单到数据库（实际应实现）
            savePaymentOrder(paymentId, paymentRequest);
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("创建支付订单失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public PaymentStatusVO queryPaymentStatus(String paymentId) {
        // 查询数据库获取支付订单信息
        // 这里简化实现，实际应查询数据库或调用支付平台API
        PaymentStatusVO status = new PaymentStatusVO();
        status.setPaymentId(paymentId);
        status.setStatus("WAITING"); // 默认等待支付
        status.setAmount(10.0); // 示例金额
        status.setActualAmount(10.0);
        
        return status;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaymentNotify(Map<String, String> notifyData) {
        try {
            // 验证通知数据签名
            boolean signatureValid = verifySignature(notifyData);
            if (!signatureValid) {
                return false;
            }
            
            // 获取支付ID和状态
            String paymentId = notifyData.get("out_trade_no");
            String payStatus = notifyData.get("trade_status");
            
            if ("SUCCESS".equals(payStatus)) {
                // 更新支付订单状态
                updatePaymentStatus(paymentId, "SUCCESS");
                
                // 更新预约状态为已支付
                Long reservationId = getReservationIdByPaymentId(paymentId);
                reservationService.updatePaymentStatus(reservationId, 1); // 1表示已支付
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closePayment(String paymentId) {
        try {
            // 调用支付平台关闭订单接口
            boolean closed = closePaymentOnPlatform(paymentId);
            
            if (closed) {
                // 更新本地支付订单状态
                updatePaymentStatus(paymentId, "CLOSED");
                return true;
            }
            
            return false;
        } catch (Exception e) {
            throw new RuntimeException("关闭支付订单失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refund(String paymentId, double refundAmount, String refundReason) {
        try {
            // 验证退款金额
            PaymentStatusVO paymentStatus = queryPaymentStatus(paymentId);
            if (refundAmount > paymentStatus.getActualAmount()) {
                throw new RuntimeException("退款金额不能大于实际支付金额");
            }
            
            // 调用支付平台退款接口
            boolean refunded = refundOnPlatform(paymentId, refundAmount, refundReason);
            
            if (refunded) {
                // 更新支付订单状态
                updatePaymentStatus(paymentId, "REFUNDED");
                
                // 更新预约状态
                Long reservationId = getReservationIdByPaymentId(paymentId);
                reservationService.updateRefundStatus(reservationId, 1); // 1表示已退款
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            throw new RuntimeException("申请退款失败: " + e.getMessage(), e);
        }
    }
    
    // 生成支付ID
    private String generatePaymentId() {
        // 生成格式：PAY + 时间戳 + 4位随机数
        return "PAY" + System.currentTimeMillis() + String.format("%04d", random.nextInt(10000));
    }
    
    // 验证预约有效性
    private boolean validateReservation(Long reservationId) {
        // 实际应调用预约服务验证
        return reservationId != null;
    }
    
    // 生成微信支付二维码（模拟）
    private String generateWechatQrCode(String paymentId, Double amount) {
        // 实际应调用微信支付API
        return "weixin://wxpay/bizpayurl?pr=ABC123&nonce_str=" + paymentId;
    }
    
    // 生成支付宝支付链接（模拟）
    private String generateAlipayUrl(String paymentId, Double amount) {
        // 实际应调用支付宝API
        return "https://openapi.alipay.com/gateway.do?out_trade_no=" + paymentId + "&total_amount=" + amount;
    }
    
    // 保存支付订单（简化实现）
    private void savePaymentOrder(String paymentId, PaymentRequestDTO request) {
        // 实际应保存到数据库
        System.out.println("保存支付订单: " + paymentId);
    }
    
    // 验证签名（简化实现）
    private boolean verifySignature(Map<String, String> data) {
        // 实际应根据支付平台要求验证签名
        return true;
    }
    
    // 更新支付状态
    private void updatePaymentStatus(String paymentId, String status) {
        // 实际应更新数据库
        System.out.println("更新支付状态: " + paymentId + " -> " + status);
    }
    
    // 根据支付ID获取预约ID
    private Long getReservationIdByPaymentId(String paymentId) {
        // 实际应从数据库查询
        return 1L; // 示例返回
    }
    
    // 在支付平台关闭订单
    private boolean closePaymentOnPlatform(String paymentId) {
        // 实际应调用支付平台API
        return true;
    }
    
    // 在支付平台申请退款
    private boolean refundOnPlatform(String paymentId, double amount, String reason) {
        // 实际应调用支付平台API
        return true;
    }
}