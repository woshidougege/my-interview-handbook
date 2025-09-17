package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户积分详情响应（仅包含积分数据，不包含订阅信息）
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Schema(
    name = "UserCreditDetailsResponse",
    title = "用户积分详情响应",
    description = "仅包含用户积分相关信息，订阅信息通过独立接口获取"
)
public class UserCreditDetailsResponse {

    @Schema(
        description = "总积分（包含所有类型积分）", 
        example = "3500"
    )
    private String totalBalance;

    @Schema(
        description = "当日刷新积分", 
        example = "300"
    )
    private String dailyRefreshCredits;

    @Schema(
        description = "永久积分（购买获得，永不过期）", 
        example = "1900"
    )
    private String permanentCredits;

    @Schema(
        description = "限时积分（免费积分+活动积分）", 
        example = "800"
    )
    private String limitedCredits;

    @Schema(
        description = "用户是否已创建积分账户", 
        example = "true"
    )
    private Boolean hasCreditAccount;
}
