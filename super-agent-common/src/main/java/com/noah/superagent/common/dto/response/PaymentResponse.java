package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付响应
 */
@Data
@Schema(description = "支付响应")
public class PaymentResponse {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "支付金额")
    private BigDecimal amount;

    @Schema(description = "支付方式")
    private String paymentMethod;

    @Schema(description = "支付状态")
    private String status;

    @Schema(description = "支付二维码")
    private String qrCode;

    @Schema(description = "支付链接")
    private String paymentUrl;

    @Schema(description = "第三方支付订单号")
    private String thirdPartyOrderNo;

    @Schema(description = "过期时间")
    private LocalDateTime expiredAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    
    @Schema(description = "响应消息")
    private String message;
}
