package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户积分账户响应
 *
 * @author Noah
 * @since 1.0.0
 */
@Data
@Schema(description = "用户积分账户响应")
public class UserCreditResponse {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "总积分余额")
    private BigDecimal totalBalance;

    @Schema(description = "免费积分余额")
    private BigDecimal freeBalance;

    @Schema(description = "包月积分余额")
    private BigDecimal subscriptionBalance;

    @Schema(description = "永久积分余额")
    private BigDecimal permanentBalance;

    @Schema(description = "累计获得积分")
    private BigDecimal totalEarned;

    @Schema(description = "累计消费积分")
    private BigDecimal totalSpent;

    @Schema(description = "账户创建时间")
    private LocalDateTime createTime;

    @Schema(description = "账户最后更新时间")
    private LocalDateTime updateTime;
}
