package com.noah.superagent.common.dto;

import lombok.Data;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付状态事件DTO
 */
@Data
@Builder
public class PaymentStatusEvent {
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 支付状态：
     * - waiting: 等待用户扫码
     * - scanned: 用户已扫码，支付中
     * - paying: 支付处理中
     * - paid: 支付成功
     * - failed: 支付失败
     * - expired: 支付超时
     * - cancelled: 支付取消
     * - listening: 开始监听状态
     */
    private String status;
    
    /**
     * 订单金额
     */
    private BigDecimal amount;
    
    /**
     * 支付方式：wechat-微信支付
     */
    private String paymentMethod;
    
    /**
     * 事件消息
     */
    private String message;
    
    /**
     * 事件时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTime;
    
    /**
     * 额外数据（可选）
     */
    private String extra;
}
