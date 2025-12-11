package com.parking.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 支付状态VO
 */
@Data
public class PaymentStatusVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 支付ID
     */
    private String paymentId;
    
    /**
     * 支付状态
     * WAITING: 等待支付
     * SUCCESS: 支付成功
     * FAILED: 支付失败
     * CLOSED: 已关闭
     * REFUNDED: 已退款
     */
    private String status;
    
    /**
     * 订单金额（元）
     */
    private Double amount;
    
    /**
     * 实际支付金额（元）
     */
    private Double actualAmount;
    
    /**
     * 支付时间
     */
    private Date paymentTime;
    
    /**
     * 交易流水号
     */
    private String transactionId;
    
    /**
     * 支付渠道
     */
    private String channel;
    
    /**
     * 失败原因
     */
    private String failReason;
}