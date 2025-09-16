package com.noah.superagent.response;

import com.noah.superagent.model.UserSubscriptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户订阅信息和积分数量响应
 * 整合了订阅信息和积分数量，避免多次API调用
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Schema(
    name = "UserSubscriptionWithCreditResponse",
    title = "用户订阅和积分信息响应",
    description = "整合了用户订阅信息和积分数量的完整响应，避免前端多次API调用"
)
public class UserSubscriptionWithCreditResponse {

    @Schema(
        description = "用户当前有效订阅信息，如果用户没有活跃订阅则为null", 
        nullable = true
    )
    private UserSubscriptionDTO subscription;

    @Schema(
        description = "用户当前可用积分总数（包含所有类型积分）", 
        example = "3500",
        minimum = "0"
    )
    private Long availableCredits;

    @Schema(
        description = "用户是否已创建积分账户", 
        example = "true"
    )
    private Boolean hasCreditAccount;

    @Schema(
        description = "套餐名称", 
        example = "基础版"
    )
    private String planName;

    @Schema(
        description = "限时积分（免费积分+活动积分）", 
        example = "800",
        minimum = "0"
    )
    private Long limitedCredits;

    @Schema(
        description = "当日刷新积分", 
        example = "300",
        minimum = "0"
    )
    private Long dailyRefreshCredits;
}
