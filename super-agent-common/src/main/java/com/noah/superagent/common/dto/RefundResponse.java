package com.noah.superagent.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "退款响应结果")
public class RefundResponse {
    
    @Schema(description = "订单号", example = "ORDER_1234567890")
    private String orderNo;
    
    @Schema(description = "退款单号", example = "REFUND_1234567890")
    private String refundNo;
    
    @Schema(description = "微信退款单号", example = "50000000382019052709732678859")
    private String refundId;
    
    @Schema(description = "退款状态", example = "refund_processing")
    private String refundStatus;
    
    @Schema(description = "退款金额", example = "99.00")
    private BigDecimal refundAmount;
    
    @Schema(description = "订单总金额", example = "99.00") 
    private BigDecimal totalAmount;
    
    @Schema(description = "退款原因", example = "用户申请退款")
    private String refundReason;
    
    @Schema(description = "退款时间")
    private LocalDateTime refundTime;
    
    @Schema(description = "退款成功时间")
    private LocalDateTime successTime;
    
    @Schema(description = "消息", example = "退款申请提交成功")
    private String message;
}
