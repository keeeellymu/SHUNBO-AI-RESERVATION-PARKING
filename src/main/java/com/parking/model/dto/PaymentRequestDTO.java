package com.parking.model.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * 支付请求DTO
 */
@Data
public class PaymentRequestDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
    
    /**
     * 预约ID
     */
    @NotNull(message = "预约ID不能为空")
    private Long reservationId;
    
    /**
     * 支付金额（元）
     */
    @NotNull(message = "支付金额不能为空")
    @Min(value = 0, message = "支付金额不能为负数")
    private Double amount;
    
    /**
     * 支付类型（微信支付、支付宝等）
     */
    @NotBlank(message = "支付类型不能为空")
    private String paymentType;
    
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    /**
     * 商品描述
     */
    @NotBlank(message = "商品描述不能为空")
    private String description;
    
    /**
     * 客户端IP
     */
    @NotBlank(message = "客户端IP不能为空")
    private String clientIp;
}