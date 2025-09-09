package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建订单请求
 */
@Data
@Schema(description = "创建订单请求")
public class CreateOrderRequest {

    @NotNull(message = "套餐ID不能为空")
    @Schema(description = "套餐ID", example = "1")
    private Long planId;

    @NotBlank(message = "计费周期不能为空")
    @Schema(description = "计费周期", example = "monthly", allowableValues = {"monthly", "yearly"})
    private String billingCycle;

    @NotBlank(message = "支付方式不能为空")
    @Schema(description = "支付方式", example = "wechat", allowableValues = {"wechat", "alipay"})
    private String paymentMethod;
}
