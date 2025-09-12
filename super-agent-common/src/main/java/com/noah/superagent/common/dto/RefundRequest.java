package com.noah.superagent.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 退款请求DTO
 */
@Data
@Schema(description = "退款请求参数")
public class RefundRequest {
    
    @Schema(description = "订单号", required = true, example = "ORDER_1234567890")
    @NotBlank(message = "订单号不能为空")
    private String orderNo;
    
    @Schema(description = "退款金额", required = true, example = "99.00")
    @NotNull(message = "退款金额不能为空")
    @Positive(message = "退款金额必须大于0")
    private BigDecimal refundAmount;
    
    @Schema(description = "退款原因", example = "用户申请退款")
    private String refundReason;
    
    @Schema(description = "通知URL", example = "https://example.com/notify/refund")
    private String notifyUrl;
}
