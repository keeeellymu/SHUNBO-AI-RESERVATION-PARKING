package com.parking.service;

import com.parking.model.dto.PaymentRequestDTO;
import com.parking.model.vo.PaymentResultVO;
import com.parking.model.vo.PaymentStatusVO;
import java.util.Map;

/**
 * 支付服务接口
 * 负责处理支付相关的功能，包括创建支付订单、查询支付状态等
 */
public interface PaymentService {
    
    /**
     * 创建支付订单
     * @param paymentRequest 支付请求信息
     * @return 支付结果，包含支付链接或二维码信息
     */
    PaymentResultVO createPayment(PaymentRequestDTO paymentRequest);
    
    /**
     * 查询支付状态
     * @param paymentId 支付ID
     * @return 支付状态信息
     */
    PaymentStatusVO queryPaymentStatus(String paymentId);
    
    /**
     * 处理支付回调
     * @param notifyData 支付平台回调数据
     * @return 处理结果
     */
    boolean handlePaymentNotify(Map<String, String> notifyData);
    
    /**
     * 关闭支付订单
     * @param paymentId 支付ID
     * @return 是否关闭成功
     */
    boolean closePayment(String paymentId);
    
    /**
     * 申请退款
     * @param paymentId 支付ID
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     * @return 退款结果
     */
    boolean refund(String paymentId, double refundAmount, String refundReason);
}