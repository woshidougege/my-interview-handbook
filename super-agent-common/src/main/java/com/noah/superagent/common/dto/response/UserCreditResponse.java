package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户积分账户响应
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Schema(description = "用户积分账户响应")
public class UserCreditResponse {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "总积分余额")
    private BigDecimal totalBalance;

    @Schema(description = "免费积分余额（新用户1000积分，90天有效）")
    private BigDecimal freeBalance;

    @Schema(description = "包月积分余额（已废弃）")
    private BigDecimal subscriptionBalance;

    @Schema(description = "当日积分余额（每日登录300积分，1天有效）")
    private BigDecimal dailyBalance;

    @Schema(description = "活动积分余额（分享奖励500积分等，90天有效）")
    private BigDecimal activityBalance;

    @Schema(description = "永久积分余额（付费积分，无期限）")
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
