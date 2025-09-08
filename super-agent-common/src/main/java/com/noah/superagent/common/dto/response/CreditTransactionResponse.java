package com.noah.superagent.common.dto.response;

import com.noah.superagent.common.enums.CreditTransactionTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分交易记录响应
 *
 * @author Noah
 * @since 1.0.0
 */
@Data
@Schema(description = "积分交易记录响应")
public class CreditTransactionResponse {

    @Schema(description = "交易记录ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "交易类型")
    private CreditTransactionTypeEnum transactionType;

    @Schema(description = "交易类型描述")
    private String transactionTypeDesc;

    @Schema(description = "交易金额（正数表示收入，负数表示支出）")
    private BigDecimal amount;

    @Schema(description = "交易前余额")
    private BigDecimal balanceBefore;

    @Schema(description = "交易后余额")
    private BigDecimal balanceAfter;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "关联订单ID（如果有）")
    private Long relatedOrderId;

    @Schema(description = "关联订阅ID（如果有）")
    private Long relatedSubscriptionId;

    @Schema(description = "过期时间（包月积分会有过期时间）")
    private LocalDateTime expireTime;

    @Schema(description = "交易时间")
    private LocalDateTime createTime;
}
