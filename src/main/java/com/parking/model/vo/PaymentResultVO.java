package com.parking.model.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 支付结果VO
 */
@Data
public class PaymentResultVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 支付ID
     */
    private String paymentId;
    
    /**
     * 支付状态
     */
    private String status;
    
    /**
     * 支付链接（H5支付）
     */
    private String payUrl;
    
    /**
     * 二维码数据（扫码支付）
     */
    private String qrCodeData;
    
    /**
     * 支付时间戳
     */
    private Long timestamp;
    
    /**
     * 订单金额（元）
     */
    private Double amount;
    
    /**
     * 过期时间（毫秒时间戳）
     */
    private Long expireTime;
}